package com.opentable.reservation.concurrency;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.CreateReservationRequest.Diner;
import com.opentable.reservation.dto.CreateReservationRequest.MonetaryAmount;
import com.opentable.reservation.exception.BusinessException;
import com.opentable.reservation.exception.RoomAlreadyBookedException;
import com.opentable.reservation.model.*;
import com.opentable.reservation.repository.ReservationRepository;
import com.opentable.reservation.repository.RestaurantRepository;
import com.opentable.reservation.repository.RoomRepository;
import com.opentable.reservation.service.ReservationService;
import com.opentable.reservation.testutil.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests to prove that the system prevents double-booking under high load.
 * These tests simulate multiple threads attempting to book the same room/time slot simultaneously.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.opentable.reservation.TestContainersConfiguration.class)
class ConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Restaurant restaurant;
    private Room room;
    private LocalDate testDate;
    private TimeSlot testTimeSlot;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();

        // Create test data
        restaurant = TestDataBuilder.restaurant()
                .name("Concurrency Test Restaurant")
                .build();
        restaurant = restaurantRepository.save(restaurant);

        room = TestDataBuilder.room()
                .restaurant(restaurant)
                .name("Concurrency Test Room")
                .minCapacity(2)
                .maxCapacity(10)
                .minimumSpend(new BigDecimal("500.00"), "USD")
                .build();
        room = roomRepository.save(room);

        testDate = LocalDate.now().plusDays(7);
        testTimeSlot = TimeSlot.DINNER;
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    /**
     * Proves that concurrent reservation attempts for the same slot
     * result in exactly ONE successful reservation and all others fail.
     * <p>
     * This test launches 20 threads simultaneously, all trying to book the same room/date/time.
     * Only one should succeed, all others should get RoomAlreadyBookedException.
     */
    @Test
    void testConcurrentReservationAttempts_OnlyOneSucceeds() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        log.info("Starting concurrency test with {} threads", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // All threads attempt to book the same slot simultaneously
                    CreateReservationRequest request = new CreateReservationRequest(
                            room.getId(),
                            testDate,
                            testTimeSlot,
                            4, // party size
                            new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                            "Thread " + threadNumber + " reservation",
                            new Diner(
                                    "Diner " + threadNumber,
                                    "diner" + threadNumber + "@example.com",
                                    "+1-555-" + String.format("%04d", threadNumber)
                            )
                    );

                    reservationService.createReservation(request);

                    // If we get here, the reservation succeeded
                    int count = successCount.incrementAndGet();
                    log.info("Thread {} SUCCEEDED (total successes: {})", threadNumber, count);

                } catch (RoomAlreadyBookedException e) {
                    // Expected for all but one thread
                    int count = failureCount.incrementAndGet();
                    log.debug("Thread {} failed - room already booked (total failures: {})", threadNumber, count);
                    exceptions.add(e);

                } catch (Exception e) {
                    log.error("Thread {} failed with unexpected exception", threadNumber, e);
                    exceptions.add(e);
                    failureCount.incrementAndGet();

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        log.info("Releasing all threads to start concurrent reservation attempts...");
        startLatch.countDown();

        // Wait for all threads to complete (with timeout)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue().withFailMessage("Test timed out waiting for threads to complete");

        // Exactly ONE reservation should succeed
        assertThat(successCount.get())
                .isEqualTo(1)
                .withFailMessage("Expected exactly 1 successful reservation, but got " + successCount.get());

        // All other attempts should fail
        assertThat(failureCount.get())
                .isEqualTo(threadCount - 1)
                .withFailMessage("Expected " + (threadCount - 1) + " failures, but got " + failureCount.get());

        // Verify only one reservation exists in the database
        List<Reservation> reservations = reservationRepository.findByRoomIdAndReservationDateBetween(
                room.getId(), testDate, testDate);

        assertThat(reservations).hasSize(1)
                .withFailMessage("Database should contain exactly 1 reservation");

        Reservation savedReservation = reservations.get(0);
        assertThat(savedReservation.getReservationDate()).isEqualTo(testDate);
        assertThat(savedReservation.getTimeSlot()).isEqualTo(testTimeSlot);
        assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        log.info("CONCURRENCY TEST PASSED: {} threads attempted reservation, exactly 1 succeeded, {} failed as expected",
                threadCount, failureCount.get());
    }

    /**
     * Tests that concurrent reservations for DIFFERENT time slots all succeed.
     * This verifies that locking mechanism is granular enough to allow concurrent
     * reservations for different slots.
     */
    @Test
    void testConcurrentReservationsForDifferentSlots_AllSucceed() throws InterruptedException {
        TimeSlot[] slots = TimeSlot.values();
        int threadCount = slots.length;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        log.info("Starting concurrency test for different time slots ({} threads)", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            final TimeSlot slot = slots[i];

            executor.submit(() -> {
                try {
                    startLatch.await();

                    CreateReservationRequest request = new CreateReservationRequest(
                            room.getId(),
                            testDate,
                            slot, // Different slot for each thread
                            4,
                            new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                            null,
                            new Diner(
                                    "Diner " + threadNumber,
                                    "diner" + threadNumber + "@example.com",
                                    "+1-555-" + String.format("%04d", threadNumber)
                            )
                    );

                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                    log.info("Thread {} succeeded for slot {}", threadNumber, slot);

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Thread {} failed for slot {}", threadNumber, slot, e);

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // ALL reservations should succeed since they're for different slots
        assertThat(successCount.get()).isEqualTo(threadCount)
                .withFailMessage("All " + threadCount + " reservations for different slots should succeed");
        assertThat(failureCount.get()).isZero()
                .withFailMessage("No reservations should fail when booking different slots");

        // Verify all reservations exist in database
        List<Reservation> reservations = reservationRepository.findByRoomIdAndReservationDateBetween(
                room.getId(), testDate, testDate);
        assertThat(reservations).hasSize(threadCount);

        log.info("CONCURRENT DIFFERENT SLOTS TEST PASSED: All {} reservations succeeded", threadCount);
    }

    /**
     * Tests that the system handles high load with many concurrent requests
     * for different dates and times.
     */
    @Test
    void testHighLoadWithMixedRequests() throws InterruptedException {
        int threadCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        log.info("Starting high load test with {} threads", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Distribute requests across different dates and slots
                    LocalDate date = testDate.plusDays(threadNumber % 7);
                    TimeSlot slot = TimeSlot.values()[threadNumber % TimeSlot.values().length];

                    CreateReservationRequest request = new CreateReservationRequest(
                            room.getId(),
                            date,
                            slot,
                            4,
                            new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                            null,
                            new Diner(
                                    "Diner " + threadNumber,
                                    "diner" + threadNumber + "@example.com",
                                    "+1-555-" + String.format("%04d", threadNumber)
                            )
                    );

                    reservationService.createReservation(request);
                    successCount.incrementAndGet();

                } catch (RoomAlreadyBookedException e) {
                    // Expected for duplicate date/slot combinations
                    failureCount.incrementAndGet();
                } catch (BusinessException e) {
                    // Expected for validation failures
                    failureCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Unexpected error in thread {}", threadNumber, e);
                    failureCount.incrementAndGet();

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        log.info("HIGH LOAD TEST COMPLETED: {} successes, {} failures out of {} total requests",
                successCount.get(), failureCount.get(), threadCount);

        // At least some reservations should succeed
        assertThat(successCount.get()).isGreaterThan(0);

        // Total should equal thread count
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
    }

    /**
     * Tests concurrent UPDATE race condition handling with optimistic locking.
     * Simulates multiple threads trying to cancel the same reservation simultaneously.
     * Only one should succeed, others should get OptimisticLockException.
     */
    @Test
    void testConcurrentCancellations_OptimisticLocking() throws InterruptedException {
        // Create a reservation
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                testDate,
                testTimeSlot,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Test User", "test@example.com", "+1-555-0000")
        );
        var created = reservationService.createReservation(request);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        log.info("Starting concurrent cancellation test with {} threads", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // All threads attempt to cancel the same reservation
                    reservationService.cancelReservation(
                            created.id(),
                            "canceller" + threadNumber + "@example.com",
                            "Concurrent cancellation test"
                    );

                    successCount.incrementAndGet();
                    log.info("Thread {} succeeded in cancelling", threadNumber);

                } catch (Exception e) {
                    // Expected: OptimisticLockException or BusinessException (already cancelled)
                    failureCount.incrementAndGet();
                    log.debug("Thread {} failed: {}", threadNumber, e.getClass().getSimpleName());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Only ONE cancellation should succeed (optimistic locking prevents lost updates)
        assertThat(successCount.get()).isEqualTo(1)
                .withFailMessage("Expected exactly 1 successful cancellation due to optimistic locking");

        assertThat(failureCount.get()).isEqualTo(threadCount - 1);

        // Verify reservation is cancelled in database
        var cancelled = reservationService.getReservationDetails(created.id());
        assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);

        log.info("OPTIMISTIC LOCKING TEST PASSED: 1 cancellation succeeded, {} failed as expected",
                failureCount.get());
    }
}
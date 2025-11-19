package com.opentable.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PrivateDiningReservationSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrivateDiningReservationSystemApplication.class, args);
	}

}

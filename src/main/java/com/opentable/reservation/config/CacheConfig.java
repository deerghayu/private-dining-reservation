package com.opentable.reservation.config;

import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration using Spring's default simple cache (ConcurrentHashMap-based).
 *
 * @EnableCaching is configured on the main application class.
 * For production, consider configuring Redis or Caffeine via spring.cache properties.
 */
@Configuration
public class CacheConfig {
    // Uses Spring Boot's auto-configured CacheManager
    // Cache names: "restaurants", "rooms", "availability"
}
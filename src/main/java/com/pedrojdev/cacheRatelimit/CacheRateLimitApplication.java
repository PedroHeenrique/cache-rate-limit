package com.pedrojdev.cacheRatelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CacheRateLimitApplication {

	public static void main(String[] args) {
		SpringApplication.run(CacheRateLimitApplication.class, args);
	}

}

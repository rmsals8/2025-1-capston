package com.example.TripSpring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TripSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(TripSpringApplication.class, args);
	}

}

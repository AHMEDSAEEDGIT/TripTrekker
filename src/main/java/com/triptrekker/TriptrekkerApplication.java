package com.triptrekker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic
@EnableCaching
public class TriptrekkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TriptrekkerApplication.class, args);
	}

}

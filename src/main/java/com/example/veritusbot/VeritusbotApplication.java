package com.example.veritusbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VeritusbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(VeritusbotApplication.class, args);
	}

}

package com.samanatteu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Active les @Scheduled (sinon ignorés en silence).
@EnableScheduling
public class SamanatteuApplication {

	public static void main(String[] args) {
		SpringApplication.run(SamanatteuApplication.class, args);
	}

}

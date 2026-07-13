package dev.pubchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PubchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(PubchatApplication.class, args);
	}

}

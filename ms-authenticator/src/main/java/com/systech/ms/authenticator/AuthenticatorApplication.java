package com.systech.ms.authenticator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.systech.ms.*")
public class AuthenticatorApplication {
	public static void main(String[] args) {
		SpringApplication.run(AuthenticatorApplication.class, args);
	}
}

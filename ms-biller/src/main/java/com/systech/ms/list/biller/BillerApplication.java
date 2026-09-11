package com.systech.ms.list.biller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.systech.ms.*"})
public class BillerApplication {
	public static void main(String[] args) {
		SpringApplication.run(BillerApplication.class, args);
	}

}

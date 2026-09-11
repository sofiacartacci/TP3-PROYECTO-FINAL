package com.systech.ms.list.retriever;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.systech.ms.*"})
public class RetrieverApplication {
	public static void main(String[] args) {
		SpringApplication.run(RetrieverApplication.class, args);
	}

}

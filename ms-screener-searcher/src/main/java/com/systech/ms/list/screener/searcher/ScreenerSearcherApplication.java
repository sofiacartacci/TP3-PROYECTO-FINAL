package com.systech.ms.list.screener.searcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.systech.ms.*"})
public class ScreenerSearcherApplication {
	
	
	public static void main(String[] args) {
		SpringApplication.run(ScreenerSearcherApplication.class, args);
	}

}

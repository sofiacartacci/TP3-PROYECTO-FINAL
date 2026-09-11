package com.systech.ms.list.searcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.systech.ms.*"})
public class SearcherApplication {
	
	
	public static void main(String[] args) {
		SpringApplication.run(SearcherApplication.class, args);
	}

}

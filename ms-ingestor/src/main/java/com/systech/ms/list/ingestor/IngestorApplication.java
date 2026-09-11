package com.systech.ms.list.ingestor;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan(basePackages={"com.systech.ms.core","com.systech.ms.list.ingestor"})
@EnableBatchProcessing
public class IngestorApplication {
	public static void main(String[] args) {
		SpringApplication.run(IngestorApplication.class, args);
	}	
}

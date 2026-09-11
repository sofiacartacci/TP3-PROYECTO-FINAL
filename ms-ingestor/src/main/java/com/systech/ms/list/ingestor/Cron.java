package com.systech.ms.list.ingestor;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.systech.ms.list.ingestor.service.JobService;

import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@Component
@Slf4j
public class Cron {
	@Autowired
	JobService jobService;
	
	
	
	@Scheduled(cron="${batch.cron.nocompare}")
	public void runNoCompare() throws Exception{
		log.info("Launching job scheduled by cron expression 'batch.cron.nocompare'");
		jobService.start("false");
	}

	@Scheduled (cron="${batch.cron.comparing}")
	public void runComparing() throws Exception{
		log.info("Launching job scheduled by cron expression 'batch.cron.comparing'");
		jobService.start("true");
	}
}

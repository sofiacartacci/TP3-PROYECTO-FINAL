package com.systech.ms.list.screener;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.systech.ms.list.screener.model.ScreenerProcess;
import com.systech.ms.list.screener.repo.ScreenerProcessRepo;
import com.systech.ms.list.screener.service.JobService;

import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@Component
@Slf4j
public class Cron {
	@Autowired
	JobService jobService;

	@Autowired
	ScreenerProcessRepo repo;

	@Scheduled(cron = "*/10 * * * * *") // cada 10 segundos revisa si hay algo m�s para procesar
	public void run() throws Exception {
		if (!jobService.isActive()) {
			log.info("Looking for more files to screen");
			List<ScreenerProcess> sps = repo.findByStartedIsNullOrderBySubmittedAsc();
			if (!sps.isEmpty()) {
				log.info("Scanning " + sps.get(0).getId());
				jobService.start(sps.get(0));
			}
		}
	}

}

package com.systech.ms.list.ingestor.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.systech.ms.list.ingestor.Constants;
import com.systech.ms.list.ingestor.model.JobStatus;
import com.systech.ms.list.ingestor.model.StepStatus;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class JobService {
	@Autowired
	Job job;

	@Autowired
	JobOperator jobOperator;

	JobExecution jobExecution;

	@Autowired
	@Qualifier("myJobLauncher")
	JobLauncher jobLauncher;
	
	
	public void start(String compare) throws Exception {
		if(jobExecution!=null&&jobExecution.isRunning()){
			throw new Exception("Job already running");
		}
		if(jobExecution!=null&&jobExecution.isStopping()){
			throw new Exception("Job still stopping");
		}
		
		log.info("Starting Job");
		if (compare == null || compare.isBlank()) {
			compare = "false";
		}
		JobParameters jp = new JobParametersBuilder()
				.addString(Constants.COMPARE_CHANGES, compare)
				.addDate("date", new Date())
				.toJobParameters();
		
		jobExecution = jobLauncher.run(job, jp);
	}

	public JobExecution getJobExecution() {
		return jobExecution;
	}

	public void stop() throws Exception {
		if (jobExecution == null) {
			throw new Exception("No job running");
		}
		log.info("Stopping Job");
		jobOperator.stop(jobExecution.getId());
	}

	public JobStatus getStatus() throws Exception {
		log.info("Getting job status");
		JobStatus s = new JobStatus();
		if (jobExecution == null) {
			return s;
		}
		s.setParameters(jobExecution.getJobParameters());
		s.setJobName(jobExecution.getJobInstance().getJobName());
		s.setStartTime(jobExecution.getStartTime());
		s.setLastUpdated(jobExecution.getLastUpdated());
		s.setEndedTime(jobExecution.getEndTime());
		s.setStatus(jobExecution.getStatus());
		List<StepStatus> stepsList = new ArrayList();

		Collection<StepExecution> steps = jobExecution.getStepExecutions();
		steps.stream().forEach(st -> {
			StepStatus ss = new StepStatus();
			ss.setStepName(st.getStepName());
			ss.setStartTime(st.getStartTime());
			ss.setLastUpdated(st.getLastUpdated());
			ss.setEndedTime(st.getEndTime());
			ss.setStatus(st.getStatus());
			ss.setReads(st.getReadCount());
			ss.setWrites(st.getWriteCount());
			ss.setCommits(st.getCommitCount());
			stepsList.add(ss);
		});

		s.setSteps(stepsList);
		return s;
	}
}

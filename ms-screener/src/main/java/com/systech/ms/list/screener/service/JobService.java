package com.systech.ms.list.screener.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.systech.ms.list.screener.Constants;
import com.systech.ms.list.screener.model.JobStatus;
import com.systech.ms.list.screener.model.ScreenerProcess;
import com.systech.ms.list.screener.model.StepStatus;

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

	public boolean isActive() {
		if (jobExecution != null && (jobExecution.isRunning() || jobExecution.isStopping())) {
			return true;
		} else {
			return false;
		}
	}

	public void start(ScreenerProcess sp) throws Exception {
		if (isActive()) {
			log.info("Trying to start job but job is still active. Will retry later");
			return;
		}

		log.info("Starting Job");

		JobParameters jp = new JobParametersBuilder()
				.addString(Constants.ID, sp.getId())
				.addString(Constants.USER, sp.getUser())
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
		List<StepStatus> stepsList = new ArrayList<>();

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

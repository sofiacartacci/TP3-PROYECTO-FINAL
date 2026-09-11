package com.systech.ms.list.screener.model;

import java.util.Date;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;

import lombok.Data;

@Data
public class JobStatus {
	JobParameters parameters;
	String jobName;
	Date startTime;
	Date lastUpdated;
	Date endedTime;
	BatchStatus status;
	List<StepStatus> steps;
	
}

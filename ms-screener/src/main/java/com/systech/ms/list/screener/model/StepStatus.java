package com.systech.ms.list.screener.model;

import java.util.Date;

import org.springframework.batch.core.BatchStatus;

import lombok.Data;

@Data
public class StepStatus {
	String stepName;
	Date startTime;
	Date lastUpdated;
	Date endedTime;
	BatchStatus status;
	Integer reads;
	Integer writes;
	Integer commits;
	
}

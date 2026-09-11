package com.systech.ms.list.screener;

import java.util.Date;
import java.util.Optional;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.systech.ms.core.model.list.Bill;
import com.systech.ms.core.service.ApiKeyHeaders;
import com.systech.ms.list.screener.model.ScreenerProcess;
import com.systech.ms.list.screener.repo.ScreenerProcessRepo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScreenerJobListener implements JobExecutionListener{
	@Autowired
	ScreenerProcessRepo repo;
	
	@Autowired 
	RestTemplate restTemplate;

	@Autowired
	ApiKeyHeaders apiKeyHeaders;

	@Value("${biller.url}/bill")
	String billerUrl;
	
	@Override
	public void beforeJob(JobExecution je) {
		String id = je.getJobParameters().getString(Constants.ID);
		Optional <ScreenerProcess> op=repo.findById(id); 
		ScreenerProcess sp=op.get();
		sp.setStarted(new Date());
		sp.setStatus(je.getStatus());
		repo.save(sp);
	}

	@Override
	public void afterJob(JobExecution je) {
		String id = je.getJobParameters().getString(Constants.ID);
		Optional <ScreenerProcess> op=repo.findById(id); 
		ScreenerProcess sp=op.get();
		
		
		
		sp.setCost(je.getExecutionContext().getLong(Constants.COST));
		sp.setRecords(je.getExecutionContext().getLong(Constants.RECORDS));
		sp.setCoincidences(je.getExecutionContext().getLong(Constants.COINCIDENCES));
		sp.setEnded(new Date());
		if(je.getStatus().equals(BatchStatus.FAILED)) {
			sp.getErrors().add(je.getExitStatus().getExitDescription());
		}
		sp.setStatus(je.getStatus());
		
		bill(id,sp.getUser(),sp.getRecords(),sp.getCost(),sp.getCoincidences());
		repo.save(sp);
		
		
	}
	
	private void bill(String id,String user, long records, long cost,long coincidences) {
		Bill bill=new Bill();
		bill.setIdSearch(id);
		bill.setSearchDate(new Date());
		bill.setRecords(records);
		bill.setUser(user);
		bill.setCost(cost);
		bill.setCoincidencesCount(coincidences);
		
		
		log.info("{}",bill);
		
		restTemplate.exchange(billerUrl, HttpMethod.POST, new HttpEntity<Bill>(bill,apiKeyHeaders.getApiKey()),Void.class);
	}

}

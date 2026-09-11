package com.systech.ms.list.ingestor.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.systech.ms.list.ingestor.model.JobStatus;
import com.systech.ms.list.ingestor.model.Provider;
import com.systech.ms.list.ingestor.repo.ProviderRepo;
import com.systech.ms.list.ingestor.service.JobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class IngestorController {
	
	@Autowired 
	JobService jobService;
	
	@Autowired
	ProviderRepo providerRepo;
	
	
	
	
	@PreAuthorize("hasAuthority('admin')")	
	@Operation(summary = "Iniciar o reiniciar Job", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/startJob")
	public ResponseEntity<String> startJob(@RequestParam String compareChanges)throws Exception {		
		jobService.start(compareChanges);
		return ResponseEntity.ok("Job start requested");
	}
	
	@PreAuthorize("hasAuthority('admin')")	
	@Operation(summary = "Detener Job", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/stopJob")
	public ResponseEntity<String> stopJob()throws Exception {
		
		
		jobService.stop();
		return ResponseEntity.ok("Job stop requested");
	}
	
	@Operation(summary = "Consultar Job", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getJobStatus")
	public ResponseEntity<JobStatus> getJobStatus()throws Exception {
		return ResponseEntity.ok(jobService.getStatus());
	}
	
	@Operation(summary = "Consultar estado de listas cargadas en último proceso", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getProvidersStatus")
	public ResponseEntity<List<Provider>> getProvidersStatus()throws Exception {
		return ResponseEntity.ok(providerRepo.findAll());
	}

}

package com.systech.ms.list.screener.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.systech.ms.list.model.ScreenerParams;
import com.systech.ms.list.screener.model.JobStatus;
import com.systech.ms.list.screener.model.ScreenerProcess;
import com.systech.ms.list.screener.repo.ScreenerProcessRepo;
import com.systech.ms.list.screener.service.JobService;
import com.systech.ms.list.screener.service.ScreenerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/screener")
@RestController
public class ScreenerController {

	@Autowired
	ScreenerService screenerService;
	@Autowired
	JobService jobService;

	@Autowired
	ScreenerProcessRepo repo;

	@Operation(summary = "Enviar archivo a procesar", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping(value = "/screen", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<ScreenerProcess> receiveFile(@RequestPart("file") MultipartFile file,
			@RequestPart("params") ScreenerParams params,
			Principal principal) throws Exception {

		return ResponseEntity.ok(screenerService.receiveFile(file, params, principal));
	}

	@Operation(summary = "Obtener estado de procesamiento actual", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getJobStatus")
	public ResponseEntity<JobStatus> getJobStatus() throws Exception {
		return ResponseEntity.ok(jobService.getStatus());
	}

	@PreAuthorize("hasAuthority('admin')")
	@Operation(summary = "Detener Job", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/stopJob")
	public ResponseEntity<String> stopJob() throws Exception {
		jobService.stop();
		return ResponseEntity.ok("Job stop requested");
	}

	@Operation(summary = "Consultar estado de archivo", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getStatus/{id}")
	public ResponseEntity<ScreenerProcess> getStatus(@PathVariable("id") String fileId) throws Exception {
		Optional<ScreenerProcess> sp = repo.findById(fileId);
		if (sp.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(sp.get());
	}

	@Operation(summary = "Obtener resultado", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getResult/{id}")
	public ResponseEntity<FileSystemResource> getResult(@PathVariable("id") String fileId,
			Principal principal) throws Exception {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Content-Encoding", "gzip");

		FileSystemResource file = new FileSystemResource(screenerService.getResult(fileId, principal));
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv"))
				.body(file);
	}

	@PreAuthorize("hasAuthority('admin')")
	@Operation(summary = "Obtener procesos pendientes", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getPending")
	public ResponseEntity<List<ScreenerProcess>> getPending() throws Exception {
		return ResponseEntity.ok(repo.findByStartedIsNullOrderBySubmittedAsc());
	}

	@PreAuthorize("hasAuthority('admin')")
	@Operation(summary = "Obtener procesos en ejecuci�n", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/getRunning")
	public ResponseEntity<List<ScreenerProcess>> getRunning() throws Exception {
		return ResponseEntity.ok(repo.findByStartedIsNotNullAndEndedIsNull());
	}

	@PreAuthorize("hasAuthority('admin')")
	@Operation(summary = "Cancelar y eliminar archivo si a�n no se proces�", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/cancel/{id}")
	public ResponseEntity<ScreenerProcess> cancel(@PathVariable("id") String id) throws Exception {
		Optional<ScreenerProcess> sp = repo.findById(id);
		if (sp.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		if (sp.get().getStatus() != null) {
			throw new Exception("Processing of file has already started. Cancel is not allowed");
		}
		repo.deleteById(id);
		return ResponseEntity.ok(sp.get());
	}

}

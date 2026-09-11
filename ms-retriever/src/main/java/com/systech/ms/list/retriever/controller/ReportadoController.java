package com.systech.ms.list.retriever.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.systech.ms.core.model.list.Reportado;
import com.systech.ms.core.model.list.ReportadoHist;
import com.systech.ms.list.retriever.repo.ReportadoHistRepo;
import com.systech.ms.list.retriever.repo.ReportadoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/retriever")
public class ReportadoController {

	Logger logger = LoggerFactory.getLogger(ReportadoController.class);

	@Autowired
	private ReportadoRepo repo;

	@Autowired
	private ReportadoHistRepo histRepo;

	@Operation(summary = "Obtener datos de un reportado por su ui", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{ui}")
	public ResponseEntity<Optional<Reportado>> getData(@PathVariable String ui) {
		Optional<Reportado> r = repo.findById(ui);
		if (r.isEmpty()) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(r);
		}
	}

	@Operation(summary = "Obtener historial de un reportado por su ui", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/hist/{ui}")
	public ResponseEntity<Optional<List<ReportadoHist>>> getHistorial(@PathVariable String ui) {
		Optional<List<ReportadoHist>> r = histRepo.findAllByUi(ui);
		if (r.isEmpty()) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(r);
		}
	}
	/*
	 * solo para pruebas, no habilitar nunca.
	 * 
	 * @GetMapping("/list")
	 * public ResponseEntity<List<Reportado>> list() {
	 * List<Reportado> r = repo.findAll();
	 * if (r.isEmpty()) {
	 * return ResponseEntity.notFound().build();
	 * } else {
	 * return ResponseEntity.ok(r);
	 * }
	 * }
	 */
}
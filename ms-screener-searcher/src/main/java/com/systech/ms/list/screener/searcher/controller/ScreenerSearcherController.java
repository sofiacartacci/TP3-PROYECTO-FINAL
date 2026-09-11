package com.systech.ms.list.screener.searcher.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.systech.ms.list.model.ScreenerSearchDTO;
import com.systech.ms.list.model.screener.search.Result;
import com.systech.ms.list.screener.searcher.service.ScreenerSearcherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
public class ScreenerSearcherController {
	@Autowired
	private ScreenerSearcherService service;
	
	@Operation(summary = "Buscar (desde screener) lote de personas en listas", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/search")
	public ResponseEntity<Result> search(@RequestBody ScreenerSearchDTO search) throws Exception{
		Result r;
		
		r=service.check(search);
		
		return ResponseEntity.ok(r);
	}	
}
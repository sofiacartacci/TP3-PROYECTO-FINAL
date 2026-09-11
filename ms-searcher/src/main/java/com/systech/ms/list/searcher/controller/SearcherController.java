package com.systech.ms.list.searcher.controller;

import java.security.Principal;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.searcher.model.Result;
import com.systech.ms.list.searcher.model.SearchDTO;
import com.systech.ms.list.searcher.service.SearcherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
public class SearcherController {

	Logger logger = LoggerFactory.getLogger(SearcherController.class);

	@Autowired
    private ModelMapper modelMapper;
	
	@Autowired
	private SearcherService service;
	
	@Operation(summary = "Buscar en listas", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/search")
	public ResponseEntity<Result> search(@RequestBody SearchDTO searchDTO,
											Principal principal) throws Exception{
		Result r;
		String user=principal.getName();
		logger.info("Search: " + searchDTO.getText() + ", User: " + user);
		
		SearchQuery search=modelMapper.map(searchDTO, SearchQuery.class);
		search.setUser(user);
		r=service.check(search);
		
		return ResponseEntity.ok(r);
	}	
}
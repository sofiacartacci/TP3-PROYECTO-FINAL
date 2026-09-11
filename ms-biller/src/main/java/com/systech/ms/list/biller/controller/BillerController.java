package com.systech.ms.list.biller.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.systech.ms.core.model.list.Bill;
import com.systech.ms.list.biller.model.UserTotalQuery;
import com.systech.ms.list.biller.model.UserTotalResult;
import com.systech.ms.list.biller.repo.BillerRepo;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BillerController {

	Logger logger = LoggerFactory.getLogger(BillerController.class);

	@Autowired
	private BillerRepo repo;

	@Operation(summary = "Registrar el consumo de una consulta")
	@PostMapping("/bill")
	public void bill(@RequestBody Bill bill) {
		repo.save(bill);
	}

	@Operation(summary = "Obtener consumo entre fechas por usuario")
	@PostMapping("/getUserTotal")
	public UserTotalResult bill(@RequestBody UserTotalQuery utq) {
		UserTotalResult userSearchs = repo.getUserSearchs(utq.getUser(), utq.getFrom(), utq.getTo());
		if (userSearchs == null) {
			return new UserTotalResult();
		} else {
			return userSearchs;
		}
	}

}
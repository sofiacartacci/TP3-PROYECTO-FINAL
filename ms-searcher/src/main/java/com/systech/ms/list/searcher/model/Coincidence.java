package com.systech.ms.list.searcher.model;

import com.systech.ms.core.model.list.Reportado;

import lombok.Data;
@Data
public class Coincidence {
	int ranking;
	double coincidenceLevel;
	String ui;
	String tipo;
	Reportado detail;
}

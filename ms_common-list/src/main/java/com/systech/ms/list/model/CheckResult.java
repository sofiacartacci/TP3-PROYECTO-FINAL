package com.systech.ms.list.model;

import java.util.List;

import lombok.Data;

@Data
public class CheckResult {
	List<Match> matches;
	String parsedQuery;
}

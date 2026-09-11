package com.systech.ms.list.searcher.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.utils.MatchingUtilsParams;

import lombok.Data;

@Data
public class Result {
	Date searchDate=new Date();
	String returnCode = "";
	String returnMessage = "";
	long coincidencesCount = 0;
	List<Coincidence> coincidences=new ArrayList<Coincidence>();	
	SearchQuery search;
	long elapsedTime;
	long cost;
	MatchingUtilsParams params;
	String parsedQuery;
}

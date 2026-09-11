package com.systech.ms.list.model.screener.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.utils.MatchingUtilsParams;

import lombok.Data;

@Data
public class Result {
	List<Id> ids=new ArrayList<Id>();	
	long cost;	
}

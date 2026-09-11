package com.systech.ms.list.model.screener.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.utils.MatchingUtilsParams;

import lombok.Data;

@Data
public class Id {
	String id;
	long coincidencesCount = 0;
	List<Search> searchs=new ArrayList<Search>();	
}

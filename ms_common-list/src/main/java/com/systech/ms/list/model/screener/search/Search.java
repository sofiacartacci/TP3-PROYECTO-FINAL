package com.systech.ms.list.model.screener.search;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Search {
	String text;
	List<Coinc> coincs=new ArrayList<Coinc>();	
}

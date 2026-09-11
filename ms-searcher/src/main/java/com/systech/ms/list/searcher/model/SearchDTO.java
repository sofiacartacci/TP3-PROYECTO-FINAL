package com.systech.ms.list.searcher.model;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SearchDTO {
	@NotBlank
	String text;
	@NotBlank
	Integer minLevel;
	
	Date updatedAfter;
	
	List<String> includedCategories;
	List<String> includedSubCategories;
	List<String> includedKeywords;
	
	List<String> excludedCategories;
	List<String> excludedSubCategories;
	List<String> excludedKeywords;
	
	Date includeIfDeprecatedAfter;
	boolean searchAliases=true;
	boolean showDetails;	
}


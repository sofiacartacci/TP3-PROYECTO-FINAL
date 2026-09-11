package com.systech.ms.list.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ScreenerParams {
	@NotBlank
	Integer minLevel;
	
	List<String> includedCategories;
	List<String> includedSubCategories;
	List<String> includedKeywords;
	
	List<String> excludedCategories;
	List<String> excludedSubCategories;
	List<String> excludedKeywords;
	
	Date includeIfDeprecatedAfter;
	boolean searchAliases=true;
}


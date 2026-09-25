package com.systech.ms.list.model;

import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import lombok.Data;

@Data
public class SearchQuery {
	String idSearch;
	String text;
	Date updatedAfter;
	Integer minLevel;
	List<String> includedCategories;
	List<String> includedSubCategories;
	List<String> includedKeywords;
	List<String> includedCountries;

	List<String> excludedCategories;
	List<String> excludedSubCategories;
	List<String> excludedKeywords;
	List<String> excludedCountries;

	Date includeIfDeprecatedAfter;
	boolean searchAliases = true;
	boolean showDetails;

	// B1: listas a las que se restringe la busqueda. Vacio/null = todas (comportamiento previo).
	List<String> listIds;

	String user;
}

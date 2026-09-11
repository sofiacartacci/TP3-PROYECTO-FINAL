package com.systech.ms.list.model.searcher;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Filters {
	List<String> categories;
	List<String> keywords;
	List<String> countries;

	public Filters() {
		this.categories = new ArrayList<>();
		this.keywords = new ArrayList<>();
		this.countries = new ArrayList<>();

	}
}

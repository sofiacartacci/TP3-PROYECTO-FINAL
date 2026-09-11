package com.systech.ms.core.model.list;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
public abstract class ReportadoAbstract {
	public String ui;
	public String nam; //name
	public List<String> als; //aliases
	
	public List<String> ass; //alternative spellings
	public String category;
	public String subcategory;
	public String e_i;
	public String title;
	public String social_position;
	
	public String age;
	public String dob;
	public String pob;
	public String deceased;
	public List<String> ids;
	public List<String> locations;
	public List<String> countries;
	public List<String> companies;
	public List<String> linked_to;
	public String further_information;
	public List<String> keywords;
	public List<String> external_sources;
	public String age_date;
	public String editor;
	public Date entered;
	public Date updated;
	public Date deprecated;
	
	public Date sysInsert;
	public Date sysUpdate;
	public Date sysUpdateRelevante;

	}

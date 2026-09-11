package com.systech.ms.core.model.list;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "biller")
public class Bill {
	@Id
	String idSearch;
	Date searchDate;
	String user;
	String parsedQuery;
	long coincidencesCount = 0;
	long records=0;
	List<String>uis;
	long elapsedTime;
	long cost;	
}

package com.systech.ms.list.screener.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.systech.ms.list.model.ScreenerParams;

import lombok.Data;

@Data
@Document(collection = "screener")
public class ScreenerProcess {
	@Id
	String id;
	Long size;
	Date submitted;
	String user;
	ScreenerParams params;
	Date started;
	Date ended;
	BatchStatus status;
	long records;
	long cost;
	long coincidences;
	List<String> errors=new ArrayList<String>(); 
}

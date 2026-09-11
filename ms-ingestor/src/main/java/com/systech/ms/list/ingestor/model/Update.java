package com.systech.ms.list.ingestor.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Document(collection = "updates")
public class Update {
	@Id
	public String ui;
	public Date updated;

}

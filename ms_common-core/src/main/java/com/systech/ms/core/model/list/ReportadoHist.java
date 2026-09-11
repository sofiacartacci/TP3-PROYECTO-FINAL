package com.systech.ms.core.model.list;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "hist")
public class ReportadoHist extends ReportadoAbstract {
	@Id
	public HistId id;

}

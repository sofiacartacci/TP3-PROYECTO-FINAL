package com.systech.ms.list.ingestor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "testmongo")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class Modelo {
	@Id
	String id;
	String datos;
}

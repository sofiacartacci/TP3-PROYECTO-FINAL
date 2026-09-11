package com.systech.ms.list.screener.task.chunks;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.systech.ms.list.screener.model.Person;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScreenProcessor implements ItemProcessor<CSVRecord, Person> {
	int recordNumber = 0;
	boolean hasErrors=false;
	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	int errorCount = 0;

	@BeforeStep
	public void beforeStep(StepExecution se) {
		recordNumber = 1;// 1 es el header
	}

	
	@Override
	public Person process(CSVRecord m) throws Exception {
		try {
		Person p = new Person();
		recordNumber++;

		p.setUid(get(m, "uid"));
		p.setName(get(m,"name"));
		p.setId1(get(m, "id1"));
		p.setId2(get(m, "id2"));
		p.setId3(get(m, "id3"));
		p.setId4(get(m, "id4"));		
		p.setScreened(getDate(m, "screened"));

		return p;
		}catch(Exception e) {
			hasErrors=true;
			throw e;
		}
	}

	private String get(CSVRecord csv, String campo) throws Exception {
		try {
			return csv.get(campo);
		} catch (Exception e) {
			log.error("Error parsing record number {}. Record: {}", recordNumber, csv);
			throw e;
		}
	}

	
	private Date getDate(CSVRecord m, String campo) throws Exception {
		String valor = get(m, campo);
		if (valor == null || valor.trim().equals("")) {
			return null;
		} else {
			try {
				return sdf.parse(valor);
			} catch (Exception e) {
				log.error(e.getMessage() + " in field {}. Format: {}, Record: {}", campo, sdf.toPattern(), m);
				throw e;
			}
		}
	}

	@AfterStep
	public void close() throws Exception {
	}
}

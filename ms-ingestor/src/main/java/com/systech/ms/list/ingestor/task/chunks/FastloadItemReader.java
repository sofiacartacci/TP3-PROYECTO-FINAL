package com.systech.ms.list.ingestor.task.chunks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FastloadItemReader<T> implements ItemReader<CSVRecord> {
	boolean inited = false;

	@Value("${mapping.id}")
	String id;
	@Value("${mapping.name}")
	String name;

	Iterable<CSVRecord> records = null;
	Iterator<CSVRecord> it;
	@Value("${mapping.working-dir}")
	String workingDir;
	String fileName;
	@Value("${mapping.delimiter:\t}")
	String delimiter;
	@Value("${mapping.quote:null}")
	Character quote;
	String src;

	void init() throws Exception {
		fileName = id + ".csv";
		log.info("Fastloading provider: " + name);
		src = workingDir + File.separator + fileName;

		CSVFormat csvFileFormat = CSVFormat.Builder.create().setSkipHeaderRecord(false).setHeader()
				.setIgnoreHeaderCase(true).setDelimiter(delimiter).setQuote(quote)
				// .withIgnoreEmptyLines()
				.build();
		records = CSVParser.parse(new File(src), StandardCharsets.UTF_8, csvFileFormat);
		it = records.iterator();
		inited = true;
	}

	@AfterStep
	public void close() throws Exception {
		inited = false;
	}

	@Override
	public CSVRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if (!inited)
			init();
		if (it.hasNext()) {
			return it.next();
		} else {
			return null;
		}
	}

}

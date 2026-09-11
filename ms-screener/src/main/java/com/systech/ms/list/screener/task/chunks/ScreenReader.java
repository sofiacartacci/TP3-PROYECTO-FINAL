package com.systech.ms.list.screener.task.chunks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.systech.ms.list.screener.Constants;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScreenReader<T> implements ItemReader<CSVRecord> {
	boolean hasErrors = false;

	String id, user;

	@Value("${screener.work.dir}/work")
	private String workingDir;

	Iterable<CSVRecord> records = null;
	Iterator<CSVRecord> it;

	static final String DELIMITER = "\t";
	static final Character QUOTE = '"';
	String src;

	@BeforeStep
	public void beforeStep(StepExecution se) throws Exception {
		id = se.getJobExecution().getJobParameters().getString(Constants.ID);
		user = se.getJobExecution().getJobParameters().getString(Constants.USER);

		src = workingDir + File.separator + user + File.separator + id + ".csv";
		log.info("Reading from " + src);

		CSVFormat csvFileFormat = CSVFormat.Builder.create().setSkipHeaderRecord(false).setHeader()
				.setIgnoreHeaderCase(true).setDelimiter(DELIMITER).setQuote(QUOTE)
				// .withIgnoreEmptyLines()
				.build();
		records = CSVParser.parse(new File(src), StandardCharsets.UTF_8, csvFileFormat);
		it = records.iterator();

	}

	@AfterStep
	public void close() throws Exception {

	}

	@Override
	public CSVRecord read() throws Exception {
		try {
			if (it.hasNext()) {
				return it.next();
			} else {
				return null;
			}
		} catch (Exception e) {
			hasErrors = true;
			throw e;
		}
	}

}

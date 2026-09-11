package com.systech.ms.list.screener.task.chunks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systech.ms.core.service.ApiKeyHeaders;
import com.systech.ms.list.model.ScreenerSearchDTO;
import com.systech.ms.list.model.ScreenerSearchData;
import com.systech.ms.list.model.screener.search.Result;
import com.systech.ms.list.screener.Constants;
import com.systech.ms.list.screener.model.Person;
import com.systech.ms.list.screener.model.ScreenerProcess;
import com.systech.ms.list.screener.repo.ScreenerProcessRepo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScreenWriter implements ItemWriter<Person> {

	int processed;
	boolean hasErrors = false;
	final static String DELIMITER = "\t";
	long cost = 0;
	long coincidences = 0;

	private Date ahora;

	String id, user;

	@Value("${screener.work.dir}/work")
	private String workingDir;

	@Value("${screener.work.dir}/outfiles")
	private String outDir;

	@Value("${screener.seracher.url}/search")
	private String searcherUrl;

	@Value("${batch.size}")
	Integer batchSize;
	@Value("${screener.searcher.forksize}")
	Integer searcherForkSize;

	String out;
	long di;

	BufferedOutputStream bufStream;
	GZIPOutputStream zipStream;
	FileOutputStream fos;

	@Autowired
	ApiKeyHeaders apiKeyHeaders;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ScreenerProcessRepo repo;

	ScreenerProcess sp;

	@BeforeStep
	public void before(StepExecution se) throws Exception {
		hasErrors = false;
		processed = 0;
		cost = 0;
		coincidences = 0;
		ahora = new Date();
		di = ahora.getTime();

		id = se.getJobExecution().getJobParameters().getString(Constants.ID);
		user = se.getJobExecution().getJobParameters().getString(Constants.USER);

		sp = repo.findById(id).get();

		File outDirUser = new File(outDir + File.separator + user);
		outDirUser.mkdirs();
		out = outDirUser.getAbsolutePath() + File.separator + id + ".out.gz";

		log.info("Writing to " + out);
		fos = new FileOutputStream(new File(out));
		bufStream = new BufferedOutputStream(fos);
		zipStream = new GZIPOutputStream(bufStream);

	}

	@Override
	public void write(List<? extends Person> items) throws Exception {
		try {
			long d0 = new Date().getTime();
			List<List<Person>> batchs = splitInBatchs(items);

			// ForkJoinPool customThreadPool = new ForkJoinPool(searcherForkSize);
			// customThreadPool.submit(() ->{
			System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
					String.valueOf(searcherForkSize));

			batchs.parallelStream().forEach((l) -> {
				screen(l);
			});
			// }
			//
			// );
			// customThreadPool.shutdownNow();

			zipStream.flush();
			processed += items.size();

			long d1 = new Date().getTime();
			log.info("Procesados: " + processed + ". {}ms.TT: {}s", (d1 - d0), (d1 - di) / 1000L);
		} catch (Exception e) {
			hasErrors = true;
			throw e;
		}
	}

	private List<List<Person>> splitInBatchs(List<? extends Person> persons) {
		List<List<Person>> ret = new ArrayList<List<Person>>();
		List<Person> tmp = new ArrayList<Person>();
		int size = batchSize / searcherForkSize;
		for (int i = 0; i < persons.size(); i++) {
			if (i % size == 0 && i > 0) {
				ret.add(tmp);
				tmp = new ArrayList<Person>();
			}
			tmp.add(persons.get(i));
		}
		if (!tmp.isEmpty()) {
			ret.add(tmp);
		}
		return ret;
	}

	private void screen(List<Person> persons) throws RuntimeException {
		ScreenerSearchDTO sdto = objectMapper.convertValue(sp.getParams(), ScreenerSearchDTO.class);

		persons.stream().forEach(p -> {
			ScreenerSearchData ssd = new ScreenerSearchData();
			ssd.setUid(p.getUid());
			ssd.setUpdatedAfter(p.getScreened());
			ssd.setName(p.getName());
			ssd.setId1(p.getId1());
			ssd.setId2(p.getId2());
			ssd.setId3(p.getId3());
			ssd.setId4(p.getId4());

			sdto.getData().add(ssd);
		});

		Result ssr = restTemplate.exchange(searcherUrl, HttpMethod.POST,
				new HttpEntity<ScreenerSearchDTO>(sdto, apiKeyHeaders.getApiKey()), Result.class).getBody();

		writeResults(ssr);
	}

	private void writeResults(Result ssr) {
		cost += ssr.getCost();
		ssr.getIds().parallelStream().forEach(ssri -> {
			if (ssri.getCoincidencesCount() == 0) {
				writeToZip(ssri.getId() + DELIMITER + 0);
			} else {
				coincidences += ssri.getCoincidencesCount();
				try {
					writeToZip(ssri.getId() + DELIMITER + ssri.getCoincidencesCount() + DELIMITER
							+ objectMapper.writeValueAsString(ssri.getSearchs()));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private void writeToZip(String s) {
		try {
			zipStream.write((s + "\r\n").getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@AfterStep
	public void close(StepExecution se) throws Exception {
		log.info("Closing " + out);
		zipStream.close();

		se.getJobExecution().getExecutionContext().putLong(Constants.COST, cost);
		se.getJobExecution().getExecutionContext().putLong(Constants.RECORDS, processed);
		se.getJobExecution().getExecutionContext().putLong(Constants.COINCIDENCES, coincidences);
	}

}

package com.systech.ms.list.searcher.service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.systech.ms.core.model.list.Bill;
import com.systech.ms.core.model.list.Reportado;
import com.systech.ms.core.service.ApiKeyHeaders;
import com.systech.ms.list.model.CheckResult;
import com.systech.ms.list.model.Match;
import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.searcher.model.Coincidence;
import com.systech.ms.list.searcher.model.Result;
import com.systech.ms.list.searcher.repo.ReportadoRepo;
import com.systech.ms.list.service.CostCalculator;
import com.systech.ms.list.service.Matcher;
import com.systech.ms.list.utils.MatchingUtilsParams;

@Service
public class SearcherService {
	Logger logger = LoggerFactory.getLogger(SearcherService.class);
	@Autowired
	Matcher m;

	@Autowired
	private ReportadoRepo repo;
	@Autowired
	MatchingUtilsParams params;

	@Autowired
	CostCalculator costCalculator;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	RestTemplate restTemplate;

	@Value("${biller.url}/bill")
	String billerUrl;

	@Autowired
	ApiKeyHeaders apiKeyHeaders;

	public Result check(SearchQuery search, String authorization) throws Exception {
		Date dde = new Date();
		validateInput(search);
		Result result = doCheck(search);
		result.setElapsedTime(getElapsedTime(dde));

		Bill bill = modelMapper.map(result, Bill.class);
		bill.setRecords(1);
		bill.setUser(result.getSearch().getUser());
		bill.setUis(result.getCoincidences().stream().map(c -> c.getUi()).collect(Collectors.toList()));

		logger.info("{}", bill);

		HttpHeaders billerHeaders = new HttpHeaders();
		billerHeaders.add("Authorization", authorization);
		restTemplate.exchange(billerUrl, HttpMethod.POST, new HttpEntity<Bill>(bill, billerHeaders), Void.class);
		return result;
	}

	private void validateInput(SearchQuery search) throws Exception {
		if (search.getText() == null || search.getText().equals(""))
			throw new Exception("Empty search text. Searched text is mandatory");
		if (search.getMinLevel() == null)
			throw new Exception("Empty minLevel. MinLevel is mandatory");
		if (search.getMinLevel() < 0)
			search.setMinLevel(0);
		if (search.getMinLevel() > 100)
			search.setMinLevel(100);
	}

	private Result doCheck(SearchQuery search) throws ParseException, Exception {
		Result result = new Result();
		result.setSearch(search);
		Map<String, Reportado> reportadosMap = new TreeMap<String, Reportado>();
		String idBusqueda = UUID.randomUUID().toString();
		search.setIdSearch(idBusqueda);

		CheckResult checkResult = m.check(search);
		List<Match> matches = checkResult.getMatches();
		if (matches.size() == 0) {
			result.setReturnCode("0");
			result.setReturnMessage("No matching results found");
		} else {
			result.setReturnCode("1");
			result.setReturnMessage("Search has returned " + matches.size() + " matching results");
			result.setCoincidencesCount(matches.size());
		}

		// se buscan todos los ui de una vez para mejor performance y se guardan en un map
		if (search.isShowDetails()) {
			List<String> uis = matches.stream().map(ma -> ma.getUi()).collect(Collectors.toList());
			Iterable<Reportado> reportados = repo.findAllById(uis);
			reportados.forEach(re -> reportadosMap.put(re.getUi(), re));
		}

		matches.forEach(match -> {
			Coincidence c = new Coincidence();
			List<Coincidence> coincidencias = result.getCoincidences();
			String ui = match.getUi();
			c.setRanking(coincidencias.size());
			c.setCoincidenceLevel(match.getBestCoincidence() * 100);
			c.setUi(ui);
			c.setTipo(match.getMatchType());
			if (search.isShowDetails()) {
				c.setDetail(reportadosMap.get(ui));
			}
			coincidencias.add(c);
		});

		result.setParams(params);
		result.setParsedQuery(checkResult.getParsedQuery());
		result.setCost(costCalculator.getCost(search));

		return result;
	}

	private long getElapsedTime(Date timestamp) {
		long elapsedtime = 0;
		Date now = new java.util.Date();
		elapsedtime = now.getTime() - timestamp.getTime();
		return elapsedtime;
	}
}
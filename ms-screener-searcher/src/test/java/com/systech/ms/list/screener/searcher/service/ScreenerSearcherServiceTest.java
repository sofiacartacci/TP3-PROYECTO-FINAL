package com.systech.ms.list.screener.searcher.service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import com.systech.ms.core.service.ApiKeyHeaders;
import com.systech.ms.list.model.CheckResult;
import com.systech.ms.list.model.Match;
import com.systech.ms.list.model.ScreenerSearchDTO;
import com.systech.ms.list.model.ScreenerSearchData;
import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.model.screener.search.Coinc;
import com.systech.ms.list.model.screener.search.Id;
import com.systech.ms.list.model.screener.search.Result;
import com.systech.ms.list.model.screener.search.Search;
import com.systech.ms.list.service.CostCalculator;
import com.systech.ms.list.service.Matcher;
import com.systech.ms.list.utils.MatchingUtilsParams;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@Disabled
public class ScreenerSearcherServiceTest {
	@Autowired
	Matcher m;

	@Autowired
	MatchingUtilsParams params;

	@Autowired
	CostCalculator costCalculator;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ApiKeyHeaders apiKeyHeaders;

	@Test
	public void prueba() throws Exception {
		ScreenerSearchDTO dto = new ScreenerSearchDTO();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		List<ScreenerSearchData> data = new ArrayList<>();

		ScreenerSearchData ssd = new ScreenerSearchData();
		ssd.setUid("2");
		ssd.setName("asala");
		ssd.setId1("22913365");
		ssd.setId2("22913365");
		ssd.setId3("22913365");
		ssd.setId4("22913365");
		ssd.setUpdatedAfter(sdf.parse("20220401"));

		data.add(ssd);

		dto.setMinLevel(50);
		dto.setSearchAliases(true);
		dto.setData(data);

		for (int i = 0; i < 100; i++) {
			long d0 = new Date().getTime();
			Result r = check(dto);
			long d1 = new Date().getTime();
			log.info("{}: coinc:{}, TT:{}", i, r.getIds().size(), d1 - d0);
		}
	}

	public Result check(ScreenerSearchDTO searchDTO) throws Exception {
		Result result = new Result();
		SearchQuery searchQuery = modelMapper.map(searchDTO, SearchQuery.class);
		log.info("Processing {} searchs.", searchDTO.getData().size());
		searchDTO.getData().stream().forEach(d -> {
			if (d == null) {
				throw new RuntimeException("SearchDTO.Data element is null: " + searchDTO.getData());
			}
			Id resultId = new Id();
			resultId.setId(d.getUid());

			searchQuery.setUpdatedAfter(d.getUpdatedAfter());
			List<Search> searchs = new ArrayList<Search>();

			long subcost = 0;
			subcost += prepareCheck(searchs, searchQuery, d.getName());
			subcost += prepareCheck(searchs, searchQuery, d.getId1());
			subcost += prepareCheck(searchs, searchQuery, d.getId2());
			subcost += prepareCheck(searchs, searchQuery, d.getId3());
			subcost += prepareCheck(searchs, searchQuery, d.getId4());

			int total = 0;
			for (int i = 0; i < searchs.size(); i++) {
				total = total + searchs.get(i).getCoincs().size();
			}
			resultId.setCoincidencesCount(total);

			resultId.setSearchs(searchs);
			result.getIds().add(resultId);
			result.setCost(result.getCost() + subcost);
		});

		return result;
	}

	private long prepareCheck(List<Search> searchs, SearchQuery searchQuery, String text) {
		long cost = 0;
		if (text == null || text.isBlank()) {
			// do nothing
		} else {
			searchQuery.setText(text);
			Search search = doCheck(searchQuery);
			cost = costCalculator.getCost(searchQuery);
			if (search != null) {
				searchs.add(search);
			}
		}
		return cost;
	}

	private Search doCheck(SearchQuery search) {
		String idBusqueda = UUID.randomUUID().toString();
		search.setIdSearch(idBusqueda);

		CheckResult checkResult = null;
		try {
			checkResult = m.check(search);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		List<Match> matches = checkResult.getMatches();

		if (matches.size() == 0) {
			return null;
		}
		Search result = new Search();
		result.setText(search.getText());

		matches.forEach(match -> {
			Coinc c = new Coinc();
			List<Coinc> coincidencias = result.getCoincs();
			String ui = match.getUi();
			c.setRanking(coincidencias.size());
			c.setLevel(match.getBestCoincidence() * 100);
			c.setUi(ui);
			c.setTipo(match.getMatchType());

			coincidencias.add(c);
		});

		return result;
	}
}

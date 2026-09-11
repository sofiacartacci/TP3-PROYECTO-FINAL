package com.systech.ms.list.screener.searcher.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.systech.ms.core.service.ApiKeyHeaders;
import com.systech.ms.list.model.CheckResult;
import com.systech.ms.list.model.Match;
import com.systech.ms.list.model.ScreenerSearchDTO;
import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.model.screener.search.Coinc;
import com.systech.ms.list.model.screener.search.Id;
import com.systech.ms.list.model.screener.search.Result;
import com.systech.ms.list.model.screener.search.Search;
import com.systech.ms.list.service.CostCalculator;
import com.systech.ms.list.service.Matcher;
import com.systech.ms.list.utils.MatchingUtilsParams;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScreenerSearcherService {
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
	
	
	public Result check(ScreenerSearchDTO searchDTO) throws Exception {
		Result result = new Result();
		SearchQuery searchQuery = modelMapper.map(searchDTO, SearchQuery.class);
		log.info("Processing {} searchs.",searchDTO.getData().size());
		searchDTO.getData().stream().forEach(d -> {
			if(d==null) {
				throw new RuntimeException("SearchDTO.Data element is null: " + searchDTO.getData());
			}
			Id resultId = new Id();
			resultId.setId(d.getUid());

			
			searchQuery.setUpdatedAfter(d.getUpdatedAfter());
			List<Search>searchs=new ArrayList<Search>();
			
			long subcost=0;
			subcost+=prepareCheck(searchs,searchQuery,d.getName());
			subcost+=prepareCheck(searchs,searchQuery,d.getId1());
			subcost+=prepareCheck(searchs,searchQuery,d.getId2());
			subcost+=prepareCheck(searchs,searchQuery,d.getId3());
			subcost+=prepareCheck(searchs,searchQuery,d.getId4());
			
			
			int total=0;
			for(int i=0;i<searchs.size();i++) {
				total=total + searchs.get(i).getCoincs().size();
			}
			resultId.setCoincidencesCount(total);
			
			resultId.setSearchs(searchs);
			result.getIds().add(resultId);
			result.setCost(result.getCost()+subcost);
		});
		
		return result;
	}
	
	private long prepareCheck(List<Search>searchs, SearchQuery searchQuery, String text) {
		long cost=0;
		if(text==null||text.isBlank()) {
			//do nothing
		}else {
			searchQuery.setText(text);
			Search search=doCheck(searchQuery);
			cost=costCalculator.getCost(searchQuery);
			if(search!=null) {
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

		if(matches.size()==0) {
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

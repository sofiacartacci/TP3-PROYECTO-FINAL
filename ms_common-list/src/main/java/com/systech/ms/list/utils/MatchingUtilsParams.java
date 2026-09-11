package com.systech.ms.list.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class MatchingUtilsParams {
	@Value("${matcher.depth:0}")
	int MATCHDEPTH;
	@Value("${matcher.prlen:2}")
	int MATCHPRLEN;
	@Value("${matcher.minto:}")
	String MATCHMINTO;
	@Value("${matcher.maxhits:100000}")
	int MATCHMAXHITS;
	@Value("${matcher.maxres:100}")
	int MATCHMAXRES;
}

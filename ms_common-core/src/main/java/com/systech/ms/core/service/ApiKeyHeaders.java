package com.systech.ms.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class ApiKeyHeaders {
	@Value("${systech.api-key:NO SYSTECH-API-KEY ENVIRONMENT VARIABLE FOUND}")
	private String apiKey;

	public HttpHeaders getApiKey() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + apiKey);
		return headers;
	}
}

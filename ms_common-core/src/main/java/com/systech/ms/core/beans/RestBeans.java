package com.systech.ms.core.beans;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.security.cert.CertificateException;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;



@Configuration
public class RestBeans {
	@Value("${TRUSTSTORE_FILE}")
	String tsf;
	@Value("${TRUSTSTORE_PASS}")
	String tsp;
	@Value("$TRUSTSTORE_TYPE}")
	String tst;
	
	
	@Bean
	public RestTemplate restTemplateWithTrustStore(RestTemplateBuilder builder) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, java.security.cert.CertificateException {  
	    SSLContext sslContext = new SSLContextBuilder()
	        .loadTrustMaterial(new File(tsf), tsp.toCharArray())
	        .build();
	    SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

	    HttpClient httpClient = HttpClients.custom()
	        .setSSLSocketFactory(socketFactory)
	        .build();

	    return builder
	        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
	        .build();
	}

}

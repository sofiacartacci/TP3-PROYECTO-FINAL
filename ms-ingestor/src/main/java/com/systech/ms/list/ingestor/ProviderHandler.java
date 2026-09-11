package com.systech.ms.list.ingestor;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.modelmapper.internal.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.systech.ms.list.ingestor.model.Provider;
import com.systech.ms.list.ingestor.repo.ProviderRepo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Data
@Slf4j
public class ProviderHandler{
	
	@Autowired 
	ProviderRepo providerRepo;
	
	Provider provider;
	
	@Value("${mapping.id}")
	String id;
	@Value("${mapping.name}")
	String name;
	
	@Value("${mapping.errors.max:10000000}")
	Integer errorsMax;
	
	public ProviderHandler(){
		
	}
	
	@PostConstruct
	public void init() {
		log.info("Max errors: {}", errorsMax);
		Optional<Provider>opt=providerRepo.findById(id);
		if(opt.isPresent()) {
			provider=opt.get();
		}else {
			provider=new Provider(id,name);
		}
	}
	public void addError(String error)throws Exception {
		Map<String,Integer> errors=provider.getErrors();
		
		int c=Objects.firstNonNull(errors.get(error), 0);
		errors.put(error, ++c);
		provider.setErrors(errors);
		
		provider.setErrorCount(provider.getErrorCount()+1);
		if(provider.getErrorCount()>=errorsMax) {
			provider.setAborted(true);
			providerRepo.save(provider);
			throw new Exception ("The max number of errors allowed (errors.max=" + errorsMax + ") has been reached.");
		}		
	}

}

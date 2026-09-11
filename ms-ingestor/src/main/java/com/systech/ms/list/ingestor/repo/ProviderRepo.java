package com.systech.ms.list.ingestor.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.list.ingestor.model.Provider;

@Repository
public interface ProviderRepo extends MongoRepository<Provider,String> {
}

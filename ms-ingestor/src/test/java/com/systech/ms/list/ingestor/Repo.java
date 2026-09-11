package com.systech.ms.list.ingestor;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.Reportado;

@Repository
public interface Repo extends MongoRepository<Modelo,String>{

}

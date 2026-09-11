package com.systech.ms.list.ingestor.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.Reportado;
import com.systech.ms.list.ingestor.model.Update;


@Repository
public interface UpdateRepo extends MongoRepository<Update,String> {
	
}

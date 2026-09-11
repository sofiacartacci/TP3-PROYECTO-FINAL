package com.systech.ms.list.searcher.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.Reportado;

@Repository
public interface ReportadoRepo extends MongoRepository<Reportado,String> {
	public Optional<Reportado> findById(String user);
	public List<Reportado> findAll();
	
	
}

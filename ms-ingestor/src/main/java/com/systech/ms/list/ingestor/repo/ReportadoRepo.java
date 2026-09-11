package com.systech.ms.list.ingestor.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.Reportado;

@Repository
public interface ReportadoRepo extends MongoRepository<Reportado, String> {
	public Optional<Reportado> findById(String ui);

	public List<Reportado> findAll();

	@Query(value = "{ 'ui' : ?0 }", fields = "{ 'updated' : 1}")
	public Optional<Reportado> findUpdatedById(String ui);
}

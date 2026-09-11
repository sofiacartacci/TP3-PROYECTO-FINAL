package com.systech.ms.list.ingestor.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.HistId;
import com.systech.ms.core.model.list.ReportadoHist;

@Repository
public interface ReportadoHistRepo extends MongoRepository<ReportadoHist, HistId> {
	public Optional<ReportadoHist> findById(String user);
}

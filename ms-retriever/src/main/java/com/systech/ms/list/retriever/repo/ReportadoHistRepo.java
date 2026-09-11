package com.systech.ms.list.retriever.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.HistId;
import com.systech.ms.core.model.list.ReportadoHist;

@Repository
public interface ReportadoHistRepo extends MongoRepository<ReportadoHist, HistId> {
	public Optional<ReportadoHist> findById(String user);

	public Optional<List<ReportadoHist>> findAllByUi(String ui);
}

package com.systech.ms.list.screener.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.Reportado;
import com.systech.ms.list.screener.model.ScreenerProcess;


@Repository
public interface ScreenerProcessRepo extends MongoRepository<ScreenerProcess,String> {
	public Optional<ScreenerProcess> findById(String ui);
	public List<ScreenerProcess> findAll();
	public List<ScreenerProcess> findByStartedIsNullOrderBySubmittedAsc();
	public List<ScreenerProcess> findByStartedIsNotNullAndEndedIsNull();
}

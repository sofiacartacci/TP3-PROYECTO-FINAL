package com.systech.ms.list.biller.repo;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.systech.ms.core.model.list.Bill;
import com.systech.ms.list.biller.model.UserTotalResult;
@Repository
public interface BillerRepo extends MongoRepository<Bill,String> {
	public Optional<Bill> findById(String user);
	public List<Bill> findAll();
	
	@Aggregation(pipeline = {
	        "{'$match':{'user': ?0, 'searchDate' : {$gte : ?1,$lt : ?2}}}"
	        ,"{$group: {_id: null, count: { $sum: $records},total: {$sum: $cost}}}"
	})
	UserTotalResult getUserSearchs(String user, Date from,  Date to);
	
}

package com.systech.ms.list.service;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.systech.ms.list.model.SearchQuery;

@Service
public class CostCalculator {
	long MES=1000L*60L*60L*24L*30L;
	long MAXCOST=120;
	
	//el costo es 1 por cada 30 días entre la fecha actual y la fecha updatedAfter, (que debería coincidir con la del ultimo cruce)
	//si la cantidad de meses es mayor a 120 el costo es 120
	//si la cantidad de meses da negativa (porque updatedAfter es futuro, es decir está mal) el costo es 1
	public long getCost(SearchQuery search) {
		if(search.getUpdatedAfter()==null) {
			return MAXCOST;
		}
		long ahora=new Date().getTime();
		long updatedAfter=search.getUpdatedAfter().getTime();
		if(updatedAfter==0) {
			return MAXCOST;
		}
		
		long milisdif=ahora - updatedAfter;
		if(milisdif<0) {
			return 1;
		}
		long cost=milisdif / MES;
		if(cost>MAXCOST) {
			cost=MAXCOST;
		}
		return cost;
	}
}

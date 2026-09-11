package com.systech.ms.list.ingestor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest
class IngestorApplicationTests {

	@Autowired Repo repo;
	@Autowired Modelo modelo;
	@Autowired MongoTemplate mt;
	DecimalFormat df=new DecimalFormat("0000");
	
	//@Test
	void test() throws Exception{
		long d0=new Date().getTime();
		List list=new ArrayList();
		BulkOperations bo=mt.bulkOps(BulkMode.ORDERED, Modelo.class);
		mt.dropCollection(Modelo.class);
		for(int i=1; i<20000;i++) {
			
			
			Modelo m=new Modelo(df.format(i),String.valueOf(i) );
			
			
			//repo.save(m);
			//mt.insert(m);
			//mt.save(m);
			
			Query query = new Query(Criteria.where("id").is(m.getId()));
			Document doc = new Document(); // org.bson.Document
			mt.getConverter().write(m, doc);
			Update update=Update.fromDocument(doc, "id");
			
			//mt.upsert(query, update, Modelo.class);
			
			
			
			
			FindAndReplaceOptions options = FindAndReplaceOptions.options();
			//bo.upsert(query, update);
			bo.replaceOne(query, doc,options.upsert());
			
			//list.add(m);
			
			
			
			if(i%1000==0) {
				bo.execute();
				bo=mt.bulkOps(BulkMode.ORDERED, Modelo.class);
				//repo.saveAll(list);
				list=new ArrayList();
				long d1=new Date().getTime();
				System.out.println(i + ": " + (d1-d0));
				d0=new Date().getTime();
			}
		}
	}

}

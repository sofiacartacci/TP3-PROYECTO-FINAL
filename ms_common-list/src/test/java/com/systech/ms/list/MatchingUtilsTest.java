package com.systech.ms.list;

import java.io.File;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.systech.ms.list.utils.MatchingUtils;

public class MatchingUtilsTest {
	@InjectMocks
	MatchingUtils mu;
	
	//@Test
	void keywordsTest() throws Exception{
		System.out.println("------------------------------------------------------------");
		File f = new File("c:/$listasfiles/lucene-index" );
		IndexSearcher sTEXT= new IndexSearcher(DirectoryReader.open(new NIOFSDirectory(f.toPath())));;
		Query query=null;
		BooleanQuery.Builder bqb=new BooleanQuery.Builder();
		bqb.add(new TermQuery(new Term("kwd","ice")),Occur.MUST);
		
		query=bqb.build();
		System.out.println(query);
		TopDocs td=sTEXT.search(query, 100);
		System.out.println(td.scoreDocs.length);
	}

}

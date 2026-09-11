package com.systech.ms.list.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.systech.ms.list.model.CheckResult;
import com.systech.ms.list.model.Match;
import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.model.StopWords;
import com.systech.ms.list.utils.MatchingUtils;

@Component
@EnableScheduling
public class Matcher {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	protected boolean initialized = false;
	protected Analyzer textAnalyzer = null;
	protected Analyzer numberAnalyzer = null;
	protected IndexSearcher sTEXT = null;
	protected LuceneHighlighter lh = null;

	private DirectoryReader dirReader = null;

	@Value("${matcher.indexpath}")
	protected String indexPath;

	@Autowired
	protected MatchingUtils mu;

	public Matcher() throws Exception {
	}

	@PostConstruct
	protected void init() throws Exception {
		textAnalyzer = new StandardAnalyzer(new CharArraySet(StopWords.getInstance().getStopWords(), true));
		File f = new File(getIndexPath());
		dirReader = DirectoryReader.open(new NIOFSDirectory(f.toPath()));
		// dirReader = DirectoryReader.open(new MMapDirectory(f.toPath()));
		sTEXT = new IndexSearcher(dirReader);
		IndexSearcher.setMaxClauseCount(4096);
		lh = new LuceneHighlighter();
		initialized = true;
	}

	@Scheduled(cron = "${matcher.look-for-changes-in-index.cron:*/30 * * * * *}")
	private void lookForChanges() throws Exception {
		DirectoryReader newReader = DirectoryReader.openIfChanged(dirReader);
		if (newReader != null) {
			logger.info("Looking for changes in index. Changes found, reopening");
			dirReader.close();
			dirReader = newReader;
			sTEXT = new IndexSearcher(dirReader);
		} else {
			logger.info("Looking for changes in index. No changes found");
		}

	}

	protected String getIndexPath() throws Exception {
		return indexPath;
	}

	public CheckResult check(SearchQuery search) throws Exception {
		CheckResult checkResult = new CheckResult();
		Query query = mu.getQuery(search);
		// logger.info("{}",query);
		float minMatchScore = search.getMinLevel() / 100;
		TreeMap matches = mu.getUIMatchesList(search.getText(), query, sTEXT, minMatchScore);

		checkResult.setMatches(filter(matches, search));
		checkResult.setParsedQuery(query.toString());
		return checkResult;
	}

	@PreDestroy
	private void close() throws Exception {
		sTEXT.getIndexReader().close();
	}

	protected List<Match> filter(TreeMap matches, SearchQuery search) throws Exception {
		List<Match> resultados = new ArrayList<Match>();
		Iterator it = matches.keySet().iterator();
		while (it.hasNext()) {
			String k = (String) it.next();
			Match match = (Match) matches.get(k);
			if (match.getBestCoincidence() < (search.getMinLevel() / 100F))
				continue;
			resultados.add(match);
		}
		return resultados;
	}
}

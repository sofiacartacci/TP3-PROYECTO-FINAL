
package com.systech.ms.list.ingestor.tasklets;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.NIOFSDirectory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.systech.ms.list.model.StopWords;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MergeIndex implements Tasklet, StepExecutionListener {

	Analyzer analyzer;
	public IndexWriter ixwriter;
	@Value("${index.folder}")
	String index_folder;

	@Override
	public void beforeStep(StepExecution stepExecution)

	{
		try {
			analyzer = new StandardAnalyzer(new CharArraySet(StopWords.getInstance().getStopWords(), true));

			NIOFSDirectory d = new NIOFSDirectory(new File(index_folder).toPath());

			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			ixwriter = new IndexWriter(d, iwc);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		log.info("Merging index" + index_folder);
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		log.info("Merging index");
		ixwriter.forceMerge(1);
		log.info("Closing index");
		ixwriter.close();
		return RepeatStatus.FINISHED;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return ExitStatus.COMPLETED;
	}
}

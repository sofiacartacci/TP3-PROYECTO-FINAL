/*
 * LuceneHighlighter.java
 *
 * Created on 9 de febrero de 2006, 17:47
 */
package com.systech.ms.list.service;


import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * @author ngeltman
 */
public class LuceneHighlighter {
	Logger logger=LoggerFactory.getLogger(this.getClass());
    private final String CONTENTS_FIELD = "contents";
    public final String leftHighlight="<span class='resalte'>";
    public final String rightHighlight="</span>";

    // TODO Remove and make Spring hold it
    private final Analyzer analyzer = new SimpleAnalyzer();

    /**
     * Construye un objeto LuceneHighlighter
     *
     */
    public LuceneHighlighter() {
        // default constructor
    }

    public String highlight( String text, String query, String separator,
            int fragSize, int numFrags, boolean complete ) {

        String result = "";

        try {

            Directory ramDir = new ByteBuffersDirectory();

            addDocument(text, ramDir);

            IndexReader reader = DirectoryReader.open(ramDir);

            Query queryObj = new QueryParser(CONTENTS_FIELD,analyzer).parse(query);
            queryObj = queryObj.rewrite(reader);

            QueryScorer scorer = new QueryScorer(queryObj);
            Formatter formatter = new SimpleHTMLFormatter(leftHighlight,rightHighlight );
            Highlighter highlighter = new Highlighter(formatter, scorer);

            highlighter.setTextFragmenter(new SimpleFragmenter(fragSize));

            TokenStream token = analyzer.tokenStream(CONTENTS_FIELD, new StringReader(text));

            if (!complete) {
                result = highlighter.getBestFragments(token, text, numFrags, separator);

            } else {

                //result = highlighter.getCompleteTextHighlight(token, text);
                result = text;

            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            result = avoidEmpty(result, text);

        }

        return result;

    }

    /**
     *
     *
     * @param actualText
     * @param ramDir
     * @throws IOException
     */
    private void addDocument( String actualText, Directory ramDir )
    throws IOException {
        Document document = new Document();
        document.add((IndexableField)new TextField(CONTENTS_FIELD, actualText, Field.Store.YES));
        
        IndexWriterConfig iwc=new IndexWriterConfig(analyzer);
        
        
        IndexWriter writer = new IndexWriter(ramDir, iwc);
        
        
        writer.addDocument(document);
        //writer.optimize();
        writer.close();

    }

    /**
     *
     *
     * @param string
     * @param text
     */
    private String avoidEmpty( String string , String text) {

        String result = string;

        if (string == null || "".equals(string.trim())) {
            result = text;
        }

        return result;
    }


}


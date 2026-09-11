package com.systech.ms.list.ingestor.task.chunks;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.NIOFSDirectory;
import org.modelmapper.ModelMapper;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.systech.ms.core.model.list.HistId;
import com.systech.ms.core.model.list.Reportado;
import com.systech.ms.core.model.list.ReportadoHist;
import com.systech.ms.list.ingestor.Constants;
import com.systech.ms.list.ingestor.ProviderHandler;
import com.systech.ms.list.ingestor.model.Provider;
import com.systech.ms.list.ingestor.model.Update;
import com.systech.ms.list.ingestor.repo.ProviderRepo;
import com.systech.ms.list.ingestor.repo.ReportadoHistRepo;
import com.systech.ms.list.ingestor.repo.ReportadoRepo;
import com.systech.ms.list.ingestor.repo.UpdateRepo;
import com.systech.ms.list.model.StopWords;
import com.systech.ms.list.utils.Utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FastloadItemWriter implements ItemWriter<Reportado> {
	private static final String UPDATEDNULL = "Updated cannot be null";

	int processed;
	int updatedDocs;
	int updatedRegs;
	int updatedHist;
	boolean hasErrors = false;

	FindAndReplaceOptions bulkOptions = FindAndReplaceOptions.options();

	@Autowired
	ReportadoRepo reportadoRepo;
	@Autowired
	ReportadoHistRepo reportadoHistRepo;
	@Autowired
	UpdateRepo updateRepo;
	@Autowired
	ProviderRepo providerRepo;
	@Autowired
	ProviderHandler providerHandler;
	@Autowired
	MongoTemplate mt;
	@Autowired
	private ModelMapper modelMapper;

	Provider provider;

	private Date ahora;

	@Value("${mapping.id}")
	String id;

	@Value("${mapping.separator}")
	String SEPARADOR;

	@Value("${index.folder}")
	String index_folder;

	@Value("${mapping.errors.ignore:false}")
	Boolean ignoreErrors;

	@Value("${mapping.save.history:false}")
	Boolean saveHistory;

	Date lastUpdate = null;

	Analyzer analyzer;
	public IndexWriter ixwriter;

	boolean inited = false;
	Boolean compareChanges = false;
	long di;

	@BeforeStep
	public void before(StepExecution stepExecution) throws Exception {
		this.compareChanges = Boolean
				.valueOf(stepExecution.getJobParameters().getString(Constants.COMPARE_CHANGES, "false"));
		init();
	}

	private void init() throws Exception {
		hasErrors = false;
		processed = 0;
		updatedDocs = 0;
		updatedRegs = 0;
		updatedHist = 0;
		ahora = new Date();
		di = ahora.getTime();
		analyzer = new StandardAnalyzer(new CharArraySet(StopWords.getInstance().getStopWords(), true));

		NIOFSDirectory d = new NIOFSDirectory(new File(index_folder).toPath());

		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		ixwriter = new IndexWriter(d, iwc);

		providerHandler.init();
		provider = providerHandler.getProvider();
		lastUpdate = provider.getLastUpdate();

		log.info("Compare changes: {}", compareChanges);
		log.info("Ignore errors: {}", ignoreErrors);
		log.info("Save history: {}", saveHistory);
		inited = true;
	}

	@Override
	public void write(List<? extends Reportado> items) throws Exception {
		try {
			long d0 = new Date().getTime();
			BulkOperations bor = mt.bulkOps(BulkMode.ORDERED, Reportado.class);
			BulkOperations boh = mt.bulkOps(BulkMode.ORDERED, ReportadoHist.class);
			BulkOperations bou = mt.bulkOps(BulkMode.ORDERED, Update.class);
			boolean hasRWrites = false;
			boolean hasHWrites = false;
			boolean hasUWrites = false;
			for (Reportado r : items) {
				if (isUpdate(r)) {
					r.setSysUpdate(new Date());

					Doc doc = new Doc(r);
					indexNam(r, doc);
					indexAls(r, doc);
					indexAss(r, doc);
					indexIds(r, doc);
					indexCountries(r, doc);

					// registro a insertar o actualizar
					Query queryR = new Query(Criteria.where("ui").is(r.getUi()));
					org.bson.Document reportadoDoc = new org.bson.Document(); // org.bson.Document
					mt.getConverter().write(r, reportadoDoc);
					bor.replaceOne(queryR, reportadoDoc, bulkOptions.upsert());
					hasRWrites = true;

					// update a insertar o actualizar
					Query queryU = new Query(Criteria.where("ui").is(r.getUi()));
					org.bson.Document updateDoc = new org.bson.Document(); // org.bson.Document
					Update u = new Update(r.getUi(), r.getUpdated());
					mt.getConverter().write(u, updateDoc);
					bou.replaceOne(queryU, updateDoc, bulkOptions.upsert());
					hasUWrites = true;

					// resguardo de version anterior
					ReportadoHist rh = getHist(r.getUi());
					if (rh != null) {
						org.bson.Document reportadoHistDoc = new org.bson.Document(); // org.bson.Document
						mt.getConverter().write(rh, reportadoHistDoc);
						boh.insert(reportadoHistDoc);
						updatedHist++;
						hasHWrites = true;
					}

					updatedRegs++;
				}
			}

			if (hasHWrites) {
				boh.execute();
			}
			if (hasUWrites) {
				bou.execute();
			}
			if (hasRWrites) {
				bor.execute();
			}

			processed += items.size();
			provider.setProcessed(processed);

			long d1 = new Date().getTime();
			log.info("Procesados: " + processed + ". Actualizados (regs,docs,hist): ({},{},{}). {}ms.TT: {}s",
					updatedRegs, updatedDocs, updatedHist, (d1 - d0), (d1 - di) / 1000L);
		} catch (Exception e) {
			hasErrors = true;
			throw e;
		}
	}

	private void indexNam(Reportado r, Doc doc) throws Exception {
		doc.setFieldname("nam");
		doc.setText(prepareToStore(r.getNam()));
		addDocument(doc, 0);
	}

	private void indexAls(Reportado r, Doc doc) throws Exception {
		doc.setFieldname("als");
		Iterator<String> it = r.getAls().iterator();
		int i = 0;
		while (it.hasNext()) {
			doc.setText(prepareToStore(it.next()));
			addDocument(doc, i);
			i++;
		}
	}

	private void indexAss(Reportado r, Doc doc) throws Exception {
		doc.setFieldname("ass");
		Iterator<String> it = r.getAss().iterator();
		int i = 0;
		while (it.hasNext()) {
			doc.setText(prepareToStore(it.next()));
			addDocument(doc, i);
			i++;
		}
	}

	private void indexIds(Reportado r, Doc doc) throws Exception {
		doc.setFieldname("ids");
		Iterator<String> it = r.getIds().iterator();
		int i = 0;
		while (it.hasNext()) {
			doc.setText(prepareToStoreIds(it.next()));
			addDocument(doc, i);
			i++;
		}
	}

	private void indexCountries(Reportado r, Doc doc) throws Exception {
		doc.setFieldname("cntry");
		int i = 0;
		for (String country : r.getCountries()) {
			doc.setText(prepareToStoreIds(country));
			addDocument(doc, i);
			i++;
		}

	}

	private void addDocument(Doc doc, int iterationNumber) throws Exception {
		if (doc.getText().trim().equals("")) {
			return;
		}
		Document document = new Document();
		Date upd = doc.getUpdated();
		if (upd == null) {
			upd = doc.getEntered();
		}

		String id = doc.getUi() + "-" + doc.getFieldname() + "-" + iterationNumber;
		long deprecated = 0L;
		if (doc.getDeprecated() != null) {
			deprecated = doc.getDeprecated().getTime();
		}

		document.add((IndexableField) new StringField("id", id, Store.YES));
		document.add((IndexableField) new StringField("ui", doc.getUi(), Store.YES));
		document.add((IndexableField) new TextField("text", doc.getText(), Store.YES));
		document.add((IndexableField) new StringField("tipo", doc.getFieldname(), Store.YES));

		document.add((IndexableField) new LongPoint("upd", upd.getTime()));
		document.add((IndexableField) new SortedNumericDocValuesField("upd", upd.getTime()));

		document.add((IndexableField) new LongPoint("dep", deprecated));
		document.add((IndexableField) new StringField("cat",
				Utils.removeNonAlphaNumericAndSpaceAndLower(doc.getCategory()), Store.NO));
		document.add((IndexableField) new StringField("sub",
				Utils.removeNonAlphaNumericAndSpaceAndLower(doc.getSubcategory()), Store.NO));
		document.add((IndexableField) new TextField("kwd", doc.getKeywords(), Store.NO));
		document.add((IndexableField) new TextField("cntry", doc.getCountries(), Store.NO));

		ixwriter.updateDocument(new Term("id", id), document);
		updatedDocs++;
	}

	private String prepareKeywords(List<String> keywords) {
		keywords = keywords.stream().map(k -> {
			return Utils.removeNonAlphaNumericAndSpaceAndLower(k);
		}).collect(Collectors.toList());
		return String.join(";", keywords);
	}

	private String prepareCountries(List<String> keywords) {
		keywords = keywords.stream().map(k -> {
			return Utils.removeNonAlphaNumericAndSpaceAndLower(k);
		}).collect(Collectors.toList());
		return String.join(";", keywords);
	}

	private String prepareToStoreIds(String text) {
		// reemplazo todo los caracteres especiales,
		// incluso los puntos por vacio
		return text.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "").trim();
	}

	private String prepareToStore(String text) {
		// reemplazo todo los caracteres
		// especiales, excepto los . y & por un
		// espacio
		text = text.replaceAll("[!-%'--/-/:-@\\[-`{-~¨¡°]", " ").trim();
		text = Utils.removerAcentos(text);
		return text;

	}

	private boolean isUpdate(Reportado r) throws Exception {

		if (r.getUpdated() == null) {
			log.error(UPDATEDNULL + ". Reportado: {}", r);

			providerHandler.addError(UPDATEDNULL);
			if (!ignoreErrors) {
				throw new Exception(UPDATEDNULL + ": " + r);
			} else {
				r.setUpdated(ahora);
			}
		}

		if (compareChanges) {
			return isUpdateComparing(r);
		} else {
			return isUpdateNoCompare(r);
		}

	}

	private boolean isUpdateComparing(Reportado r) throws Exception {
		// Optional<Reportado> opt = reportadoRepo.findUpdatedById(r.getUi());
		Optional<Update> opt = updateRepo.findById(r.getUi());
		if (opt.isPresent()) {
			Update upd = opt.get();
			if (r.updated.compareTo(upd.updated) > 0) {
				setLastUpdate(r);
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}

	}

	private boolean isUpdateNoCompare(Reportado r) throws Exception {
		if (r.getUpdated().compareTo(provider.getLastUpdate()) > 0) {
			setLastUpdate(r);
			return true;
		} else {
			return false;
		}
	}

	private void setLastUpdate(Reportado r) {
		if (r.getUpdated().compareTo(lastUpdate) > 0) {
			lastUpdate = r.getUpdated();
		}
	}

	private ReportadoHist getHist(String ui) {
		ReportadoHist rh = null;
		if (saveHistory) {
			Optional<Reportado> r = reportadoRepo.findById(ui);
			if (r.isPresent()) {
				rh = modelMapper.map(r.get(), ReportadoHist.class);
				HistId hi = new HistId(ui, ahora);
				rh.setId(hi);
			}
		}
		return rh;
	}

	@AfterStep
	public void close() throws Exception {
		if (!hasErrors) {
			log.info("Saving lastUpdate");
			provider.setLastUpdate(lastUpdate);
			provider.setSysUpdate(new Date());
			providerRepo.save(provider);
		}
		log.info("Committing index");
		ixwriter.commit();
		log.info("Closing index");
		ixwriter.close();

		inited = false;
	}

	@Data
	private class Doc {
		String ui;
		String text;
		String fieldname;
		String category;
		String subcategory;
		String keywords;
		String countries;
		Date entered;
		Date updated;
		Date deprecated;

		Doc(Reportado r) {
			ui = r.getUi();
			keywords = prepareKeywords(r.getKeywords());
			countries = prepareCountries(r.getCountries());
			category = r.getCategory();
			subcategory = r.getSubcategory();
			entered = r.getEntered();
			updated = r.getUpdated();
			deprecated = r.getDeprecated();
		}
	}

}

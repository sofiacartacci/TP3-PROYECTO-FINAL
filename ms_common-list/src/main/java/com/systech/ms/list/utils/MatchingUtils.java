
package com.systech.ms.list.utils;

import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.annotation.PostConstruct;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.systech.ms.list.model.Match;
import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.model.SearchedDoc;
import com.systech.ms.list.model.StopWords;
import com.systech.ms.list.service.ComparadorLongitud;
import com.systech.ms.list.service.StringSimilarity;

/**
 *
 * @author ngeltman
 */
@Component
public class MatchingUtils {
	Logger logger = LoggerFactory.getLogger(MatchingUtils.class);

	protected Vector stopWords = new Vector();
	protected StandardAnalyzer an = new StandardAnalyzer(new CharArraySet(new HashSet(), true));
	protected String docAnt = "";
	protected MemoryIndex mi = new MemoryIndex();
	public int qHitsLastSearch = 0;
	protected final static int MAXTO = 10;

	private final long FUTUREDATE = 32583340800000L; // año 3000

	@Autowired
	MatchingUtilsParams params;

	int MAXEXPANSIONS = 100000;
	int[] terminosObligatorios = new int[MAXTO];

	/** Creates a new instance of MatchingUtils */
	@PostConstruct
	private void init() throws Exception {
		StopWords sw = StopWords.getInstance();
		stopWords = sw.getStopWordsToCompare();

		Collections.sort(stopWords, new ComparadorLongitud());
		StringTokenizer st = new StringTokenizer(params.getMATCHMINTO(), ",");
		int i = 1;
		int n = 0;
		terminosObligatorios[0] = 0;
		while (st.hasMoreElements()) {
			String s = st.nextToken();
			try {
				n = Integer.parseInt(s);
			} catch (Exception p) {
				logger.error("Error en el formato del parametro del sistema MATCHMINTOCR");
			}
			terminosObligatorios[i] = n;
			i++;
		}
		for (i = i; i < MAXTO; i++) {
			terminosObligatorios[i] = n;
		}
	}

	public TreeMap<String, Match> getUIMatchesList(String busqueda, Query query, IndexSearcher is, float minCoincidence)
			throws Exception {
		Hashtable<String, Match> matches = new Hashtable<String, Match>();
		SearchedDoc sd = new SearchedDoc();
		sd.simplesearch = busqueda;
		MatchingUtils.matchedDoc[] mds = getHits(query, is);

		matches = review(sd, mds, query);

		TreeMap<String, Match> ret = new TreeMap<String, Match>();
		TreeMap<String, Match> sortedMatches = new TreeMap<String, Match>(); // ordenados por nivel de coincidencia
		Iterator<String> it1 = matches.keySet().iterator();
		while (it1.hasNext()) {
			Match match = matches.get(it1.next());
			if (match.getBestCoincidence() < minCoincidence)
				continue; // no alcanza el nivel de coincidencia m�nimo
			String ui = (String) match.getUi();
			sortedMatches.put(
					(String.valueOf(1000 + (1 - match.getBestCoincidence()) * 100).substring(0, 4)) + "---" + ui,
					match);
		}

		Iterator<String> it2 = sortedMatches.keySet().iterator();
		int i = 0;
		while (it2.hasNext()) {
			String k = it2.next();
			Match match = (Match) sortedMatches.get(k);
			i++;
			if (i > params.getMATCHMAXRES()) {
				logger.warn("Results limit has been reached: " + params.getMATCHMAXRES());
				return ret;
			}
			ret.put(k, match);
		}
		return ret;
	}

	private TopDocs search(IndexSearcher searcher, Query query) throws Exception {
		TopDocs td = null;
		try {
			td = searcher.search(query, 10);
		} catch (Exception e) {
			logger.error(e.getMessage() + " Query:" + query, e);
			throw e;
		}
		return td;
	}

	private TopDocs searchAfter(ScoreDoc sd, IndexSearcher searcher, Query query, int cantidad) throws Exception {
		TopDocs td = null;
		try {
			td = searcher.searchAfter(sd, query, cantidad);
		} catch (Exception e) {
			logger.error(e.getMessage() + " Query:" + query, e);
			throw e;
		}
		return td;
	}

	/*
	 * Busca los primeros 10 hits. Si existe uno devuelve todos los encontrados.
	 * 
	 */

	private MatchingUtils.matchedDoc[] collectHits(IndexSearcher searcher, Query query) throws Exception {
		TopDocs firstTd = search(searcher, query);
		ScoreDoc[] tmpHits = firstTd.scoreDocs;
		int i = 0;
		int total = (int) firstTd.totalHits.value; /// REVISAR!!!!
		if (total > params.getMATCHMAXHITS())
			total = params.getMATCHMAXHITS();
		MatchingUtils.matchedDoc[] hits = new MatchingUtils.matchedDoc[total];
		while (i < total && tmpHits.length > 0) {
			// agregar tmpHits a hits
			for (int t = 0; t < tmpHits.length; t++) {
				hits[i] = new MatchingUtils.matchedDoc(searcher.doc(tmpHits[t].doc), tmpHits[t]);
				i++;
				if (i >= params.getMATCHMAXHITS() || i >= total)
					break;
			}
			TopDocs tmpTd = searchAfter(tmpHits[tmpHits.length - 1], searcher, query, total);
			tmpHits = tmpTd.scoreDocs;
		}
		// th=hits.length;
		return hits;
	}

	public matchedDoc[] getHits(Query query, IndexSearcher searcher) throws Exception {
		MatchingUtils.matchedDoc[] hits = new MatchingUtils.matchedDoc[0];
		if (query.toString().trim().equals(""))
			return hits;
		try {
			hits = collectHits(searcher, query);
			qHitsLastSearch = hits.length;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.debug(query.toString());
			throw e;
		}
		return hits;
	}

	public float getCoincidenceLevel(String busquedaOriginal, String datoEncontrado) throws Exception {
		float ret = 0;
		String buscado = "";
		String encontrado = "";
		busquedaOriginal = busquedaOriginal + " ";
		datoEncontrado = datoEncontrado + " ";
		if (Utils.isNumeric(busquedaOriginal)) {
			// quito caracteres no alfanumericos y los reemplazo por vacio
			buscado = busquedaOriginal.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "");
		} else {
			// quito caracteres no alfanumericos excepto el . y el & y los reemplazo por
			// espacio
			buscado = busquedaOriginal.replaceAll("[!-%'--/-/:-@\\[-`{-~¨¡°]", " ");
			buscado = removeStopWords(buscado.toLowerCase());
		}
		buscado = buscado.trim();

		if (Utils.isNumeric(datoEncontrado)) {
			// quito caracteres no alfanumericos y los reemplazo por vacio
			encontrado = datoEncontrado.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "");
		} else {
			// quito caracteres no alfanumericos excepto el . y el & y los reemplazo por
			// espacio
			encontrado = datoEncontrado.replaceAll("[!-%'--/-/:-@\\[-`{-~¨¡°]", " ");
			encontrado = removeStopWords(encontrado.toLowerCase());
		}
		encontrado = encontrado.trim();
		ret = compare(buscado, encontrado);

		return ret;
	}

	public int ajustarMaxDistance(String a, int max) {
		// la profundidad no puede ser mayor a la mitad de letras de la palabra
		// para una letra siempre da 0
		int ret = max;
		int l2 = (int) Math.floor(a.length() / 2);
		if (max >= l2) {
			ret = l2;
		}
		return ret;
	}

	public float compare(String a, String b) throws Exception {
		float ret = 0;
		if (a.length() == 0 || b.length() == 0)
			return ret;
		// reemplazo los & y . y los reemplazo por espacio para separar palabras
		a = a.replaceAll("[& .]", " ").trim();
		b = b.replaceAll("[& .]", " ").trim();
		a = a.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "").toLowerCase(); // remuve caracteres no alfanumericos
		b = b.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "").toLowerCase(); // remuve caracteres no alfanumericos
		while (b.indexOf("  ") >= 0)
			b = b.replaceAll("  ", " "); // remueve espacios repetidos
		while (a.indexOf("  ") >= 0)
			a = a.replaceAll("  ", " "); // remueve espacios repetidos
		a = Utils.removerAcentos(a); // reemplaza letras con acento por la misma lerta sin el acento
		b = Utils.removerAcentos(b); // reemplaza letras con acento por la misma lerta sin el acento

		String atrim = a.replaceAll(" ", "");
		String btrim = b.replaceAll(" ", "");
		if (atrim.equals(btrim)) {// sigla con y sin puntos o espacios
			return 1;
		}

		String[] asplit = a.split(" ");
		String[] bsplit = b.split(" ");
		int asplitlen = asplit.length;
		int bsplitlen = bsplit.length;

		if (bsplitlen < asplitlen) { // invierto de lugar para que b tenga mas terminos que a
			String tmp = b;
			b = a;
			a = tmp;
		}

		/*
		 * Esto habria que habilitarlo porque estaba puntuando con 0.25 coincidencias
		 * del tipo u c r / union civica radical Sin embargo al habilitarlo generar�
		 * muchas mas coincidencias como por ejemplo ulises carlos ramirez uriel cesar
		 * rojo ursula cecilia ramos union cervecera riojana
		 */
		/*
		 * if(isSiglaCorrespondiente(asplit,bsplit)){//una palabra es la sigla
		 * correspondiente a las otras return 1; }
		 */

		StringTokenizer st1 = new StringTokenizer(b, " ");
		float letrasA = (a.replaceAll(" ", "")).length();
		float letrasB = (b.replaceAll(" ", "")).length();

		float matches = 0;
		if (letrasA == 0 || letrasB == 0)
			return ret;

		// boolean todasIniciales=false;
		// if(a.trim().split(" ").length==a.replaceAll("
		// ","").length())todasIniciales=true;
		// if(b.trim().split(" ").length==b.replaceAll("
		// ","").length())todasIniciales=true;

		Vector restantes = new Vector();

		while (st1.hasMoreElements()) { // palabras completas
			String token = st1.nextToken().trim();
			if (token.length() == 0)
				continue;
			if ((" " + a + " ").indexOf(" " + token + " ") >= 0) { // la cadena contiene el texto exacto
				matches = matches + token.length();
				a = (" " + a + " ").replaceFirst(" " + token + " ", " ").trim(); // como esta palabra ya la encontro, la
																					// quito para que no la encuentre de
																					// nuevo
				b = (" " + b + " ").replaceFirst(" " + token + " ", " ").trim();
			}
		}

		StringTokenizer st2 = new StringTokenizer(b, " ");
		while (st2.hasMoreElements()) { // palabras parecidas
			String token = st2.nextToken().trim();
			int maxdistance = ajustarMaxDistance(token, params.getMATCHDEPTH());

			if (token.length() == 1) {// si es una sola letra solo se acepta que sea igual. Deberia haber salido en
										// palabras completas
				restantes.add(token);
				continue;
			}

			String f = StringSimilarity.find(a, token, maxdistance);
			if (!"".equals(f)) {
				float len = token.length();
				if (f.length() < len)
					len = f.length();
				matches = matches + len * 0.75f;
				a = (" " + a + " ").replaceFirst(" " + f + " ", " ").trim(); // como esta palabra ya la encontro, la
																				// quito para que no la encuentre de
																				// nuevo
				b = (" " + b + " ").replaceFirst(" " + token + " ", " ").trim();
			} else {
				restantes.add(token);
			}
		}

		for (int j = 0; j < restantes.size(); j++) { // palabras vs iniciales
			float puntajeDeInicial = 0.25f;

			// esto es para que casos como PARTIDUL SOCIAL DEMOCRAT / p. s. d. tengan un
			// buen nivel de coincidencia (antes daba 25%)
			// sin embargo genera una enorme cantidad de casos como a. b. /aurelio
			// born/amelia baltar/aurora briones/angelica baez, etc
			// if(todasIniciales) puntajeDeInicial=0.90f;
			String token = (String) restantes.elementAt(j);
			String inicial = token.substring(0, 1);
			if (!Utils.isNumeric(token.substring(0, 1))) { // no se trata de un nro (de documento por ejemplo)
				if (token.length() == 1) { // token tiene la inicial
					int pos = (" " + a + " ").indexOf(" " + token);
					if (pos >= 0) {
						String palabra = a.substring(pos, (a + " ").indexOf(" ", pos + 1));
						int len = palabra.length();
						matches = matches + len * puntajeDeInicial;
						a = (" " + a + " ").replaceFirst(" " + palabra + " ", " ").trim(); // como esta palabra ya la
																							// encontro, la quito para
																							// que no la encuentre de
																							// nuevo
						b = (" " + b + " ").replaceFirst(" " + token + " ", " ").trim();
					}
				} else if ((" " + a + " ").indexOf(" " + inicial + " ") >= 0) {// token tiene la palabra completa
					matches = matches + token.length() * puntajeDeInicial;
					a = (" " + a + " ").replaceFirst(" " + inicial + " ", " ").trim(); // como esta palabra ya la
																						// encontro, la quito para que
																						// no la encuentre de nuevo
					b = (" " + b + " ").replaceFirst(" " + token + " ", " ").trim();
				} else {// token no es una inicial, es una palabra y en "a" no hay una inicial que
						// corresponda a esa palabra
						// intento ver si corresponde a las primeras letras de otra palabra
					int pos = (" " + a + " ").indexOf(" " + token);// en token tengo las primeras letras
					if (pos >= 0) {
						String palabraentera = a.substring(pos, (a + " ").indexOf(" ", pos + 1));
						matches = matches + palabraentera.length() * puntajeDeInicial;
						a = (" " + a + " ").replaceFirst(" " + palabraentera + " ", " ").trim(); // como esta palabra ya
																									// la encontro, la
																									// quito para que no
																									// la encuentre de
																									// nuevo
						b = (" " + b + " ").replaceFirst(" " + token + " ", " ").trim();
					} else {// en token tengo la palabra entera
						StringTokenizer st3 = new StringTokenizer(a, " ");
						while (st3.hasMoreElements()) {
							String parte = st3.nextToken().trim();
							if (token.startsWith(parte)) {
								matches = matches + token.length() * 0.25f;
								a = (" " + a + " ").replaceFirst(" " + parte + " ", " ").trim(); // como esta palabra ya
																									// la encontro, la
																									// quito para que no
																									// la encuentre de
																									// nuevo
								b = (" " + b + " ").replaceFirst(" " + token + " ", " ").trim();
							}
						}
					}
				}
			}
		}

		if (a.length() > 0 && b.length() > 0) {// penalizar si sobran palabras diferentes en ambos nombres
			// no hubiera coincidido en busqueda directa ni inversa
			matches = 0;
		}

		float letras = letrasA;
		if (letrasB > letras)
			letras = letrasB;

		ret = matches / letras;
		return ret;
	}

	public boolean isSiglaCorrespondiente(String[] a, String[] b) {
		boolean ret = false;
		if (a.length == 1 && b.length == 1)
			return false;
		if (a.length == 1) {// palabra sigla
			if (a[0].length() == b.length && b.length > 0) {
				ret = true;
				for (int i = 0; i < a[0].length(); i++) {
					if (a[0].charAt(i) != b[i].charAt(0)) {
						ret = false;
						break;
					}
				}
			}
		} else if (b.length == 1) {// palabra sigla
			if (b[0].length() == a.length && a.length > 0) {
				ret = true;
				for (int i = 0; i < b[0].length(); i++) {
					if (b[0].charAt(i) != a[i].charAt(0)) {
						ret = false;
						break;
					}
				}
			}
		} else if (a.length != b.length) {
			return false;
		} else {// a y b tienen la misma cantidad de palabras
			ret = true;
			for (int i = 0; i < a.length; i++) {
				if (b[i].length() > 1 && a[i].length() > 1) {
					ret = false;
					break;
				} else if (b[i].charAt(0) != a[i].charAt(0)) {
					ret = false;
					break;
				}
			}
		}

		return ret;
	}

	private Query parseQuery(String sQuery) throws Exception {
		Query query = new QueryParser("", an).parse(sQuery);
		return query;
	}

	private boolean isSearchInDoc(String doc, String search) {
		String FIELD = "texto";
		if (!docAnt.equals(doc)) { // creo uno nuevo solamente si no me sirve el anterior
			mi = new MemoryIndex();
			mi.addField(FIELD, doc, an);
			docAnt = doc;
		}
		Query query = getDocQuery(FIELD, search);
		return isInDoc(mi, query);
	}

	private Query getDocQuery(String FIELD, String search) {
		String sQuery = "+" + FIELD + ":" + search + "~";
		Query query = null;
		try {
			query = parseQuery(sQuery);
		} catch (Exception e) {
			logger.error(sQuery);
			logger.error(e.toString());
		}
		return query;
	}

	private boolean isInDoc(MemoryIndex mi, Query query) {
		float score = mi.search(query);
		if (score > 0.0f) {
			return true;
		} else {
			return false;
		}
	}

	public boolean contains(String doc, String search) throws Exception {
		int tolerancia = 2;
		if (search.trim().equals("") || doc.trim().equals(""))
			return false;

		StringTokenizer st = new StringTokenizer(doc + " ", " ");
		StringBuffer sb = new StringBuffer();
		while (st.hasMoreElements()) {
			String elem = (String) st.nextElement();
			if (elem.trim().length() + tolerancia < search.length()) {
				continue; // descarto ese t�rmino porque nunca va a coincidir con search por ser mas chico
			}
			sb.append(elem + " ");
		}
		String newDoc = sb.toString();
		if (newDoc.trim().equals(""))
			return false;
		return isSearchInDoc(doc, search);
	}

	public BooleanQuery getQuery(SearchQuery search) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();

		BooleanQuery textquery = getQuery("text", search.getText(), true);
		bqb.add(textquery, MUST);

		addTermsFromFilters(bqb, "cat", search.getIncludedCategories(), MUST);
		addTermsFromFilters(bqb, "sub", search.getIncludedSubCategories(), MUST);
		addTermsFromFilters(bqb, "kwd", search.getIncludedKeywords(), MUST);
		addTermsFromFilters(bqb, "cntry", search.getIncludedCountries(), MUST);

		addTermsFromFilters(bqb, "cat", search.getExcludedCategories(), MUST_NOT);
		addTermsFromFilters(bqb, "sub", search.getExcludedSubCategories(), MUST_NOT);
		addTermsFromFilters(bqb, "kwd", search.getExcludedKeywords(), MUST_NOT);
		addTermsFromFilters(bqb, "cntry", search.getExcludedCountries(), MUST_NOT);

		if (!search.isSearchAliases()) {
			bqb.add(new TermQuery(new Term("tipo", "als")), MUST_NOT);
		}

		// B1: filtro por lista. Si no se especifican listas, se busca en todas (comportamiento previo).
		addListFilter(bqb, search.getListIds());

		if (search.getUpdatedAfter() != null) {
			Query lpQuery = LongPoint.newRangeQuery("upd", search.getUpdatedAfter().getTime(), FUTUREDATE);
			Query dvQuery = SortedNumericDocValuesField.newSlowRangeQuery("upd", search.getUpdatedAfter().getTime(),
					FUTUREDATE);
			Query query = new IndexOrDocValuesQuery(lpQuery, dvQuery);

			bqb.add(query, MUST);

		}
		if (search.getIncludeIfDeprecatedAfter() == null) {
			bqb.add(LongPoint.newRangeQuery("dep", 0, 0), MUST);
		} else {
			bqb.add(LongPoint.newRangeQuery("dep", search.getIncludeIfDeprecatedAfter().getTime(), FUTUREDATE), SHOULD);
			bqb.add(LongPoint.newRangeQuery("dep", 0, 0), SHOULD);
		}
		return bqb.build();
	}

	private void addTermsFromFilters(BooleanQuery.Builder bqb, String field, List<String> filters, Occur occur) {
		if (filters == null)
			return;
		BooleanQuery.Builder bqb2 = new BooleanQuery.Builder();
		filters.forEach(filter -> {
			bqb2.add(new TermQuery(new Term(field, Utils.removeNonAlphaNumericAndSpaceAndLower(filter))), SHOULD);
		});
		bqb.add(bqb2.build(), occur);
	}

	// B1: filtra por listId. Se indexa como StringField sin analizar, asi que se matchea el valor
	// literal (sin normalizar a minusculas como addTermsFromFilters) para no romper codigos tipo "MO"/"SY".
	private void addListFilter(BooleanQuery.Builder bqb, List<String> listIds) {
		if (listIds == null || listIds.isEmpty())
			return;
		BooleanQuery.Builder bqb2 = new BooleanQuery.Builder();
		listIds.forEach(listId -> {
			bqb2.add(new TermQuery(new Term("listId", listId)), SHOULD);
		});
		bqb.add(bqb2.build(), MUST);
	}

	public BooleanQuery getQuery(String field, String data) {
		return getQuery(field, data, true);
	}

	public BooleanQuery getQuery(String field, String data, boolean useStopWords) {
		BooleanQuery.Builder ret = new BooleanQuery.Builder();
		if (data == null)
			return ret.build();
		if (data.equals(""))
			return ret.build();
		if (data.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "").equals(""))
			return ret.build();

		data = data.toLowerCase();
		if (Utils.isNumeric(data)) {
			// quito caracteres no alfanumericos y los reemplazo por vacio
			data = data.replaceAll("[!-/:-@\\[-`{-~¨¡°]", "").toLowerCase();
		} else {
			// quito caracteres no alfanumericos excepto el . y el & y los reemplazo por
			// espacio
			data = data.replaceAll("[!-%'--/-/:-@\\[-`{-~¨¡°]", " ").toLowerCase();
			// reemplaza letras con acento por la misma lerta sin el acento
			data = Utils.removerAcentos(data);

			if (useStopWords) {
				data = removeStopWords(data);
			}
		}

		if (data.replaceAll(" ", "").equals(""))
			return ret.build();

		StringTokenizer st1 = new StringTokenizer(data, " ");
		int minshouldmatch = getMinimumNumberShouldMatch(st1.countTokens());
		BooleanClause.Occur occur = getOccur(minshouldmatch);
		while (st1.hasMoreElements()) {
			String elem = (String) st1.nextElement();
			if (Utils.isNumeric(elem)) {
				Term term = new Term(field, elem.replaceAll("\\.", ""));
				FuzzyQuery fq = new FuzzyQuery(term, params.getMATCHDEPTH(), params.getMATCHPRLEN(), MAXEXPANSIONS,
						true);
				ret.add(fq, occur);
			}

			else if (elem.length() == 1) {// es una letra suelta o es una inicial
				elem = elem.replaceAll("\\.", "");// Si es un punto lo quito
				elem = elem.replaceAll("&", "");// Si es un & lo quito para que ni lo busque
				if (elem.length() == 1) {// todavia tiene un caracter
					BooleanQuery.Builder bq = new BooleanQuery.Builder();
					TermQuery tq = new TermQuery(new Term(field, elem));
					PrefixQuery pq = new PrefixQuery(new Term(field, elem));
					bq.add(tq, BooleanClause.Occur.SHOULD);
					bq.add(pq, BooleanClause.Occur.SHOULD);
					ret.add(bq.build(), occur);
				}
			} else if (elem.endsWith(".") && (elem.indexOf(".") + 1) >= elem.length()) {// termina en punto (y no hay
																						// otro punto), lo reemplazo por
																						// asterisco (prefix query)
				elem = elem.replaceAll("\\.", ""); // quito el punto
				BooleanQuery.Builder bq = new BooleanQuery.Builder();
				TermQuery tq = new TermQuery(new Term(field, elem));
				PrefixQuery pq = new PrefixQuery(new Term(field, elem));
				bq.add(tq, BooleanClause.Occur.SHOULD);
				bq.add(pq, BooleanClause.Occur.SHOULD);
				ret.add(bq.build(), occur);
			} else {
				elem = elem.replace(".", "*"); // reemplazo puntos por asterisco
				if (elem.startsWith("*"))
					elem = elem.substring(1); // el asterisco no puede ser el primer caracter
				if (elem.indexOf("*") >= 0) {
					Term term = new Term(field, elem);
					WildcardQuery wq = new WildcardQuery(term);
					ret.add(wq, occur);

				} else if (elem.indexOf("&") >= 0) { // si tiene un & hago dos posibilidades: con o sin &
					BooleanQuery.Builder bq = new BooleanQuery.Builder();
					TermQuery tq1 = new TermQuery(new Term(field, elem)); // com &
					BooleanQuery.Builder bq2 = new BooleanQuery.Builder();
					StringTokenizer ampt = new StringTokenizer(elem, "&"); // sin &
					while (ampt.hasMoreElements()) {
						TermQuery tq21 = new TermQuery(new Term(field, (String) ampt.nextElement()));
						bq2.add(tq21, BooleanClause.Occur.MUST);
					}
					bq.add(tq1, BooleanClause.Occur.SHOULD);
					bq.add(bq2.build(), BooleanClause.Occur.SHOULD);
					ret.add(bq.build(), occur);

				} else {
					BooleanQuery.Builder bq = new BooleanQuery.Builder();
					FuzzyQuery fq1 = new FuzzyQuery(new Term(field, elem), params.getMATCHDEPTH(),
							params.getMATCHPRLEN(),
							MAXEXPANSIONS, true); // palabras similares
					TermQuery tq2 = new TermQuery(new Term(field, separarConPuntos(elem))); // palabras similares
					bq.add(fq1, BooleanClause.Occur.SHOULD);
					bq.add(tq2, BooleanClause.Occur.SHOULD);

					ret.add(bq.build(), occur);

				}
			}
		}
		if (minshouldmatch > 0)
			ret.setMinimumNumberShouldMatch(minshouldmatch);
		return ret.build();

	}

	protected BooleanClause.Occur getOccur(int minshouldmatch) {
		if (minshouldmatch == 0) {
			return MUST;
		} else {
			return SHOULD;
		}
	}

	protected int getMinimumNumberShouldMatch(int elemCount) {
		int ret = 0;
		if ("".equals(params.getMATCHMINTO())) {
			ret = 0;
		} else if (elemCount >= MAXTO) {
			ret = terminosObligatorios[MAXTO - 1];
		} else {
			ret = terminosObligatorios[elemCount];
		}
		if (ret == elemCount)
			ret = 0;// si todos los terminos son obligatorios ni siquiera pongo el parametro
					// minimumNumberShouldmatch
		return ret;
	}

	public Hashtable<String, Match> review(SearchedDoc sd, MatchingUtils.matchedDoc[] hits, Query query)
			throws Exception {
		Hashtable<String, Match> matches = new Hashtable<String, Match>();
		if (hits == null)
			return matches;
		for (int iHitNum = 0; iHitNum < hits.length; iHitNum++) {
			Document doc = hits[iHitNum].doc;
			String ui = doc.get("ui");
			String id = doc.get("id");
			Match match = new Match();
			match.setId(id);
			match.setBestScore(hits[iHitNum].sd.score);
			match.setMatchType(doc.get("tipo"));
			match.setQuery(query.toString());
			match.setUi(ui);
			setCoincidence(match, sd, doc);
			// chooseBetterMatch(matches, match, ui);
			matches.put(id, match);
		}
		return matches;
	}

	// REVISAR!! estimo que este método ya no es necesario pero no se deberían
	// agrupar los resultados por ui sino por id
	/*
	 * private void chooseBetterMatch(Hashtable matches, Match match, String ui) {
	 * Match prevmatch = (Match) matches.get(ui);
	 * if (prevmatch == null) {// este ui no estaba
	 * matches.put(ui, match);
	 * } else if (match.getBestCoincidence() > prevmatch.getBestCoincidence()) { //
	 * habia otro pero de menor
	 * // coincidencia
	 * matches.put(ui, match);
	 * } else if (match.getBestCoincidence() == prevmatch.getBestCoincidence()) { //
	 * igual coincidencia pero campo m�s
	 * // importante
	 * if (match.getMatchType().equals("SSNS") ||
	 * match.getMatchType().equals("PASSPORTS")
	 * || (match.getMatchType().equals("RECORDS") &&
	 * prevmatch.getMatchType().equals("ALIASES"))
	 * || (match.getMatchType().equals("RECORDS") &&
	 * prevmatch.getMatchType().equals("ALTSPELLINGS"))
	 * || (match.getMatchType().equals("ALTSPELLINGS") &&
	 * prevmatch.getMatchType().equals("ALIASES"))) {
	 * matches.put(ui, match);
	 * }
	 * } else {// nada
	 * }
	 * }
	 */

	private void setCoincidence(Match match, SearchedDoc sd, Document doc) throws Exception {
		if (sd.isSimpleSearch()) {
			match.setSearch(sd.getDenominacion());
			match.setCoincidence(Objects.toString(doc.get("text"), ""));
			match.setBestCoincidence(getCoincidenceLevel(match.getSearch(), match.getCoincidence()));
		} else {/// REVISAR!!!!
			if (match.getMatchType().equals("RECORDS") || match.getMatchType().equals("ALIASES")
					|| match.getMatchType().equals("ALTSPELLINGS")) {
				match.setSearch(sd.getDenominacion());
				match.setCoincidence(Objects.toString(doc.get("TEXT"), ""));
				match.setBestCoincidence(getCoincidenceLevel(match.getSearch(), match.getCoincidence()));
			} else if (match.getMatchType().equals("SSNS") || match.getMatchType().equals("PASSPORTS")) {
				match.setCoincidence(Objects.toString(doc.get("TEXT"), ""));
				float ret = 0;
				float tmp = 0;
				ret = getCoincidenceLevel(sd.d1, match.getCoincidence());
				match.setSearch(sd.d1);

				tmp = getCoincidenceLevel(sd.d2, match.getCoincidence());
				if (tmp > ret) {
					ret = tmp;
					match.setSearch(sd.d2);
				}
				tmp = getCoincidenceLevel(sd.d3, match.getCoincidence());
				if (tmp > ret) {
					ret = tmp;
					match.setSearch(sd.d3);
				}
				tmp = getCoincidenceLevel(sd.d4, match.getCoincidence());
				if (tmp > ret) {
					ret = tmp;
					match.setSearch(sd.d4);
				}
				match.setBestCoincidence(ret);
			}
		}
	}

	public String separarConPuntos(String s) {
		String ret = "";
		for (int i = 0; i < s.length(); i++) {
			String punto = ".";
			if (i == s.length() - 1)
				punto = "";
			ret = ret + s.charAt(i) + punto;
		}
		return ret;
	}

	public String removeStopWords(String text) {
		for (Object stopWord : stopWords) {
			String sw = " " + stopWord.toString().trim() + " ";
			text = " " + text.trim() + " ";
			if (text.contains(sw)) {
				text = text.replace(sw, " ");
			}
		}

		String ret = "";
		StringTokenizer st = new StringTokenizer(text, " ");
		while (st.hasMoreElements()) {
			String elem = st.nextToken();
			ret = ret + elem + " ";
		}

		return ret.trim();
	}

	public class matchedDoc {
		public Document doc = null;
		public ScoreDoc sd = null;

		public matchedDoc(Document doc, ScoreDoc sd) {
			this.doc = doc;
			this.sd = sd;
		}
	}
}

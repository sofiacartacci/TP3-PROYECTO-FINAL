package com.systech.ms.list.ingestor.task.chunks;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.systech.ms.core.model.list.Reportado;
import com.systech.ms.list.ingestor.ProviderHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FastloadItemProcessor implements ItemProcessor<CSVRecord, Reportado> {
	boolean inited=false;
	int recordNumber=0;
	
	DecimalFormat df=new DecimalFormat("0000000000");
	@Autowired ProviderHandler providerHandler;
	
	@Value("${mapping.id}")
	String id;
	
	@Value("${mapping.fields}")
	String mapping;
	
	@Value("${mapping.separator}")
	String SEPARADOR;

	
	List <String>listaCampos;
	Map <String,String>campos=new HashMap();
	Map <String,SimpleDateFormat>formatos=new HashMap();
	
	@Value("${mapping.null}")
	String nullString;
	
	@Value("${mapping.errors.ignore}")
	boolean ignoreErrors=false;
	
	private final static SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
	
	int errorCount=0;
	void init()throws Exception{
		recordNumber=1;//1 es el header
		inited=true;
		errorCount=0;
		mapping=mapping.replaceAll("\t","");
		mapping=mapping.replaceAll("\r","");
		mapping=mapping.replaceAll("\n","");
		
		listaCampos=Arrays.asList(mapping.split(","));
		
		for(String c:listaCampos) {
			c=c.trim();
			String [] alias=c.split(":");
			campos.put(alias[0],alias[1]);
			if(alias.length==3) {
				formatos.put(alias[0], new SimpleDateFormat(alias[2]));
			}else if(alias.length==4) {
				formatos.put(alias[0], new SimpleDateFormat(alias[2],new Locale(alias[3])));
			}
		}
		
		
	}
	private String get(CSVRecord csv, String nombreCampoModelo)throws Exception {
		String nombreCampoCsv=campos.get(nombreCampoModelo.toLowerCase());
		if(nombreCampoCsv==null) {
			return "";
		}else {
			try {
				return csv.get(nombreCampoCsv);
			}catch(Exception e) {
				log.error("Error parsing record number {}. Record: {}", recordNumber ,csv);
				throw e;
			}
		}
	}
	
	@Override
	public Reportado process(CSVRecord m) throws Exception {
		if(!inited)init();
		Reportado r=new Reportado();
		recordNumber++;
		
		r.setAge(get(m,"age"));
		r.setAge_date(get(m,"age_date"));
		r.setAls(getList(get(m,"aliases")));
		r.setAss(getList(get(m,"alternative_spellings")));
		r.setCategory(get(m,"category"));
		r.setCompanies(getList(get(m,"companies")));
		r.setCountries(getList(get(m,"countries")));
		r.setDeceased(get(m,"deceased"));
		r.setDeprecated(getDate(m,"deprecated_since"));
		r.setDob(get(m,"dob"));
		r.setE_i(get(m,"e_i"));
		r.setEditor(get(m,"editor"));
		
		Date entered=getDate(m,"entered");
		Date updated=getDate(m,"updated");
		
		r.setEntered(entered);
		r.setUpdated(updated!=null?updated:entered);
		
		r.setExternal_sources(getList(get(m,"external_sources")));
		r.setFurther_information(get(m,"further_information"));
		r.setIds(getList(get(m,"ssn") + SEPARADOR + get(m,"passports") + SEPARADOR + get(m,"ids")));
		r.setKeywords(getList(get(m,"keywords")));
		r.setLinked_to(getList(get(m,"linked_to")));
		r.setLocations(getList(get(m,"locations")));
		r.setNam(get(m,"firstname") + " " + get(m,"lastname"));
		r.setPob(get(m,"pob"));
		r.setSocial_position(get(m,"social_position"));
		r.setSubcategory(get(m,"subcategory"));
		r.setTitle(get(m,"title"));
		r.setUi(getUi(get(m,"ui")));
		
		return r;
	}
	
	String getUi(String providerui) {
		return id + df.format(Long.parseLong(providerui));		
	}
	
	List<String> getList(String input){
		List<String> list = Stream.of(input.split(";", -1))
				  .collect(Collectors.toList());
		return list;
	}
	
	Date getDate(CSVRecord m, String campo)throws Exception {
		String valor=get(m,campo);
		if(valor==null||valor.trim().equals("")||valor.trim().equalsIgnoreCase(nullString)) {
			return null;
		}else {
			SimpleDateFormat sdfCampo=formatos.get(campo);
			try {
				if(sdfCampo!=null) {
					return sdfCampo.parse(valor);
				}else {
					return sdf.parse(valor);
				}
			}catch(Exception e) {
				SimpleDateFormat s=sdfCampo;
				if(sdfCampo==null) {
					s=sdf;
				}
				log.error(e.getMessage() + " in field {}. Format: {}, Record: {}",campo,s.toPattern(),m);
				
				if(ignoreErrors) {
					providerHandler.addError("Unparseable date");
					return null;
				}else {
					throw e;
				}
			}
		}
	}
	
	@AfterStep
	public void close() throws Exception {		
		inited = false;
	}
}

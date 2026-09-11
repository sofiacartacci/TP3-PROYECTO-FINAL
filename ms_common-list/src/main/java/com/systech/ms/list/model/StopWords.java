/*
 * StopWords.java
 *
 * Created on 16 de marzo de 2009, 13:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.systech.ms.list.model;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author ngeltman
 */
@Component
public class StopWords {
	Logger logger = LoggerFactory.getLogger(StopWords.class);
    protected Vector stopWords=new Vector();
    private static StopWords sw;
    
    @Value("${matcher.stopwords-file:}")
	private String stopWordsFile;
    
    /** Creates a new instance of StopWords */
    public static StopWords getInstance() throws Exception{
        if(sw==null){
            sw=new StopWords();
        }
        return sw;
    }
    private StopWords() {

    }
    
    @PostConstruct
    private void init() throws Exception {
        //String stopWordsFile=ProcessResources.getInstance().getProperty("app.StopWordsFile");
        if(stopWordsFile!=null){
            try{
                String line="";
                InputStream is =new FileInputStream(stopWordsFile);
                BufferedReader br=new BufferedReader(new InputStreamReader(is));
                while((line=br.readLine())!=null){
                    stopWords.add(line);
                }
                br.close();
                is.close();
            }
            catch (Exception e){
                logger.error("[[NO_SE_PUEDE_LEER_ARCHIVO_STOPWORDS]]", e);
                throw e;
            }
        }else{ //comportamiento anterior si no se definio archivo de stop words
            logger.info("[[SIN_ESPECIFICAR_ARCHIVO_STOPWORDS]]");
            stopWords.add(" sa ");
            stopWords.add(" srl ");
            stopWords.add(" inc ");
            stopWords.add(" saic ");
            stopWords.add(" cia ");
            stopWords.add(" ltda ");
            stopWords.add(" saciif ");
            stopWords.add(" sacif ");
            stopWords.add(" ltd ");
            stopWords.add(" sacifa ");
            stopWords.add(" asoc ");
            stopWords.add(" de ");
            stopWords.add(" y ");

            stopWords.add(" s a ");
            stopWords.add(" s r l ");
            stopWords.add(" i n c ");
            stopWords.add(" s a i c ");
            stopWords.add(" c i a ");
            stopWords.add(" l t d a ");
            stopWords.add(" s a c i i f ");
            stopWords.add(" s a c i f ");
            stopWords.add(" l t d ");
            stopWords.add(" s a c i f a ");
            stopWords.add(" a s o c ");

            stopWords.add(" s.a. ");
            stopWords.add(" s.r.l. ");
            stopWords.add(" i.n.c. ");
            stopWords.add(" s.a.i.c. ");
            stopWords.add(" c.i.a. ");
            stopWords.add(" l.t.d.a. ");
            stopWords.add(" s.a.c.i.i.f. ");
            stopWords.add(" s.a.c.i.f. ");
            stopWords.add(" l.t.d. ");
            stopWords.add(" s.a.c.i.f.a. ");
            stopWords.add(" a.s.o.c. ");

            stopWords.add(" y ");
            stopWords.add(",");
            stopWords.add("-");
            stopWords.add("/");
            stopWords.add("\\.");
        }    	
    }
    public Vector getStopWords() {
        for(int i=0;i<stopWords.size();i++){
            String sw=(String)stopWords.elementAt(i);
            if(sw.indexOf("\\")==-1){
                sw=sw.replace(".","\\.");
                sw=sw.replace("-","\\-");
            }

            if(sw.equals("\\\\."))sw="\\.";//convierto \\. en \.
            
            stopWords.setElementAt(sw,i);
        }
        
        return stopWords;
    }

    public Vector getStopWordsToCompare() {
        Vector ret=new Vector();
        for(int i=0;i<stopWords.size();i++){
            String sw=(String)stopWords.elementAt(i);
            ret.add(sw.replace("\\", ""));
        }
        return ret;
    }
    
    public String [] getArray(){
        Vector tmp=new Vector(stopWords);
        
        int c=tmp.size();
        String [] arr=new String [c];
        for(int i=0;i<c;i++){
            arr[i]=((String)tmp.elementAt(i)).trim(); //se quitan los espacios (para el analyzer)
        }
        return arr;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.systech.ms.list.model;

import java.util.Date;

/**
 *
 * @author ngeltman
 */
public class SearchedDoc {
    public String id="";
        public String tp="";
        public String pn="";
        public String sn="";
        public String ap="";
        public String ac="";
        public String rz="";
        public String d1="";
        public String d2="";
        public String d3="";
        public String d4="";
        public String nrotrx="";
        public String nrooperacion="";
        public Date f_proceso=null;
        public String simplesearch="";
        public SearchedDoc(){
        }
        
        public boolean isSimpleSearch(){
            if(simplesearch.equals("")){
                return false;
            }else{
                return true;
            }
        }
        public String getDenominacion(){
            String ret="";
            if(isSimpleSearch()){
                return simplesearch.trim();
            }
            if(tp.equals("F")){
                    ret=pn + " " + sn+ " " + ap+" " + ac;
            }
            else if(tp.equals("J")){
                ret=rz;
            }else{
                ret=pn + " " + sn+ " " + ap+" " + ac + " " + rz;
            }
            return ret.trim();
        }
}

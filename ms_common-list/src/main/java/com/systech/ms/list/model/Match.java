/*
 * Match.java
 *
 * Created on 2 de marzo de 2009, 11:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.systech.ms.list.model;
import java.util.*;
/**
 *
 * @author ngeltman
 */
public class Match {
    private float bestScore=0;
    private float bestCoincidence=0;
    private String matchType="";
    private String search="";
    private String query="";
    private String coincidence="";
    private String id="";
    private String ui="";
    /** Creates a new instance of Match */
    public Match() {
    }
    
    public void setBestScore(float f){bestScore=f;}
    public void setBestCoincidence(float f){bestCoincidence=f;}
    public void setMatchType(String s){matchType=s;}
    public void setSearch(String s){search=s;}
    public void setQuery(String s){query=s;}
    public void setCoincidence(String s){coincidence=s;}
    public void setId(String id){this.id=id;}
    public void setUi(String ui){this.ui=ui;}
    
    public float getBestScore(){return bestScore;}
    public float getBestCoincidence(){return bestCoincidence;}
    public String getMatchType(){return matchType;}
    public String getSearch(){return search;}
    public String getQuery(){return query;}
    public String getCoincidence(){return coincidence;}
    public String getId(){return id;}
    public String getUi(){return ui;}
}

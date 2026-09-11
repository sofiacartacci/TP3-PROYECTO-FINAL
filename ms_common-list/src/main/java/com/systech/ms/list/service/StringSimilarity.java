/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.systech.ms.list.service;

import java.sql.Array;
import java.util.Arrays;
import java.util.StringTokenizer;

public class StringSimilarity {
 
    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     */
    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        /* // If you have StringUtils, you can use it to calculate the edit distance:
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                                                             (double) longerLength; */
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
 
    }
    
    public static double phraseSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        /* // If you have StringUtils, you can use it to calculate the edit distance:
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                                                             (double) longerLength; */
        return (longerLength - phraseDistance(longer, shorter)) / (double) longerLength;
 
    }
    
    public static int phraseDistance(String s1, String s2) {
        if (s1.length() == 0&&s2.length()==0) { return 0; /* both strings are zero length */ }
        
        //Ordeno las palabras de cada frase para que la distancia se calcule correctamente y no dependa del orden en que estan escritas
        String[] a=s1.split(" ");
        Arrays.sort(a);
        StringBuilder builder1 = new StringBuilder();
        for(String s : a) {
            builder1.append(s + " ");
        }
        s1 = builder1.toString();
        
        String[] b=s2.split(" ");
        Arrays.sort(b);
        StringBuilder builder2 = new StringBuilder();
        for(String s : b) {
            builder2.append(s + " ");
        }
        s2 = builder2.toString();
        
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        
        return editDistance(longer, shorter);
 
    }
    public static int distance(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 0; /* both strings are zero length */ }
        
        return editDistance(longer, shorter);
 
    }
    public static String find(String frase, String palabra, int maxdistance){
        StringTokenizer st=new StringTokenizer(frase," ");
        String candidata="";
        int mindistance=999999;
        while(st.hasMoreTokens()){
            String t=st.nextToken();
            int d=distance(t,palabra);
            if(d==0){//palabras iguales
                return t;
            }
            if(d<=maxdistance&&d<mindistance){
                candidata=t;
                mindistance=d;
            }
        }
        return candidata;
    }
 
    // Example implementation of the Levenshtein Edit Distance
    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
 
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}

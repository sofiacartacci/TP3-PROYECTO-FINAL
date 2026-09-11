package com.systech.ms.list.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class Utils {
	/**
     * Valida que una cadena sea un numero entero o con decimales
     *
     * @param cadena la cadena a validar
     * @return TRUE si y solo si es parseable como entero o decimal, de lo
     * contrario FALSE
     */
    public static boolean isNumeric(String cadena){
	        if(isBlankOrNull(cadena)) return false;

	        String primeraLetra=cadena.substring(0,1);

	        try {
	                Integer.parseInt(primeraLetra);
	        } catch (NumberFormatException nfe){
	                return false;
	        }

	        cadena=cadena.replaceAll("\\.","");
	        cadena=cadena.replaceAll("\\,","");
	        try {
	                Double.parseDouble(cadena);
	                return true;
	        } catch (NumberFormatException nfe){
	                return false;
	        }
    }
    
    /**
     * Valida que un String no sea null , vacio o 'null'.
     * 
     * @param cadena El String a validar
     * @return <strong>FALSE si es null o '' o 'null'.</strong> De lo contrario TRUE.
     */
    public static boolean isBlankOrNull(String cadena) {
        return cadena == null || cadena.trim().isEmpty() || cadena.trim().equalsIgnoreCase("null");
    }
    
    public static String removerAcentos(String input) {
        // Descomposicion canonica
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Nos quedamos unicamente con los caracteres ASCII
        Pattern pattern = Pattern.compile("\\P{ASCII}");
        return pattern.matcher(normalized).replaceAll("");
    }
    
    
    public static String removeNonAlphaNumericAndSpaceAndLower(String data) {
    	// quito caracteres no alfanumericos y los reemplazo por vacio
    	return data.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}

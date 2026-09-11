package com.systech.ms.list.service;

import java.util.Comparator;

public class ComparadorLongitud implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        if (o1.toString().length() > o2.toString().length()) {
            return -1;
        } else if (o1.toString().length() < o2.toString().length()) {
            return 1;
        } else {
            return 0;
        }
    }
}

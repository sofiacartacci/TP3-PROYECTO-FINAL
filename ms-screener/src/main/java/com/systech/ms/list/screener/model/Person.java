package com.systech.ms.list.screener.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person {
	String uid;
	String name;
	String id1,id2,id3,id4;
	Date dob;
	Date screened;
}

package com.systech.ms.list.biller.model;

import java.util.Date;

import lombok.Data;
@Data
public class UserTotalQuery {
	String user;
	Date from;
	Date to;	
}

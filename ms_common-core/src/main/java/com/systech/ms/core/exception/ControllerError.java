package com.systech.ms.core.exception;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ControllerError {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.sss")
	private Date date;
	private String msg;
	private Throwable cause;
	
	public ControllerError(String msg) {
		date=new Date();
		this.msg=msg;
	}
	public ControllerError(String msg, Throwable cause) {
		date=new Date();
		this.msg=msg;
		this.cause=cause;
	}
}
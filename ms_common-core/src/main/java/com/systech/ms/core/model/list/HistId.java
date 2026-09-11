package com.systech.ms.core.model.list;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistId {
	public String ui;
	public Date updateProcessed;
}

package com.systech.ms.list.ingestor.task.chunks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DownloadItemWriter implements ItemWriter<byte[]> {

	@Value("${mapping.working-dir}")
	private String workingDir;

	@Value("${mapping.id}")
	private String id;

	FileOutputStream fos;
	BufferedOutputStream bout;
	boolean inited = false;

	private void init() throws Exception {
		log.info("Initiating Writer");
		log.info(workingDir + File.separator + id + ".zip");
		fos = new java.io.FileOutputStream(workingDir + File.separator + id + ".zip");
		bout = new BufferedOutputStream(fos);
		inited = true;
	}

	@Override
	public void write(List<? extends byte[]> items) throws Exception {
		if (!inited) {
			init();
		}
		if (items != null) {
			Iterator it = items.iterator();
			while (it.hasNext()) {
				byte[] item = (byte[]) it.next();
				bout.write(item, 0, item.length);
			}
			bout.flush();
		} else {
			bout.close();
			fos.close();
		}

	}

	@AfterStep
	public void close() throws Exception {
		inited = false;
	}

}

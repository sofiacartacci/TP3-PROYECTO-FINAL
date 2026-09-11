package com.systech.ms.list.ingestor.task.chunks;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DownloadItemProcessor implements ItemProcessor<byte[],byte[]>{

	@Override
	public byte[] process(byte[] item) throws Exception {
		return item;
	}

}

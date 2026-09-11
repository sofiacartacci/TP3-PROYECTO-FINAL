
package com.systech.ms.list.ingestor.tasklets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Unzip implements Tasklet, StepExecutionListener {
	public final String prefijo = "/desctemp-";

	@Value("${mapping.working-dir}")
	private String workingDir;

	@Value("${mapping.id}")
	private String id;

	@Value("${mapping.file-name}")
	private String fileName;

	@Value("${unzip.buffersize:65536}")
	private int buffersize;

	@Value("${mapping.src-url}")
	String srcUrl;

	long totalBytes;
	long logcount = 1;
	String src, dst_folder;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		src = workingDir + File.separator + id + ".zip";
		dst_folder = workingDir + File.separator + id;
		log.info("Unzipping from " + src + " to " + dst_folder);
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		unzip();
		log.info("Total bytes escritos: " + totalBytes);
		return RepeatStatus.FINISHED;
	}

	protected void unzip() throws Exception {
		try {
			if (srcUrl.toLowerCase().endsWith(".gz") || srcUrl.toLowerCase().endsWith(".gzip")) {
				upzipGzip();
			} else {// zip
				upzipZip();
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			eliminarDesctemp();
			throw e;
		}
	}

	private void upzipZip() throws Exception {
		int BUFFER = buffersize;

		FileOutputStream fos = null;
		BufferedOutputStream dest = null;
		ZipInputStream zis = getZipStream();
		ZipEntry entry;
		File fileNew=new File(workingDir + File.separator + id + ".csv");
		if (fileNew.exists()) {
			fileNew.delete();
		}
		try {
			while ((entry = zis.getNextEntry()) != null) {
				if(!entry.getName().equals(fileName)) {
					continue;
				}
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				fos = new FileOutputStream(fileNew);

				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
					totalBytes += count;
					log();
				}
				dest.flush();
				dest.close();
			}
		} catch (Exception ex) {
			try {
				zis.close();
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			}
			;
			try {
				fos.close();
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			}
			;
			throw ex;
		}
		//
		try {
			zis.close();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		;
		try {
			if (fos != null)
				fos.close();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		;		
	}

	private void upzipGzip() throws Exception {
		int BUFFER = buffersize;

		File outdir = new File(dst_folder);
		// Open the gzip file
		String inFilename = src;
		GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(inFilename));
		// Open the output file
		OutputStream out = new FileOutputStream(getTempName());
		// Transfer bytes from the compressed file to the output file
		byte[] buf = new byte[BUFFER];
		int len;
		try {
			while ((len = gzipInputStream.read(buf)) > 0) {
				out.write(buf, 0, len);
				totalBytes += len;
				log();
			}
		} catch (Exception ex) {
			out.close();
			throw ex;
		}
		// Close the file and stream
		gzipInputStream.close();
		out.close();
		renombrarTempGZ();
	}

	public ZipInputStream getZipStream() throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(src);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		return zis;
	}


	private void renombrarTempGZ() throws Exception {
		File fileNew = new File(workingDir + File.separator + id + ".csv");
		File fileTemp = new File(getTempName());

		if (fileNew.exists()) {
			fileNew.delete();
		}
		// Rename file (or directory)
		fileTemp.renameTo(fileNew);
	}

	private void eliminarDesctemp() throws Exception {
		if (srcUrl.toLowerCase().endsWith(".gz") || srcUrl.toLowerCase().endsWith(".gzip")) {
			File fileTemp = new File(getTempName());
			if (fileTemp.exists()) {
				fileTemp.delete();
			}

		} else {
			ZipInputStream zis = getZipStream();
			ZipEntry entry;
			String name = "";
			File outdir = new File(dst_folder);

			while ((entry = zis.getNextEntry()) != null) {
				name = entry.getName();
				String nombreViejo = outdir + prefijo + name;
				File fileTemp = new File(nombreViejo);
				if (fileTemp.exists()) {
					fileTemp.delete();
				}
			}
			zis.close();
		}
	}

	public String getTempName() {
		return workingDir + File.separator + id + "_tmp";
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		stepExecution.getJobExecution().getExecutionContext().put("Total bytes", this.totalBytes);
		return ExitStatus.COMPLETED;
	}

	private void log() {
		if (logcount++ % 10000 == 0) {
			log.info("Total bytes escritos: " + totalBytes);
		}
	}

}

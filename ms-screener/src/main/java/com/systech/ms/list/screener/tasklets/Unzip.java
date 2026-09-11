
package com.systech.ms.list.screener.tasklets;

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

import com.systech.ms.list.screener.Constants;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Unzip implements Tasklet, StepExecutionListener {
	public final String prefijo = "/desctemp-";

	@Value("${screener.work.dir}/infiles")
	String infilesDir;

	@Value("${screener.work.dir}/work")
	private String workingDir;

	private String id;
	private String user;

	@Value("${unzip.buffersize:65536}")
	private int buffersize;

	long totalBytes;
	long logcount = 1;
	String src, dst, tmp;

	@Override
	public void beforeStep(StepExecution se) {
		id = se.getJobExecution().getJobParameters().getString(Constants.ID);
		user = se.getJobExecution().getJobParameters().getString(Constants.USER);
		src = infilesDir + File.separator +user + File.separator + id + ".gz";
		dst = workingDir + File.separator +user + File.separator + id + ".csv";
		tmp = workingDir + File.separator +user + File.separator + id + "_tmp";
		log.info("Unzipping from " + src + " to " + dst);
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		unzip();
		log.info("Total bytes escritos: " + totalBytes);
		return RepeatStatus.FINISHED;
	}

	protected void unzip() throws Exception {
		try {
			upzipGzip();
		} catch (Exception e) {
			log.error(e.getMessage());
			eliminarDesctemp();
			throw e;
		}
	}

	private void upzipGzip() throws Exception {
		int BUFFER = buffersize;

		File outdir = new File(tmp).getParentFile();
		if(!outdir.exists()) {
			outdir.mkdirs();
		}
		// Open the gzip file
		String inFilename = src;
		GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(inFilename));
		// Open the output file
		OutputStream out = new FileOutputStream(tmp);
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
		File fileNew = new File(dst);
		File fileTemp = new File(tmp);

		if (fileNew.exists()) {
			fileNew.delete();
		}
		fileTemp.renameTo(fileNew);
	}

	private void eliminarDesctemp() throws Exception {
		File fileTemp = new File(tmp);
		if (fileTemp.exists()) {
			fileTemp.delete();
		}
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

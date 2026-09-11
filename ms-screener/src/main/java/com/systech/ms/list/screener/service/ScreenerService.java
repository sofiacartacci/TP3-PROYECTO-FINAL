package com.systech.ms.list.screener.service;

import java.io.File;
import java.security.Principal;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.systech.ms.list.model.ScreenerParams;
import com.systech.ms.list.screener.model.ScreenerProcess;
import com.systech.ms.list.screener.repo.ScreenerProcessRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScreenerService {
	@Value("${screener.work.dir}/infiles")
	String infilesDir;

	@Value("${screener.work.dir}/outfiles")
	String outfilesDir;

	@Autowired
	ScreenerProcessRepo repo;

	@Autowired
	JobService jobService;

	public ScreenerProcess receiveFile(MultipartFile file, ScreenerParams params, Principal principal)
			throws Exception {
		String id = UUID.randomUUID().toString();

		File dstDir = new File(infilesDir + File.separator + principal.getName());
		if (!dstDir.exists()) {
			dstDir.mkdirs();
		}

		File dstFile = new File(dstDir.getAbsolutePath() + File.separator + id + ".gz");
		file.transferTo(dstFile);

		ScreenerProcess sp = new ScreenerProcess();
		sp.setId(id);
		sp.setUser(principal.getName());
		sp.setSize(dstFile.length());
		sp.setSubmitted(new Date());
		sp.setParams(params);

		repo.save(sp);
		processFile(sp);
		return sp;
	}

	public File getResult(String id, Principal principal) throws Exception {
		Optional<ScreenerProcess> sp = repo.findById(id);
		if (sp.isEmpty()) {
			throw new Exception("id not found");
		}
		if (!sp.get().getStatus().equals(BatchStatus.COMPLETED) && !sp.get().getStatus().equals(BatchStatus.STOPPED)) {
			throw new Exception("id not ready");
		}
		File dstDir = new File(outfilesDir + File.separator + principal.getName());
		File dstFile = new File(dstDir.getAbsolutePath() + File.separator + id + ".out.gz");
		return dstFile;
	}

	public void processFile(ScreenerProcess sp) throws Exception {
		jobService.start(sp);
	}
}

package com.systech.ms.list.ingestor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.systech.ms.list.ingestor.task.chunks.DownloadItemProcessor;
import com.systech.ms.list.ingestor.task.chunks.DownloadItemReader;
import com.systech.ms.list.ingestor.task.chunks.DownloadItemWriter;
import com.systech.ms.list.ingestor.task.chunks.FastloadItemProcessor;
import com.systech.ms.list.ingestor.task.chunks.FastloadItemReader;
import com.systech.ms.list.ingestor.task.chunks.FastloadItemWriter;
import com.systech.ms.list.ingestor.tasklets.MergeIndex;
import com.systech.ms.list.ingestor.tasklets.Unzip;

@Configuration
public class TaskConfiguration {

    @Value("${batch.size:1000}")
    private int batch_size;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    private DownloadItemReader dir;
    @Autowired
    private DownloadItemProcessor dip;
    @Autowired
    private DownloadItemWriter diw;

    @Autowired
    private Unzip unzip;

    @Autowired
    private FastloadItemReader fir;
    @Autowired
    private FastloadItemProcessor fip;
    @Autowired
    private FastloadItemWriter fiw;

    @Autowired
    private MergeIndex mi;

    @Bean
    public Job job1() {
        SimpleJobBuilder jobBuilder = jobBuilderFactory.get("Ingestor")
                .incrementer(new RunIdIncrementer())
                .start(download())
                .next(unziptask())
                .next(fastload())
                .next(mergetask());

        return jobBuilder.build();
    }

    @Bean
    public Step download() {
        return stepBuilderFactory
                .get("Download")
                .allowStartIfComplete(true)
                .chunk(batch_size)
                .reader(dir)
                .processor(dip)
                .writer(diw)
                .build();
    }

    @Bean
    public Step unziptask() {
        return stepBuilderFactory
                .get("Unzip")
                .tasklet(unzip)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step fastload() {
        return stepBuilderFactory
                .get("FastLoad")
                .allowStartIfComplete(true)
                .chunk(batch_size)
                .reader(fir)
                .processor(fip)
                .writer(fiw)
                .build();
    }

    @Bean
    Step mergetask() {
        return stepBuilderFactory
                .get("Merge")
                .tasklet(mi)
                .allowStartIfComplete(true)
                .build();
    }

}
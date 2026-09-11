package com.systech.ms.list.screener;

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

import com.systech.ms.list.screener.model.Person;
import com.systech.ms.list.screener.task.chunks.ScreenProcessor;
import com.systech.ms.list.screener.task.chunks.ScreenReader;
import com.systech.ms.list.screener.task.chunks.ScreenWriter;
import com.systech.ms.list.screener.tasklets.Unzip;

@Configuration
public class TaskConfiguration {

    @Value("${batch.size:1000}")
    private int batch_size;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    private ScreenReader sr;
    @Autowired
    private ScreenProcessor sp;
    @Autowired
    private ScreenWriter sw;

    @Autowired
    private Unzip unzip;

    @Autowired
    ScreenerJobListener jobListener;

    @Bean
    public Job job1() {
        SimpleJobBuilder jobBuilder = jobBuilderFactory.get("Screener")
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                .start(unziptask())
                .next(screen());

        return jobBuilder.build();
    }

    @Bean
    public Step screen() {
        return stepBuilderFactory
                .get("Screen")
                .allowStartIfComplete(true)
                .chunk(batch_size)
                .reader(sr)
                .processor(sp)
                .writer(sw)
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

}
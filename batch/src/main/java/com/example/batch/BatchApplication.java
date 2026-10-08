package com.example.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

@SpringBootApplication
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

}


record Dog(int id, String name, String description) {
}

@Configuration
class BatchConfiguration {

    @Bean
    Job job(JobRepository repository, ResetStepConfiguration resetStepConfiguration,
            IngestStepConfiguration step) {
        var s1 = resetStepConfiguration.resetStep(null, null);
        var s2 = step.step(null, null, null);
        return new JobBuilder("job", repository)
                .flow(s1)
                .next(s2)
                .build()
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}

@Configuration
class ResetStepConfiguration {

    @Bean
    Step resetStep(
            JdbcClient jdbcClient,
            JobRepository repository) {
        return new StepBuilder("resetStep", repository)
                .tasklet((_, _) -> {
                    jdbcClient.sql("delete from dog").update();
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

}

@Configuration
class IngestStepConfiguration {

    @Bean
    FlatFileItemReader<Dog> flatFileCsvItemReader(//
            @Value("classpath:/animals.csv") Resource csv//
    ) {
        return new FlatFileItemReaderBuilder<Dog>()
                .name("flatFileCsvItemReader")
                .resource(csv)
                .delimited(c -> c.delimiter(",")
                        .names("id", "name", "description", "dob",
                        "gender", "type"))
                .fieldSetMapper(fieldSet -> new Dog(fieldSet.readInt("id"),
                        fieldSet.readString("name"), fieldSet.readString("description")))
                .linesToSkip(1)
                .build();
    }

    @Bean
    JdbcBatchItemWriter<Dog> jdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Dog>()
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setInt(1, item.id());
                    ps.setString(2, item.name());
                    ps.setString(3, item.description());
                })
                .sql("insert into dog (id, name, description) values (?, ?, ?)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    Step step(JobRepository repository, FlatFileItemReader<Dog> flatFileCsvItemReader,
              ItemWriter<Dog> itemWriter) {
        return new StepBuilder("step", repository)
                .<Dog, Dog>chunk(10)
                .reader(flatFileCsvItemReader)
                .writer(itemWriter)
                .build();
    }


}
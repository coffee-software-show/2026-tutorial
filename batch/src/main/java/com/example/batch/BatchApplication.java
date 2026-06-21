package com.example.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchJdbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.Map;

@SpringBootApplication(exclude = {BatchJdbcAutoConfiguration.class})
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

}

record Customer(int id, String name, String email) {
}

@Configuration
class BatchConfiguration {

    private final JobRepository repository;

    BatchConfiguration(JobRepository repository) {
        this.repository = repository;
    }

    @Bean
    FlatFileItemReader<Customer> customerFlatFileItemReader(
            @Value("classpath:/customers.csv") Resource csv) {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customer-reader")
                .resource(csv)
                .delimited(c -> c.delimiter(",").names("id", "name", "email"))
                .fieldSetMapper(fieldSet -> new Customer(
                        fieldSet.readInt("id"),
                        fieldSet.readString("name"),
                        fieldSet.readString("email")))
                .build();
    }

    @Bean
    JdbcBatchItemWriter<Customer> customerJdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Customer>()
                .assertUpdates(true)
                .dataSource(dataSource)
                .sql("INSERT INTO customers(id, name, email) VALUES (:id, :name, :email) on conflict do nothing")
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource(Map.of("id", item.id(), "name", item.name(), "email", item.email())))
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setInt(1, item.id());
                    ps.setString(2, item.name());
                    ps.setString(3, item.email());
                })
                .build();
    }

    static final String STEP_FILES_TO_DB = "files-to-db";
    static final String STEP_RESET = "reset";

    @Bean(STEP_FILES_TO_DB)
    Step step(FlatFileItemReader<Customer> customerFlatFileItemReader,
              JdbcBatchItemWriter<Customer> customerJdbcBatchItemWriter) {
        return new StepBuilder("files-to-db", this.repository)
                .<Customer, Customer>chunk(10)
                .reader(customerFlatFileItemReader)
                .processor(customer -> {
                    IO.println("processing " + customer);
                    return customer;
                })
                .writer(customerJdbcBatchItemWriter)
                .faultTolerant()
                .retryLimit(10)
                .retry(IllegalArgumentException.class)
                .build();
    }

    @Bean(STEP_RESET)
    Step cleanTableStep(JdbcClient db, JobRepository repository) {
        return new StepBuilder("reset", repository)
                .tasklet((contribution, chunkContext) -> {
                    db.sql("delete from customers").update();
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    Job job(@Qualifier(STEP_RESET) Step stepReset,
            @Qualifier(STEP_FILES_TO_DB) Step stepFilesToDb) {
        return new JobBuilder("etl", this.repository)
                .start(stepReset)
                .next(stepFilesToDb)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @EventListener
    void after(JobExecutionEvent event) {
        IO.println("Job execution #" + event.getJobExecution() + " finished");
    }
}
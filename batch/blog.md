# Spring Batch and MongoDB

Spring Batch was introduced many years before MongoDB existed and its design assumed the presence of a SQL database in which to store the state of Spring Batch  jobs. 

But, that was _decades_ ago, and  a common question for anyone new to Spring Batch  was, *"why does this thing need to talk to a SQL database?"* The answer, of course, was that Spring Batch keeps a meticulous record of every job, step, and execution in a `JobRepository`, and for years that repository spoke one dialect: SQL. If you were happily living in MongoDB-land, you still had to drag a Postgres or MySQL instance along just so Batch could write down what it did last Tuesday.

In recent Spring Batch iterations, Spring Batch decoupled its `JobRepository` from JDBC, and Spring Boot 4.1 finally puts a bow on the experience with a proper `spring-boot-starter-batch-data-mongodb` autoconfiguration.  You get the same zero-config Boot experience for your batch metadata that JDBC users have enjoyed since the beginning. 

Fun fact: Dr. Dave Syer, cofounder of Spring Boot, was the founder and longtime lead of Spring Batch. Naturally, the first autoconfiguration he wrote for Spring Boot was for Spring Batch! So when I say that Spring Boot users have enjoyed JDBC-backed support in Spring Boot since the _very beginning_, I mean it!

This post walks through a small but complete example. It:

- Stores the Spring Batch `JobRepository` in **MongoDB** (via the new 4.1 starter).
- Reads `customers.csv` from the classpath.
- Writes the rows into a **PostgreSQL** `customers` table.
- Runs everything against services launched by a `compose.yaml` in the project root.

## Getting the infrastructure up

Before we touch a line of Java, spin up the two backing services:

```bash
docker compose up
```

The `compose.yaml` brings up a MongoDB instance configured as a single-node replica set (Batch's MongoDB support needs transactions, and transactions need a replica set), a PostgreSQL instance for the destination table, and a Grafana LGTM container for observability if you want it later.

The job we'll define is a simple ETL (extraction, transformation, load) job that reads from a `customers.csv` file and writes to a table called `customers` in a PostgreSQL database. 

We'll need to initialize the Postgres `customers` table; that's handled by `src/main/resources/schema.sql`:

```sql
create table if not exists customers (
    id    serial primary key,
    name  varchar(255),
    email varchar(255)
);
```

Spring Boot's SQL init (`spring.sql.init.mode=always`) runs it on startup. The MongoDB side is similarly self-serve — `spring.batch.data.mongodb.schema.initialize=true` tells the new starter to create the collections the `JobRepository` needs.

The relevant bits of `application.properties`:

```properties
spring.mongodb.host=localhost
spring.mongodb.port=27017
spring.mongodb.database=mydatabase

spring.batch.data.mongodb.schema.initialize=true

spring.datasource.url=jdbc:postgresql://localhost/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
```

We have _both_ JDBC and MongoDB connections. And because we want the MongoDB-backed `JobRepository` rather than the JDBC one, we tell Spring Boot's autoconfiguration to stand down:

```java
@SpringBootApplication(exclude = {BatchJdbcAutoConfiguration.class})
public class BatchApplication { ... }
```

That single exclusion is what flips the repository from JDBC to MongoDB. Everything else — the Mongo client, the collections, the transaction manager — comes from the new starter.

## The job

Our job is called `etl` and it runs two steps in sequence:

```java
@Bean
Job job(@Qualifier(STEP_RESET) Step stepReset,
        @Qualifier(STEP_FILES_TO_DB) Step stepFilesToDb) {
    return new JobBuilder("etl", this.repository)
            .start(stepReset)
            .next(stepFilesToDb)
            .incrementer(new RunIdIncrementer())
            .build();
}
```

First `reset` (a tasklet that wipes the destination table), then `files-to-db` (the reader → processor → writer that actually moves data). The `RunIdIncrementer` is a small but important detail: it bumps a `run.id` parameter on each launch so that Spring Batch treats every invocation as a new job instance instead of refusing to re-run a "completed" one.

## Step one: a tasklet

The simplest kind of Spring Batch step is a `Tasklet` — a single chunk of work with no notion of reading or writing items. It's the right tool when you just need to *do* something between steps. Here it's a clean slate:

```java
@Bean(STEP_RESET)
Step cleanTableStep(JdbcClient db, JobRepository repository) {
    return new StepBuilder("reset", repository)
            .tasklet((contribution, chunkContext) -> {
                db.sql("delete from customers").update();
                return RepeatStatus.FINISHED;
            })
            .build();
}
```

The tasklet runs once, returns `RepeatStatus.FINISHED`, and we move on.

## Step two: reader, processor, writer

The interesting step is the chunked one. Spring Batch's bread-and-butter pattern is *read an item, process it, accumulate a chunk, write the chunk*. The reader pulls rows out of `customers.csv`:

```java
@Bean
FlatFileItemReader<Customer> customerFlatFileItemReader(
        @Value("classpath:/customers.csv") Resource csv) {
    return new FlatFileItemReaderBuilder<Customer>()
            .name("customer-reader")
            .resource(csv)
            .delimited(c -> c.delimiter(",").names("id", "name", "email"))
            .fieldSetMapper(fs -> new Customer(
                    fs.readInt("id"),
                    fs.readString("name"),
                    fs.readString("email")))
            .build();
}
```

The CSV itself:

```csv
id,name,email
1,josh,josh@joshlong.com
2,dashaun,dashaun@dashaun.com
3,james,james@jamesward.dev
```

You get the idea.

The writer pushes each chunk into Postgres, using `ON CONFLICT DO NOTHING` so reruns don't blow up on primary-key collisions:

```java
@Bean
JdbcBatchItemWriter<Customer> customerJdbcBatchItemWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Customer>()
            .dataSource(dataSource)
            .sql("INSERT INTO customers(id, name, email) VALUES (:id, :name, :email) on conflict do nothing")
            .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource(
                    Map.of("id", item.id(), "name", item.name(), "email", item.email())))
            .build();
}
```

And the step itself ties them together with a tiny pass-through processor (a great place to drop transformation, enrichment, or filtering logic later) and a chunk size of 10:

```java
@Bean(STEP_FILES_TO_DB)
Step step(FlatFileItemReader<Customer> reader,
          JdbcBatchItemWriter<Customer> writer) {
    return new StepBuilder("files-to-db", this.repository)
            .<Customer, Customer>chunk(10)
            .reader(reader)
            .processor(customer -> {
                IO.println("processing " + customer);
                return customer;
            })
            .writer(writer)
            .faultTolerant()
            .retryLimit(10)
            .retry(IllegalArgumentException.class)
            .build();
}
```

Note the `faultTolerant()` switch and the retry policy — Spring Batch will quietly retry items that throw `IllegalArgumentException` up to ten times before failing the chunk. It's a one-liner because the framework is doing all the bookkeeping for you, and that bookkeeping is precisely the thing that now lives in MongoDB.

## Run it

With Docker Compose up:

```bash
./mvnw spring-boot:run
```

The job kicks off, `reset` clears Postgres, `files-to-db` streams the CSV through the chunk pipeline into Postgres, and every step transition, item count, exit status, and execution timestamp gets written to MongoDB. Open up a `mongosh` and you'll see the familiar Batch collections — `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION` — except they're documents now, not tables.


## Observability 

Spring Batch jobs emit a whole bunch of interesting Spring `ApplicationEvent`s! I listen for one, `JobExecutionEvent`, which gets published whenever a job has finished (successfully or not).

```java
    @EventListener
    void after(JobExecutionEvent event) {
        IO.println("Job execution #" + event.getJobExecution() + " finished");
    }

```

When I initialzied the program on the Spring Initializr, I made sure to add the `OpenTelemetry` Spring Boot starter. Spring Boot and Micrometer have long supported OpenTelemetry, but there was always some finagling required. Now, if you've got the OpenTelemetry starter on the classpath it'll publish metrics to any OpenTelemetry endpoint (the default assumes port `3000`). If you choose Docker Compose support on the Spring Initializr, it'll give you a Grafana configuration that will listen for OpenTelemetry information on port 3000, too! 

So, run the application then visit localhost:3000 and then click `Drilldown` and then `Metrics` and then search for `spring_batch` in the search field.

Alternatively, you could visit http://localhost:8080/actuator/metrics and see the same metrics. But I like shiny and colorful so the Grafana page does a lot for me.



## Bonus: Native Images with GraalVM

GraalVM native image technology has the potential to reduce overall memory usage. Spring Batch already _mostly_ works with GraalVM native images, but there are some new classes I needed to account for. And some new schema files. 

```java
    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
            for (var c : new Class[]{
                    org.springframework.batch.core.repository.persistence.JobInstance.class,
                    org.springframework.batch.core.repository.persistence.ExecutionContext.class,
                    org.springframework.batch.core.repository.persistence.ExitStatus.class,
                    org.springframework.batch.core.repository.persistence.StepExecution.class,
                    org.springframework.batch.core.repository.persistence.JobExecution.class,
                    org.springframework.batch.core.repository.persistence.JobParameter.class,
            }) {
                hints.reflection().registerType(c, MemberCategory.values());
            }

            var prefix = "org/springframework/batch/core/";
            for (var r : new String[]{
                    "schema-mongodb", //
                    "schema-drop-mongodb"}) {
                for (var suffix : "jsonl,js".split(",")) {
                    var path = prefix + r + "." + suffix;
                    var resource = new ClassPathResource(path);
                    if (resource.exists()) {
                        hints.resources().registerResource(resource);
                    }
                }
            }
        }
    }
```

And we also need to tell GraalVM about  `customers.csv`. 

```java

    static class ResourceHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.resources().registerResource(new ClassPathResource("/customers.csv"));
        }
    }
```

Register both in the usual way by adding this to the `BatchConfiguration` class (or any class with `@Configuration` on it):

```java
@ImportRuntimeHints({BatchConfiguration.ResourceHints.class, BatchConfiguration.Hints.class})
```


## Why this matters

The historical coupling between Spring Batch and a relational database was always a pragmatic compromise, not a design ideal. The framework needs *somewhere* durable to remember what it did, and SQL was the path of least resistance. With the `JobRepository` abstraction now properly decoupled — and Spring Boot 4.1 shipping first-class autoconfiguration for MongoDB — teams running on document stores no longer have to keep a JDBC database around just to humor the batch tier.

Pick the database that fits your data. Spring Batch will fit itself around your choice.
 
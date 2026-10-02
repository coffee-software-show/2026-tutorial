package com.example.http_service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;

@SpringBootApplication
public class HttpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner runner(AnimalsService animals) {
        return _ -> {
            animals.deleteAll();
            animals.add(new Animal(null, "Fido", "A friendly dog", Animal.Type.DOG));
            animals.add(new Animal(null, "Rex", "A loud but loyal guard dog", Animal.Type.DOG));
            animals.add(new Animal(null, "Garfield", "A lazy orange cat", Animal.Type.CAT));

            IO.println("== all (postgres) ==");
            animals.all().forEach(IO::println);

            IO.println("== search 'dog' (elasticsearch) ==");
            animals.search("dog").forEach(IO::println);

            IO.println("== search 'layzy' -- fuzzy, note the typo (elasticsearch) ==");
            animals.search("layzy").forEach(IO::println);

        };
    }

    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }

}

package com.example.data_jdbc;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Set;

@SpringBootApplication
public class DataJdbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataJdbcApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(CustomerRepository repository) {
        return _ -> {

            var customer = repository.save(new Customer(null, "josh@joshlong.com",
                    "Josh Long", Set.of(
                    new LineItem(null, 1, "sku")
            )));

            repository.findAll().forEach(IO::println);
        };
    }
}

interface CustomerRepository extends ListCrudRepository<@NonNull Customer, @NonNull Integer> {
}

record LineItem(@Id Integer id, int quantity, String sku) {
}

record Customer(@Id Integer id, String email, String name, Set<LineItem> lineItems) {
}
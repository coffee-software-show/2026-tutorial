package com.example.data_jdbc;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SpringBootApplication
public class DataJdbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataJdbcApplication.class, args);
    }

    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }
}

@Controller
@ResponseBody
class CustomersController {

    private final CustomerRepository repository;

    CustomersController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/customers")
    Collection<Customer> customers(@RequestParam Optional<String> usernameOptional) {
        return usernameOptional.map(u -> List.of(this.repository.findByUsername(u)))
                .orElseGet(this.repository::findAll);
    }
}

interface CustomerRepository extends ListCrudRepository<@NonNull Customer, @NonNull Integer> {
    Customer findByUsername(String username);
}

record LineItem(@Id Integer id, int quantity, String sku) {
}

record Customer(@Id Integer id, String username, String name, Set<LineItem> lineItems) {
}
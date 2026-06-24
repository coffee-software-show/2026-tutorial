package com.example.data_jdbc;

import net.ttddyy.observation.boot.event.JdbcMethodExecutionEvent;
import net.ttddyy.observation.boot.event.JdbcQueryExecutionEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
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

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    ApplicationRunner dataJdbcInitializer(CustomerRepository repository) {
        return _ -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        new Customer(null, "josh", "Joshua", Set.of(new LineItem(null, 1, "sku1"))),
                        new Customer(null, "dashaun", "DaShaun", Set.of(new LineItem(null, 2, "sku2")))
                ));
            }
        };
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
    Collection<Customer> customers(@RequestParam Optional<String> username) {
        return username.map(u -> List.of(this.repository.findByUsername(u)))
                .orElseGet(this.repository::findAll);
    }
}

interface CustomerRepository
        extends ListCrudRepository<@NonNull Customer, @NonNull Integer> {

    @Query("SELECT * FROM customer WHERE username = :#{ principal?.username } ")
    Customer findByAuthenticatedUser();

    Customer findByUsername(String username);
}

record LineItem(@Id Integer id, int quantity, String sku) {
}

record Customer(@Id Integer id, String username, String name, Set<LineItem> lineItems) {
}
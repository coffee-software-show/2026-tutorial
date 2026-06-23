package com.example.data_jdbc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.Set;


@Configuration
class DataTestConfiguration {

    @Bean
    SecurityEvaluationContextExtension securityEvaluationContextExtension() {
        return new SecurityEvaluationContextExtension();
    }
}

@Import(DataTestConfiguration.class)
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    private final String username = "josh";

    private final CustomerRepository repository;

    private Customer customer;

    CustomerRepositoryTest(@Autowired CustomerRepository repository) {
        this.repository = repository;
    }

    @BeforeEach
    void before() {
        //
        this.repository.deleteAll();
        this.customer = this.repository.save(new Customer(null, this.username,
                "Josh Long", Set.of(
                new LineItem(null, 1, "sku1"),
                new LineItem(null, 2, "sku2"))));

        //

    }

    @Commit
    @Test
    void customersByUsername() {
        var byEmail = this.repository.findByUsername(this.username);
        Assertions.assertEquals(customer.id(), byEmail.id(), "they should match");
        Assertions.assertNotNull(byEmail, "there must be exactly one record matching this username");
        Assertions.assertEquals(2, byEmail.lineItems().size(), "there must be exactly one line item");
        Assertions.assertTrue(byEmail.lineItems()
                .stream().anyMatch(li -> li.quantity() == 1 && li.sku().equals("sku1")));
        Assertions.assertTrue(byEmail.lineItems()
                .stream().anyMatch(li -> li.quantity() == 2 && li.sku().equals("sku2")));

    }

    @Test
    void secure() {

        var user = new User(this.username, "", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        var authentication = new UsernamePasswordAuthenticationToken(user, null);

        SecurityContextHolder
                .getContextHolderStrategy()
                .getContext()
                .setAuthentication(authentication);

        var customer = this.repository.findByAuthenticatedUser();
        IO.println("Authenticated user: " + customer.username());
        Assertions.assertNotNull(customer, "the customer " + this.username
                + " must not be null");

    }
}
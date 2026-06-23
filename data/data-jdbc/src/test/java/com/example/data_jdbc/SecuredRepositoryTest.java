package com.example.data_jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
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

@Configuration
class MyConfig {

    @Bean
    SecurityEvaluationContextExtension securityEvaluationContextExtension() {
        return new SecurityEvaluationContextExtension();
    }
}

@Import(MyConfig.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecuredRepositoryTest {

    private final CustomerRepository repository;

    SecuredRepositoryTest(@Autowired CustomerRepository repository) {
        this.repository = repository;
    }

    @BeforeEach
    void beforeEach() {
        var user = new User("josh", "", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var authentication = new UsernamePasswordAuthenticationToken(user, null);
        SecurityContextHolder
                .getContextHolderStrategy()
                .getContext()
                .setAuthentication(authentication);
    }

    @Commit
    @Test
    void secure() {
        var customer = this.repository.findByAuthenticatedUser();
        IO.println("Authenticated user: " + customer.username());
    }
}

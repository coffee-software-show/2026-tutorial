package com.example.v2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.annotation.*;
import java.util.Collection;

@ComponentScan
@PropertySource("classpath:config.properties")
@EnableTransactionManagement
@Configuration
class DogsConfiguration {

    @Bean
    static MyBeanFactoryPostProcessor myBeanFactoryPostProcessor (){
        return new MyBeanFactoryPostProcessor();
    }

    static class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(
                ConfigurableListableBeanFactory beanFactory) throws BeansException {

            for (var beanName: beanFactory.getBeanDefinitionNames() ){
                IO.println("beanName: " + beanName + ":" +
                        beanFactory.getType(beanName));
            }

        }
    }

    @Bean
    DriverManagerDataSource dataSource(Environment environment) {
        return new DriverManagerDataSource(environment.getProperty("db.url"),
                environment.getProperty("db.user"), environment.getProperty("db.pw"));
    }

    @Bean
    DataSourceTransactionManager dataSourceTransactionManager(DataSource db) {
        return new DataSourceTransactionManager(db);
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @EventListener
    void afterPropertiesSet(ContextRefreshedEvent event) {
        IO.println("context refreshed!");
    }
}

public class CustomersApplication {

    /*public static void main(String[] args) {
        var annotationConfigApplicationContext = new AnnotationConfigApplicationContext(DogsConfiguration.class);
        var dogRepository = annotationConfigApplicationContext.getBean(DogRepository.class);
        test(dogRepository);
    }*/

    private static void test(DogRepository txDogRepository) {
        txDogRepository.findAll().forEach(IO::println);
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface MizuhoRepository {

    /**
     * Alias for {@link Component#value}.
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

}


@MizuhoRepository
@Transactional
class DogRepository {

    private final JdbcClient db;

    DogRepository(JdbcClient db) {
        this.db = db;
    }

    public Collection<Dog> findAll() {
        return this.db //
                .sql("select * from dog") //
                .query((rs, _) -> new Dog(rs.getInt("id"), rs.getString("name")))//
                .list();
    }
}

record Dog(int id, String name) {
}

// spring framework
// > portable service abstractions
// > aop (aspect oriented programming)

// spring boot
// > autoconfiguration



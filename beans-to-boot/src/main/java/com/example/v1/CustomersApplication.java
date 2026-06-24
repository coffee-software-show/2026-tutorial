package com.example.v1;

import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

@Configuration
class DogsConfiguration {

    @Bean
    DriverManagerDataSource dataSource() {
        return new DriverManagerDataSource("jdbc:postgresql://localhost/mydatabase",
                "myuser", "secret");
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

    @Bean
    DogRepository dogRepository(JdbcClient jdbcClient, TransactionTemplate template) {
        return new DogRepository(jdbcClient);
    }

    @Bean
    TxBeanPostProcessor txBeanPostProcessor(TransactionTemplate template) {
        return new TxBeanPostProcessor(template);
    }

    static class TxBeanPostProcessor implements BeanPostProcessor {

        private final TransactionTemplate template;

        TxBeanPostProcessor(TransactionTemplate template) {
            this.template = template;
        }

        @Override
        public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            IO.println("after init [" + beanName + "]");
            if (bean instanceof Tx) {
                IO.println("beanname " + beanName + " is a transactional bean!");
                return concreteClassTransactionalProxy(template, bean.getClass(), bean);
            }
            return bean;
        }
    }

    private static Object concreteClassTransactionalProxy(TransactionTemplate template,
                                                          Class<?> tClass, Object target) {
        var pfb = new ProxyFactoryBean();
        for (var i : (tClass.getClass().getInterfaces())) {
            pfb.addInterface(i);
        }
        pfb.setProxyTargetClass(true);
        pfb.setTargetClass(tClass);
        pfb.setTarget(target);
        pfb.addAdvice((MethodInterceptor) invocation ->
                wrapInTransaction(template, target, invocation.getMethod(), invocation.getArguments()));
        return pfb.getObject();
    }

//        var transactionTemplate = new TransactionTemplate(tx);
//        var dogRepository = concreteClassTransactionalProxy(transactionTemplate, DogRepository.class, new DogRepository(jdbc));

    /// /        var txDogRepository = new TransactionalDogRepository(tx, dogRepository);
//        test(dogRepository);
    private static Object wrapInTransaction(TransactionTemplate template,
                                            Object target, Method method, Object[] args) throws Exception {
        return template.execute(status -> {
            try {
                IO.println("before [" + method.getName() + "]");
                var result = method.invoke(target, args);
                IO.println("after [" + method.getName() + "]");
                return result;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T jdkTransactionalProxy(TransactionTemplate template, Class<T> tClass, T target) {
        // java 1.4 (sun certified java programmer!)
        var result = Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass},
                (proxy, method, args) -> wrapInTransaction(template, target, method, args));
        return (T) result;
    }

}

public class CustomersApplication {

  /*  public static void main(String[] args) {
        var annotationConfigApplicationContext = new AnnotationConfigApplicationContext(DogsConfiguration.class);
        var dogRepository = annotationConfigApplicationContext.getBean(DogRepository.class);
        test(dogRepository);
    }
*/
    /*  public static void oldMain(String[] args) {
          var db = new DriverManagerDataSource("jdbc:postgresql://localhost/mydatabase",
                  "myuser", "secret");
          var jdbc = JdbcClient.create(db);
          var tx = new DataSourceTransactionManager(db);
          var transactionTemplate = new TransactionTemplate(tx);
          var dogRepository = concreteClassTransactionalProxy(transactionTemplate, DogRepository.class, new DogRepository(jdbc));
  //        var txDogRepository = new TransactionalDogRepository(tx, dogRepository);
          test(dogRepository);
      }
  */

    private static void test(DogRepository txDogRepository) {
        txDogRepository.findAll().forEach(IO::println);
    }
}

interface Tx {
}

class DogRepository implements Tx {

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

// start tx
// - do work
// - do work
// commit || rollback


// > all support transactions
// jdbc
// jms
// jdo (yuck!)
// jta (yuck!)
// amqp (rabbitmq)
// kafka
// elastic
// jpa (yuck!)
// redis
// neo4j
// mongodb

/*
class TransactionalDogRepository implements DogRepository {

    private final TransactionTemplate tx;
    private final DogRepository delegate;

    TransactionalDogRepository(TransactionTemplate tx, DogRepository delegate) {
        this.tx = tx;
        this.delegate = delegate;
    }

    @Override
    public Collection<Dog> findAll() {
        return this.tx.execute((_) -> this.delegate.findAll());
    }
}
 */

//interface DogRepository {
//    Collection<Dog> findAll();
//}
//
record Dog(int id, String name) {
}

// spring framework
// > portable service abstractions
// > aop (aspect oriented programming)

// spring boot
// > autoconfiguration



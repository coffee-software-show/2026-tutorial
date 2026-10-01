# 2026 Tutorial

Hi Spring fans! In this installment we look at _everything_! 

Every project section should cover: 
 - Security 
 - Observability 
 - GraalVM 
 - cloud deployment (`cf push`) 

So if I use Spring Data, show how to test and build native images for it and observe it and provision a simple app on cloud.

So:

## Desk check
- Sdkman 
- Direnv
- !!Mise!!
- spring javaformat maven plugin 
- Devtools
- IDEs and their start.spring.io experiences
- Testcontainers && Docker compose

## beans to boot
uses: postgres (pgvector), data jdbc, flyway, actuator, opentelemetry, docker compose support,
notes: we need to have a table called animals in which we find just a few records. we'll need to load a bunch manually, later using batch. make sure to pre-define this sql file and sql table so we can show them and use them to init the db.
- Spring Framework
	- Build the above using the framework
	- Events
	- Environment 
	- Nullability and the build plugins
- Spring Boot
- Start.spring.io 
- auto configuration 
- Starters

### notes
`one` — "mistakes were made" (raw JDBC)
Establishes the domain (Animal record + Type enum) and the AnimalRepository interface, then gives you three implementations stacked in one file as mini-steps. DefaultAnimalRepository1 is the worst case: it `new`s its own `DriverManagerDataSource` as a field, so the class hard-codes its connection s test or reconfigure. DefaultAnimalRepository2 is the first real improvement — theDataSource is constructor-injected (plain dependency injection, no framework), but every method still manages Connection/PreparedStatement/ResultSet by hand, wraps everything in try-with-resources, and
swallows SQLException into RuntimeException; findById even string-concatenates the id inttory3 swaps all that boilerplate for Spring's JdbcClient and a single reusableRowMapper<Animal>, collapsing ~60 lines of JDBC ceremony into fluent sql(...).params(...).query(...) calls — and main wires it up by hand.

`two` — "good OOP" (decoration instead of duplication)
The dead-end implementations are gone; only the JdbcClient version survives. The new idea is transactions, and the point is that you add them without touching the repository: TransactionalAnimalRepository
implements the same AnimalRepository interface, holds a TransactionTemplate and a delegattransactionTemplate.execute(...). main now hand-builds the whole object graph — DataSource → JdbcClient → DataSourceTransactionManager → TransactionTemplate → repository → transactional wrapper. This is textbook decorator composition, and its flaw is obvious: the wrapper must re-declare and
re-implement every single interface method, so the cross-cutting concern scales linearly
What would happen if we wanted to add security, logging, auditing, etc.?  

`three` — AOP with JDK/CGLIB proxies  Replaces the hand-written decorator with a Transactions helper that generates the wrapper flavors: a jdkProxy using java.lang.reflect.Proxy (interface-only), and the proxy methodactually used, which goes through Spring's ProxyFactoryBean with setProxyTargetClass(true) and a MethodInterceptor advice — i.e. CGLIB-style subclass proxying that works even without an interface. Both funnel into one delegate method that opens the transaction, reflectively invokes the targtx / after the tx so you can see the advice firing. Same behavior as two, but thetransactional concern is now written once for all methods.

`four` — Spring Framework (the container does the wiring) The manual main-method object graph becomes declarative configuration. MyConfiguration is @Configuration + @ComponentScan + @EnableTransactionManagement + @PropertySource, with @Bean methods for the        DataSource, PlatformTransactionManager, JdbcClient, and TransactionTemplate; the connecti code into application.properties and is read via Environment. The repository just gets@Repository + @Transactional — the hand-rolled proxy from three disappears because @EnableTransactionManagement registers the BeanPostProcessor that creates exactly that proxy for you. main shrinks to new  AnnotationConfigApplicationContext(MyConfiguration.class) plus a getBean lookup, and an @freshedEvent shows the lifecycle hook.

`five` — Spring Boot (the configuration disappears too)
MyConfiguration is deleted outright. A single @SpringBootApplication replaces @ConfiguratopertySource, and all four @Bean methods vanish: auto-configuration builds the DataSource(a pooled HikariCP one, not DriverManagerDataSource) from the same properties, the transaction manager, and the JdbcClient. @EnableTransactionManagement is gone as well since Boot enables it by default —   the repository keeps only @Transactional. getBean is replaced by an ApplicationRunner @Beitory by injection, and with spring-boot-docker-compose and schema.sql on the classpath,Boot also starts the Postgres container from compose.yaml, wires its connection details, and creates the animal table — all things you had to do by hand in steps one through four.                          
One thing to check before demoing five: DefaultAnimalRepository3 lost its @Repository along with the other annotations, so component scanning won't register it and runner(AnimalRepository) will fail with a NoSuchBeanDefinitionException. The other four packages don't need a stereotype (they're was @Repository — it looks like it was dropped a step too far.


## Optimizations
- AOT 
- java 27 Leyden 
- Virtual threads
- Graalvm


### notes 
in this section, you just take the code from the previous step and add a few optimizations. 
* `spring.threads.virtual.enabled=true`.
* 

## testing		
- Basics of testing 
- Boot slices 

## observability 	
- actuator 
	- Metrics 
	- Health indicators
	- sboms
	- git commit id plugin
- micrometer 
	- Gauges
	- Timers
	- Meters 
	- OpenTelemetry 

## Batch 
weve got a few records, but we wanna load all of 'em. we could have millions! this is a job for spring batch! 
uses: spring batch, jdbc, postgres, flyway, 
- choose the jdbc implementation 
- to load all the animals from `src/main/resources/animals.csv` to `animals`.

### data 
- now that we have our table setup. let's build a service.
- the animals ('pooch palace') api offers search, listing all the animals,  saving new animals, and updating them (this is important!); well eventualyl also support adopting them! but.. not now. well also eventually support asking ai questons abotu them
- the initial implementation uses jdbc
- then use spring data jdbc to implement the repository
- ok but how do we connect our client to the backend api 
- eventually, well support: looking at all the animals in the shelter, asking ai questons abotu them, searching with elasticsearch, and adopting

## jdbc
- `JdbcClient`
- lazy connections
- schema initialization - earlier we saw this in the batch section so no need to linger
- Flyway 
- net.ttddyy.observation : datasource-micrometer-spring-boot-starter 
- implement the repository using spring data jdbc
- use spring data elasticsearch to implement the search capability 
- make sure people see that the initializr has the ability to give me a docker image for elasticsearch
- we need to read everything, then pass each record back to the update method to force the elasticsearch indexing and so on.

## web programming
its cool that weve got the data situation worked out, but if we build a data layer and dont give our network clients a way to access it, did we actually build it? no. no we didnt. we need an API!

### apis with spring mvc 
- build a simple spring mvc api (`http-service`) on top of the data layer that supports the search, read, update
- virtual threads 
- nice to have the api, but we need a client that can read it all

### frontend client
- new application called `http-client`
- basics of the servlet api 
- controllers
- mvc + jte || thymeleaf
- tomcat customization
- build a simple .html page. it'll call the new service we've just stood up. but how?

## http clients
- the new starter
- `RestClient`
- declarative interface clients
- so now we have an http client and service. but this isn't the only game in town 
- while were at it, lets pull in some catfacts 
- at this point, we should have a animals controller in the root package supporting `POST` and `GET` requests for `/dogs` and `/cats`, filtering behind the scenes the one `animals` table. we should have a search '/search' endpoint
- let's rebuild the application to use graphql 

## spring shell
- now we can use the httpclient to call the backend api.

## graphql 
- basically were just gonna copy and paste the existing data service into a new module called `graphql-serviec` 
- build the api in graphql 
- write in the `graphql-client` using grapqhl client to call the downstream service 

## grpc 
- basically just copy the same backend repository stuff 
- implement the service 
- implement the client


## modulith
- go back to the http example from earlier.
- add a new adoptions method to the service. expose it via the animaals controller 
- add vet module 
- testing 
- documentation
- erxternalization via many things including rabbitmq and integration


## rabbitmq 
- show spring for amqp defining the exchange, binding, and queue for the spring modulith app. it will listen for the results and then print them out. 
- this is nice but u can see why this sort of plug-and-play would get tedious as soon as u started dealing with other kinds of evented sources and sinks 
- what if we wanted to write the results out to a filesystem, or to kafka, or an email system, or whatever?
- what we need is some way to integrate

## integration
- patterns of EAI 
- gregor hohpe + bobby woolf
- pipes and filters


## AI
- we have an endpoint in our http example thatll return all the dogs and another one to adopt an animal and another to search. lets add one 
- Ollama or?
- ChatClient
- User prompt 
- System prompt
- Skills 
- Spring Ai sessions for memory
- RAG with question answer advisor 
- Actuator 
- mcp 
	- Security of mcp
- testing with judges 
- Observability (token usage matters!)


## Spring security 101
- The SecurityFilterChain
- Customizer\<HttpSecurity\>
- Authentication 
	- InMemory
	- JDBC
	- Password migration inline
	- One time tokens
	- Webauthn 
	- Multi Factor
- Authorization 
	- `AuthorizationManagerFactories`


## spring authorization server && oauth 
- How to configure one 
- How to setup clients 
- using jdbc for other persistence 
- Go back and secure the web app and http clients with oauth 

## spring cloud 
- Spring cloud config server
- Eureka + DiscoveryClient  + LoadBalancer 
- Gateway 
	- Load balanced routes 
	- Token Relay 
- security of gateway proxies services
	- Building a route that loads the UI and backend api from the same place w/ token relay


## Graphql 
	- Schema
	- Different transports
	- Graphql clients
	- Method security for graphql
	- Native images 

## grpc 
	- (use the code Dave and i did for spring io 2026)
	- Schema
	- Services
	- Clients
	- observability (events?)
	- Security with oauth 
	- GraalVM native images (important now that it’s part of Boot 4.1!)

## spring amqp

## Spring for Apache Kafka 

## spring integration 
- Kafka 
- Files
- Rabbitmq 
- Debezium! [Debezium Support :: Spring Integration](https://docs.spring.io/spring-integration/reference/debezium.html)[Debezium Support :: Spring Integration](https://docs.spring.io/spring-integration/reference/debezium.html)
- Securing messaging with oauth
- Testing [Testing support :: Spring Integration](https://docs.spring.io/spring-integration/reference/testing.html)[Testing support :: Spring Integration](https://docs.spring.io/spring-integration/reference/testing.html)

## modulith 
- OOP  
- Events
- Tests
- Externalization 
	- With spring integration 
- Testing (layer slices) 
- Observability
- Graalvm native images
- CF push 

## batch 
- Jobs 
	- Tasklets
	- Steps 
		- Item readers
		- Item writers 
		- Item processors 
- JDBC 
- MongoDB 
- Remote chunking 
- Observability [Java Flight Recorder (JFR) support :: Spring Batch Reference](https://docs.spring.io/spring-batch/reference/spring-batch-observability/jfr.html)
- GraalVM I had issues see the batch folder under 2026-tutorial but I got it working 

## Shell 
	- TUI stuff ?
	- Secure using device code grant
	- Graalvm 

## Spring WS
	- Schema
	- Service 
	- Clients 
	- Oauth 
	- GraalVM 
	- Did Stéphane and Brian’s work on observation land?



<!-- 

# Spring Tutorial in 2027

## basics
* desk check with sdkman/direnv/mise
* java 25 onward
* jbang
* beans to boot
* performance with java aot + graalvm


## data

- all the datas in this csv file. we cant even begin until its loaded. postgres  + jdbc + batch. we _could_ try to do this on our own using jdbclient. but its error prone. lets use batch instead. 
  - use docker compose support to spin up postgresql + redis + whatever else.
- ok all the data is in the db. lets create our first cut of the service 

interface DogService {
	Collection<Dog> all();
	Dog save (name,description);
	Dog update (id,name,description);
	Collection<Dog> search(String q);
}

* start a new project with webmvc + postgresql + rabbitmq + data jdbc + elasticsearch + flyway + pgvector + ollama 
 - start with a simple jdbc + postgres impl using ilike %
 - then move to spring data repos
* create a service that has a findAll and a create method. the create
* we have all this data in a csv file but we need it in our database

## web: for the following  make sure to do both cleint and service
* spring mvc api + spring mvc html page / restclient client (here weve got a thymeleaf page; this will be useful when we do an oauth client later)
* spring graphql / graphqlclient
* spring grpc / grpc client
* spring cloud config server to centralize configuration 
* spring cloud gateway (proxy the graphql and )
* spring security 

## ai 

## modulith 
## shell 

 -->
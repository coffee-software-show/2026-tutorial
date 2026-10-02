# 2026 Tutorial


## Why This Video?

- i'm often asked if they can recommedn a good tutorial or training. I can. it's these end-to-ends. 
- is this stuff still relevant in the age of AI? 
- more than ever! 
- lot's of code doesnt mean better code 
- somebody needs to know waht good looks like. u need to know what ur looking for. u may not remember every little thing, but u need to know whats possible.
- ths video is a breadth-first look at building production-worthy systems and services with Spring 
- watch all of it, and emerge with a familiarity with a good chunk of the spring landscape. not all of it, of course, but a lot.

## Desk check

- Mise
- spring javaformat maven plugin
- Devtools
- IDEs and their start.spring.io experiences
- agentic coding
- Testcontainers && Docker compose


## beans to boot

uses: postgres (pgvector), data jdbc, flyway, actuator, opentelemetry, docker compose support,
notes: we need to have a table called animals in which we find just a few records. we'll need to load a bunch manually,
later using batch. make sure to pre-define this sql file and sql table so we can show them and use them to init the db.

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
Establishes the domain (Animal record + Type enum) and the AnimalRepository interface, then gives you three
implementations stacked in one file as mini-steps. DefaultAnimalRepository1 is the worst case: it `new`s its own
`DriverManagerDataSource` as a field, so the class hard-codes its connection s test or reconfigure.
DefaultAnimalRepository2 is the first real improvement — theDataSource is constructor-injected (plain dependency
injection, no framework), but every method still manages Connection/PreparedStatement/ResultSet by hand, wraps
everything in try-with-resources, and
swallows SQLException into RuntimeException; findById even string-concatenates the id inttory3 swaps all that
boilerplate for Spring's JdbcClient and a single reusableRowMapper<Animal>, collapsing ~60 lines of JDBC ceremony into
fluent sql (...).params (...).query (...) calls — and main wires it up by hand.

`two` — "good OOP" (decoration instead of duplication)
The dead-end implementations are gone; only the JdbcClient version survives. The new idea is transactions, and the point
is that you add them without touching the repository: TransactionalAnimalRepository
implements the same AnimalRepository interface, holds a TransactionTemplate and a delegattransactionTemplate.execute
(...). main now hand-builds the whole object graph — DataSource → JdbcClient → DataSourceTransactionManager →
TransactionTemplate → repository → transactional wrapper. This is textbook decorator composition, and its flaw is
obvious: the wrapper must re-declare and
re-implement every single interface method, so the cross-cutting concern scales linearly
What would happen if we wanted to add security, logging, auditing, etc.?

`three` — AOP with JDK/CGLIB proxies Replaces the hand-written decorator with a Transactions helper that generates the
wrapper flavors: a jdkProxy using java.lang.reflect.Proxy (interface-only), and the proxy methodactually used, which
goes through Spring's ProxyFactoryBean with setProxyTargetClass (true) and a MethodInterceptor advice — i.e. CGLIB-style
subclass proxying that works even without an interface. Both funnel into one delegate method that opens the transaction,
reflectively invokes the targtx / after the tx so you can see the advice firing. Same behavior as two, but
thetransactional concern is now written once for all methods.

`four` — Spring Framework (the container does the wiring) The manual main-method object graph becomes declarative
configuration. MyConfiguration is @Configuration + @ComponentScan + @EnableTransactionManagement + @PropertySource, with
@Bean methods for the DataSource, PlatformTransactionManager, JdbcClient, and TransactionTemplate; the connecti code
into application.properties and is read via Environment. The repository just gets@Repository + @Transactional — the
hand-rolled proxy from three disappears because @EnableTransactionManagement registers the BeanPostProcessor that
creates exactly that proxy for you. main shrinks to new AnnotationConfigApplicationContext (MyConfiguration.class) plus
a getBean lookup, and an @freshedEvent shows the lifecycle hook.

`five` — Spring Boot (the configuration disappears too)
MyConfiguration is deleted outright. A single @SpringBootApplication replaces @ConfiguratopertySource, and all four
@Bean methods vanish: auto-configuration builds the DataSource (a pooled HikariCP one, not DriverManagerDataSource) from
the same properties, the transaction manager, and the JdbcClient. @EnableTransactionManagement is gone as well since
Boot enables it by default — the repository keeps only @Transactional. getBean is replaced by an ApplicationRunner
@Beitory by injection, and with spring-boot-docker-compose and schema.sql on the classpath,Boot also starts the Postgres
container from compose.yaml, wires its connection details, and creates the animal table — all things you had to do by
hand in steps one through four.                          
One thing to check before demoing five: DefaultAnimalRepository3 lost its @Repository along with the other annotations,
so component scanning won't register it and runner (AnimalRepository) will fail with a NoSuchBeanDefinitionException.
The other four packages don't need a stereotype (they're was @Repository — it looks like it was dropped a step too far.

`six` - add `JdbcPostgresDialect` and `RuntimeHints` to enable GraalVM native image compilation and to fix a regression
in grpc

## Optimizations

* AOT
* java 27 Leyden (i have a global script called `leyden.sh`)
* Virtual threads (`spring.threads.virtual.enabled=true`)
* Graalvm (i have a script called `native.sh` that works here)
    * there is a regression in grpc + micrometer. i can fix it by adding a custom hint. this demonstrates some of the
      interesting semantics of aot in graalvm.

### notes

* i added `bin/native.sh` and `bin/leyden.sh` in the `PATH` for this folder in `mise.toml`. therye duplicates of the
  ones i have globally.


## data 
* flyway 
* jdbc client 
* spring data repositories
* virtual threads
* lazy connection proxies 
* elasticsearch?
* net.ttddyy.observation : datasource-micrometer-spring-boot-starter


### notes 
should i just take the existing code and create the beginnings of a new service, called `data`? i think so. make sure to preserve only the sixth package when copying over the old code. i should re-initialize the whole thing from start.spring.io to use spring data jdbc, elasticsearch, web, postgresql.


- copy `Animal` from the last module 
- repository? it's a one liner! create a typical Spring Data JDBC repository. IMPORTANT: `@Table` to the entity 
- build a simple (`@Transactional`) service called `AnimalService` with the following methods:  all(), search(String)
- what about that search? 
- let's create a AnimalDocument: 

```
@Document(indexName = "animals")
record AnimalDocument(
        @Id String id,
        @Field(type = FieldType.Text) String name,
        @Field(type = FieldType.Text) String description,
        // Keyword, not Text: types are an exact-match facet, not free text to analyze
        @Field(type = FieldType.Keyword) String type) {

    static AnimalDocument from(Animal animal) {
        return new AnimalDocument(String.valueOf(animal.id()), animal.name(),
                animal.description(), animal.type().name());
    }
}
```

- here's an `AnimalSearchRepository`
```

interface AnimalSearchRepository extends ElasticsearchRepository<AnimalDocument, String> {

    /**
     * Full-text search across name and description. This is the thing Postgres can't do
     * well: relevance ranking, analysis, and fuzzy matching on typos.
     */
    @Query("""
            {
              "multi_match": {
                "query": "?0",
                "fields": [ "name^2", "description" ],
                "fuzziness": "AUTO"
              }
            }
            """)
    List<AnimalDocument> search(String query);

}
```

- here's the service

```

@Service
@Transactional
class AnimalsService {

    private final AnimalRepository animalRepository;

    private final AnimalSearchRepository animalSearchRepository;

    AnimalsService(AnimalRepository animalRepository, AnimalSearchRepository animalSearchRepository) {
        this.animalRepository = animalRepository;
        this.animalSearchRepository = animalSearchRepository;
    }

    Collection<Animal> all() {
        return this.animalRepository.findAll();
    }

    Collection<Animal> search(String query) {
        var ranked = this.animalSearchRepository.search(query)
                .stream()
                .map(AnimalDocument::id)
                .map(Integer::valueOf)
                .toList();
        if (ranked.isEmpty())
            return List.of();
        var byId = this.animalRepository.findAllById(ranked)
                .stream()
                .collect(Collectors.toMap(Animal::id, Function.identity()));
        return ranked.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    Animal add(Animal animal) {
        var saved = this.animalRepository.save(animal);
        this.animalSearchRepository.save(AnimalDocument.from(saved), RefreshPolicy.IMMEDIATE);
        return saved;
    }

    void deleteAll() {
        this.animalRepository.deleteAll();
        this.animalSearchRepository.deleteAll(RefreshPolicy.IMMEDIATE);
    }

}

```


## Batch

weve got a few records, but how do we get a batch of data into the system? all of 'em. we could have millions! this is a job for spring batch!
uses: spring batch, jdbc, postgres, flyway,

- choose the jdbc implementation
- to load all the animals from `src/main/resources/animals.csv` to `animals`

### notes 
- add spring batch jdbc to the data project. 
- change the code so the 'runner' is gone 
- well build a job that loads all the data from the animals.csv into objects we can interrogate and then uses the newly minted service to add the records to the sql table 
- two steps
- 1) tasklet that deletes from the animasl table 
- 2) reader/writer from .csv to our AnimalService (should we add a batching `add()` method?)


## web programming

its cool that weve got the data situation worked out, but if we build a data layer and dont give our network clients a
way to access it, did we actually build it? no. no we didnt. we need an API!

### apis with spring mvc

- build a simple spring mvc api (`http-service`) on top of the data layer that supports the search, read, update
- explain that this is not a rest api. 
- show the https://en.wikipedia.org/wiki/Richardson_Maturity_Model
- talk about spring hateoas
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
- at this point, we should have a animals controller in the root package supporting `POST` and `GET` requests for
  `/dogs` and `/cats`, filtering behind the scenes the one `animals` table. we should have a search '/search' endpoint
- let's rebuild the application to use graphql

## spring shell

- now we can use the httpclient to call the backend api, too!
- graalvm 


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

- show spring for amqp defining the exchange, binding, and queue for the spring modulith app. it will listen for the
  results and then print them out.
- this is nice but u can see why this sort of plug-and-play would get tedious as soon as u started dealing with other
  kinds of evented sources and sinks
- what if we wanted to write the results out to a filesystem, or to kafka, or an email system, or whatever?
- what we need is some way to integrate

## integration

- patterns of EAI
- gregor hohpe + bobby woolf
- pipes and filters

## AI

- we have an endpoint in our http example thatll return all the dogs and another one to adopt an animal and another to
  search. lets add one
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
- Eureka + DiscoveryClient + LoadBalancer
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
-

Debezium! [Debezium Support :: Spring Integration](https://docs.spring.io/spring-integration/reference/debezium.html)[Debezium Support :: Spring Integration](https://docs.spring.io/spring-integration/reference/debezium.html)

- Securing messaging with oauth
-

Testing [Testing support :: Spring Integration](https://docs.spring.io/spring-integration/reference/testing.html)[Testing support :: Spring Integration](https://docs.spring.io/spring-integration/reference/testing.html)

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
-

Observability [Java Flight Recorder (JFR) support :: Spring Batch Reference](https://docs.spring.io/spring-batch/reference/spring-batch-observability/jfr.html)

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
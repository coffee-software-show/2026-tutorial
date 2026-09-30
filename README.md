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
uses: postgres, data jdbc, flyway, actuator, opentelemetry, docker compose support, 
- Spring Framework
	- Build the above using the framework
	- Events
	- Environment 
	- Nullability and the build plugins
- Spring Boot
- Start.spring.io 
- auto configuration 
- Starters

## Optimizations
- AOT 
- java 27 Leyden 
- Virtual threads
- Graalvm 


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
uses: spring batch, jdbc, postgres, flyway, 
- choose the jdbc implementation 
- to laod all the animals.

### build a backend rest api 
- the dogs api offers search, listing all the dogs, and saving new dogs 
- initial implementation uses data jdbc 
- ok but how do we connect our client to the backend api 
- eventually, well support: looking at all the animals in the shelter, asking ai questons abotu them, searching with elasticsearch, and adopting

## data
- JdbcClient
- JdbcTemplate
- lazy connections
- schema initialization 
- Flyway 
- net.ttddyy.observation : datasource-micrometer-spring-boot-starter 
- implement the repository using spring data jdbc
- use spring data elasticsearch to implement the search capability 

## web programming 
### frontend client
- Basics of the servlet api 
- Controllers
- MVC + jte || thymeleaf
- Tomcat customization 

## http clients
- The new starter
- RestClient or RestTemplate  
- Declarative interface clients
- so now we have an http client and service. but this isnt the only game in town 
- lets rebuild the application to use graphql 

## graphql 
- basically were just gonna copy and paste the existing jdbc repository 
- build the api in graphql 
- write in te graphql client using grapqhl client to call the downstream service 


## grpc 

- basically just copy the same backend repository stuff 
- implement the service 
- implement the client 



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


## modulith 
- lets look at the http example from earlier. 

## integration 

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
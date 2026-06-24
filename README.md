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
- spring javaformat maven plugin 
- Devtools
- IDEs and their start.spring.io experiences
- Testcontainers && Docker compose

## support lifetimes for spring oss and enterprise projects 
- app advisor (get dashaun in sf?)
- Upgrading for fun and profit  

## beans to boot
- Spring Framework
	- Build the above using the framework
	- Events
	- Environment 
	- Nullability and the build plugins
- Spring Boot
- Start.spring.io 
- auto configuration 
- Starters


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

## Optimizations
- AOT 
- Leyden 
- Virtual threads
- Graalvm 

## JDBC
- JdbcClient
- JdbcTemplate
- AbstractRoutingDataSource
- Schema initialization 
- Flyway 
- net.ttddyy.observation : datasource-micrometer-spring-boot-starter 

## web programming 
- Basics of the servlet api 
- Controllers
- MVC + jte 
- Tomcat customization 

## http clients
- The new starter
- RestClient or RestTemplate  
- Declarative interface clients

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

## Spring data
- Repositories 
- Spring data jdbc 
- Spring data MongoDB 
- Dto projections 
- Auditing 
- Spring security spel queries 
- AOT repositories && graalvm 
- @DataJpaTest and @AutoConfigureTestDatabase 

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

## AI
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




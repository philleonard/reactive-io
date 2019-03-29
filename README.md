# Reactive I/O

Example of two end-to-end reactive, backpressure aware web applications utilising 3 modern reactive streams oriented drivers and I/O connectors:

- [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html) for reactvie web
- [MongoDB Reactive Streams driver](https://mongodb.github.io/mongo-java-driver-reactivestreams/) for reactive NoSQL access 
- [RSocket](http://rsocket.io) for reactive S2S network communication

The application streams integers from mongo, to an RSocket server which in turn sums them and streams them back via an RSocket channel to the Spring Boot WebFlux application which in turn streams them back to the client requester via JSON streaming. 

All of this happens with single request backpressure with 1 second delay on the RSocket server. 

## Running

Run the Spring Boot (WebFlux) web application with:
```bash
mvn spring-boot:run
```

and startup the RSocket server implementation with: 
```bash
mvn exec:java -Dexec.mainClass="reactive.demo.Client"
```

Request the streaming endpoint with:
```bash
curl localhost:8080/sum
```


package reactive.demo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.reactivex.Flowable;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import java.util.logging.Logger;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@EnableWebFlux
@SpringBootApplication
public class Client {

    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    private final MongoCollection<Document> integers =
            MongoClients.create("mongodb://localhost")
                    .getDatabase("local-dev")
                    .getCollection("integers");

    public static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions.route(GET("/sum").and(accept(TEXT_EVENT_STREAM)), this::requesthandler);
    }

    private Mono<ServerResponse> requesthandler(ServerRequest request) {
        Flowable<Integer> intStream =
                Flowable.fromPublisher(integers.find(and(gte("int", 100), lte("int", 1000))))
                        .map(doc -> doc.getInteger("int"));
        Flowable<Payload> payload = intStream.map(String::valueOf).map(DefaultPayload::create);
        Flux<Integer> integerFlux = rsocketConnect()
                .flatMapMany(localhost ->
                        localhost.requestChannel(payload)
                                .map(Payload::getDataUtf8)
                                .map(Integer::valueOf));
        return ServerResponse.ok().contentType(TEXT_EVENT_STREAM).body(integerFlux, Integer.class);
    }

    private Mono<RSocket> rsocketConnect() {
        return RSocketFactory.connect()
                .transport(TcpClientTransport.create("localhost", 1234))
                .start();
    }

}

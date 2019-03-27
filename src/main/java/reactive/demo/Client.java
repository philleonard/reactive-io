package reactive.demo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.reactivex.Flowable;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    private static final String INT = "int";

    private final MongoCollection<Document> integers =
            MongoClients.create()
                    .getDatabase("local-dev")
                    .getCollection("integers");

    public static void main(String[] args) {
        SpringApplication.run(Client.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions.route(GET("/sum"), this::requestHandler);
    }

    private Mono<ServerResponse> requestHandler(ServerRequest request) {
        Flowable<Integer> intStream =
                Flowable.fromPublisher(
                        integers.find(and(gte(INT, 10), lte(INT, 100)))
                                .batchSize(1))
                        .doOnRequest(o -> LOG.info("Request to Mongo: " + o))
                        .map(doc -> doc.getInteger(INT));

        Flowable<Payload> payload =
                intStream.map(String::valueOf)
                        .doOnNext(o -> LOG.info("Sending to Server: " + o))
                        .map(DefaultPayload::create);

        Flux<Integer> integerFlux = rsocketConnect()
                .flatMapMany(localhost ->
                        localhost.requestChannel(payload)
                                .map(Payload::getDataUtf8)
                                .map(Integer::valueOf));

        return ServerResponse.ok().contentType(APPLICATION_STREAM_JSON).body(
                integerFlux.map(Something::new), Something.class);
    }

    private Mono<RSocket> rsocketConnect() {
        return RSocketFactory.connect()
                .transport(TcpClientTransport.create(1234))
                .start();
    }

    class Something {

        private final int integer;

        Something(int integer) {
            this.integer = integer;
        }

        public int getInteger() {
            return integer;
        }
    }

}

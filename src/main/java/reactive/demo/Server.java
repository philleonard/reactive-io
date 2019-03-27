package reactive.demo;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import java.time.Duration;
import java.util.logging.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Server {

    public static void main(String[] args) {
        System.out.println("Startup RSocket Server");


        RSocketFactory.receive()
                .acceptor((pl, socket) -> Mono.just(new AbstractRSocket() {
                    @Override
                    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                        return Flux.from(payloads)
                                .doOnRequest(o -> System.out.println("Request from client: " + o))
                                .limitRate(1)
                                .delayElements(Duration.ofSeconds(1))
                                .map(Payload::getDataUtf8)
                                .doOnNext(o -> System.out.println("Received from client: " + o))
                                .map(Integer::valueOf)
                                .scan(Integer::sum) // Sum as a service!
                                .map(Object::toString)
                                .map(DefaultPayload::create);
                    }
                }))
                .transport(TcpServerTransport.create(1234))
                .start()
                .block()
                .onClose()
                .block();
    }
}

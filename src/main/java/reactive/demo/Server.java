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

    private static Logger LOG = Logger.getLogger("Server");

    public static void main(String[] args) {
        System.out.println("Startup RSocket Server");

        RSocketFactory.receive()
                .acceptor((pl, socket) -> Mono.just(new SumSocket()))
                .transport(TcpServerTransport.create("localhost", 1234))
                .start()
                .block()
                .onClose()
                .block();
    }

    private static class SumSocket  extends AbstractRSocket {
        @Override
        public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
            return Flux.from(payloads)
                    .doOnNext(o -> LOG.info("Received Payload"))
                    .delayElements(Duration.ofSeconds(1))
                    .map(Payload::getDataUtf8)
                    .map(Integer::valueOf)
                    .scan(Integer::sum) // Sum as a service!
                    .map(Object::toString)
                    .map(DefaultPayload::create);
        }
    }
}

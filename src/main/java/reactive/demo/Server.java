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
    }
}

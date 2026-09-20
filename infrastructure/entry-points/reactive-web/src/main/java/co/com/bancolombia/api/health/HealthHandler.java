package co.com.bancolombia.api.health;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class HealthHandler {
    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        return ServerResponse.ok().build();
    }
}

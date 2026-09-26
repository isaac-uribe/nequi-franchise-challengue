package co.com.bancolombia.api.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class HealthHandler {
    @Operation(
            tags = "Health",
            summary = "Health check",
            description = "Returns 200 with an empty body when the service is up.",
            responses = @ApiResponse(responseCode = "200", description = "Service is up")
    )
    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        return ServerResponse.ok().build();
    }
}

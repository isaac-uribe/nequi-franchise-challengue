package co.com.bancolombia.api;

import co.com.bancolombia.api.health.HealthHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler, HealthHandler healthHandler) {
        return route(GET("/api/health"), healthHandler::healthCheck)
                .andRoute(POST("/api/franchises"), handler::createFranchise)
                .andRoute(PATCH("/api/franchises/{franchiseId}"), handler::renameFranchise)
                .andRoute(POST("/api/franchises/{franchiseId}/branches"), handler::addBranch)
                .andRoute(PATCH("/api/franchises/{franchiseId}/branches/{branchId}"), handler::renameBranch)
                .andRoute(POST("/api/franchises/{franchiseId}/branches/{branchId}/products"), handler::addProduct)
                .andRoute(DELETE("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}"), handler::removeProduct)
                .andRoute(PATCH("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}"), handler::renameProduct)
                .andRoute(PATCH("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock"), handler::modifyStock)
                .andRoute(GET("/api/franchises/{franchiseId}/top-stock-products"), handler::getTopStockProducts);
    }
}

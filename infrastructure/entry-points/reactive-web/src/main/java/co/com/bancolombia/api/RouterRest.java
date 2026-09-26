package co.com.bancolombia.api;

import co.com.bancolombia.api.health.HealthHandler;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(path = "/api/health", method = RequestMethod.GET,
                    beanClass = HealthHandler.class, beanMethod = "healthCheck"),
            @RouterOperation(path = "/api/franchises", method = RequestMethod.POST,
                    beanClass = Handler.class, beanMethod = "createFranchise"),
            @RouterOperation(path = "/api/franchises/{franchiseId}", method = RequestMethod.PATCH,
                    beanClass = Handler.class, beanMethod = "renameFranchise"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches", method = RequestMethod.POST,
                    beanClass = Handler.class, beanMethod = "addBranch"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}", method = RequestMethod.PATCH,
                    beanClass = Handler.class, beanMethod = "renameBranch"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products", method = RequestMethod.POST,
                    beanClass = Handler.class, beanMethod = "addProduct"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}", method = RequestMethod.DELETE,
                    beanClass = Handler.class, beanMethod = "removeProduct"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}", method = RequestMethod.PATCH,
                    beanClass = Handler.class, beanMethod = "renameProduct"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock", method = RequestMethod.PATCH,
                    beanClass = Handler.class, beanMethod = "modifyStock"),
            @RouterOperation(path = "/api/franchises/{franchiseId}/top-stock-products", method = RequestMethod.GET,
                    beanClass = Handler.class, beanMethod = "getTopStockProducts")
    })
    public RouterFunction<ServerResponse> franchiseRoutes(Handler handler, HealthHandler healthHandler) {
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

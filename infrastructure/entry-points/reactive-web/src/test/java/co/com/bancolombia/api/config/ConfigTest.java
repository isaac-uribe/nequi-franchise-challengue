package co.com.bancolombia.api.config;

import co.com.bancolombia.api.Handler;
import co.com.bancolombia.api.RouterRest;
import co.com.bancolombia.api.health.HealthHandler;
import co.com.bancolombia.usecase.branch.AddBranchUseCase;
import co.com.bancolombia.usecase.branch.RenameBranchUseCase;
import co.com.bancolombia.usecase.franchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.franchise.RenameFranchiseUseCase;
import co.com.bancolombia.usecase.product.AddProductUseCase;
import co.com.bancolombia.usecase.product.GetTopStockProductByBranchUseCase;
import co.com.bancolombia.usecase.product.ModifyStockUseCase;
import co.com.bancolombia.usecase.product.RemoveProductUseCase;
import co.com.bancolombia.usecase.product.RenameProductUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@ContextConfiguration(classes = {RouterRest.class, Handler.class, HealthHandler.class})
@WebFluxTest
@Import({CorsConfig.class, SecurityHeadersConfig.class})
class ConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;
    @MockitoBean
    private RenameFranchiseUseCase renameFranchiseUseCase;
    @MockitoBean
    private AddBranchUseCase addBranchUseCase;
    @MockitoBean
    private RenameBranchUseCase renameBranchUseCase;
    @MockitoBean
    private AddProductUseCase addProductUseCase;
    @MockitoBean
    private RemoveProductUseCase removeProductUseCase;
    @MockitoBean
    private ModifyStockUseCase modifyStockUseCase;
    @MockitoBean
    private RenameProductUseCase renameProductUseCase;
    @MockitoBean
    private GetTopStockProductByBranchUseCase getTopStockProductByBranchUseCase;

    @Test
    void corsConfigurationShouldAllowOrigins() {
        webTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

}
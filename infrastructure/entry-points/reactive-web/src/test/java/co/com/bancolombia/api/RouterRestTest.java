package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.CreateFranchiseRequest;
import co.com.bancolombia.api.health.HealthHandler;
import co.com.bancolombia.model.aggregate.Franchise;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RouterRest.class, Handler.class, HealthHandler.class})
@WebFluxTest
class RouterRestTest {

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
    void testHealthRouteIsWired() {
        webTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testCreateFranchiseRouteIsWired() {
        Franchise franchise = Franchise.builder().id("franchise-1").name("Juan Valdez").build();
        when(createFranchiseUseCase.createFranchise("Juan Valdez")).thenReturn(Mono.just(franchise));

        webTestClient.post()
                .uri("/api/franchises")
                .bodyValue(new CreateFranchiseRequest("Juan Valdez"))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testUnknownRouteReturnsNotFound() {
        webTestClient.get()
                .uri("/api/unknown/path")
                .exchange()
                .expectStatus().isNotFound();
    }
}

package co.com.bancolombia.api;

import co.com.bancolombia.api.health.HealthHandler;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.branch.AddBranchUseCase;
import co.com.bancolombia.usecase.branch.RenameBranchUseCase;
import co.com.bancolombia.usecase.franchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.franchise.RenameFranchiseUseCase;
import co.com.bancolombia.usecase.product.AddProductUseCase;
import co.com.bancolombia.usecase.product.GetTopStockProductByBranchUseCase;
import co.com.bancolombia.usecase.product.ModifyStockUseCase;
import co.com.bancolombia.usecase.product.RemoveProductUseCase;
import co.com.bancolombia.usecase.product.RenameProductUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Wires the real Handler, use cases and router around a mocked repository, so validation is
 * exercised end to end at the HTTP layer without a Spring context.
 */
class ReactiveWebValidationTest {

    private static final String STOCK_URI = "/api/franchises/" + TestIds.FRANCHISE_ID
            + "/branches/" + TestIds.BRANCH_ID + "/products/" + TestIds.PRODUCT_ID + "/stock";

    private FranchiseRepository repository;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        repository = mock(FranchiseRepository.class);
        Handler handler = new Handler(
                new CreateFranchiseUseCase(repository),
                new RenameFranchiseUseCase(repository),
                new AddBranchUseCase(repository),
                new RenameBranchUseCase(repository),
                new AddProductUseCase(repository),
                new RemoveProductUseCase(repository),
                new ModifyStockUseCase(repository),
                new RenameProductUseCase(repository),
                new GetTopStockProductByBranchUseCase(repository));
        webTestClient = WebTestClient
                .bindToRouterFunction(new RouterRest().franchiseRoutes(handler, new HealthHandler()))
                .build();
    }

    @Test
    void shouldReturn400ForTooLongFranchiseNameWithoutTouchingRepository() {
        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"" + "a".repeat(10_000) + "\"}")
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturn400ForMalformedJson() {
        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturn400ForEmptyBody() {
        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturn400ForWrongStockType() {
        webTestClient.patch()
                .uri(STOCK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"stock\": \"abc\"}")
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturn400ForInvalidFranchiseIdWithoutTouchingRepository() {
        webTestClient.get()
                .uri("/api/franchises/-1/top-stock-products")
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturn404ForWellFormedButMissingFranchiseId() {
        when(repository.findById(TestIds.MISSING_FRANCHISE_ID)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/franchises/" + TestIds.MISSING_FRANCHISE_ID + "/top-stock-products")
                .exchange()
                .expectStatus().isNotFound();

        verify(repository).findById(TestIds.MISSING_FRANCHISE_ID);
        verify(repository, never()).save(any());
    }
}

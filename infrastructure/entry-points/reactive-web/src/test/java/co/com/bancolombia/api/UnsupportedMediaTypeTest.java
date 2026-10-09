package co.com.bancolombia.api;

import co.com.bancolombia.api.health.HealthHandler;
import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Every route that reads a body must answer 415 (never 500) when the Content-Type is missing or
 * not JSON, without leaking internal class names, and still succeed with application/json.
 * Use cases are mocked, so this only exercises the HTTP layer.
 */
class UnsupportedMediaTypeTest {

    private static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE = "Unsupported media type, use application/json";
    private static final String INTERNAL_PACKAGE = "co.com.bancolombia";

    private static final String FRANCHISE_URI = "/api/franchises/" + TestIds.FRANCHISE_ID;
    private static final String BRANCH_URI = FRANCHISE_URI + "/branches/" + TestIds.BRANCH_ID;
    private static final String PRODUCT_URI = BRANCH_URI + "/products/" + TestIds.PRODUCT_ID;

    private final CreateFranchiseUseCase createFranchiseUseCase = mock(CreateFranchiseUseCase.class);
    private final RenameFranchiseUseCase renameFranchiseUseCase = mock(RenameFranchiseUseCase.class);
    private final AddBranchUseCase addBranchUseCase = mock(AddBranchUseCase.class);
    private final RenameBranchUseCase renameBranchUseCase = mock(RenameBranchUseCase.class);
    private final AddProductUseCase addProductUseCase = mock(AddProductUseCase.class);
    private final RemoveProductUseCase removeProductUseCase = mock(RemoveProductUseCase.class);
    private final ModifyStockUseCase modifyStockUseCase = mock(ModifyStockUseCase.class);
    private final RenameProductUseCase renameProductUseCase = mock(RenameProductUseCase.class);
    private final GetTopStockProductByBranchUseCase getTopStockProductByBranchUseCase =
            mock(GetTopStockProductByBranchUseCase.class);

    private WebTestClient webTestClient;

    record BodyRoute(String label, HttpMethod method, String uri, String validJson, HttpStatus successStatus) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<BodyRoute> bodyRoutes() {
        return Stream.of(
                new BodyRoute("create franchise", HttpMethod.POST, "/api/franchises",
                        "{\"name\":\"Franchise\"}", HttpStatus.CREATED),
                new BodyRoute("rename franchise", HttpMethod.PATCH, FRANCHISE_URI,
                        "{\"name\":\"Renamed\"}", HttpStatus.OK),
                new BodyRoute("add branch", HttpMethod.POST, FRANCHISE_URI + "/branches",
                        "{\"name\":\"Branch\"}", HttpStatus.CREATED),
                new BodyRoute("rename branch", HttpMethod.PATCH, BRANCH_URI,
                        "{\"name\":\"Renamed\"}", HttpStatus.OK),
                new BodyRoute("add product", HttpMethod.POST, BRANCH_URI + "/products",
                        "{\"name\":\"Product\",\"stock\":5}", HttpStatus.CREATED),
                new BodyRoute("rename product", HttpMethod.PATCH, PRODUCT_URI,
                        "{\"name\":\"Renamed\"}", HttpStatus.OK),
                new BodyRoute("modify stock", HttpMethod.PATCH, PRODUCT_URI + "/stock",
                        "{\"stock\":7}", HttpStatus.OK));
    }

    static Stream<Arguments> bodyRoutesWithUnsupportedMediaTypes() {
        return bodyRoutes().flatMap(route -> Stream.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED)
                .map(mediaType -> Arguments.of(route, mediaType)));
    }

    @BeforeEach
    void setUp() {
        Handler handler = new Handler(createFranchiseUseCase, renameFranchiseUseCase, addBranchUseCase,
                renameBranchUseCase, addProductUseCase, removeProductUseCase, modifyStockUseCase,
                renameProductUseCase, getTopStockProductByBranchUseCase);
        webTestClient = WebTestClient
                .bindToRouterFunction(new RouterRest().franchiseRoutes(handler, new HealthHandler()))
                .build();
    }

    @ParameterizedTest(name = "{0} with {1} -> 415")
    @MethodSource("bodyRoutesWithUnsupportedMediaTypes")
    void shouldReturn415ForUnsupportedContentType(BodyRoute route, MediaType mediaType) {
        webTestClient.method(route.method())
                .uri(route.uri())
                .contentType(mediaType)
                .bodyValue(route.validJson())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .isEqualTo(UNSUPPORTED_MEDIA_TYPE_MESSAGE)
                        .doesNotContain(INTERNAL_PACKAGE));

        verifyNoUseCaseInteractions();
    }

    @ParameterizedTest(name = "{0} without Content-Type -> 415")
    @MethodSource("bodyRoutes")
    void shouldReturn415WhenContentTypeHeaderIsMissing(BodyRoute route) {
        webTestClient.method(route.method())
                .uri(route.uri())
                .headers(headers -> headers.remove(HttpHeaders.CONTENT_TYPE))
                .bodyValue(route.validJson().getBytes(StandardCharsets.UTF_8))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectBody(String.class)
                .consumeWith(result -> {
                    assertThat(result.getRequestHeaders().getContentType()).isNull();
                    assertThat(result.getResponseBody())
                            .isEqualTo(UNSUPPORTED_MEDIA_TYPE_MESSAGE)
                            .doesNotContain(INTERNAL_PACKAGE);
                });

        verifyNoUseCaseInteractions();
    }

    @ParameterizedTest(name = "{0} with application/json still succeeds")
    @MethodSource("bodyRoutes")
    void shouldStillSucceedWithJsonContentType(BodyRoute route) {
        stubBodyReadingUseCases();

        webTestClient.method(route.method())
                .uri(route.uri())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(route.validJson())
                .exchange()
                .expectStatus().isEqualTo(route.successStatus());
    }

    private void stubBodyReadingUseCases() {
        Mono<Franchise> franchise = Mono.just(existingFranchise());
        when(createFranchiseUseCase.createFranchise(anyString())).thenReturn(franchise);
        when(renameFranchiseUseCase.renameFranchise(anyString(), anyString())).thenReturn(franchise);
        when(addBranchUseCase.addBranch(anyString(), anyString())).thenReturn(franchise);
        when(renameBranchUseCase.renameBranch(anyString(), anyString(), anyString())).thenReturn(franchise);
        when(addProductUseCase.addProduct(anyString(), anyString(), anyString(), any())).thenReturn(franchise);
        when(renameProductUseCase.renameProduct(anyString(), anyString(), anyString(), anyString())).thenReturn(franchise);
        when(modifyStockUseCase.modifyStock(anyString(), anyString(), anyString(), any())).thenReturn(franchise);
    }

    private void verifyNoUseCaseInteractions() {
        verifyNoInteractions(createFranchiseUseCase, renameFranchiseUseCase, addBranchUseCase,
                renameBranchUseCase, addProductUseCase, removeProductUseCase, modifyStockUseCase,
                renameProductUseCase, getTopStockProductByBranchUseCase);
    }

    private static Franchise existingFranchise() {
        Product product = Product.builder().id(TestIds.PRODUCT_ID).name("Product").stock(1).build();
        Branch branch = Branch.builder().id(TestIds.BRANCH_ID).name("Branch").products(List.of(product)).build();
        return Franchise.builder().id(TestIds.FRANCHISE_ID).name("Franchise").branches(List.of(branch)).build();
    }
}

package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.AddBranchRequest;
import co.com.bancolombia.api.dto.AddProductRequest;
import co.com.bancolombia.api.dto.CreateFranchiseRequest;
import co.com.bancolombia.api.dto.ModifyStockRequest;
import co.com.bancolombia.api.dto.RenameRequest;
import co.com.bancolombia.api.health.HealthHandler;
import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.vo.TopStockProduct;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RouterRest.class, Handler.class, HealthHandler.class})
@WebFluxTest
class HandlerTest {

    private static final String FRANCHISE_ID = "franchise-1";
    private static final String BRANCH_ID = "branch-1";
    private static final String PRODUCT_ID = "product-1";

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
    void shouldHealthCheckAndReturn200() {
        webTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldCreateFranchiseAndReturn201() {
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").build();
        when(createFranchiseUseCase.createFranchise("Juan Valdez")).thenReturn(Mono.just(franchise));

        webTestClient.post()
                .uri("/api/franchises")
                .bodyValue(new CreateFranchiseRequest("Juan Valdez"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(FRANCHISE_ID)
                .jsonPath("$.name").isEqualTo("Juan Valdez");
    }

    @Test
    void shouldReturn400WhenCreatingFranchiseWithBlankName() {
        when(createFranchiseUseCase.createFranchise(""))
                .thenReturn(Mono.error(new BusinessException("Franchise name must not be blank")));

        webTestClient.post()
                .uri("/api/franchises")
                .bodyValue(new CreateFranchiseRequest(""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldRenameFranchiseAndReturn200() {
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez Cafe").build();
        when(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "Juan Valdez Cafe")).thenReturn(Mono.just(franchise));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}", FRANCHISE_ID)
                .bodyValue(new RenameRequest("Juan Valdez Cafe"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(FRANCHISE_ID)
                .jsonPath("$.name").isEqualTo("Juan Valdez Cafe");
    }

    @Test
    void shouldReturn404WhenRenamingNonexistentFranchise() {
        when(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "Juan Valdez Cafe"))
                .thenReturn(Mono.error(new NotFoundException("Franchise not found: " + FRANCHISE_ID)));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}", FRANCHISE_ID)
                .bodyValue(new RenameRequest("Juan Valdez Cafe"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldAddBranchAndReturn201() {
        Product product = Product.builder().id(PRODUCT_ID).name("Coffee Beans 1kg").stock(50).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Downtown Branch").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").branches(List.of(branch)).build();
        when(addBranchUseCase.addBranch(FRANCHISE_ID, "Downtown Branch")).thenReturn(Mono.just(franchise));

        webTestClient.post()
                .uri("/api/franchises/{franchiseId}/branches", FRANCHISE_ID)
                .bodyValue(new AddBranchRequest("Downtown Branch"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.branches[0].id").isEqualTo(BRANCH_ID)
                .jsonPath("$.branches[0].name").isEqualTo("Downtown Branch");
    }

    @Test
    void shouldReturn404WhenAddingBranchToNonexistentFranchise() {
        when(addBranchUseCase.addBranch(FRANCHISE_ID, "Downtown Branch"))
                .thenReturn(Mono.error(new NotFoundException("Franchise not found: " + FRANCHISE_ID)));

        webTestClient.post()
                .uri("/api/franchises/{franchiseId}/branches", FRANCHISE_ID)
                .bodyValue(new AddBranchRequest("Downtown Branch"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldRenameBranchAndReturn200() {
        Branch branch = Branch.builder().id(BRANCH_ID).name("Uptown Branch").build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").branches(List.of(branch)).build();
        when(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "Uptown Branch")).thenReturn(Mono.just(franchise));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}", FRANCHISE_ID, BRANCH_ID)
                .bodyValue(new RenameRequest("Uptown Branch"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].name").isEqualTo("Uptown Branch");
    }

    @Test
    void shouldReturn404WhenRenamingNonexistentBranch() {
        when(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "Uptown Branch"))
                .thenReturn(Mono.error(new NotFoundException("Branch not found: " + BRANCH_ID)));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}", FRANCHISE_ID, BRANCH_ID)
                .bodyValue(new RenameRequest("Uptown Branch"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldAddProductAndReturn201() {
        Product product = Product.builder().id(PRODUCT_ID).name("Coffee Beans 1kg").stock(50).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Downtown Branch").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").branches(List.of(branch)).build();
        when(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "Coffee Beans 1kg", 50)).thenReturn(Mono.just(franchise));

        webTestClient.post()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products", FRANCHISE_ID, BRANCH_ID)
                .bodyValue(new AddProductRequest("Coffee Beans 1kg", 50))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.branches[0].products[0].id").isEqualTo(PRODUCT_ID)
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Coffee Beans 1kg")
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(50);
    }

    @Test
    void shouldReturn400WhenAddingProductWithNegativeStock() {
        when(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "Coffee Beans 1kg", -5))
                .thenReturn(Mono.error(new BusinessException("Product stock must not be negative")));

        webTestClient.post()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products", FRANCHISE_ID, BRANCH_ID)
                .bodyValue(new AddProductRequest("Coffee Beans 1kg", -5))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldRemoveProductAndReturn200() {
        Branch branch = Branch.builder().id(BRANCH_ID).name("Downtown Branch").build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").branches(List.of(branch)).build();
        when(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.just(franchise));

        webTestClient.delete()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].products").isEmpty();
    }

    @Test
    void shouldReturn404WhenRemovingNonexistentProduct() {
        when(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .thenReturn(Mono.error(new NotFoundException("Product not found: " + PRODUCT_ID)));

        webTestClient.delete()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}", FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldModifyStockAndReturn200() {
        Product product = Product.builder().id(PRODUCT_ID).name("Coffee Beans 1kg").stock(100).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Downtown Branch").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").branches(List.of(branch)).build();
        when(modifyStockUseCase.modifyStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 100)).thenReturn(Mono.just(franchise));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock",
                        FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .bodyValue(new ModifyStockRequest(100))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(100);
    }

    @Test
    void shouldReturn400WhenModifyingStockToNegativeValue() {
        when(modifyStockUseCase.modifyStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, -10))
                .thenReturn(Mono.error(new BusinessException("Stock must not be negative")));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock",
                        FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .bodyValue(new ModifyStockRequest(-10))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldRenameProductAndReturn200() {
        Product product = Product.builder().id(PRODUCT_ID).name("Colombian Coffee Beans 1kg").stock(50).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Downtown Branch").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Juan Valdez").branches(List.of(branch)).build();
        when(renameProductUseCase.renameProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Colombian Coffee Beans 1kg"))
                .thenReturn(Mono.just(franchise));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .bodyValue(new RenameRequest("Colombian Coffee Beans 1kg"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Colombian Coffee Beans 1kg");
    }

    @Test
    void shouldReturn404WhenRenamingProductInNonexistentBranch() {
        when(renameProductUseCase.renameProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Colombian Coffee Beans 1kg"))
                .thenReturn(Mono.error(new NotFoundException("Branch not found: " + BRANCH_ID)));

        webTestClient.patch()
                .uri("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)
                .bodyValue(new RenameRequest("Colombian Coffee Beans 1kg"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldGetTopStockProductsAndReturn200() {
        Product topProduct = Product.builder().id(PRODUCT_ID).name("Coffee Beans 1kg").stock(200).build();
        TopStockProduct topStockProduct = new TopStockProduct(BRANCH_ID, "Downtown Branch", topProduct);
        when(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .thenReturn(Flux.just(topStockProduct));

        webTestClient.get()
                .uri("/api/franchises/{franchiseId}/top-stock-products", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TopStockProduct.class)
                .hasSize(1)
                .contains(topStockProduct);
    }

    @Test
    void shouldReturnEmptyListWhenFranchiseHasNoTopStockProducts() {
        when(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/franchises/{franchiseId}/top-stock-products", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TopStockProduct.class)
                .hasSize(0);
    }

    @Test
    void shouldReturn404WhenGettingTopStockProductsForNonexistentFranchise() {
        when(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .thenReturn(Flux.error(new NotFoundException("Franchise not found: " + FRANCHISE_ID)));

        webTestClient.get()
                .uri("/api/franchises/{franchiseId}/top-stock-products", FRANCHISE_ID)
                .exchange()
                .expectStatus().isNotFound();
    }
}

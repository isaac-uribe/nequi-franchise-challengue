package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductByBranchUseCaseTest {

    private static final String FRANCHISE_ID = "franchise-1";
    private static final String BRANCH_ID = "branch-1";
    private static final String OTHER_BRANCH_ID = "branch-2";

    @Mock
    private FranchiseRepository franchiseRepository;

    private GetTopStockProductByBranchUseCase getTopStockProductByBranchUseCase;

    @BeforeEach
    void setUp() {
        getTopStockProductByBranchUseCase = new GetTopStockProductByBranchUseCase(franchiseRepository);
    }

    private Franchise franchiseWithBranches(Branch... branches) {
        return Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(branches))
                .build();
    }

    @Test
    void shouldReturnTopStockProductPerBranchInOrder() {
        Product lowStock = Product.builder().id("product-1").name("Product One").stock(10).build();
        Product highStock = Product.builder().id("product-2").name("Product Two").stock(50).build();
        Branch firstBranch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(lowStock, highStock))
                .build();

        Product onlyProduct = Product.builder().id("product-3").name("Product Three").stock(5).build();
        Branch secondBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two")
                .products(List.of(onlyProduct))
                .build();

        Franchise franchise = franchiseWithBranches(firstBranch, secondBranch);
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .assertNext(result -> {
                    assertThat(result.branchId()).isEqualTo(BRANCH_ID);
                    assertThat(result.branchName()).isEqualTo("Branch One");
                    assertThat(result.product()).isEqualTo(highStock);
                })
                .assertNext(result -> {
                    assertThat(result.branchId()).isEqualTo(OTHER_BRANCH_ID);
                    assertThat(result.branchName()).isEqualTo("Branch Two");
                    assertThat(result.product()).isEqualTo(onlyProduct);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnFirstProductWhenStockIsTied() {
        Product firstProduct = Product.builder().id("product-1").name("Product One").stock(20).build();
        Product secondProduct = Product.builder().id("product-2").name("Product Two").stock(20).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(firstProduct, secondProduct))
                .build();
        Franchise franchise = franchiseWithBranches(branch);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .assertNext(result -> assertThat(result.product()).isEqualTo(firstProduct))
                .verifyComplete();
    }

    @Test
    void shouldSkipBranchesWithNoProducts() {
        Branch emptyBranch = Branch.builder().id(BRANCH_ID).name("Branch One").build();
        Product onlyProduct = Product.builder().id("product-1").name("Product One").stock(15).build();
        Branch branchWithProduct = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two")
                .products(List.of(onlyProduct))
                .build();
        Franchise franchise = franchiseWithBranches(emptyBranch, branchWithProduct);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .assertNext(result -> {
                    assertThat(result.branchId()).isEqualTo(OTHER_BRANCH_ID);
                    assertThat(result.product()).isEqualTo(onlyProduct);
                })
                .verifyComplete();
    }

    @Test
    void shouldCompleteWithoutElementsWhenFranchiseHasNoBranches() {
        Franchise franchise = franchiseWithBranches();
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenFranchiseNotFound() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise not found: " + FRANCHISE_ID);
                })
                .verify();
    }

    @Test
    void shouldPropagateRepositoryErrorOnFindById() {
        RuntimeException repositoryError = new RuntimeException("lookup failure");
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(getTopStockProductByBranchUseCase.topStockProductsByFranchise(FRANCHISE_ID))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }
}

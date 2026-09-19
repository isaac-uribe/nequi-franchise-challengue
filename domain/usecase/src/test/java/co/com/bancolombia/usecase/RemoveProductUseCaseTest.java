package co.com.bancolombia.usecase;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveProductUseCaseTest {

    private static final String FRANCHISE_ID = "franchise-1";
    private static final String BRANCH_ID = "branch-1";
    private static final String OTHER_BRANCH_ID = "branch-2";
    private static final String PRODUCT_ID = "product-1";

    @Mock
    private FranchiseRepository franchiseRepository;

    private RemoveProductUseCase removeProductUseCase;

    @BeforeEach
    void setUp() {
        removeProductUseCase = new RemoveProductUseCase(franchiseRepository);
    }

    private Franchise franchiseWithBranches(Branch... branches) {
        return Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(branches))
                .build();
    }

    @Test
    void shouldRemoveProductWhenBranchAndProductExist() {
        Product productToRemove = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Product otherProduct = Product.builder().id("product-2").name("Product Two").stock(5).build();
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(otherProduct, productToRemove))
                .build();
        Branch otherBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Franchise franchise = franchiseWithBranches(targetBranch, otherBranch);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .assertNext(result -> {
                    assertThat(result.getBranches()).hasSize(2);

                    Branch updatedBranch = result.getBranches().stream()
                            .filter(branch -> branch.getId().equals(BRANCH_ID))
                            .findFirst()
                            .orElseThrow();
                    assertThat(updatedBranch.getProducts()).containsExactly(otherProduct);

                    Branch untouchedBranch = result.getBranches().stream()
                            .filter(branch -> branch.getId().equals(OTHER_BRANCH_ID))
                            .findFirst()
                            .orElseThrow();
                    assertThat(untouchedBranch).isEqualTo(otherBranch);
                })
                .verifyComplete();

        ArgumentCaptor<Franchise> franchiseCaptor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(franchiseCaptor.capture());
        assertThat(franchiseCaptor.getValue().getBranches()).hasSize(2);
    }

    @Test
    void shouldNotMutateOriginalProductsList() {
        Product productToRemove = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        List<Product> originalProducts = List.of(productToRemove);
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One").products(originalProducts).build();
        Franchise franchise = franchiseWithBranches(targetBranch);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(originalProducts).containsExactly(productToRemove);
    }

    @Test
    void shouldFailWhenFranchiseNotFound() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise not found: " + FRANCHISE_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenBranchNotFound() {
        Franchise franchise = franchiseWithBranches(Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build());
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch not found: " + BRANCH_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenProductNotFound() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One").build();
        Franchise franchise = franchiseWithBranches(targetBranch);
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Product not found: " + PRODUCT_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnFindById() {
        RuntimeException repositoryError = new RuntimeException("lookup failure");
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnSave() {
        Product product = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One").products(List.of(product)).build();
        Franchise franchise = franchiseWithBranches(targetBranch);
        RuntimeException repositoryError = new RuntimeException("persistence failure");

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(removeProductUseCase.removeProduct(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }
}

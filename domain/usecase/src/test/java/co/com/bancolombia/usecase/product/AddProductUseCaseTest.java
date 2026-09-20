package co.com.bancolombia.usecase.product;

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
class AddProductUseCaseTest {

    private static final String FRANCHISE_ID = "franchise-1";
    private static final String BRANCH_ID = "branch-1";
    private static final String OTHER_BRANCH_ID = "branch-2";

    @Mock
    private FranchiseRepository franchiseRepository;

    private AddProductUseCase addProductUseCase;

    @BeforeEach
    void setUp() {
        addProductUseCase = new AddProductUseCase(franchiseRepository);
    }

    private Franchise franchiseWithBranches(Branch... branches) {
        return Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(branches))
                .build();
    }

    @Test
    void shouldAddProductWhenBranchExistsAndDataIsValid() {
        Product existingProduct = Product.builder().id("product-0").name("Existing Product").stock(5).build();
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One").products(List.of(existingProduct)).build();
        Branch otherBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Franchise franchise = franchiseWithBranches(targetBranch, otherBranch);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", 10))
                .assertNext(result -> {
                    assertThat(result.getBranches()).hasSize(2);

                    Branch updatedBranch = result.getBranches().stream()
                            .filter(branch -> branch.getId().equals(BRANCH_ID))
                            .findFirst()
                            .orElseThrow();
                    assertThat(updatedBranch.getProducts()).hasSize(2);
                    assertThat(updatedBranch.getProducts().get(0)).isEqualTo(existingProduct);
                    Product addedProduct = updatedBranch.getProducts().get(1);
                    assertThat(addedProduct.getId()).isNotBlank();
                    assertThat(addedProduct.getName()).isEqualTo("New Product");
                    assertThat(addedProduct.getStock()).isEqualTo(10);

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
    void shouldAllowZeroStock() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One").build();
        Franchise franchise = franchiseWithBranches(targetBranch);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", 0))
                .assertNext(result -> {
                    Product addedProduct = result.getBranches().get(0).getProducts().get(0);
                    assertThat(addedProduct.getStock()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void shouldFailWhenProductNameIsBlank() {
        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "   ", 10))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Product name must not be blank");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenProductNameIsNull() {
        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, null, 10))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(BusinessException.class))
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    @Test
    void shouldFailWhenStockIsNull() {
        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", null))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Product stock must not be negative");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenStockIsNegative() {
        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", -1))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Product stock must not be negative");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
    }

    @Test
    void shouldFailWhenFranchiseNotFound() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", 10))
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

        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", 10))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch not found: " + BRANCH_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnFindById() {
        RuntimeException repositoryError = new RuntimeException("lookup failure");
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", 10))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnSave() {
        Franchise franchise = franchiseWithBranches(Branch.builder().id(BRANCH_ID).name("Branch One").build());
        RuntimeException repositoryError = new RuntimeException("persistence failure");

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(addProductUseCase.addProduct(FRANCHISE_ID, BRANCH_ID, "New Product", 10))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }
}

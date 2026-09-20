package co.com.bancolombia.usecase.support;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FranchiseAggregateSupportTest {

    private static final String FRANCHISE_ID = "franchise-1";
    private static final String BRANCH_ID = "branch-1";
    private static final String OTHER_BRANCH_ID = "branch-2";
    private static final String PRODUCT_ID = "product-1";
    private static final String OTHER_PRODUCT_ID = "product-2";

    @Test
    void findBranchShouldEmitMatchingBranch() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Branch One").build();
        Branch otherBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(targetBranch, otherBranch))
                .build();

        StepVerifier.create(FranchiseAggregateSupport.findBranch(franchise, BRANCH_ID))
                .expectNext(targetBranch)
                .verifyComplete();
    }

    @Test
    void findBranchShouldFailWhenBranchIsMissing() {
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build()))
                .build();

        StepVerifier.create(FranchiseAggregateSupport.findBranch(franchise, BRANCH_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch not found: " + BRANCH_ID);
                })
                .verify();
    }

    @Test
    void findBranchShouldFailWhenFranchiseHasNoBranches() {
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Franchise One").build();

        StepVerifier.create(FranchiseAggregateSupport.findBranch(franchise, BRANCH_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch not found: " + BRANCH_ID);
                })
                .verify();
    }

    @Test
    void findProductShouldEmitMatchingProduct() {
        Product targetProduct = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Product otherProduct = Product.builder().id(OTHER_PRODUCT_ID).name("Product Two").stock(5).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(otherProduct, targetProduct))
                .build();

        StepVerifier.create(FranchiseAggregateSupport.findProduct(branch, PRODUCT_ID))
                .expectNext(targetProduct)
                .verifyComplete();
    }

    @Test
    void findProductShouldFailWhenProductIsMissing() {
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(Product.builder().id(OTHER_PRODUCT_ID).name("Product Two").stock(5).build()))
                .build();

        StepVerifier.create(FranchiseAggregateSupport.findProduct(branch, PRODUCT_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Product not found: " + PRODUCT_ID);
                })
                .verify();
    }

    @Test
    void findProductShouldFailWhenBranchHasNoProducts() {
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One").build();

        StepVerifier.create(FranchiseAggregateSupport.findProduct(branch, PRODUCT_ID))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Product not found: " + PRODUCT_ID);
                })
                .verify();
    }

    @Test
    void replaceBranchShouldReplaceOnlyTheMatchingBranch() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Old Name").build();
        Branch otherBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(targetBranch, otherBranch))
                .build();
        Branch renamedBranch = targetBranch.toBuilder().name("New Name").build();

        Franchise result = FranchiseAggregateSupport.replaceBranch(franchise, BRANCH_ID, renamedBranch);

        assertThat(result.getBranches()).hasSize(2);
        assertThat(result.getBranches()).containsExactly(renamedBranch, otherBranch);
    }

    @Test
    void replaceBranchShouldNotMutateOriginalBranchesList() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Old Name").build();
        List<Branch> originalBranches = List.of(targetBranch);
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(originalBranches)
                .build();
        Branch renamedBranch = targetBranch.toBuilder().name("New Name").build();

        FranchiseAggregateSupport.replaceBranch(franchise, BRANCH_ID, renamedBranch);

        assertThat(originalBranches).containsExactly(targetBranch);
        assertThat(originalBranches.get(0).getName()).isEqualTo("Old Name");
    }

    @Test
    void replaceProductShouldReplaceOnlyTheMatchingProduct() {
        Product targetProduct = Product.builder().id(PRODUCT_ID).name("Old Name").stock(10).build();
        Product otherProduct = Product.builder().id(OTHER_PRODUCT_ID).name("Product Two").stock(5).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(targetProduct, otherProduct))
                .build();
        Product renamedProduct = targetProduct.toBuilder().name("New Name").build();

        Branch result = FranchiseAggregateSupport.replaceProduct(branch, PRODUCT_ID, renamedProduct);

        assertThat(result.getProducts()).hasSize(2);
        assertThat(result.getProducts()).containsExactly(renamedProduct, otherProduct);
    }

    @Test
    void replaceProductShouldNotMutateOriginalProductsList() {
        Product targetProduct = Product.builder().id(PRODUCT_ID).name("Old Name").stock(10).build();
        List<Product> originalProducts = List.of(targetProduct);
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One").products(originalProducts).build();
        Product renamedProduct = targetProduct.toBuilder().name("New Name").build();

        FranchiseAggregateSupport.replaceProduct(branch, PRODUCT_ID, renamedProduct);

        assertThat(originalProducts).containsExactly(targetProduct);
        assertThat(originalProducts.get(0).getName()).isEqualTo("Old Name");
    }
}

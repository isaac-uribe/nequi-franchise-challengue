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
    void replaceBranchShouldReplaceOnlyTheMatchingBranchAndKeepOrder() {
        Branch firstBranch = Branch.builder().id("branch-0").name("Branch Zero").build();
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Old Name").build();
        Branch lastBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(firstBranch, targetBranch, lastBranch))
                .build();
        Branch renamedBranch = targetBranch.toBuilder().name("New Name").build();

        StepVerifier.create(FranchiseAggregateSupport.replaceBranch(franchise, BRANCH_ID, renamedBranch))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(FRANCHISE_ID);
                    assertThat(result.getName()).isEqualTo("Franchise One");
                    assertThat(result.getBranches()).containsExactly(firstBranch, renamedBranch, lastBranch);
                })
                .verifyComplete();
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

        StepVerifier.create(FranchiseAggregateSupport.replaceBranch(franchise, BRANCH_ID, renamedBranch))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(originalBranches).containsExactly(targetBranch);
        assertThat(franchise.getBranches()).containsExactly(targetBranch);
    }

    @Test
    void replaceProductShouldReplaceOnlyTheMatchingProductAndKeepOrder() {
        Product firstProduct = Product.builder().id("product-0").name("Product Zero").stock(1).build();
        Product targetProduct = Product.builder().id(PRODUCT_ID).name("Old Name").stock(10).build();
        Product lastProduct = Product.builder().id(OTHER_PRODUCT_ID).name("Product Two").stock(5).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(firstProduct, targetProduct, lastProduct))
                .build();
        Product renamedProduct = targetProduct.toBuilder().name("New Name").build();

        StepVerifier.create(FranchiseAggregateSupport.replaceProduct(branch, PRODUCT_ID, renamedProduct))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(BRANCH_ID);
                    assertThat(result.getName()).isEqualTo("Branch One");
                    assertThat(result.getProducts()).containsExactly(firstProduct, renamedProduct, lastProduct);
                })
                .verifyComplete();
    }

    @Test
    void replaceProductShouldNotMutateOriginalProductsList() {
        Product targetProduct = Product.builder().id(PRODUCT_ID).name("Old Name").stock(10).build();
        List<Product> originalProducts = List.of(targetProduct);
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One").products(originalProducts).build();
        Product renamedProduct = targetProduct.toBuilder().name("New Name").build();

        StepVerifier.create(FranchiseAggregateSupport.replaceProduct(branch, PRODUCT_ID, renamedProduct))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(originalProducts).containsExactly(targetProduct);
        assertThat(branch.getProducts()).containsExactly(targetProduct);
    }

    @Test
    void addProductShouldAppendProductAtTheEnd() {
        Product existingProduct = Product.builder().id(OTHER_PRODUCT_ID).name("Product Two").stock(5).build();
        Product newProduct = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One").products(List.of(existingProduct)).build();

        StepVerifier.create(FranchiseAggregateSupport.addProduct(branch, newProduct))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(BRANCH_ID);
                    assertThat(result.getProducts()).containsExactly(existingProduct, newProduct);
                })
                .verifyComplete();

        assertThat(branch.getProducts()).containsExactly(existingProduct);
    }

    @Test
    void addProductShouldWorkOnBranchWithoutProducts() {
        Product newProduct = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One").build();

        StepVerifier.create(FranchiseAggregateSupport.addProduct(branch, newProduct))
                .assertNext(result -> assertThat(result.getProducts()).containsExactly(newProduct))
                .verifyComplete();
    }

    @Test
    void removeProductShouldRemoveOnlyTheMatchingProductAndKeepOrder() {
        Product firstProduct = Product.builder().id("product-0").name("Product Zero").stock(1).build();
        Product targetProduct = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Product lastProduct = Product.builder().id(OTHER_PRODUCT_ID).name("Product Two").stock(5).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One")
                .products(List.of(firstProduct, targetProduct, lastProduct))
                .build();

        StepVerifier.create(FranchiseAggregateSupport.removeProduct(branch, PRODUCT_ID))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(BRANCH_ID);
                    assertThat(result.getProducts()).containsExactly(firstProduct, lastProduct);
                })
                .verifyComplete();

        assertThat(branch.getProducts()).containsExactly(firstProduct, targetProduct, lastProduct);
    }

    @Test
    void removeProductShouldLeaveEmptyListWhenRemovingTheOnlyProduct() {
        Product onlyProduct = Product.builder().id(PRODUCT_ID).name("Product One").stock(10).build();
        Branch branch = Branch.builder().id(BRANCH_ID).name("Branch One").products(List.of(onlyProduct)).build();

        StepVerifier.create(FranchiseAggregateSupport.removeProduct(branch, PRODUCT_ID))
                .assertNext(result -> assertThat(result.getProducts()).isEmpty())
                .verifyComplete();
    }

    @Test
    void addBranchShouldAppendBranchAtTheEnd() {
        Branch existingBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Branch newBranch = Branch.builder().id(BRANCH_ID).name("Branch One").build();
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(existingBranch))
                .build();

        StepVerifier.create(FranchiseAggregateSupport.addBranch(franchise, newBranch))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(FRANCHISE_ID);
                    assertThat(result.getBranches()).containsExactly(existingBranch, newBranch);
                })
                .verifyComplete();

        assertThat(franchise.getBranches()).containsExactly(existingBranch);
    }

    @Test
    void addBranchShouldWorkOnFranchiseWithoutBranches() {
        Branch newBranch = Branch.builder().id(BRANCH_ID).name("Branch One").build();
        Franchise franchise = Franchise.builder().id(FRANCHISE_ID).name("Franchise One").build();

        StepVerifier.create(FranchiseAggregateSupport.addBranch(franchise, newBranch))
                .assertNext(result -> assertThat(result.getBranches()).containsExactly(newBranch))
                .verifyComplete();
    }
}

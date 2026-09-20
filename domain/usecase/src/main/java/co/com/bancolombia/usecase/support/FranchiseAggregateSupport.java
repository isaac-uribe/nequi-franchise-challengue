package co.com.bancolombia.usecase.support;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public final class FranchiseAggregateSupport {

    private FranchiseAggregateSupport() {
    }

    public static Mono<Branch> findBranch(Franchise franchise, String branchId) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new NotFoundException("Branch not found: " + branchId)));
    }

    public static Mono<Product> findProduct(Branch branch, String productId) {
        return Flux.fromIterable(branch.getProducts())
                .filter(product -> product.getId().equals(productId))
                .next()
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found: " + productId)));
    }

    public static Franchise replaceBranch(Franchise franchise, String branchId, Branch updatedBranch) {
        List<Branch> updatedBranches = new ArrayList<>();
        for (Branch branch : franchise.getBranches()) {
            updatedBranches.add(branch.getId().equals(branchId) ? updatedBranch : branch);
        }
        return franchise.toBuilder().branches(updatedBranches).build();
    }

    public static Branch replaceProduct(Branch branch, String productId, Product updatedProduct) {
        List<Product> updatedProducts = new ArrayList<>();
        for (Product product : branch.getProducts()) {
            updatedProducts.add(product.getId().equals(productId) ? updatedProduct : product);
        }
        return branch.toBuilder().products(updatedProducts).build();
    }
}

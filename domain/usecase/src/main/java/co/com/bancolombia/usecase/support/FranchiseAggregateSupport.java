package co.com.bancolombia.usecase.support;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class FranchiseAggregateSupport {

    private FranchiseAggregateSupport() {
    }

    public static Mono<Branch> findBranch(Franchise franchise, String branchId) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Branch not found: " + branchId)));
    }

    public static Mono<Product> findProduct(Branch branch, String productId) {
        return Flux.fromIterable(branch.getProducts())
                .filter(product -> product.getId().equals(productId))
                .next()
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Product not found: " + productId)));
    }

    public static Mono<Franchise> replaceBranch(Franchise franchise, String branchId, Branch updatedBranch) {
        return Flux.fromIterable(franchise.getBranches())
                .map(branch -> branch.getId().equals(branchId) ? updatedBranch : branch)
                .collectList()
                .map(branches -> franchise.toBuilder().branches(branches).build());
    }

    public static Mono<Branch> replaceProduct(Branch branch, String productId, Product updatedProduct) {
        return Flux.fromIterable(branch.getProducts())
                .map(product -> product.getId().equals(productId) ? updatedProduct : product)
                .collectList()
                .map(products -> branch.toBuilder().products(products).build());
    }

    public static Mono<Branch> addProduct(Branch branch, Product product) {
        return Flux.concat(Flux.fromIterable(branch.getProducts()), Flux.just(product))
                .collectList()
                .map(products -> branch.toBuilder().products(products).build());
    }

    public static Mono<Branch> removeProduct(Branch branch, String productId) {
        return Flux.fromIterable(branch.getProducts())
                .filter(product -> !product.getId().equals(productId))
                .collectList()
                .map(products -> branch.toBuilder().products(products).build());
    }

    public static Mono<Franchise> addBranch(Franchise franchise, Branch branch) {
        return Flux.concat(Flux.fromIterable(franchise.getBranches()), Flux.just(branch))
                .collectList()
                .map(branches -> franchise.toBuilder().branches(branches).build());
    }
}

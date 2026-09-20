package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RemoveProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> removeProduct(String franchiseId, String branchId, String productId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                .flatMap(franchise -> removeProductFromBranch(franchise, branchId, productId))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> removeProductFromBranch(Franchise franchise, String branchId, String productId) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new BusinessException("Branch not found: " + branchId)))
                .flatMap(targetBranch -> Flux.fromIterable(targetBranch.getProducts())
                        .filter(product -> product.getId().equals(productId))
                        .next()
                        .switchIfEmpty(Mono.error(new BusinessException("Product not found: " + productId)))
                        .map(existingProduct -> replaceProducts(franchise, targetBranch, branchId, existingProduct)));
    }

    private Franchise replaceProducts(Franchise franchise, Branch targetBranch, String branchId, Product toRemove) {
        List<Product> updatedProducts = new ArrayList<>(targetBranch.getProducts());
        updatedProducts.remove(toRemove);
        Branch updatedBranch = targetBranch.toBuilder().products(updatedProducts).build();

        List<Branch> updatedBranches = new ArrayList<>();
        for (Branch branch : franchise.getBranches()) {
            updatedBranches.add(branch.getId().equals(branchId) ? updatedBranch : branch);
        }
        return franchise.toBuilder().branches(updatedBranches).build();
    }
}

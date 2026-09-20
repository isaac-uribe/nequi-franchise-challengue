package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RemoveProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> removeProduct(String franchiseId, String branchId, String productId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + franchiseId)))
                .flatMap(franchise -> removeProductFromBranch(franchise, branchId, productId))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> removeProductFromBranch(Franchise franchise, String branchId, String productId) {
        return FranchiseAggregateSupport.findBranch(franchise, branchId)
                .flatMap(targetBranch -> FranchiseAggregateSupport.findProduct(targetBranch, productId)
                        .map(existingProduct -> replaceProducts(franchise, targetBranch, branchId, existingProduct)));
    }

    private Franchise replaceProducts(Franchise franchise, Branch targetBranch, String branchId, Product toRemove) {
        List<Product> updatedProducts = new ArrayList<>(targetBranch.getProducts());
        updatedProducts.remove(toRemove);
        Branch updatedBranch = targetBranch.toBuilder().products(updatedProducts).build();
        return FranchiseAggregateSupport.replaceBranch(franchise, branchId, updatedBranch);
    }
}

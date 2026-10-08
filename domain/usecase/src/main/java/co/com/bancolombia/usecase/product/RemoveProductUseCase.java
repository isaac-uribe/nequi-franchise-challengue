package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

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
                        .flatMap(existingProduct -> FranchiseAggregateSupport.removeProduct(targetBranch, productId)))
                .flatMap(updatedBranch -> FranchiseAggregateSupport.replaceBranch(franchise, branchId, updatedBranch));
    }
}

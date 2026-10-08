package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ModifyStockUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> modifyStock(String franchiseId, String branchId, String productId, Integer newStock) {
        return Mono.justOrEmpty(newStock)
                .filter(stock -> stock >= 0)
                .switchIfEmpty(Mono.error(new BusinessException("Stock must not be negative")))
                .flatMap(stock -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> updateStock(franchise, branchId, productId, stock)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> updateStock(Franchise franchise, String branchId, String productId, Integer newStock) {
        return FranchiseAggregateSupport.findBranch(franchise, branchId)
                .flatMap(targetBranch -> FranchiseAggregateSupport.findProduct(targetBranch, productId)
                        .map(existingProduct -> existingProduct.toBuilder().stock(newStock).build())
                        .flatMap(updatedProduct -> FranchiseAggregateSupport.replaceProduct(targetBranch, productId, updatedProduct)))
                .flatMap(updatedBranch -> FranchiseAggregateSupport.replaceBranch(franchise, branchId, updatedBranch));
    }
}

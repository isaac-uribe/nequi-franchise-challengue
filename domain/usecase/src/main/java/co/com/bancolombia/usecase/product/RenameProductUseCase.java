package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameProduct(String franchiseId, String branchId, String productId, String newName) {
        return Mono.justOrEmpty(newName)
                .filter(name -> !name.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Product name must not be blank")))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> renameProductInFranchise(franchise, branchId, productId, name)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> renameProductInFranchise(Franchise franchise, String branchId, String productId, String newName) {
        return FranchiseAggregateSupport.findBranch(franchise, branchId)
                .flatMap(targetBranch -> FranchiseAggregateSupport.findProduct(targetBranch, productId)
                        .map(existingProduct -> {
                            Product renamedProduct = existingProduct.toBuilder().name(newName).build();
                            Branch updatedBranch = FranchiseAggregateSupport.replaceProduct(targetBranch, productId, renamedProduct);
                            return FranchiseAggregateSupport.replaceBranch(franchise, branchId, updatedBranch);
                        }));
    }
}

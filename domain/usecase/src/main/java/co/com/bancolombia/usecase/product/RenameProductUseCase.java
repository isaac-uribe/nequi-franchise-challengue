package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import co.com.bancolombia.usecase.support.InputValidator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameProduct(String franchiseId, String branchId, String productId, String newName) {
        return InputValidator.validId(franchiseId, "Franchise")
                .then(InputValidator.validId(branchId, "Branch"))
                .then(InputValidator.validId(productId, "Product"))
                .then(InputValidator.validName(newName, "Product"))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(() -> new NotFoundException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> renameProductInFranchise(franchise, branchId, productId, name)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> renameProductInFranchise(Franchise franchise, String branchId, String productId, String newName) {
        return FranchiseAggregateSupport.findBranch(franchise, branchId)
                .flatMap(targetBranch -> FranchiseAggregateSupport.findProduct(targetBranch, productId)
                        .map(existingProduct -> existingProduct.toBuilder().name(newName).build())
                        .flatMap(renamedProduct -> FranchiseAggregateSupport.replaceProduct(targetBranch, productId, renamedProduct)))
                .flatMap(updatedBranch -> FranchiseAggregateSupport.replaceBranch(franchise, branchId, updatedBranch));
    }
}

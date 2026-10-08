package co.com.bancolombia.usecase.branch;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import co.com.bancolombia.usecase.support.InputValidator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameBranch(String franchiseId, String branchId, String newName) {
        return InputValidator.validId(franchiseId, "Franchise")
                .then(InputValidator.validId(branchId, "Branch"))
                .then(InputValidator.validName(newName, "Branch"))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(() -> new NotFoundException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> renameBranchInFranchise(franchise, branchId, name)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> renameBranchInFranchise(Franchise franchise, String branchId, String newName) {
        return FranchiseAggregateSupport.findBranch(franchise, branchId)
                .map(targetBranch -> targetBranch.toBuilder().name(newName).build())
                .flatMap(renamedBranch -> FranchiseAggregateSupport.replaceBranch(franchise, branchId, renamedBranch));
    }
}

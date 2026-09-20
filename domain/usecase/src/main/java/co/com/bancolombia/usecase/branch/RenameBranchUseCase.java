package co.com.bancolombia.usecase.branch;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RenameBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameBranch(String franchiseId, String branchId, String newName) {
        return Mono.justOrEmpty(newName)
                .filter(name -> !name.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Branch name must not be blank")))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> renameBranchInFranchise(franchise, branchId, name)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> renameBranchInFranchise(Franchise franchise, String branchId, String newName) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new BusinessException("Branch not found: " + branchId)))
                .map(targetBranch -> {
                    Branch renamedBranch = targetBranch.toBuilder().name(newName).build();
                    List<Branch> updatedBranches = new ArrayList<>();
                    for (Branch branch : franchise.getBranches()) {
                        updatedBranches.add(branch.getId().equals(branchId) ? renamedBranch : branch);
                    }
                    return franchise.toBuilder().branches(updatedBranches).build();
                });
    }
}

package co.com.bancolombia.usecase.branch;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AddBranchUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> addBranch(String franchiseId, String branchName) {
        return Mono.justOrEmpty(branchName)
                .filter(name -> !name.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Branch name must not be blank")))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + franchiseId)))
                        .map(franchise -> appendBranch(franchise, name)))
                .flatMap(franchiseRepository::save);
    }

    private Franchise appendBranch(Franchise franchise, String branchName) {
        Branch newBranch = Branch.builder()
                .id(UUID.randomUUID().toString())
                .name(branchName)
                .build();

        List<Branch> updatedBranches = new ArrayList<>(franchise.getBranches());
        updatedBranches.add(newBranch);

        return franchise.toBuilder()
                .branches(updatedBranches)
                .build();
    }

}

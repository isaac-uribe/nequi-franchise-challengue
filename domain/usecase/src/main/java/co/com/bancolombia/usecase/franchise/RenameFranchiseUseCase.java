package co.com.bancolombia.usecase.franchise;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameFranchise(String franchiseId, String newName) {
        return Mono.justOrEmpty(newName)
                .filter(name -> !name.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Franchise name must not be blank")))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                        .map(franchise -> franchise.toBuilder().name(name).build()))
                .flatMap(franchiseRepository::save);
    }

}

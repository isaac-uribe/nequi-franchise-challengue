package co.com.bancolombia.usecase.franchise;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.InputValidator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameFranchise(String franchiseId, String newName) {
        return InputValidator.validId(franchiseId, "Franchise")
                .then(InputValidator.validName(newName, "Franchise"))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(() -> new NotFoundException("Franchise not found: " + franchiseId)))
                        .map(franchise -> franchise.toBuilder().name(name).build()))
                .flatMap(franchiseRepository::save);
    }

}

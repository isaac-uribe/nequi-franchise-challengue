package co.com.bancolombia.usecase.franchise;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.InputValidator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> createFranchise(String name) {
        return InputValidator.validName(name, "Franchise")
                .map(n -> Franchise.builder()
                        .id(UUID.randomUUID().toString())
                        .name(n)
                        .build())
                .flatMap(franchiseRepository::save);
    }
}

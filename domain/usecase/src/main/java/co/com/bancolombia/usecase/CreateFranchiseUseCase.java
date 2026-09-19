package co.com.bancolombia.usecase;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> createFranchise(String name) {
        return Mono.justOrEmpty(name)
                .filter(n -> !n.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Franchise name must not be blank")))
                .map(n -> Franchise.builder()
                        .id(UUID.randomUUID().toString())
                        .name(n)
                        .build())
                .flatMap(franchiseRepository::save);
    }
}

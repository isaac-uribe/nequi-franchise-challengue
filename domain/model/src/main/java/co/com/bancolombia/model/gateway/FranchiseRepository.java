package co.com.bancolombia.model.gateway;

import co.com.bancolombia.model.aggregate.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepository {
    Mono<Franchise> save(Franchise franchise);
    Mono<Franchise> findById(String id);
}

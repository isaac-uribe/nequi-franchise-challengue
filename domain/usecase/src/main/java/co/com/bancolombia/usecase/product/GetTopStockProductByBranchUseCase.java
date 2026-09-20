package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.model.vo.TopStockProduct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetTopStockProductByBranchUseCase {
    private final FranchiseRepository franchiseRepository;

    public Flux<TopStockProduct> topStockProductsByFranchise(String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + franchiseId)))
                .flatMapMany(franchise -> Flux.fromIterable(franchise.getBranches()))
                .concatMap(branch -> Flux.fromIterable(branch.getProducts())
                        .reduce((p1, p2) -> p1.getStock() >= p2.getStock() ? p1 : p2)
                        .map(topProduct -> new TopStockProduct(branch.getId(), branch.getName(), topProduct)));
    }
}

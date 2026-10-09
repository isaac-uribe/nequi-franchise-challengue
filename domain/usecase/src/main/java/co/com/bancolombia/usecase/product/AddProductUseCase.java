package co.com.bancolombia.usecase.product;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.NotFoundException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.support.FranchiseAggregateSupport;
import co.com.bancolombia.usecase.support.InputValidator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class AddProductUseCase {
    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> addProduct(String franchiseId, String branchId, String productName, Integer stock) {
        return InputValidator.validId(franchiseId, "Franchise")
                .then(InputValidator.validId(branchId, "Branch"))
                .then(InputValidator.validName(productName, "Product"))
                .flatMap(name -> InputValidator.validStock(stock)
                        .map(validStock -> Product.builder()
                                .id(UUID.randomUUID().toString())
                                .name(name)
                                .stock(validStock)
                                .build()))
                .flatMap(product -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(() -> new NotFoundException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> addProductToBranch(franchise, branchId, product)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> addProductToBranch(Franchise franchise, String branchId, Product product) {
        return FranchiseAggregateSupport.findBranch(franchise, branchId)
                .flatMap(targetBranch -> FranchiseAggregateSupport.addProduct(targetBranch, product))
                .flatMap(updatedBranch -> FranchiseAggregateSupport.replaceBranch(franchise, branchId, updatedBranch));
    }
}

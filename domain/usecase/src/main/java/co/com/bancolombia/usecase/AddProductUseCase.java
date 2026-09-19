package co.com.bancolombia.usecase;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AddProductUseCase {
    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> addProduct(String franchiseId, String branchId, String productName, Integer stock) {
        return Mono.justOrEmpty(productName)
                .filter(name -> !name.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Product name must not be blank")))
                .flatMap(name -> Mono.justOrEmpty(stock)
                        .filter(s -> s >= 0)
                        .switchIfEmpty(Mono.error(new BusinessException("Product stock must not be negative")))
                        .map(validStock -> Product.builder()
                                .id(UUID.randomUUID().toString())
                                .name(name)
                                .stock(validStock)
                                .build()))
                .flatMap(product -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> addProductToBranch(franchise, branchId, product)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> addProductToBranch(Franchise franchise, String branchId, Product product) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new BusinessException("Branch not found: " + branchId)))
                .map(targetBranch -> {
                    List<Product> updatedProducts = new ArrayList<>(targetBranch.getProducts());
                    updatedProducts.add(product);
                    Branch updatedBranch = targetBranch.toBuilder().products(updatedProducts).build();

                    List<Branch> updatedBranches = new ArrayList<>();
                    for (Branch branch : franchise.getBranches()) {
                        updatedBranches.add(branch.getId().equals(branchId) ? updatedBranch : branch);
                    }
                    return franchise.toBuilder().branches(updatedBranches).build();
                });
    }
}

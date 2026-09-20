package co.com.bancolombia.usecase.product;

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

@RequiredArgsConstructor
public class ModifyStockUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> modifyStock(String franchiseId, String branchId, String productId, Integer newStock) {
        return Mono.justOrEmpty(newStock)
                .filter(stock -> stock >= 0)
                .switchIfEmpty(Mono.error(new BusinessException("Stock must not be negative")))
                .flatMap(stock -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> updateStock(franchise, branchId, productId, stock)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> updateStock(Franchise franchise, String branchId, String productId, Integer newStock) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new BusinessException("Branch not found: " + branchId)))
                .flatMap(targetBranch -> Flux.fromIterable(targetBranch.getProducts())
                        .filter(product -> product.getId().equals(productId))
                        .next()
                        .switchIfEmpty(Mono.error(new BusinessException("Product not found: " + productId)))
                        .map(existingProduct -> replaceStock(franchise, targetBranch, branchId, existingProduct, newStock)));
    }

    private Franchise replaceStock(Franchise franchise, Branch targetBranch, String branchId,
                                   Product existingProduct, Integer newStock) {
        Product updatedProduct = existingProduct.toBuilder().stock(newStock).build();

        List<Product> updatedProducts = new ArrayList<>();
        for (Product product : targetBranch.getProducts()) {
            updatedProducts.add(product.getId().equals(existingProduct.getId()) ? updatedProduct : product);
        }
        Branch updatedBranch = targetBranch.toBuilder().products(updatedProducts).build();

        List<Branch> updatedBranches = new ArrayList<>();
        for (Branch branch : franchise.getBranches()) {
            updatedBranches.add(branch.getId().equals(branchId) ? updatedBranch : branch);
        }
        return franchise.toBuilder().branches(updatedBranches).build();
    }

}

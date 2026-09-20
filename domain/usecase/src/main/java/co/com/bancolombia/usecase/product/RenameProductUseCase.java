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
public class RenameProductUseCase {

    private final FranchiseRepository franchiseRepository;

    public Mono<Franchise> renameProduct(String franchiseId, String branchId, String productId, String newName) {
        return Mono.justOrEmpty(newName)
                .filter(name -> !name.isBlank())
                .switchIfEmpty(Mono.error(new BusinessException("Product name must not be blank")))
                .flatMap(name -> franchiseRepository.findById(franchiseId)
                        .switchIfEmpty(Mono.error(new BusinessException("Franchise not found: " + franchiseId)))
                        .flatMap(franchise -> renameProductInFranchise(franchise, branchId, productId, name)))
                .flatMap(franchiseRepository::save);
    }

    private Mono<Franchise> renameProductInFranchise(Franchise franchise, String branchId, String productId, String newName) {
        return Flux.fromIterable(franchise.getBranches())
                .filter(branch -> branch.getId().equals(branchId))
                .next()
                .switchIfEmpty(Mono.error(new BusinessException("Branch not found: " + branchId)))
                .flatMap(targetBranch -> Flux.fromIterable(targetBranch.getProducts())
                        .filter(product -> product.getId().equals(productId))
                        .next()
                        .switchIfEmpty(Mono.error(new BusinessException("Product not found: " + productId)))
                        .map(existingProduct -> {
                            Product renamedProduct = existingProduct.toBuilder().name(newName).build();

                            List<Product> updatedProducts = new ArrayList<>();
                            for (Product product : targetBranch.getProducts()) {
                                updatedProducts.add(product.getId().equals(productId) ? renamedProduct : product);
                            }
                            Branch updatedBranch = targetBranch.toBuilder().products(updatedProducts).build();

                            List<Branch> updatedBranches = new ArrayList<>();
                            for (Branch branch : franchise.getBranches()) {
                                updatedBranches.add(branch.getId().equals(branchId) ? updatedBranch : branch);
                            }
                            return franchise.toBuilder().branches(updatedBranches).build();
                        }));
    }
}

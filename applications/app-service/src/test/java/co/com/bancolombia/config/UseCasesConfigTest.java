package co.com.bancolombia.config;

import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.branch.AddBranchUseCase;
import co.com.bancolombia.usecase.branch.RenameBranchUseCase;
import co.com.bancolombia.usecase.franchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.franchise.RenameFranchiseUseCase;
import co.com.bancolombia.usecase.product.AddProductUseCase;
import co.com.bancolombia.usecase.product.GetTopStockProductByBranchUseCase;
import co.com.bancolombia.usecase.product.ModifyStockUseCase;
import co.com.bancolombia.usecase.product.RemoveProductUseCase;
import co.com.bancolombia.usecase.product.RenameProductUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class UseCasesConfigTest {

    private static final List<Class<?>> USE_CASES = List.of(
            CreateFranchiseUseCase.class,
            RenameFranchiseUseCase.class,
            AddBranchUseCase.class,
            RenameBranchUseCase.class,
            AddProductUseCase.class,
            RemoveProductUseCase.class,
            ModifyStockUseCase.class,
            RenameProductUseCase.class,
            GetTopStockProductByBranchUseCase.class
    );

    @Test
    void shouldRegisterEveryUseCaseAsBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(FranchiseRepository.class, () -> mock(FranchiseRepository.class));
            context.register(UseCasesConfig.class);
            context.refresh();

            assertAll(USE_CASES.stream().map(useCase -> () ->
                    assertEquals(1, context.getBeanNamesForType(useCase).length,
                            useCase.getSimpleName() + " should be registered exactly once")));
        }
    }
}

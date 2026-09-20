package co.com.bancolombia.usecase.franchise;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    private CreateFranchiseUseCase createFranchiseUseCase;

    @BeforeEach
    void setUp() {
        createFranchiseUseCase = new CreateFranchiseUseCase(franchiseRepository);
    }

    @Test
    void shouldCreateFranchiseWhenNameIsValid() {
        String name = "Franchise One";
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(createFranchiseUseCase.createFranchise(name))
                .assertNext(franchise -> {
                    assertThat(franchise.getId()).isNotBlank();
                    assertThat(franchise.getName()).isEqualTo(name);
                    assertThat(franchise.getBranches()).isEmpty();
                })
                .verifyComplete();

        ArgumentCaptor<Franchise> franchiseCaptor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(franchiseCaptor.capture());
        assertThat(franchiseCaptor.getValue().getName()).isEqualTo(name);
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        StepVerifier.create(createFranchiseUseCase.createFranchise("   "))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise name must not be blank");
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenNameIsNull() {
        StepVerifier.create(createFranchiseUseCase.createFranchise(null))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(BusinessException.class))
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryError() {
        RuntimeException repositoryError = new RuntimeException("persistence failure");
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(createFranchiseUseCase.createFranchise("Franchise One"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }
}

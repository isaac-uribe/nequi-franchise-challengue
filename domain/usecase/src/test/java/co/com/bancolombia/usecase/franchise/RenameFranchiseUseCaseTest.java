package co.com.bancolombia.usecase.franchise;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
class RenameFranchiseUseCaseTest {

    private static final String FRANCHISE_ID = "franchise-1";

    @Mock
    private FranchiseRepository franchiseRepository;

    private RenameFranchiseUseCase renameFranchiseUseCase;

    @BeforeEach
    void setUp() {
        renameFranchiseUseCase = new RenameFranchiseUseCase(franchiseRepository);
    }

    private Franchise franchise() {
        return Franchise.builder().id(FRANCHISE_ID).name("Old Name").build();
    }

    @Test
    void shouldRenameFranchiseWhenFranchiseExistsAndNameIsValid() {
        Franchise franchise = franchise();
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "New Name"))
                .assertNext(result -> assertThat(result.getName()).isEqualTo("New Name"))
                .verifyComplete();

        ArgumentCaptor<Franchise> franchiseCaptor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(franchiseCaptor.capture());
        assertThat(franchiseCaptor.getValue().getName()).isEqualTo("New Name");
        assertThat(franchiseCaptor.getValue().getId()).isEqualTo(FRANCHISE_ID);
    }

    @Test
    void shouldNotMutateOriginalFranchise() {
        Franchise franchise = franchise();
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "New Name"))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(franchise.getName()).isEqualTo("Old Name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailWhenNewNameIsBlank(String blankName) {
        StepVerifier.create(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, blankName))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise name must not be blank");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenFranchiseNotFound() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "New Name"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise not found: " + FRANCHISE_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnFindById() {
        RuntimeException repositoryError = new RuntimeException("lookup failure");
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "New Name"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnSave() {
        Franchise franchise = franchise();
        RuntimeException repositoryError = new RuntimeException("persistence failure");

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(renameFranchiseUseCase.renameFranchise(FRANCHISE_ID, "New Name"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }
}

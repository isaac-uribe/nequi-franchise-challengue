package co.com.bancolombia.usecase;

import co.com.bancolombia.model.aggregate.Branch;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddBranchUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    private AddBranchUseCase addBranchUseCase;

    @BeforeEach
    void setUp() {
        addBranchUseCase = new AddBranchUseCase(franchiseRepository);
    }

    @Test
    void shouldAddBranchWhenFranchiseExistsAndNameIsValid() {
        String franchiseId = "franchise-1";
        Branch existingBranch = Branch.builder().id("branch-0").name("Existing Branch").build();
        Franchise franchise = Franchise.builder()
                .id(franchiseId)
                .name("Franchise One")
                .branches(List.of(existingBranch))
                .build();
        String newBranchName = "New Branch";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(addBranchUseCase.addBranch(franchiseId, newBranchName))
                .assertNext(result -> {
                    assertThat(result.getBranches()).hasSize(2);
                    assertThat(result.getBranches().get(0)).isEqualTo(existingBranch);
                    Branch addedBranch = result.getBranches().get(1);
                    assertThat(addedBranch.getId()).isNotBlank();
                    assertThat(addedBranch.getName()).isEqualTo(newBranchName);
                })
                .verifyComplete();

        ArgumentCaptor<Franchise> franchiseCaptor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(franchiseCaptor.capture());
        assertThat(franchiseCaptor.getValue().getBranches()).hasSize(2);
    }

    @Test
    void shouldNotMutateOriginalFranchiseBranchesList() {
        String franchiseId = "franchise-1";
        List<Branch> originalBranches = List.of(Branch.builder().id("branch-0").name("Existing Branch").build());
        Franchise franchise = Franchise.builder()
                .id(franchiseId)
                .name("Franchise One")
                .branches(originalBranches)
                .build();

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(addBranchUseCase.addBranch(franchiseId, "New Branch"))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(originalBranches).hasSize(1);
    }

    @Test
    void shouldFailWhenBranchNameIsBlank() {
        StepVerifier.create(addBranchUseCase.addBranch("franchise-1", "   "))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch name must not be blank");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenBranchNameIsNull() {
        StepVerifier.create(addBranchUseCase.addBranch("franchise-1", null))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(BusinessException.class))
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenFranchiseNotFound() {
        String franchiseId = "missing-franchise";
        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.empty());

        StepVerifier.create(addBranchUseCase.addBranch(franchiseId, "New Branch"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise not found: " + franchiseId);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnFindById() {
        String franchiseId = "franchise-1";
        RuntimeException repositoryError = new RuntimeException("lookup failure");
        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(addBranchUseCase.addBranch(franchiseId, "New Branch"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnSave() {
        String franchiseId = "franchise-1";
        Franchise franchise = Franchise.builder().id(franchiseId).name("Franchise One").build();
        RuntimeException repositoryError = new RuntimeException("persistence failure");

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(addBranchUseCase.addBranch(franchiseId, "New Branch"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }
}

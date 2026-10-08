package co.com.bancolombia.usecase.branch;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.exception.BusinessException;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.usecase.TestIds;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenameBranchUseCaseTest {

    private static final String FRANCHISE_ID = TestIds.FRANCHISE_ID;
    private static final String BRANCH_ID = TestIds.BRANCH_ID;
    private static final String OTHER_BRANCH_ID = "branch-2";

    @Mock
    private FranchiseRepository franchiseRepository;

    private RenameBranchUseCase renameBranchUseCase;

    @BeforeEach
    void setUp() {
        renameBranchUseCase = new RenameBranchUseCase(franchiseRepository);
    }

    private Franchise franchiseWithBranches(Branch... branches) {
        return Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(List.of(branches))
                .build();
    }

    @Test
    void shouldRenameBranchWhenFranchiseAndBranchExist() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Old Name").build();
        Branch otherBranch = Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build();
        Franchise franchise = franchiseWithBranches(targetBranch, otherBranch);

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "New Name"))
                .assertNext(result -> {
                    assertThat(result.getBranches()).hasSize(2);

                    Branch renamedBranch = result.getBranches().stream()
                            .filter(branch -> branch.getId().equals(BRANCH_ID))
                            .findFirst()
                            .orElseThrow();
                    assertThat(renamedBranch.getName()).isEqualTo("New Name");

                    Branch untouchedBranch = result.getBranches().stream()
                            .filter(branch -> branch.getId().equals(OTHER_BRANCH_ID))
                            .findFirst()
                            .orElseThrow();
                    assertThat(untouchedBranch).isEqualTo(otherBranch);
                })
                .verifyComplete();

        ArgumentCaptor<Franchise> franchiseCaptor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(franchiseCaptor.capture());
        assertThat(franchiseCaptor.getValue().getBranches()).hasSize(2);
    }

    @Test
    void shouldNotMutateOriginalBranchesList() {
        Branch targetBranch = Branch.builder().id(BRANCH_ID).name("Old Name").build();
        List<Branch> originalBranches = List.of(targetBranch);
        Franchise franchise = Franchise.builder()
                .id(FRANCHISE_ID)
                .name("Franchise One")
                .branches(originalBranches)
                .build();

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "New Name"))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(originalBranches).containsExactly(targetBranch);
        assertThat(originalBranches.get(0).getName()).isEqualTo("Old Name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailWhenNewNameIsBlank(String blankName) {
        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, blankName))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch name must not be blank");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenFranchiseNotFound() {
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "New Name"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise not found: " + FRANCHISE_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenBranchNotFound() {
        Franchise franchise = franchiseWithBranches(Branch.builder().id(OTHER_BRANCH_ID).name("Branch Two").build());
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));

        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "New Name"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch not found: " + BRANCH_ID);
                })
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnFindById() {
        RuntimeException repositoryError = new RuntimeException("lookup failure");
        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "New Name"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();

        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateRepositoryErrorOnSave() {
        Branch branch = Branch.builder().id(BRANCH_ID).name("Old Name").build();
        Franchise franchise = franchiseWithBranches(branch);
        RuntimeException repositoryError = new RuntimeException("persistence failure");

        when(franchiseRepository.findById(FRANCHISE_ID)).thenReturn(Mono.just(franchise));
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.error(repositoryError));

        StepVerifier.create(renameBranchUseCase.renameBranch(FRANCHISE_ID, BRANCH_ID, "New Name"))
                .expectErrorMatches(error -> error == repositoryError)
                .verify();
    }

    @Test
    void shouldRejectTooLongNameWithoutTouchingRepository() {
        StepVerifier.create(renameBranchUseCase.renameBranch(TestIds.FRANCHISE_ID, TestIds.BRANCH_ID, "a".repeat(10_000)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch name must not exceed 100 characters");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldRejectNameOfOneHundredOneCharacters() {
        StepVerifier.create(renameBranchUseCase.renameBranch(TestIds.FRANCHISE_ID, TestIds.BRANCH_ID, "a".repeat(101)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch name must not exceed 100 characters");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidFranchiseIdWithoutTouchingRepository() {
        StepVerifier.create(renameBranchUseCase.renameBranch("-1", TestIds.BRANCH_ID, "Valid Name"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise id is invalid");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidBranchIdWithoutTouchingRepository() {
        StepVerifier.create(renameBranchUseCase.renameBranch(TestIds.FRANCHISE_ID, "-1", "Valid Name"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Branch id is invalid");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void shouldValidateIdsBeforeName() {
        StepVerifier.create(renameBranchUseCase.renameBranch("-1", TestIds.BRANCH_ID, "   "))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo("Franchise id is invalid");
                })
                .verify();

        verify(franchiseRepository, never()).findById(any());
        verify(franchiseRepository, never()).save(any());
    }
}

package co.com.bancolombia.usecase.support;

import co.com.bancolombia.model.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidatorTest {

    private static final String VALID_UUID = "3f2b8c1e-4a6d-4e2f-9b7a-1c5d8e9f0a21";

    private static void expectBusinessError(Mono<?> result, String message) {
        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.class);
                    assertThat(error.getMessage()).isEqualTo(message);
                })
                .verify();
    }

    @Test
    void validNameShouldAcceptExactlyOneHundredCharacters() {
        String name = "a".repeat(InputValidator.MAX_NAME_LENGTH);

        StepVerifier.create(InputValidator.validName(name, "Franchise"))
                .expectNext(name)
                .verifyComplete();
    }

    @Test
    void validNameShouldAcceptRegularName() {
        StepVerifier.create(InputValidator.validName("Juan Valdez", "Franchise"))
                .expectNext("Juan Valdez")
                .verifyComplete();
    }

    @Test
    void validNameShouldRejectOneHundredOneCharacters() {
        expectBusinessError(InputValidator.validName("a".repeat(101), "Branch"),
                "Branch name must not exceed 100 characters");
    }

    @Test
    void validNameShouldRejectTenThousandCharacters() {
        expectBusinessError(InputValidator.validName("a".repeat(10_000), "Product"),
                "Product name must not exceed 100 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void validNameShouldRejectBlankOrNull(String name) {
        expectBusinessError(InputValidator.validName(name, "Franchise"), "Franchise name must not be blank");
    }

    @Test
    void validIdShouldAcceptCanonicalLowercaseUuid() {
        StepVerifier.create(InputValidator.validId(VALID_UUID, "Franchise"))
                .expectNext(VALID_UUID)
                .verifyComplete();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "-1",
            "f1",
            "abc",
            "3F2B8C1E-4A6D-4E2F-9B7A-1C5D8E9F0A21",
            "3f2b8c1e-4a6d-4e2f-9b7a-1c5d8e9f0a210",
            "3f2b8c1e-4a6d-4e2f-9b7a-1c5d8e9f0a21x",
            "3f2b8c1e-4a6d-4e2f-9b7a-1c5d8e9f0a2",
            "1-1-1-1-1",
            "+1-+1-+1-+1-+1",
            "3f2b8c1e4a6d4e2f9b7a1c5d8e9f0a21",
            " 3f2b8c1e-4a6d-4e2f-9b7a-1c5d8e9f0a21",
            "3f2b8c1e-4a6d-4e2f-9b7a-1c5d8e9f0a21\n"
    })
    void validIdShouldRejectMalformedIds(String id) {
        expectBusinessError(InputValidator.validId(id, "Branch"), "Branch id is invalid");
    }

    @Test
    void validStockShouldRejectNull() {
        expectBusinessError(InputValidator.validStock(null), "Stock must not be null");
    }

    @Test
    void validStockShouldRejectMinusOne() {
        expectBusinessError(InputValidator.validStock(-1), "Stock must not be negative");
    }

    @Test
    void validStockShouldAcceptZero() {
        StepVerifier.create(InputValidator.validStock(0))
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    void validStockShouldAcceptPositiveValue() {
        StepVerifier.create(InputValidator.validStock(42))
                .expectNext(42)
                .verifyComplete();
    }
}

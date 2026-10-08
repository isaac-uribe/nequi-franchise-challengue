package co.com.bancolombia.usecase.support;

import co.com.bancolombia.model.exception.BusinessException;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public final class InputValidator {

    public static final int MAX_NAME_LENGTH = 100;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private InputValidator() {
    }

    public static Mono<String> validName(String name, String label) {
        return Mono.justOrEmpty(name)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new BusinessException(label + " name must not be blank")))
                .filter(value -> value.length() <= MAX_NAME_LENGTH)
                .switchIfEmpty(Mono.error(() -> new BusinessException(
                        label + " name must not exceed " + MAX_NAME_LENGTH + " characters")));
    }

    public static Mono<String> validId(String id, String label) {
        return Mono.justOrEmpty(id)
                .filter(value -> UUID_PATTERN.matcher(value).matches())
                .switchIfEmpty(Mono.error(() -> new BusinessException(label + " id is invalid")));
    }

    public static Mono<Integer> validStock(Integer stock) {
        return Mono.justOrEmpty(stock)
                .switchIfEmpty(Mono.error(() -> new BusinessException("Stock must not be null")))
                .filter(value -> value >= 0)
                .switchIfEmpty(Mono.error(() -> new BusinessException("Stock must not be negative")));
    }
}

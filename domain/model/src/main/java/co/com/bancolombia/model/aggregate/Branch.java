package co.com.bancolombia.model.aggregate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Branch {
    String id;
    String name;
    @Builder.Default
    List<Product> products = Collections.emptyList();
}

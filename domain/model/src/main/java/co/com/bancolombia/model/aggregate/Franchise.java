package co.com.bancolombia.model.aggregate;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class Franchise {
    String id;
    String name;
    @Builder.Default
    List<Branch> branches = Collections.emptyList();
}

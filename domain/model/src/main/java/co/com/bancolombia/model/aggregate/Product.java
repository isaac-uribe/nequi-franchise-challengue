package co.com.bancolombia.model.aggregate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Product {
    String id;
    String name;
    Integer stock;
}

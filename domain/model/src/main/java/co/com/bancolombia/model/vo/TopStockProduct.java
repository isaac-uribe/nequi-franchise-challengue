package co.com.bancolombia.model.vo;

import co.com.bancolombia.model.aggregate.Product;

public record TopStockProduct(String branchId, String branchName, Product product) {
}

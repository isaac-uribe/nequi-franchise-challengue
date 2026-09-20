package co.com.bancolombia.mongo.mapper;

import co.com.bancolombia.model.aggregate.Branch;
import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.aggregate.Product;
import co.com.bancolombia.mongo.document.BranchDocument;
import co.com.bancolombia.mongo.document.FranchiseDocument;
import co.com.bancolombia.mongo.document.ProductDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FranchiseDocumentMapper {
    FranchiseDocument toDocument(Franchise franchise);

    Franchise toDomain(FranchiseDocument document);

    BranchDocument toDocument(Branch branch);

    Branch toDomain(BranchDocument document);

    ProductDocument toDocument(Product product);

    Product toDomain(ProductDocument document);
}

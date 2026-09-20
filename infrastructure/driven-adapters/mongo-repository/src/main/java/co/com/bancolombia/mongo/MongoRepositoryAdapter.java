package co.com.bancolombia.mongo;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.mongo.document.FranchiseDocument;
import co.com.bancolombia.mongo.helper.AdapterOperations;
import co.com.bancolombia.mongo.mapper.FranchiseDocumentMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MongoRepositoryAdapter extends AdapterOperations<Franchise, FranchiseDocument, String, MongoDBRepository> {


    private final FranchiseDocumentMapper documentMapper;

    public MongoRepositoryAdapter(MongoDBRepository repository, ObjectMapper mapper, FranchiseDocumentMapper documentMapper) {
        super(repository, mapper, documentMapper::toDomain);
        this.documentMapper = documentMapper;
    }

    @Override
    protected FranchiseDocument toData(Franchise entity) {
        return documentMapper.toDocument(entity);
    }
}

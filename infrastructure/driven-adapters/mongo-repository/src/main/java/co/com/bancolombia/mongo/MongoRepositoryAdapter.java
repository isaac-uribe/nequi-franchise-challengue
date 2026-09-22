package co.com.bancolombia.mongo;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.model.gateway.FranchiseRepository;
import co.com.bancolombia.mongo.document.FranchiseDocument;
import co.com.bancolombia.mongo.helper.AdapterOperations;
import co.com.bancolombia.mongo.mapper.FranchiseDocumentMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class MongoRepositoryAdapter extends AdapterOperations<Franchise, FranchiseDocument, String, MongoDBRepository>
        implements FranchiseRepository {


    private final FranchiseDocumentMapper documentMapper;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Retry retry;

    public MongoRepositoryAdapter(MongoDBRepository repository, ObjectMapper mapper, FranchiseDocumentMapper documentMapper, CircuitBreaker circuitBreaker, TimeLimiter timeLimiter, Retry retry) {
        super(repository, mapper, documentMapper::toDomain);
        this.documentMapper = documentMapper;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.retry = retry;
    }

    @Override
    protected FranchiseDocument toData(Franchise entity) {
        return documentMapper.toDocument(entity);
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return applyResilience(super.save(franchise));
    }

    @Override
    public Mono<Franchise> findById(String id) {
        return applyResilience(super.findById(id));
    }

    private Mono<Franchise> applyResilience(Mono<Franchise> mono) {
        return mono
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }
}

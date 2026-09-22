package co.com.bancolombia.mongo.helper;

import co.com.bancolombia.model.aggregate.Franchise;
import co.com.bancolombia.mongo.MongoDBRepository;
import co.com.bancolombia.mongo.MongoRepositoryAdapter;
import co.com.bancolombia.mongo.document.FranchiseDocument;
import co.com.bancolombia.mongo.mapper.FranchiseDocumentMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdapterOperationsTest {

    @Mock
    private MongoDBRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FranchiseDocumentMapper documentMapper;

    private MongoRepositoryAdapter adapter;

    private Franchise entity;
    private FranchiseDocument document;
    private Flux<Franchise> entities;
    private Flux<FranchiseDocument> documents;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        entity = Franchise.builder().id("1").name("Franchise").build();
        document = FranchiseDocument.builder().id("1").name("Franchise").build();
        entities = Flux.just(entity);
        documents = Flux.just(document);

        when(documentMapper.toDocument(entity)).thenReturn(document);
        when(documentMapper.toDomain(document)).thenReturn(entity);

        adapter = new MongoRepositoryAdapter(repository, objectMapper, documentMapper,
                CircuitBreaker.ofDefaults("test"), TimeLimiter.ofDefaults(), Retry.ofDefaults("test"));
    }

    @Test
    void testSave() {
        when(repository.save(document)).thenReturn(Mono.just(document));

        StepVerifier.create(adapter.save(entity))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void testSaveAll() {
        when(repository.saveAll(any(Flux.class))).thenReturn(documents);

        StepVerifier.create(adapter.saveAll(entities))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void testFindById() {
        when(repository.findById("1")).thenReturn(Mono.just(document));

        StepVerifier.create(adapter.findById("1"))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void testFindByExample() {
        when(repository.findAll(any(Example.class))).thenReturn(documents);

        StepVerifier.create(adapter.findByExample(entity))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void testFindAll() {
        when(repository.findAll()).thenReturn(documents);

        StepVerifier.create(adapter.findAll())
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void testDeleteById() {
        when(repository.deleteById("1")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.deleteById("1"))
                .verifyComplete();
    }
}

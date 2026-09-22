package co.com.bancolombia.mongo;

import co.com.bancolombia.mongo.document.FranchiseDocument;
import co.com.bancolombia.mongo.mapper.FranchiseDocumentMapper;
import co.com.bancolombia.model.aggregate.Franchise;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;

class MongoRepositoryAdapterResilienceTest {

    private MongoDBRepository mongoDBRepository;
    private FranchiseDocumentMapper documentMapper;
    private MongoRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mongoDBRepository = Mockito.mock(MongoDBRepository.class);
        documentMapper = Mockito.mock(FranchiseDocumentMapper.class);
        ObjectMapper mapper = Mockito.mock(ObjectMapper.class);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(50))
                .build();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();

        adapter = new MongoRepositoryAdapter(
                mongoDBRepository,
                mapper,
                documentMapper,
                CircuitBreaker.of("test", circuitBreakerConfig),
                TimeLimiter.of("test", timeLimiterConfig),
                Retry.of("test", retryConfig)
        );
    }

    @Test
    void shouldRetryAndEventuallySucceedAfterTransientFailures() {
        AtomicInteger attempts = new AtomicInteger(0);
        FranchiseDocument document = FranchiseDocument.builder().id("f1").name("Nequi").build();
        Franchise franchise = Franchise.builder().id("f1").name("Nequi").build();

        when(mongoDBRepository.findById("f1")).thenAnswer(invocation -> Mono.defer(() -> {
            if (attempts.incrementAndGet() < 3) {
                return Mono.error(new RuntimeException("Simulated transient Mongo failure"));
            }
            return Mono.just(document);
        }));
        when(documentMapper.toDomain(document)).thenReturn(franchise);

        StepVerifier.create(adapter.findById("f1"))
                .expectNext(franchise)
                .verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(3, attempts.get());
    }

    @Test
    void shouldFailAfterExhaustingAllRetryAttempts() {
        AtomicInteger attempts = new AtomicInteger(0);
        when(mongoDBRepository.findById("f1")).thenAnswer(invocation -> Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new RuntimeException("Persistent Mongo failure"));
        }));

        StepVerifier.create(adapter.findById("f1"))
                .expectError()
                .verify();

        org.junit.jupiter.api.Assertions.assertEquals(3, attempts.get());
    }

    @Test
    void shouldOpenCircuitBreakerAfterConsecutiveFailures() {
        when(mongoDBRepository.findById("f1"))
                .thenReturn(Mono.error(new RuntimeException("Persistent Mongo failure")));

        for (int i = 0; i < 4; i++) {
            StepVerifier.create(adapter.findById("f1"))
                    .expectError()
                    .verify();
        }

        StepVerifier.create(adapter.findById("f1"))
                .expectErrorMatches(error ->
                        error.getClass().getSimpleName().equals("CallNotPermittedException"))
                .verify();
    }
}

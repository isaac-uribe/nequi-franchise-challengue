package co.com.bancolombia.mongo.config;

import co.com.bancolombia.secretsmanager.api.exceptions.SecretException;
import co.com.bancolombia.secretsmanager.api.GenericManagerAsync;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundles;

import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoConfigTest {

    @Mock
    private GenericManagerAsync manager;

    private MongoConfig mongoConfigUnderTest;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mongoConfigUnderTest = new MongoConfig();
    }

    @Test
    void dbSecretTest() throws SecretException {
        String secretName = "secret";
        MongoDBSecret dbSecretUnderTest = new MongoDBSecret();
        dbSecretUnderTest.setUri("uri");

        when(manager.getSecret(secretName, MongoDBSecret.class)).thenReturn(Mono.just(dbSecretUnderTest));

        final MongoDBSecret result = mongoConfigUnderTest.dbSecret("secret", manager);

        assertEquals(dbSecretUnderTest, result);
    }

    @Test
    void testMongoProperties() {
        MongoDBSecret secret = mock(MongoDBSecret.class);
        SslBundles sslBundles = mock(SslBundles.class);
        when(secret.getUri()).thenReturn("uri");

        MongoConnectionDetails result = mongoConfigUnderTest.mongoProperties(secret, sslBundles);

        assertNotNull(result);
    }
}

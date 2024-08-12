package com.github.streamshub.flink;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KubernetesSecretReplacerImplTest {

    private KubernetesClient mockClient;

    @BeforeEach
    public void setUp() {
        mockClient = mock(KubernetesClient.class);
        MixedOperation mockSecrets = mock(MixedOperation.class);
        Resource mockResource = mock(Resource.class);

        String namespace = "default";
        String secretName = "my-secret";

        // Set up the mock behavior
        when(mockClient.secrets()).thenReturn(mockSecrets);
        when(mockSecrets.inNamespace(namespace)).thenReturn(mockSecrets);
        when(mockSecrets.withName(secretName)).thenReturn(mockResource);


        Map<String, String> secretData = new HashMap<>();
        secretData.put("username", "Ym9iCg=="); //base64 for bob
        secretData.put("password", "MTIzNDU2Cg=="); //base64 for 123456

        Secret secret = new SecretBuilder()
                .withNewMetadata().withName("my-secret").endMetadata()
                .withData(secretData)
                .withType("Opaque")
                .build();

        when(mockResource.get()).thenReturn(secret);
    }

    @Test
    void testInterpolateSecrets() {
        KubernetesSecretReplacer ksr = new KubernetesSecretReplacerImpl(mockClient);

        String statement = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username={{secret:default/my-secret/username}} password={{secret:default/my-secret/password}}');";

        String expected = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required" +
                " username=bob password=123456');";

        assertEquals(expected, ksr.interpolateSecrets(statement));
    }


    @Test
    void testSecretsDoNotExist() {
        KubernetesSecretReplacer ksr = new KubernetesSecretReplacerImpl(mockClient);

        String statement = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username={{secret:default/my-secret2/username}} password={{secret:default/my-secret2/password}}');";

        Exception e = assertThrows(RuntimeException.class, () -> ksr.interpolateSecrets(statement));
        assertEquals("Secret my-secret2 does not exist", e.getMessage());
    }

    @Test
    void testSecretsDataDoesNotExist() {
        KubernetesSecretReplacer ksr = new KubernetesSecretReplacerImpl(mockClient);

        String statement = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username={{secret:default/my-secret/username1}} password={{secret:default/my-secret/password1}}');";

        Exception e = assertThrows(RuntimeException.class, () -> ksr.interpolateSecrets(statement));
        assertEquals("Could not read data with key username1 from secret my-secret", e.getMessage());
    }
}
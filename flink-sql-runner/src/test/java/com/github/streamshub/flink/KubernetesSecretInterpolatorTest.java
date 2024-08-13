package com.github.streamshub.flink;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KubernetesSecretInterpolatorTest {

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
        Interpolator ksi = new KubernetesSecretInterpolator(mockClient);

        String statement = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username={{secret:default/my-secret/username}} password={{secret:default/my-secret/password}}');";

        assertThat(ksi.interpolate(statement))
                .contains("username=bob")
                .contains("password=123456");
    }


    @Test
    void testSecretsDoNotExist() {
        Interpolator ksi = new KubernetesSecretInterpolator(mockClient);

        String statement = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username={{secret:default/my-secret2/username}} password={{secret:default/my-secret2/password}}');";

        assertThatThrownBy(() -> ksi.interpolate(statement))
                .hasMessage("Secret my-secret2 does not exist")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testSecretsDataDoesNotExist() {
        Interpolator ksi = new KubernetesSecretInterpolator(mockClient);

        String statement = "CREATE TABLE MyTable ( message STRING ) WITH ( " +
                "'connector' = 'kafka', " +
                "'topic' = 'topic'" +
                "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                "'properties.sasl.mechanism' = 'PLAIN', " +
                "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username={{secret:default/my-secret/username1}} password={{secret:default/my-secret/password1}}');";

        assertThatThrownBy(() -> ksi.interpolate(statement))
                .hasMessage("Could not read data with key username1 from secret my-secret")
                .isInstanceOf(RuntimeException.class);
    }
}


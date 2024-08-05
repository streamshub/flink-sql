package com.github.streamshub.flink;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesSecretReplacer {
    // Expected pattern for a secret is {{secret:<namespace>/<name>/<key>}}
    private static final Pattern SECRET_PATTERN = Pattern.compile("\\{\\{secret:([^/]+)/([^}]+)/([^}]+)}}");
    static String interpolateSecrets(String input) {
        Matcher matcher = SECRET_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String namespace = matcher.group(1);
            String secretName = matcher.group(2);
            String secretKey = matcher.group(3);
            String secretValue = getSecretValue(namespace, secretName, secretKey);
            matcher.appendReplacement(result, secretValue != null ? secretValue : "");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String getSecretValue(String namespace, String secretName, String secretKey) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()){
            Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
            if (secret == null) {
                throw new RuntimeException("Secret"  + secretName + " does not exist");
            }
            if (secret.getData() != null && secret.getData().containsKey(secretKey)) {
                return new String(Base64.getDecoder().decode(secret.getData().get(secretKey)), StandardCharsets.UTF_8);
            } else {
                throw new RuntimeException("Could not read data with key " +  secretKey + "from secret " +  secretName);
            }
        } catch (KubernetesClientException e) {
            throw new RuntimeException(e);
        }
    }
}
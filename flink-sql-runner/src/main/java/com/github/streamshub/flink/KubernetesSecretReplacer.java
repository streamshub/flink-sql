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
    private static final Pattern SECRET_PATTERN = Pattern.compile("\\{\\{secret:([^/]+)/([^}]+)/([^}]+)}}");
    static String replaceSecrets(String input) {
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
            if (secret != null && secret.getData() != null && secret.getData().containsKey(secretKey)) {
                return new String(Base64.getDecoder().decode(secret.getData().get(secretKey)), StandardCharsets.UTF_8);
            }
        } catch (KubernetesClientException e) {
            e.printStackTrace();
        }
        return null;
    }
}
package com.github.streamshub.flink;

/**
* An abstraction over a class that resolves Kubernetes secrets
 */
public interface KubernetesSecretReplacer {

    /**
     * Interpolates templated secrets in a given string
     * @param input String containing templated secrets
     * @return String with secret values
     */
    String interpolateSecrets(String input);
}

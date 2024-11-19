# Flink SQL Runner

An application to execute Flink SQL jobs.

## Prerequisites
* Java Development Kit (JDK) 17
* Apache Maven 3.8.x or higher
* Kubernetes cluster

## Building and running Flink SQL Runner

1. Build application
    ```
    mvn package
    ```
2. Build an image
    ```
    minikube image build . -t flink-sql-runner:latest
    ```
3. Create a `flink` namespace:
   ```
   kubectl create namespace flink
   ```
4. Follow the steps in [Deploying the operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/docs/try-flink-kubernetes-operator/quick-start/#deploying-the-operator) section of the Flink Kubernetes Operator's Quick Start.

5. Install the required RBAC rules for Flink Job:
   ```
   kubectl create -f install/
   ```
6. Create a Flink job using the [FlinkDeployment.yaml](./examples/FlinkDeployment.yaml) example. Supply your SQL statements to the job by replacing `<SQL_STATEMENTS>`.
   You can use Kubernetes secrets with Flink SQL Runner, to provide security credentials to Flink job for connecting to the source or the target systems.
   Secrets can be directly templated in the SQL statements with the following pattern:
   ```
   {{secret:<NAMESPACE>/<SECRET NAME>/<DATA KEY>}}
   ```
   If running with the local image built in step 2, ensure that the FlinkDeployment's image is updated.
8. Start a Flink job:
   ```
   kubectl create example/FlinkDeployment.yaml -n flink
   ```
   Update `example/FlinkDeployment.yaml` with your own SQL statements e.g. `args: ["<SQL_STATEMENTS>"]`. 
   Note that semicolon `;` is a special character used as a statement delimiter. If it's part of your SQL statements, make sure it is escaped by `\\`. 
   For example, it might be used for `properties.sasl.jaas.config` value when using Kafka connector. In this case, it would look something like this:
   ```
   'properties.sasl.jaas.config' = 'org.apache.flink.kafka.shaded.org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test-user\" password=\"{{secret:flink/test-user/user.password}}\"\\;'
   ```

## Developing
We welcome your contributions to the Flink SQL project! To ensure a smooth collaboration:

* **Pull Requests**: Open a PR with your proposed changes.
* **Build Success**: Make sure the build passes without errors.
* **Code Quality**: Your code must pass SonarCloud code analysis checks.
* **Unit Tests**: Update existing unit tests for any modifications and write new tests for new features.
* **System Tests**: Repository developers can trigger [Packit CI](tmt/README.md/#packit-as-a-service-for-pr-check) for running system tests.

## Releasing

Follow the [Releasing](RELEASING.md) guide.
# Flink SQL Runner

An application to execute Flink SQL jobs.

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

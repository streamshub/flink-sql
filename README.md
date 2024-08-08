# Flink SQL Runner

An application to execute Flink SQL jobs.

## Building and running Flink SQL Runner

1. Build application
    ```
    mvn package
    ```
2. Build an image
    ```
    minikube image build flink-sql-runner -t flink-sql-runner:latest
    ```
3. Create a `flink` namespace:
   ```
   kubectl create namespace flink
   ```
4. Install cert-manager (this creates cert-manager in a namespace called `cert-manager`):
   ```
   kubectl create -f https://github.com/jetstack/cert-manager/releases/download/v1.8.2/cert-manager.yaml
   ```
5. Deploy Flink Kubernetes Operator:
   ```
   helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-<VERSION>/
   helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator -n flink
   ```
6. Install the required RBAC rules for Flink Job:
   ```
   kubectl create -f install/
   ```
7. Create a Flink job using the [FlinkDeployment.yaml](./examples/FlinkDeployment.yaml) example. Supply your SQL statements to the job by replacing `<SQL_STATEMENTS>`.
   You can use Kubernetes secrets with Flink SQL Runner, to provide security credentials to Flink job for connecting to the source or the target systems.
   Secrets can be directly templated in the SQL statements with the following pattern:
   ```
   {{secret:<NAMESPACE>/<SECRET NAME>/<DATA KEY>}}
   ```

8. Start a Flink job:
   ```
   kubectl create example/FlinkDeployment.yaml -n flink
   ```

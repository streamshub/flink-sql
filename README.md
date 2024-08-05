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

See /examples directory to see how to run FlinkDeployment using the SQL runner.
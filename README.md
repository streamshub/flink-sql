# Flink SQL Runner

An application to execute Flink SQL jobs.

## Prerequisites
* Java Development Kit (JDK) 17
* Apache Maven 3.8.x or higher
* Kubernetes cluster

## Building and running Flink SQL Runner

_Note_: Refer to the instructions `docs/installation.adoc` to install the Flink Kubernetes Operator.

1. Build application
    ```
    mvn package
    ```
2. Build an image
    ```
    minikube image build . -t flink-sql-runner:latest
    ```
4. Create a `FlinkDeployment` custom resource that references the image you just built (`flink-sql-runner:latest`).
5. Apply the `FlinkDeployment` to the `flink` namespace. This namespace has the RBAC setup (via helm) to run Flink Job. If you want to run in another namespace then apply the `install/flink-namespace-rbac.yaml` to the chosen namespace.   

## Building the documentation

The documentation is written in [asciidoc](https://asciidoc.org/) and follow a single large page format. 
These docs are pulled into the main StreamsHub website and hosted there.

To build a local copy of the docs, you will need [asciidoctor](https://asciidoctor.org/) installed.

```shell
asciidoctor docs/index.adoc
```

This builds `docs/index.html` containing the documentation.

## Developing
We welcome your contributions to the Flink SQL project! To ensure a smooth collaboration:

* **Pull Requests**: Open a PR with your proposed changes.
* **Build Success**: Make sure the build passes without errors.
* **Code Quality**: Your code must pass SonarCloud code analysis checks.
* **Unit Tests**: Update existing unit tests for any modifications and write new tests for new features.
* **System Tests**: Repository developers can trigger [Packit CI](tmt/README.md/#packit-as-a-service-for-pr-check) for running system tests.

## Releasing

Follow the [Releasing](RELEASING.md) guide.
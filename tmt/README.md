# Testing farm

This document gives a detailed breakdown of the testing processes using testing farm service.

## Pre-requisites

* Python >=3.9
* TMT command line tool (optional) - for lint and check tmt formatted test plans and tests
  * `pip install tmt[all]`
* Testing farm command line tool - for trigger your test plan in testing-farm
  * `pip install tft-cli`

## Links

* [Test Management Tool (tmt)](https://tmt.readthedocs.io/en/latest/index.html)
* [Testing farm](https://docs.testing-farm.io/general/0.1/index.html)

## Current plans and tests
Plans are stored in [plans](./plans) folder, there is a file called `main.fmf` which contains test plan definition.
This definition is composed of hw requirements, prepare steps for created VM executor and specific plans. Specific
plan defines selectors for [tests](./tests) which should be executed.

### List of plans
* flink-all
* smoke
* flink-sql-example

## Usage

### Pre-requisites
1. Get API token for testing farm [(how-to obtain token)](https://docs.testing-farm.io/general/0.1/onboarding.html)
2. Store token into env var ```export TESTING_FARM_API_TOKEN="your_token"```

### Run tests

Run all plans
```commandline
testing-farm request --compose CentOS-Stream-9 --git-url https://github.com/streamshub/flink-sql.git -e IP_FAMILY=ipv4
```

Select specific plan and git branch
```commandline
testing-farm request --compose CentOS-Stream-9 \
 --git-url https://github.com/streamshub/flink-sql.git \
 --git-ref some-branch \
 --plan smoke \
 -e IP_FAMILY=ipv4
```

Run multi-arch build
```commandline
testing-farm request --compose CentOS-Stream-9 \
 --git-url https://github.com/streamshub/flink-sql.git \
 --git-ref some-branch \
 --plan smoke \
 --arch aarch64,x86_64 \
 -e IP_FAMILY=ipv4
```

## Packit-as-a-service for PR check

[Packit-as-a-service](https://github.com/marketplace/packit-as-a-service) is a github application
for running testing-farm jobs from PR requested by command. Definition of the jobs is stored in
[.packit.yaml](../.packit.yaml). Packit can be triggered from the PR by comment, but only members of streamshub
organization are able to run tests.

The prepare phase install kind cluster and some required packages it takes around 5min

### Usage

Run all jobs for PR
```
/packit test
```

Run selected jobs by label
```
/packit test --labels flink-all
```

To use different branch or fork of streams-e2e test suite you can override it by env configuration
```
/packit test --labels smoke --env TEST_REPO_URL=https://github.com/kornys/streams-e2e.git --env TEST_REPO_BRANCH=new-test
```

#### List of labels
* flink-all - all tests with flink tag from [streams-e2e](https://github.com/streamshub/streams-e2e/tree/main/src/test/java/io/streams/e2e/flink), takes around 15min
* smoke -  all tests with flink tag from [streams-e2e](https://github.com/streamshub/streams-e2e/blob/862d21903e7e53955c1b4f5c4c81f3a50703f310/src/test/java/io/streams/e2e/flink/sql/SqlJobRunnerST.java#L102), takes around 10min
* flink-sql-example - run just test based on [recommendation-app](https://github.com/streamshub/flink-sql-examples/tree/main/recommendation-app), takes around 7min

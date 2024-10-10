#!/bin/sh -eux

# Move to root folder of flink-sql
cd ../../../

git clone https://github.com/streamshub/streams-e2e.git

cd streams-e2e

#get install files
./mvnw install -P get-operator-files

#run tests
./mvnw verify -P test -Dgroups="${TEST_GROUPS}"

#!/bin/sh -eux

# Move to root folder of flink-sql
cd ../../../

git clone https://github.com/streamshub/streams-e2e.git

cd streams-e2e

HOST=""
if [[ ${IP_FAMILY} == "ipv4" || ${IP_FAMILY} == "dual" ]]; then
    HOST=$(hostname --ip-address | grep -oE '\b([0-9]{1,3}\.){3}[0-9]{1,3}\b' | awk '$1 != "127.0.0.1" { print $1 }' | head -1)
elif [[ ${IP_FAMILY} == "ipv6" ]]; then
    HOST="myregistry.local"
fi

export SQL_RUNNER_IMAGE="${HOST}:5001/streamshub/flink-sql-runner:local"

#get install files
./mvnw install -P get-operator-files

#run tests
./mvnw verify -P test -Dgroups="${TEST_GROUPS}"

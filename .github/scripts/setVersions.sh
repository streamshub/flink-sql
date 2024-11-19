#!/bin/bash
# usage: setVersions.sh pomVersion

if [[ $# != 1 ]]; then
    echo "Illegal number of parameters" >&2
    exit 1
fi
mvn -B versions:set -DgenerateBackupPoms=false -DnewVersion=${1}
sed --in-place --regexp-extended "s|flink-sql-runner-dist-([^[:space:]]*)\.tar.gz|flink-sql-runner-dist-${1}.tar.gz|g" Dockerfile
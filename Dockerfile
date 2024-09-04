FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.20
USER 0
# Install Java and dependencies
RUN microdnf install -y \
    wget \
    hostname \
    gzip \
    && microdnf clean all

# Prepare environment
ENV FLINK_HOME=/opt/flink
ENV STREAMSHUB_HOME=/opt/streamshub
ENV FLINK_LIB_DIR=$FLINK_HOME/lib
ENV FLINK_OPT_DIR=$FLINK_HOME/opt
ENV PATH=$FLINK_HOME/bin:$PATH
RUN groupadd --system --gid=9999 flink && \
    useradd --system --home-dir $FLINK_HOME --uid=9999 --gid=flink flink
WORKDIR /opt

COPY flink-sql-runner-dist/target/flink-sql-runner-dist-0.0.1-SNAPSHOT-flink-sql-runner-dist.tar.gz flink-sql-runner-dist.tgz
# Install Flink
RUN set -ex && \
  tar -xzf flink-sql-runner-dist.tgz -C /opt --strip-components=1 && \
  ln -s ${STREAMSHUB_HOME}/flink-sql-runner*.jar ${STREAMSHUB_HOME}/flink-sql-runner.jar && \
  cp -r ${STREAMSHUB_HOME}/lib/* ${FLINK_HOME}/lib/ && \
  rm -rf flink-sql-runner-dist.tgz && \
  chown -R flink:flink $FLINK_HOME && \
  chown -R flink:flink $STREAMSHUB_HOME && \
  # Replace default REST/RPC endpoint bind address to use the container's network interface \
  CONF_FILE="$FLINK_HOME/conf/flink-conf.yaml" && \
  if [ ! -e "$FLINK_HOME/conf/flink-conf.yaml" ]; then \
    CONF_FILE="${FLINK_HOME}/conf/config.yaml"; \
    /bin/bash "$FLINK_HOME/bin/config-parser-utils.sh" "${FLINK_HOME}/conf" "${FLINK_HOME}/bin" "${FLINK_HOME}/lib" \
        "-repKV" "rest.address,localhost,0.0.0.0" \
        "-repKV" "rest.bind-address,localhost,0.0.0.0" \
        "-repKV" "jobmanager.bind-host,localhost,0.0.0.0" \
        "-repKV" "taskmanager.bind-host,localhost,0.0.0.0" \
        "-rmKV" "taskmanager.host=localhost"; \
  else \
    sed -i 's/rest.address: localhost/rest.address: 0.0.0.0/g' "$CONF_FILE"; \
    sed -i 's/rest.bind-address: localhost/rest.bind-address: 0.0.0.0/g' "$CONF_FILE"; \
    sed -i 's/jobmanager.bind-host: localhost/jobmanager.bind-host: 0.0.0.0/g' "$CONF_FILE"; \
    sed -i 's/taskmanager.bind-host: localhost/taskmanager.bind-host: 0.0.0.0/g' "$CONF_FILE"; \
    sed -i '/taskmanager.host: localhost/d' "$CONF_FILE"; \
  fi

# Download the script for docker entrypoint from the GitHub repository and make it executable
WORKDIR /
COPY --chown=flink:flink docker-entrypoint.sh /docker-entrypoint.sh

# Configure container
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 6123
USER flink
CMD ["help"]
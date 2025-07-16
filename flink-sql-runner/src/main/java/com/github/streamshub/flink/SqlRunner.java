/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.streamshub.flink;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and executes SQL statements. */
public class SqlRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SqlRunner.class);

    private static final String CUSTOM_SQL_ENV_VAR_KEY = "CUSTOM_SQL";

    private static final String STATEMENT_DELIMITER = ";"; // a statement should end with `;`
    private static final String LINE_DELIMITER = "\n";

    private static final Pattern SET_STATEMENT_PATTERN =
            Pattern.compile("SET\\s+'(\\S+)'\\s+=\\s+'(.*)';", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATEMENT_SET_PATTERN =
            Pattern.compile("(EXECUTE STATEMENT SET BEGIN.*?END;)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static void main(String[] args) throws Exception {
        List<String> statements;

        String sqlFromEnvVar = System.getenv(CUSTOM_SQL_ENV_VAR_KEY);

        if (args.length == 1) {
            statements = parseStatements(args[0]);
        } else if (sqlFromEnvVar != null) {
            statements = parseStatements(sqlFromEnvVar);
        } else {
            throw new Exception(String.format("Exactly 1 argument or '%s' environment variable is expected.", CUSTOM_SQL_ENV_VAR_KEY));
        }

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        var tableEnv = TableEnvironment.create(settings);
        LOG.debug("TableEnvironment config: " + tableEnv.getConfig().toMap());

        Interpolator ksr = new KubernetesSecretInterpolator();
        for (String statement : statements) {
            var processedStatement = ksr.interpolate(statement);
            Matcher setMatcher = SET_STATEMENT_PATTERN.matcher(statement.trim());

            if (setMatcher.matches()) {
                // Handle SET statements
                String key = setMatcher.group(1);
                String value = setMatcher.group(2);
                LOG.debug("Setting configurations:\n{}={}", key, value);
                tableEnv.getConfig().getConfiguration().setString(key, value);
            } else {
                LOG.info("Executing:\n{}", statement);
                tableEnv.executeSql(processedStatement);
            }
        }
    }

    static List<String> parseStatements(String rawStatements) {
        var formatted = formatSqlStatements(rawStatements.trim());

        var statements = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        Matcher matcher = STATEMENT_SET_PATTERN.matcher(formatted);

        String statementSet = "";
        String otherStatements = formatted;

        if (matcher.find()) {
            statementSet = matcher.group(1);
            otherStatements = formatted.replace(statementSet, "").trim();
        }

        boolean escaped = false;
        for (char c : otherStatements.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == STATEMENT_DELIMITER.charAt(0)) {
                current.append(c);
                statements.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (statementSet.length() > 0) {
            statements.add(statementSet);
        }

        return statements;
    }

    private static String formatSqlStatements(String content) {
        StringBuilder formatted = new StringBuilder();
        formatted.append(content);
        if (!content.endsWith(STATEMENT_DELIMITER)) {
            formatted.append(STATEMENT_DELIMITER);
        }
        formatted.append(LINE_DELIMITER);
        return formatted.toString();
    }
}
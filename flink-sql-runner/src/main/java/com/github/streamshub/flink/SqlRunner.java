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

    private static final String STATEMENT_DELIMITER = ";"; // a statement should end with `;`
    private static final String LINE_DELIMITER = "\n";

    private static final Pattern STATEMENT_DELIMETER_PATTERN = Pattern.compile("(?<!\\\\)" + STATEMENT_DELIMITER);

    private static final String ESCAPED_BACKSLASH_PATTERN_STRING = Pattern.quote("\\;");

    private static final Pattern SET_STATEMENT_PATTERN =
            Pattern.compile("SET\\s+'(\\S+)'\\s*=\\s*'(.*)'\\s*;", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATEMENT_SET_START_PATTERN =
            Pattern.compile("\\s?EXECUTE\\s+STATEMENT\\s+SET\\s+BEGIN\\s+", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATEMENT_SET_END_PATTERN = Pattern.compile("\\s*END\\s*;", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {

        var statements = parseStatementArgs(args);

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        var tableEnv = TableEnvironment.create(settings);
        LOG.debug("TableEnvironment config: {}", tableEnv.getConfig().toMap());

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

    static List<String> parseStatementArg(String rawStatementArg) {

        LOG.debug("Parsing raw statement:\n<begin>{}<end>", rawStatementArg);

        var statements = new ArrayList<String>();

        var cleaned = cleanSqlStatement(rawStatementArg);
        var formatted = formatSqlStatements(cleaned);

        var statementSetBuilder = new StringBuilder();
        boolean insideStatementSet = false;
        // Split the statements on `;` except where that `;` is preceded by a double backspace
        for (String statement: STATEMENT_DELIMETER_PATTERN.split(formatted)) {

            if (statement.isBlank()) {
                // If the statement is a blank string then skip to the next one
                continue;
            }

            // As we split on `;` we need to replace them at the end of the statement.
            // There is probably a regex incantation we can add to the STATEMENT_DELIMETER_PATTERN to do this, but this will do for now.
            statement = statement.trim() + STATEMENT_DELIMITER;
            // Deal with the specific situation where secret strings in WITH clauses contain the `\\;` literal and will end up leaving a `\` before the `;`
            // Again, there is probably regex foo to achieve this
            statement = statement.replaceAll(ESCAPED_BACKSLASH_PATTERN_STRING, ";");

            var statementSetStartMatch = STATEMENT_SET_START_PATTERN.matcher(statement);

            if (statementSetStartMatch.find()) {
                LOG.debug("Found start of statement set: <begin>{}<end>", statement);
                insideStatementSet = true;
                statementSetBuilder.append(statement);
            } else if (insideStatementSet) {
                LOG.debug("Found statement inside statement set: <begin>{}<end>", statement);
                // Regardless of what the statement is, we know we are inside the statement set so we should add it to the builder.
                statementSetBuilder.append(" ").append(statement);
                // Check if what we just added is the end of the statement set.
                var statementSetEndMatch = STATEMENT_SET_END_PATTERN.matcher(statement);
                if (statementSetEndMatch.find()) {
                    // If it is then lets add the full statement set to the statements list and reset.
                    LOG.debug("Found end of statement set: <begin>{}<end>", statement);
                    var statementSet = statementSetBuilder.toString();
                    LOG.debug("Appending full statement set: <begin>{}<end>", statementSet);
                    statements.add(statementSet);
                    insideStatementSet = false;
                    statementSetBuilder = new StringBuilder();
                }
                // else:
                // We are still inside the Statement Set so we move onto the next statement in the set and check
                // if that is an end statement.
            } else {
                LOG.debug("Found statement: <begin>{}<end>", statement);
                statements.add(statement);
            }
        }

        return statements;

    }

    static List<String> parseStatementArgs(String[] statementArgs) {

        var statements = new ArrayList<String>();
        for (String rawStatements : statementArgs) {
            statements.addAll(parseStatementArg(rawStatements));
        }

        return statements;
    }

    /**
     * Cleans the supplied statement string by:
     *  - Trimming whitespace
     *  - Removing ' and " characters from the start and end of the statement
     *  - Replacing all newline characters with spaces
     *  - Removing tab characters
     *
     * @param rawStatement The SQL statement to be cleaned
     * @return The cleaned SQL statement
     */
    private static String cleanSqlStatement(String rawStatement) {

        var statement = rawStatement.trim();

        if (statement.startsWith("'") || statement.startsWith("\"")) {
           statement = statement.substring(1);
        }

        if (statement.endsWith("'") || statement.endsWith("\"")) {
            statement = statement.substring(0, statement.length() - 1);
        }

        return statement
                .replaceAll("\\R+", " ")
                .replaceAll("\\t+", "");
    }

    /**
     * Makes sure that a statement ends with the STATEMENT_DELIMETER and a newline
     *
     * @param content The raw SQL statement string
     * @return The Formatted SQL statement string
     */
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
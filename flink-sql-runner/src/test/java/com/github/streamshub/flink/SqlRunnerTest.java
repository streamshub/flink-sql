package com.github.streamshub.flink;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlRunnerTest {

    @Test
    void testParseStatementArgs() {

        String statement1 = "CREATE TABLE Table1 (\n\tfield STRING,\n\tfield1 STRING\n) WITH (\n\t'connector' = 'datagen'\n); " +
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1; ";
        String statement2 = "'INSERT INTO Table2 SELECT * FROM Table1; '";
        String statement3 = "\"SELECT * FROM multilinestatement\nWHERE\n\tthisThing > thatThing\n\tAND\n\tname != that;\"";

        String [] statements = new String[]{statement1, statement2, statement3};

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen' );",
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1;",
                "INSERT INTO Table2 SELECT * FROM Table1;",
                "SELECT * FROM multilinestatement WHERE thisThing > thatThing AND name != that;");

        List<String> actual = SqlRunner.parseStatementArgs(statements);
        assertEquals(expected, actual);
    }

    @Test
    void testParseStatementArgsTrailingSemiColon() {
        String statements = "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen'); " +
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1; " +
                "INSERT INTO Table2 SELECT * FROM Table1";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen');",
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1;",
                "INSERT INTO Table2 SELECT * FROM Table1;");

        List<String> actual = SqlRunner.parseStatementArgs(new String[]{statements});
        assertEquals(expected, actual);
    }

    @Test
    void testParseStatementArgsWithSecrets() {
        String statements = "CREATE TABLE Table1 (\nmessage STRING\n) WITH (\n\t'connector' = 'kafka',\n" +
                        "\t'topic' = 'test',\n" +
                        "\t'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093',\n" +
                        "\t'properties.security.protocol' = 'SASL_PLAINTEXT',\n" +
                        "\t'properties.sasl.mechanism' = 'PLAIN',\n" +
                        "\t'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"{{secret:flink/mysecret/username}}\" password=\"{{secret:flink/mysecret/password}}\"\\;');\n" +
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');\n" +
                "INSERT INTO print_table SELECT * FROM Table1;\n";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( message STRING ) WITH ( 'connector' = 'kafka', " +
                        "'topic' = 'test', " +
                        "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                        "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                        "'properties.sasl.mechanism' = 'PLAIN', " +
                        "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"{{secret:flink/mysecret/username}}\" password=\"{{secret:flink/mysecret/password}}\";');",
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');",
                "INSERT INTO print_table SELECT * FROM Table1;");

        List<String> actual = SqlRunner.parseStatementArgs(new String[]{statements});
        assertEquals(expected, actual);

    }

    @Test
    void testParseStatementSet() {
        String statementSet =
                "CREATE TABLE KafkaTable (\nmessage STRING\n) WITH (\n'connector' = 'kafka'\n" +
                        "\t'topic' = 'user_behavior',\n" +
                        "\t'properties.bootstrap.servers' = 'localhost:9092',\n" +
                        "\t'properties.group.id' = 'testGroup',\n" +
                        "\t'scan.startup.mode' = 'earliest-offset',\n" +
                        "\t'format' = 'csv'\n);\n" +
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');\n" +
                "CREATE TABLE print_table2 ( message STRING ) WITH ('connector' = 'print');\n" +
                "EXECUTE STATEMENT SET\nBEGIN\n\tINSERT INTO print_table SELECT * FROM KafkaTable;\n" +
                "\tINSERT INTO print_table2 SELECT * FROM print_table;\nEND;\n" +
                "SELECT *\nFROM print_table2;";

        List<String> expected = List.of(
                "CREATE TABLE KafkaTable ( message STRING ) WITH ( 'connector' = 'kafka' " +
                        "'topic' = 'user_behavior', " +
                        "'properties.bootstrap.servers' = 'localhost:9092', " +
                        "'properties.group.id' = 'testGroup', " +
                        "'scan.startup.mode' = 'earliest-offset', " +
                        "'format' = 'csv' );",
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');",
                "CREATE TABLE print_table2 ( message STRING ) WITH ('connector' = 'print');",
                "EXECUTE STATEMENT SET BEGIN INSERT INTO print_table SELECT * FROM KafkaTable; " +
                "INSERT INTO print_table2 SELECT * FROM print_table; END;",
                "SELECT * FROM print_table2;");

        List<String> actual = SqlRunner.parseStatementArgs(new String[]{statementSet});
        assertEquals(expected, actual);
    }
}
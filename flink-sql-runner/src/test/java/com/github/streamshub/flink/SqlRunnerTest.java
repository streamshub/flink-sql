package com.github.streamshub.flink;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlRunnerTest {

    @Test
    void testParseStatements() {
        String statements = "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen'); " +
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1; " +
                "INSERT INTO Table2 SELECT * FROM Table1;";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen');",
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1;",
                "INSERT INTO Table2 SELECT * FROM Table1;");

        List<String> actual = SqlRunner.parseStatements(statements);
        assertEquals(expected, actual);
    }

    @Test
    void testParseStatementsTrailingSemiColon() {
        String statements = "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen'); " +
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1; " +
                "INSERT INTO Table2 SELECT * FROM Table1";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'datagen');",
                "CREATE TABLE Table2 WITH ( 'connector' = 'print') LIKE Table1;",
                "INSERT INTO Table2 SELECT * FROM Table1;");

        List<String> actual = SqlRunner.parseStatements(statements);
        assertEquals(expected, actual);
    }

    @Test
    void testParseStatementsWithSecrets () {
        String statements = "CREATE TABLE Table1 ( message STRING ) WITH ( 'connector' = 'kafka', " +
                        "'topic' = 'test', " +
                        "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                        "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                        "'properties.sasl.mechanism' = 'PLAIN', " +
                        "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"{{secret:flink/mysecret/username}}\" password=\"{{secret:flink/mysecret/password}}\"\\;'); " +
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print'); " +
                "INSERT INTO print_table SELECT * FROM Table1; ";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( message STRING ) WITH ( 'connector' = 'kafka', " +
                        "'topic' = 'test', " +
                        "'properties.bootstrap.servers' = 'my-cluster-kafka-bootstrap.flink.svc:9093', " +
                        "'properties.security.protocol' = 'SASL_PLAINTEXT', " +
                        "'properties.sasl.mechanism' = 'PLAIN', " +
                        "'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"{{secret:flink/mysecret/username}}\" password=\"{{secret:flink/mysecret/password}}\";');",
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');",
                "INSERT INTO print_table SELECT * FROM Table1;");

        List<String> actual = SqlRunner.parseStatements(statements);
        assertEquals(expected, actual);

    }

    @Test
    void testParseStatementSet() {
        String statementSet =
                "CREATE TABLE KafkaTable ( message STRING ) WITH ('connector' = 'kafka'" +
                        "  'topic' = 'user_behavior'," +
                        "  'properties.bootstrap.servers' = 'localhost:9092'," +
                        "  'properties.group.id' = 'testGroup'," +
                        "  'scan.startup.mode' = 'earliest-offset'," +
                        "  'format' = 'csv'); " +
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print'); " +
                "CREATE TABLE print_table2 ( message STRING ) WITH ('connector' = 'print'); " +
                "EXECUTE STATEMENT SET BEGIN INSERT INTO print_table SELECT * FROM KafkaTable; " +
                "INSERT INTO print_table2 SELECT * FROM print_table; END; ";

        List<String> expected = List.of(
                "CREATE TABLE KafkaTable ( message STRING ) WITH ('connector' = 'kafka'" +
                        "  'topic' = 'user_behavior'," +
                        "  'properties.bootstrap.servers' = 'localhost:9092'," +
                        "  'properties.group.id' = 'testGroup'," +
                        "  'scan.startup.mode' = 'earliest-offset'," +
                        "  'format' = 'csv');",
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');",
                "CREATE TABLE print_table2 ( message STRING ) WITH ('connector' = 'print');",
                "EXECUTE STATEMENT SET BEGIN INSERT INTO print_table SELECT * FROM KafkaTable; " +
                "INSERT INTO print_table2 SELECT * FROM print_table; END;");

        List<String> actual = SqlRunner.parseStatements(statementSet);
        assertEquals(expected, actual);
    }
}
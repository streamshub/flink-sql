package com.github.streamshub.flink;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlRunnerTest {

    @Test
    void testParseStatements() {
        String statements = "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'my-source-connector', 'path' = '/test1/data' ); CREATE TABLE Table2 ( field STRING, field2 STRING ) WITH ( 'connector' = 'my-sink-connector', 'path' = '/path/data2' ); INSERT INTO Table2 SELECT * FROM Table1;";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( field STRING, field1 STRING ) WITH ( 'connector' = 'my-source-connector', 'path' = '/test1/data' );",
                "CREATE TABLE Table2 ( field STRING, field2 STRING ) WITH ( 'connector' = 'my-sink-connector', 'path' = '/path/data2' );",
                "INSERT INTO Table2 SELECT * FROM Table1;");

        List<String> actual = SqlRunner.parseStatements(statements);
        assertEquals(expected, actual);
    }

    @Test
    void testParseStatementSets() {
        String setStatements =
                "CREATE TABLE Table1 ( message STRING ) WITH ( 'connector' = 'my-source-connector', 'path' = '/test1/data' ); " +
                "CREATE TABLE Table2 ( message STRING ) WITH ( 'connector' = 'my-sink-connector', 'path' = '/path/data2' ); " +
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print'); " +
                "EXECUTE STATEMENT SET BEGIN INSERT INTO print_table SELECT * FROM Table1; " +
                "INSERT INTO Table2 SELECT * FROM Table1; END; ";

        List<String> expected = List.of(
                "CREATE TABLE Table1 ( message STRING ) WITH ( 'connector' = 'my-source-connector', 'path' = '/test1/data' );",
                "CREATE TABLE Table2 ( message STRING ) WITH ( 'connector' = 'my-sink-connector', 'path' = '/path/data2' );",
                "CREATE TABLE print_table ( message STRING ) WITH ('connector' = 'print');",
                "EXECUTE STATEMENT SET BEGIN INSERT INTO print_table SELECT * FROM Table1; " +
                "INSERT INTO Table2 SELECT * FROM Table1; END;");

        List<String> actual = SqlRunner.parseStatements(setStatements);
        assertEquals(expected, actual);
    }
}
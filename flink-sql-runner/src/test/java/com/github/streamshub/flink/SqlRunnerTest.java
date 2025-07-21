package com.github.streamshub.flink;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlRunnerTest {

    private TableEnvironment tableEnv;
    private TableConfig tableConfig;
    private Configuration configuration;

    private MockedStatic<TableEnvironment> mockedTableEnvStatic;
    private MockedConstruction<KubernetesSecretInterpolator> mockedInterpolatorConstruction;

    @BeforeEach
    void setUp() {
        // Manually create mocks for each object in the chain
        tableEnv = Mockito.mock(TableEnvironment.class);
        tableConfig = Mockito.mock(TableConfig.class);
        configuration = Mockito.mock(Configuration.class);

        // Mock the static `TableEnvironment.create(...)` call
        mockedTableEnvStatic = mockStatic(TableEnvironment.class);
        mockedTableEnvStatic.when(() -> TableEnvironment.create(any(EnvironmentSettings.class))).thenReturn(tableEnv);

        // Explicitly mock each step of the chain
        when(tableEnv.getConfig()).thenReturn(tableConfig);
        when(tableConfig.getConfiguration()).thenReturn(configuration);

        // Mock the construction of KubernetesSecretInterpolator
        mockedInterpolatorConstruction = mockConstruction(KubernetesSecretInterpolator.class,
            (mock, context) -> when(mock.interpolate(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        mockedTableEnvStatic.close();
        mockedInterpolatorConstruction.close();
    }

    @Test
    void testReadsFromEnvironmentVariable() {
        SqlRunner.main(new String[0]);

        verify(tableEnv).executeSql("CREATE TABLE from_pom (id INT);");
    }

    @Test
    void testPrioritizesArgumentOverEnvVar() {
        String sqlFromArg = "CREATE TABLE from_arg (name VARCHAR);";
        SqlRunner.main(new String[]{sqlFromArg});

        verify(tableEnv).executeSql("CREATE TABLE from_arg (name VARCHAR);");
        verify(tableEnv, never()).executeSql("CREATE TABLE from_pom (id INT);");
    }

    @Test
    void testHandlesSetStatements() {
        String sql = "SET 'table.planner' = 'blink'; CREATE TABLE t1 (id INT);";
        SqlRunner.main(new String[]{sql});

        verify(configuration).setString("table.planner", "blink");
        verify(tableEnv).executeSql("CREATE TABLE t1 (id INT);");
        verify(tableEnv, never()).executeSql("SET 'table.planner' = 'blink';");
    }

    @Test
    void testUsesInterpolator() {
        String originalSql = "CREATE TABLE t1 (pass '${secret:my-secret}');";
        String interpolatedSql = "CREATE TABLE t1 (pass 'super_secret_value');";

        mockedInterpolatorConstruction.close();
        mockedInterpolatorConstruction = mockConstruction(KubernetesSecretInterpolator.class,
            (mock, context) -> when(mock.interpolate(originalSql)).thenReturn(interpolatedSql));

        SqlRunner.main(new String[]{originalSql});

        verify(tableEnv).executeSql(interpolatedSql);
    }

    @Test
    void testHandlesStatementSet() {
        String sql = "CREATE TABLE t1 (id INT); EXECUTE STATEMENT SET BEGIN INSERT INTO t2 VALUES (1); END; CREATE TABLE t3 (name VARCHAR);";
        SqlRunner.main(new String[]{sql});

        verify(tableEnv).executeSql("CREATE TABLE t1 (id INT);");
        verify(tableEnv).executeSql("EXECUTE STATEMENT SET BEGIN INSERT INTO t2 VALUES (1); END;");
        verify(tableEnv).executeSql("CREATE TABLE t3 (name VARCHAR);");
        verify(tableEnv, times(3)).executeSql(anyString());
    }
}
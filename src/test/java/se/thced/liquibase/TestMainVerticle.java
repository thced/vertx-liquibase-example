package se.thced.liquibase;

import static io.vertx.core.Future.future;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.parallel.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@Execution(CONCURRENT)
@ExtendWith(VertxExtension.class)
@DisplayName("MainVerticle test")
class TestMainVerticle {

  private static JDBCClient jdbcClient;

  @DisplayName("Prepare for test")
  @BeforeAll
  @Timeout(5000)
  static void prepare(Vertx vertx, VertxTestContext testContext) {
    JsonObject jdbcConfig = vertx.fileSystem().readFileBlocking("jdbc.json").toJsonObject();
    jdbcClient = JDBCClient.createShared(vertx, jdbcConfig, "test");
    vertx.deployVerticle(new MainVerticle(), testContext.succeedingThenComplete());
  }

  static Stream<Arguments> liquibaseTables() {
    return Stream.of(
        Arguments.of("DATABASECHANGELOG", "select 1 from DATABASECHANGELOG"),
        Arguments.of("DATABASECHANGELOGLOCK", "select 1 from DATABASECHANGELOGLOCK"));
  }

  @Timeout(5000)
  @ParameterizedTest(name = "[{index}] - {0}")
  @MethodSource("liquibaseTables")
  @DisplayName("Should have Liquibase tables")
  void thatLiquibaseCreatesTables(String ignored, String sql, VertxTestContext testContext) {
    getConnection()
        .compose(conn -> query(conn, sql))
        .onSuccess(rs -> testContext.verify(() -> assertFalse(rs.toJson().isEmpty())))
        .onComplete(testContext.succeedingThenComplete());
  }

  static Stream<Arguments> applicationTables() {
    return Stream.of(Arguments.of("users", "select 1 from users"));
  }

  @Timeout(5000)
  @ParameterizedTest(name = "[{index}] - {0}")
  @MethodSource("applicationTables")
  @DisplayName("Should have application tables")
  void thatUserTableExists(String ignored, String sql, VertxTestContext testContext) {
    getConnection()
        .compose(conn -> query(conn, sql))
        .onSuccess(rs -> testContext.verify(() -> assertFalse(rs.toJson().isEmpty())))
        .onComplete(testContext.succeedingThenComplete());
  }

  @DisplayName("Clean up after test")
  @AfterAll
  static void tearDown(VertxTestContext testContext) {
    jdbcClient.close(testContext.succeedingThenComplete());
  }

  /** Get the {@link io.vertx.ext.sql.SQLConnection} */
  private Future<SQLConnection> getConnection() {
    return future(p -> jdbcClient.getConnection(p));
  }

  /** Run query and get the result */
  private Future<ResultSet> query(SQLConnection connection, String sql) {
    return future(p -> connection.query(sql, p));
  }

  /** Execute a query */
  private Future<Void> execute(SQLConnection connection, String sql) {
    return future(p -> connection.execute(sql, p));
  }
}

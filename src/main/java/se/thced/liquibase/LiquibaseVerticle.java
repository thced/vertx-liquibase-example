package se.thced.liquibase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.jdbc.impl.JDBCClientImpl;
import io.vertx.ext.sql.SQLConnection;
import java.sql.Connection;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

/**
 * Sets up a JDBC client and connects to the database.
 * It then applies any changesets available in the liquibase-changelog.xml
 */
public class LiquibaseVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject config = vertx.fileSystem().readFileBlocking("jdbc.json").toJsonObject();
    JDBCClientImpl client = (JDBCClientImpl) JDBCClient.createShared(vertx, config, "test");
    getConnection(client)
        .compose(this::getDatabase)
        .compose(this::liquibase)
        .compose(this::applyDatabaseChanges)
        .onComplete(startPromise);
  }

  /** Retrieve the {@link java.sql.Connection} from the JDBC client */
  private Future<Connection> getConnection(JDBCClientImpl jdbcClient) {
    return jdbcClient.getConnection().map(SQLConnection::unwrap);
  }

  /** Retrieve the {@link liquibase.database.Database} from the underlying connection */
  private Future<Database> getDatabase(Connection connection) {
    Promise<Database> databasePromise = Promise.promise();
    try {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      databasePromise.complete(database);
    } catch (DatabaseException e) {
      databasePromise.fail(e);
    }
    return databasePromise.future();
  }

  /** Instantiate {@link Liquibase} against the database */
  private Future<Liquibase> liquibase(Database database) {
    String changelog = "classpath:/liquibase-changelog.xml";
    ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
    return Future.succeededFuture(new Liquibase(changelog, resourceAccessor, database));
  }

  /** Apply the database changes in the changesets */
  private Future<Void> applyDatabaseChanges(Liquibase liquibase) {
    Promise<Void> changesAppliedPromise = Promise.promise();
    try {
      liquibase.update((Contexts) null);
      changesAppliedPromise.complete();
    } catch (Exception e) {
      changesAppliedPromise.fail(e);
    }
    return changesAppliedPromise.future();
  }
}

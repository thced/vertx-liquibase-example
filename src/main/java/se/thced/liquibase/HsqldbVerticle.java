package se.thced.liquibase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;

/** Initiates a HSQLDB instance on port 10000 */
public class HsqldbVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    HsqlProperties configProps = new HsqlProperties();
    configProps.setProperty("server.port", 10000);
    configProps.setProperty("server.database.0", "file:target/db/test");
    configProps.setProperty("server.dbname.0", "test");

    Server server = new Server();
    server.setProperties(configProps);
    server.setSilent(true);
    server.start();

    startPromise.complete();
  }
}

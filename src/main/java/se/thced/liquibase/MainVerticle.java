package se.thced.liquibase;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import java.util.function.Supplier;

/**
 * Main entry point of this example application.
 *
 * <p>It will deploy a HSQLDB instance, and a Liquibase worker instance that applies changesets to
 * the database.
 *
 * <p>When {@link LiquibaseVerticle} completes its startup phase, it is undeployed as it is not
 * needed for more work throughout the application session.
 */
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    deployNonWorker(HsqldbVerticle::new)
        .compose(id -> deployWorker(LiquibaseVerticle::new).compose(vertx::undeploy))
        .onComplete(startPromise);
  }

  private Future<String> deployNonWorker(Supplier<Verticle> supplier) {
    DeploymentOptions options = new DeploymentOptions();
    return vertx.deployVerticle(supplier, options);
  }

  private Future<String> deployWorker(Supplier<Verticle> supplier) {
    DeploymentOptions options = new DeploymentOptions().setWorker(true);
    return vertx.deployVerticle(supplier, options);
  }
}

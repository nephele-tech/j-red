package com.nepheletech.jred.runtime.nodes;

import org.apache.commons.lang3.StringUtils;

import com.nepheletech.dao.NepheleDao;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public class SqlQueryNode extends AbstractNode {

  private final String dataSource;
  private final String sql;

  private final CircuitBreaker circuitBreaker;

  public SqlQueryNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.dataSource = config.getAsString("dataSource");
    this.sql = config.getAsString("sql", null);

    final CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
    circuitBreaker = registry.circuitBreaker(getId());
  }

  @Override
  protected void onMessage(final JsonObject msg) {
    logger.trace(">>> trace: id={}, msg={}", getId(), msg);

    final NepheleDao dao = msg.has("dao")
        ? (NepheleDao) msg.getAsJsonTransient("data").getValue()
        : ((DataSourceNode) getFlow().getNode(dataSource)).getDao();

    Runnable decorated = CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
      // critical code
      boolean txStarted = false;
      try {
        if (!dao.getEntityManager().getTransaction().isActive()) {
          dao.getEntityManager().getTransaction().begin();
          txStarted = true;
        }

        final JsonArray payload = new JsonArray();
        final String[] queries = sql.split(";");
        final JsonObject namedParams = msg.getAsJsonObject("payload", false);
        for (String query : queries) {
          query = StringUtils.trimToNull(query);
          if (query != null) {
            if ("SELECT".equalsIgnoreCase(query.substring(0, 6))) {
              if (namedParams != null) {
                payload.push(dao.nativeQuery(query, namedParams));
              } else {
                payload.push(dao.nativeQuery(query));
              }
            } else {
              if (namedParams != null) {
                payload.push(dao.nativeExec(query, namedParams));
              } else {
                payload.push(dao.nativeExec(query));
              }
            }
          }
        }

        if (txStarted) {
          dao.getEntityManager().getTransaction().commit();
        }

        msg.set("payload", (payload.size() == 1) ? payload.get(0) : payload);
      } catch (Exception e) {

        if (txStarted) {
          dao.getEntityManager().getTransaction().rollback();
        }

        throw e;
      }
      // critical code...
    });

    try {
      decorated.run();
    } finally {

      logger.info("\tFailureRate={}", circuitBreaker.getMetrics().getFailureRate());
      logger.info("\tMaxNumberOfBufferedCalls={}", circuitBreaker.getMetrics().getMaxNumberOfBufferedCalls());
      logger.info("\tNumberOfBufferedCalls={}", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
      logger.info("\tNumberOfFailedCalls={}", circuitBreaker.getMetrics().getNumberOfFailedCalls());
      logger.info("\tNumberOfNotPermittedCalls={}", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
      logger.info("\tNumberOfSuccessfulCalls={}", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    send(msg);
  }
}

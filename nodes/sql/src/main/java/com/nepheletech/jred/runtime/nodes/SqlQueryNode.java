/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Nodes project.
 *
 * J-RED Nodes is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Nodes is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Nodes; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;

import com.nepheletech.dao.NepheleDao;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public class SqlQueryNode extends AbstractNode {

  private final String dataSource;
  private final String sql;

  private final CircuitBreaker circuitBreaker;

  public SqlQueryNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.dataSource = config.getAsString("dataSource");
    this.sql = config.getAsString("sql", null);

    final CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
    circuitBreaker = registry.circuitBreaker(getId());
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> trace: id={}, msg={}", getId(), msg);

    final NepheleDao dao = msg.has("dao")
        ? (NepheleDao) msg.getAsJtonPrimitive("data").getValue()
        : ((DataSourceNode) getFlow().getNode(dataSource)).getDao();

    Runnable decorated = CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
      // critical code
      boolean txStarted = false;
      try {
        if (!dao.getEntityManager().getTransaction().isActive()) {
          dao.getEntityManager().getTransaction().begin();
          txStarted = true;
        }

        final JtonArray payload = new JtonArray();
        final String[] queries = sql.split(";");
        final JtonObject namedParams = msg.getAsJtonObject("payload", false);
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

    send(exchange, msg);
  }
}

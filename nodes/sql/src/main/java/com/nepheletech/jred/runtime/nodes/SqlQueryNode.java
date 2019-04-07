package com.nepheletech.jred.runtime.nodes;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;

/**
 * @see https://www.javarticles.com/2015/08/apache-camel-sql-component.html
 */
public class SqlQueryNode extends AbstractNode implements NodesStartedEventListener, Processor {
  private static final Logger logger = LoggerFactory.getLogger(SqlQueryNode.class);

  private final String datasource;
  private final String sql;

  private final CamelContext camelContext;

  public SqlQueryNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.datasource = config.getAsString("datasource");
    this.sql = config.getAsString("sql");

    this.camelContext = new DefaultCamelContext();
    this.camelContext.disableJMX();
  }

  @Override
  public void onNodesStarted(NodesStartedEvent event) throws Exception {
    final JdbcDatasourceNode jdbcDatasource = (JdbcDatasourceNode) flow.getNode(datasource);
    if (jdbcDatasource == null) { throw new NullPointerException("jdbcDatasource is null"); }

    camelContext.getComponent("sql", SqlComponent.class)
        .setDataSource(jdbcDatasource.getDataSource());

    camelContext.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        // onException(Exception.class)

        from("direct:" + getId())
            .log(">>> SQL.....")
            .to(String.format("sql:%s", sql))
            .log(">>> SQL done!")
            .process(SqlQueryNode.this);
      }
    });

    camelContext.start();
  }

  @Override
  protected void onMessage(final JsonObject msg) {
    final ProducerTemplate template = camelContext.createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
      @Override
      public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        JsonObject o = new JsonObject()
            .set("id", 108L);
        
        Map<String, Object> params = new HashMap<>();
        params.put("id", new JsonPrimitive(108L));

        in.setBody(params);

        logger.info("============= YES ========");
      }
    });
  }

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed");
    try {
      camelContext.stop();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: {}", exchange);

    logger.debug("class={}", exchange.getIn().getBody().getClass());
    logger.debug("body={}", exchange.getIn().getBody());

  }
}

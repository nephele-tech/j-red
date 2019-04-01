package com.nepheletech.jred.runtime.nodes;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;

public class JdbcDatasourceNode extends AbstractConfigurationNode {
  private static final Logger logger = LoggerFactory.getLogger(JdbcDatasourceNode.class);

  private final String driver;
  private final String url;
  private final String user;
  private final String password;
  
  private final DataSource dataSource;

  public JdbcDatasourceNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.driver = config.getAsString("driver");
    this.url = config.getAsString("url");
    
    final JsonObject credentials = config.getAsJsonObject("credentials", false);
    
    logger.debug("credentials: {}", credentials);
    
    if (credentials != null) {
      this.user = credentials.getAsString("user", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.user = "p8p";
      this.password = "p8p";
    }

    logger.debug("driver={}, url={}, user={}, password={}", driver, url, user, password);

    PoolProperties p = new PoolProperties();
    p.setUrl(this.url);
    p.setDriverClassName(this.driver);
    p.setUsername(this.user);
    p.setPassword(this.password);
    p.setJmxEnabled(false);
    p.setTestWhileIdle(false); // XXX
    p.setTestOnBorrow(true);
    p.setValidationQuery("SELECT 1");// MySQL
    p.setTestOnReturn(false); // XXX
    //p.setValidationInterval(30000);
    //p.setTimeBetweenEvictionRunsMillis(30000);
    //p.setMaxActive(Settings.getInt("SqlConnNode.maxActive", 10));
    //p.setInitialSize(10);
    // TODO p.setMaxWait(Settings.getProperty("SqlConnNode.maxWaitTime", 30000));
    //p.setRemoveAbandonedTimeout(60);
    //p.setMinEvictableIdleTimeMillis(60000);
    // TODO p.setMaxIdle(Settings.getProperty("SqlConnNode.maxIdle", 100));
    // TODO p.setMinIdle(Settings.getProperty("SqlConnNode.minIdle", 10));
    //p.setLogAbandoned(true);
    //p.setRemoveAbandoned(true);
    p.setJdbcInterceptors(
        "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;" +
            "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
    org.apache.tomcat.jdbc.pool.DataSource datasource = new org.apache.tomcat.jdbc.pool.DataSource();
    datasource.setPoolProperties(p);
    
    this.dataSource = datasource;
  }

  public DataSource getDataSource() { return dataSource; }
}

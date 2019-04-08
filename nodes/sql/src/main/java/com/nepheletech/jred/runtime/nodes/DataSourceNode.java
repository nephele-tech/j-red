package com.nepheletech.jred.runtime.nodes;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.dao.NepheleDao;
import com.nepheletech.dao.NepheleDaoFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.json.JsonObject;

public class DataSourceNode extends AbstractConfigurationNode {
  private static final Logger logger = LoggerFactory.getLogger(DataSourceNode.class);

  private final String driver;
  private final String url;
  private final String user;
  private final String password;

  private final NepheleDao dao;

  public DataSourceNode(Flow flow, JsonObject config) {
    super(flow, config);

    final String driver = config.getAsString("driver");
    final String driverType = config.getAsString("driverType", "str");

    if ("str".equals(driverType) || "env".equals(driverType)) {
      this.driver = JRedUtil.evaluateNodeProperty(driver, driverType).asString();
    } else {
      this.driver = driverType;
    }

    this.url = config.getAsString("url");

    final JsonObject credentials = getFlow().getCredentials(getId());
    if (credentials != null) {
      this.user = credentials.getAsString("user", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.user = "";
      this.password = "";
    }

    // ---

    final Map<String, String> properties = new HashMap<>();
    properties.put("javax.persistence.jdbc.driver", this.driver);
    properties.put("javax.persistence.jdbc.url", this.url);
    properties.put("javax.persistence.jdbc.user", this.user);
    properties.put("javax.persistence.jdbc.password", this.password);
    final NepheleDaoFactory factory = new NepheleDaoFactory(properties);
    this.dao = factory.create();
  }

  public NepheleDao getDao() { return dao; }

  protected void onClosed() {
    if (dao != null) {
      dao.close();
    }
  }
}

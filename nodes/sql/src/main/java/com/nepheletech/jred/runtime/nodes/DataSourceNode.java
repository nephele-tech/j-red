package com.nepheletech.jred.runtime.nodes;

import java.util.HashMap;
import java.util.Map;

import com.nepheletech.dao.NepheleDao;
import com.nepheletech.dao.NepheleDaoFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.json.JsonObject;

public final class DataSourceNode extends AbstractConfigurationNode
    implements HasCredentials {
  
  private final String driver;
  private final String url;

  private String user;
  private String password;

  private final NepheleDao dao;

  @SuppressWarnings("resource")
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

    // ---

    final Map<String, String> properties = new HashMap<>();
    properties.put("javax.persistence.jdbc.driver", this.driver);
    properties.put("javax.persistence.jdbc.url", this.url);
    properties.put("javax.persistence.jdbc.user", this.user);
    properties.put("javax.persistence.jdbc.password", this.password);
    this.dao = new NepheleDaoFactory(properties).create();
  }

  @Override
  public void setCredentials(JsonObject credentials) {
    if (credentials != null) {
      this.user = credentials.getAsString("user", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.user = "";
      this.password = "";
    }
  }

  public NepheleDao getDao() { return dao; }

  @Override
  protected void onClosed(boolean removed) {
    if (dao != null) {
      dao.close();
    }
  }
}

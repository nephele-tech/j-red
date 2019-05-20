/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nepheletech.jred.runtime.nodes;

import java.util.HashMap;
import java.util.Map;

import com.nepheletech.dao.NepheleDao;
import com.nepheletech.dao.NepheleDaoFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonObject;

public final class DataSourceNode extends AbstractConfigurationNode
    implements HasCredentials {
  
  private final String driver;
  private final String url;

  private String user;
  private String password;

  private final NepheleDao dao;

  @SuppressWarnings("resource")
  public DataSourceNode(Flow flow, JtonObject config) {
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
  public void setCredentials(JtonObject credentials) {
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

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

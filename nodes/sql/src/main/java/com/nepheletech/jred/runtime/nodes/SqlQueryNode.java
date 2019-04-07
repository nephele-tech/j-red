package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.dao.NepheleDao;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

public class SqlQueryNode extends AbstractNode {
  private final String dataSource;
  private final String sql;

  public SqlQueryNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.dataSource = config.getAsString("dataSource");
    this.sql = config.getAsString("sql", null);
  }

  @Override
  protected void onMessage(final JsonObject msg) {
    logger.trace(">>> trace: id={}, msg={}", getId(), msg);
    
    final NepheleDao dao = ((DataSourceNode) getFlow().getNode(dataSource)).getDao();
    final JsonArray data = dao.nativeQuery(sql, msg.getAsJsonObject("payload", true));
    
    msg.set("payload", data);
    
    send(msg);
  }
}

package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public class FtpServerNode extends AbstractConfigurationNode implements HasCredentials {

  private final String host;
  private final int port;
  private final boolean secure;
  private final boolean passiveMode;
  
  private String username;
  private String password;

  public FtpServerNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.host = config.getAsString("host", null);
    this.port = config.getAsInt("port", 21);
    this.secure = config.getAsBoolean("secure", false);
    this.passiveMode = config.getAsBoolean("passiveMode", false);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isSecure() {
    return secure;
  }
  
  public boolean isPassiveMode() {
    return passiveMode;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    logger.trace(">>> setCredentials: {}", credentials);
    
    if (credentials != null) {
      this.username = credentials.getAsString("username", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.username = null;
      this.password = null;
    }
  }
}

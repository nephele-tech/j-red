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

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.Security;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.sslutils.SSLUtils;

/**
 * Configuration options for TLS connections.
 * 
 * @author ggeorg
 */
public class TlsConfigNode extends AbstractConfigurationNode implements HasCredentials {

  static {
    // add only once
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      LoggerFactory.getLogger(TlsConfigNode.class).info("add BouncyCastle security provider");
      Security.addProvider(new BouncyCastleProvider());

      System.setProperty("https.protocols", "TLSv1");
      // System.setProperty("javax.net.debug", "ssl");
    }
  }

  // ---

  private final boolean verifyservercert;
  private final String servername;

  private String cert;
  private String key;
  private String ca;

  private JtonObject credentials;

  private final boolean valid;

  public TlsConfigNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.verifyservercert = config.getAsBoolean("verifyservercert", false);
    final String certPath = config.getAsString("cert", "").trim();
    final String keyPath = config.getAsString("key", "").trim();
    final String caPath = config.getAsString("ca", "").trim();
    this.servername = config.getAsString("servername", "").trim();

    boolean valid = true;

    if ((certPath.length() > 0) || (keyPath.length() > 0)) {
      if ((certPath.length() > 0) != (keyPath.length() > 0)) {
        valid = false;
      } else {
        try {
          if (!certPath.isEmpty()) {
            this.cert = new String(Files.readAllBytes(new File(certPath).toPath()), StandardCharsets.UTF_8);
          }
          if (!keyPath.isEmpty()) {
            this.key = new String(Files.readAllBytes(new File(keyPath).toPath()), StandardCharsets.UTF_8);
          }
          if (!caPath.isEmpty()) {
            this.ca = new String(Files.readAllBytes(new File(caPath).toPath()), StandardCharsets.UTF_8);
          }
        } catch (Exception e) {
          valid = false;
          this.error(e, null);
        }
      }
    } else {
      if (this.credentials != null) {
        final String certData = this.credentials.getAsString("certdata", "");
        final String keyData = this.credentials.getAsString("keydata", "");
        final String caData = this.credentials.getAsString("keydata", "");

        if ((certData.length() > 0) != (keyData.length() > 0)) {
          valid = false;
          // this.error(t, msg);
        } else {
          if (!certData.isEmpty()) {
            this.cert = certData;
          }
          if (!keyData.isEmpty()) {
            this.key = keyData;
          }
          if (!caData.isEmpty()) {
            this.ca = caData;
          }
        }
      }
    }

    this.valid = valid;
  }

  public boolean isValid() { return valid; }

  public String getCert() { return isValid() ? cert : null; }

  public String getKey() { return isValid() ? key : null; }

  public String getCa() { return isValid() ? ca : null; }

  public boolean isVerifyservercert() { return verifyservercert; }

  public String getServername() { return isValid() ? servername : null; }

  public String getPassphrase() {
    return (isValid() && credentials != null && credentials.has("passphrase"))
        ? credentials.getAsString("passphrase", null)
        : null;
  }

  public boolean isRejectUnauthorized() { return isValid() ? this.verifyservercert : false; }

  @Override
  public void setCredentials(JtonObject credentials) { this.credentials = credentials; }

  public SSLContext getSSLContext() throws Exception {
    if (!valid) { return null; }

    Map<String, Object> store = SSLUtils
        .pemsToKeyAndTrustStores(new StringReader(cert), new StringReader(key), null);

    KeyStore keyStore = (KeyStore) store.get("keystore");
    String password = (String) store.get("keystore-pw");

    final KeyManagerFactory kmf = SSLUtils.getKeyManagerFactory(keyStore, password);

    final KeyManager[] km = kmf.getKeyManagers();

    final SSLContext context = SSLContext.getInstance("TLS");
    context.init(km, null, null);

    return context;
  }
}

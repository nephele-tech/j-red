package com.nepheletech.jred.runtime.nodes;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonObject;
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

  private JsonObject credentials;

  private final boolean valid;

  public TlsConfigNode(Flow flow, JsonObject config) {
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
  public void setCredentials(JsonObject credentials) { this.credentials = credentials; }

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

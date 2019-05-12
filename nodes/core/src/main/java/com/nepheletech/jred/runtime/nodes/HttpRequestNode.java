package com.nepheletech.jred.runtime.nodes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.jton.JsonUtil;

public class HttpRequestNode extends AbstractNode implements HasCredentials {

  private final String method;
  private final String url;
  private final boolean template;
  private final String ret;
  private final String tls;

  private String user;
  private String password;

//Mustache
  private final MustacheFactory mf;
  private Mustache mustache;

  public HttpRequestNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.method = config.getAsString("method", "GET");
    this.url = config.getAsString("url", null);
    this.ret = config.getAsString("ret", "txt");

    this.tls = config.getAsString("tls", null);

    this.template = url != null ? url.trim().startsWith("{{") : false;
    if (this.template) {
      mf = new DefaultMustacheFactory();

      try {
        mustache = mf.compile(new StringReader(this.url), getName() != null ? getName() : "http-request");
      } catch (MustacheException e) {
        mustache = null;
        // TODO log(Constants.NODE_RED_ERROR, e, ExceptionUtils.getRootCauseMessage(e));
      }
    } else {
      mf = null;
      mustache = null;
    }
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    if (credentials != null) {
      this.user = credentials.getAsString("user", null);
      this.password = credentials.getAsString("password", null);
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}, thread={}", msg, Thread.currentThread().getId());

    try {
      status(new JtonObject()
          .set("fill", "blue")
          .set("shape", "dot")
          .set("text", "requesting"));

      String url = getUrl(msg); // do mustache
      if (StringUtils.isAllBlank(url)) { throw new NullPointerException("No url specified"); }

      // url must start http:// or https:// so assume http:// if not set
      if (url.indexOf("://") != -1 && url.indexOf("http") != 0) {
        // TODO warn("non-http transport requested");
        status(new JtonObject()
            .set("fill", "red")
            .set("shape", "ring")
            .set("text", "non-http transport requested"));
        return;
      }

      if (!((url.indexOf("http://") == 0) || (url.indexOf("https://") == 0))) {
        if (tls != null) {
          url = "https://" + url;
        } else {
          url = "http://" + url;
        }
      }

      if (msg.has("method") && "use".equals(this.method)) { // warn if override not set
        // TODO warn("Warning: msg properties can not override node properties.");
      }
      final String method = ("use".equals(this.method)
          ? msg.get("method").asString("GET") // use the msg parameter
          : this.method).toUpperCase();

      // request builder

      final RequestBuilder requestBuilder;
      if ("GET".equals(method)) {
        requestBuilder = RequestBuilder.get();
      } else if ("POST".equals(method)) {
        requestBuilder = RequestBuilder.post();
      } else if ("PUT".equals(method)) {
        requestBuilder = RequestBuilder.put();
      } else if ("DELETE".equals(method)) {
        requestBuilder = RequestBuilder.delete();
      } else if ("PATCH".equals(method)) {
        requestBuilder = RequestBuilder.patch();
      } else {
        requestBuilder = RequestBuilder.get();
      }

      requestBuilder.setUri(url);

      // headers

      String ctSet = "Content-Type"; // set default camel case
      @SuppressWarnings("unused")
      String clSet = "Content-Length";
      
      if (msg.has("headers")) {
        final JtonObject headers = msg.getAsJtonObject("headers", true);
        if (headers.has("x-node-red-request-node")) {
          @SuppressWarnings("unused")
          final JtonElement headerHash = headers.get("x-node-red-request-node");
          headers.remove("x-node-red-request-node");
          // TODO hasSum
        }
        for (Entry<String, JtonElement> e : headers.entrySet()) {
          final String key = e.getKey();
          final JtonElement value = e.getValue();
          String name = e.getKey().toLowerCase();
          if (!name.equals("content-type") && !name.equals("content-length")) {
            // only normalize the known headers used later in this
            // function. Otherwise leave them alone.
            name = key;
          } else {
            if ("content-type".equals(name)) {
              ctSet = e.getKey();
            } else {
              clSet = e.getKey();
            }
            // we add those fields later...
            continue;
          }
          if (value.isJtonPrimitive()) {
            requestBuilder.addHeader(name, value.asString());
          }
        }
      }

      if (msg.has("followRedirects")) {
        // TODO follow redirects
      }

      // cookies

      final BasicCookieStore cookieStore = new BasicCookieStore();
      if (msg.has("cookies")) {
        final JtonObject cookies = msg.getAsJtonObject("cookies", true);
        for (Entry<String, JtonElement> e : cookies.entrySet()) {
          final String key = e.getKey();
          final JtonElement value = e.getValue();
          if (value.isJtonPrimitive()) {
            cookieStore.addCookie(new BasicClientCookie(key, value.asString()));
          } else {
            cookieStore.addCookie(new BasicClientCookie(key, value.toString()));
          }
        }
      }

      // HTTP client context

      final HttpClientContext context = HttpClientContext.create();

      // credentials
      if (this.user != null && this.password != null) {
        logger.debug("Setup BASIC Authentication HttpClientContext");

        // -----------------------------------------------------------------
        // Out of the box, the HttpClient doesn't do preemptive authentication - this
        // has to be an explicit decision made by the client.
        //
        // By default, the entire Client-Server communication is:
        // - the Client sends the HTTP Request with no credentials
        // - the Server sends back a challenge
        // - the Client negotiates and identifies the right authentication scheme
        // - the Client sends a second Request, this time with credentials
        //
        // To bypass this default negotiation, we need to pre-populating the HttpContext
        // with an authentication-cache with the right type of authentication scheme
        // pre-selected.
        // -----------------------------------------------------------------

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        // httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

        AuthCache authCache = new BasicAuthCache();
        authCache.put(createTargetHost(url), new BasicScheme());

        // Add AuthCache to the execution context
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);
      }

      // payload

      if (!"GET".equals(method) && !"HEAD".equals(method) && !msg.get("payload").isJtonNull()) {
        HttpEntity entity;

        final JtonElement payload = msg.get("payload");
        final String contentType = JsonUtil.getProperty(msg, "headers." + ctSet).asString(null);
        if (payload.isJtonPrimitive()) {
          if (contentType == null) {
            entity = new StringEntity(payload.asString(), ContentType.TEXT_PLAIN);
          } else {
            entity = new StringEntity(payload.asString(), ContentType.create(contentType, Consts.UTF_8));
          }
        } else {
          if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType)) {
            entity = new StringEntity(jsonToURLEncoding(payload), ContentType.APPLICATION_FORM_URLENCODED);
          } else {
            if (contentType == null) {
              entity = new StringEntity(jsonToURLEncoding(payload), ContentType.APPLICATION_JSON);
              requestBuilder.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            } else {
              entity = new StringEntity(payload.toString(), ContentType.create(contentType, Consts.UTF_8));
            }
          }
        }

        requestBuilder.setEntity(entity);
      }

      // HTTP client

      final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
          .setDefaultCookieStore(cookieStore);

      if (tls != null) {
        final TlsConfigNode tlsNode = (TlsConfigNode) getFlow().getNode(tls);
        if (tlsNode != null) {
          final SSLContext sslFactory = tlsNode.getSSLContext();
          httpClientBuilder.setSSLSocketFactory(sslSocketFactory(sslFactory));
        } else {
          throw new NullPointerException("SSLContext is null");
        }
      }

      try (CloseableHttpClient httpClient = httpClientBuilder.build()) {

        // Response...
        try (CloseableHttpResponse response = httpClient.execute(requestBuilder.build(), context)) {

          final StatusLine sl = response.getStatusLine();

          final JtonObject headers = new JtonObject();
          for (Header header : response.getAllHeaders()) {
            headers.set(header.getName(), header.getValue());
          }

          msg.set("statusCode", sl.getStatusCode());
          msg.set("headers", headers);
          msg.set("responseUrl", url);

          if (headers.has("Set-Cookie")) {
            final JtonObject cookies = new JtonObject();
            for (Cookie cookie : context.getCookieStore().getCookies()) {
              cookies.set(cookie.getName(), new JtonObject()
                  .set("value", cookie.getValue())
                  .set("domain", cookie.getDomain())
                  .set("expiryDate", cookie.getExpiryDate())
                  .set("path", cookie.getPath()));
            }
            msg.set("responseCookies", cookies);
          }

          // TODO msg.headers['x-node-red-request-node'] = hashSum(msg.headers);
          // msg.url = url; // revert when warning above finally removed
          // TODO metric

          // Convert the payload to the required return type
          final HttpEntity entity = response.getEntity();
          try {
            if (!"bin".equals(this.ret)) {
              if ("obj".equals(this.ret)) {
                msg.set("payload", JsonParser
                    .parse(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)));
              } else {
                msg.set("payload", EntityUtils.toString(entity));
              }
            } else {
              // msg.set("payload", EntityUtils.toByteArray(entity), true);
              msg.set("payload", ByteBuffer.wrap(EntityUtils.toByteArray(entity)), true);
            }
          } finally {
            // ensure it is fully consumed...
            EntityUtils.consume(entity);
          }

        }

      } catch (IOException e) {
        e.printStackTrace();
      }

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      status(new JtonObject());
    }

    send(msg);
  }

  private String jsonToURLEncoding(JtonElement json) {
    try {
      final String result = jsonToURLEncoding(null, json);
      return result.replaceAll("&$", "");
    } catch (UnsupportedEncodingException e) {
      logger.debug(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private static final String DEFAULT_VALUE_NAME = "v";
  private static final String DEFAULT_ARRAY_NAME = "a";

  private String jsonToURLEncoding(String prefix, JtonElement json) throws UnsupportedEncodingException {
    final StringBuilder sb = new StringBuilder("");
    if (json.isJtonNull()) {
      // do nothing
    } else if (json.isJtonPrimitive()) {
      final JtonPrimitive _value = json.asJtonPrimitive();
      if (!_value.isJtonTransient()) {
        final String key = prefix != null ? URLEncoder.encode(prefix, "UTF-8") : DEFAULT_VALUE_NAME;
        final String val = URLEncoder.encode(json.asString(""), "UTF-8");
        sb.append(key).append("=").append(val).append("&");
      }
    } else if (json.isJtonObject()) {
      if (null != prefix) {
        final String key = URLEncoder.encode(prefix, "UTF-8");
        final String val = URLEncoder.encode(json.toString(), "UTF-8");
        sb.append(key).append("=").append(val).append("&");
      } else {
        for (Entry<String, JtonElement> entry : json.asJtonObject().entrySet()) {
          final String key = entry.getKey();
          final JtonElement value = entry.getValue();
          sb.append(jsonToURLEncoding(key, value));
        }
      }
    } else if (json.isJtonArray()) {
      for (JtonElement value : json.asJtonArray()) {
        String key = prefix != null ? prefix : DEFAULT_ARRAY_NAME;
        sb.append(jsonToURLEncoding(key, value));
      }
    }

    return sb.toString();
  }

  private HttpHost createTargetHost(String url) {
    try {
      final URL _url = new URL(url);
      return new HttpHost(_url.getHost(), _url.getPort(), _url.getProtocol());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private String getUrl(final JtonObject msg) {
    if (template) {
      try {
        final StringWriter output = new StringWriter();
        mustache.execute(output, msg.deepCopy()
            .set("flow", getFlowContext())
            .set("global", getGlobalContext()));
        return output.toString();
      } catch (MustacheException e) {
        throw new RuntimeException(e);
      }
    } else {
      return url != null ? url : msg.get("url").asString();
    }
  }

  private static SSLConnectionSocketFactory sslSocketFactory(SSLContext sslContext) {
    // Allow TLSv1 protocol only
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
        sslContext,
        new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" },
        null,
        SSLConnectionSocketFactory.getDefaultHostnameVerifier());

    return sslsf;
  }
}

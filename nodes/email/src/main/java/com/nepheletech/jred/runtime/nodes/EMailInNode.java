package com.nepheletech.jred.runtime.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.IOUtils;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class EMailInNode extends AbstractCamelNode implements Processor, HasCredentials {
  private final String protocol;
  private final String server;
  private final boolean useSSL;
  private final String port;
  private final String box; // For IMAP, The mailbox to process
  private final String disposition; // For IMAP, the disposition of the read email
  private final String criteria;
  private final String repeat;
  private final String fetch;

  private String userid;
  private String password;

  public EMailInNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.protocol = config.getAsString("protocol");
    this.server = config.getAsString("server");
    this.useSSL = config.getAsBoolean("useSSL", true);
    this.port = config.getAsString("port", "993");
    this.box = config.getAsString("box", "INBOX");
    this.disposition = config.getAsString("disposition", "Read");
    this.criteria = config.getAsString("criteria", "UNSEEN");
    this.repeat = config.getAsString("repeat", "300");
    this.fetch = config.getAsString("fetch", "auto");
  }

  @Override
  public void setCredentials(JtonObject credentials) {
    if (credentials != null) {
      this.userid = credentials.getAsString("userid", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.userid = null;
      this.password = null;
    }
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    final String emailUrl = String
        .format("%s%s://%s:%s?username=%s&password=%s&folderName=%s&delete=%b"
            + "&unseen=true&consumer.delay=30000"
            + "&skipFailedMessage=false&handleFailedMessage=true",
            protocol.toLowerCase(), (useSSL ? "s" : ""),
            server, port, userid, password, box, "Delete".equals(disposition));

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from(emailUrl)
            .to(String.format("log:DEBUG?showBody=false&showHeaders=%b", logger.isDebugEnabled()))
            .process(EMailInNode.this);
      }
    });
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    logger.trace(">>> process: exchange={}", exchange);

    final Message message = exchange.getIn();

    final JtonObject headers = new JtonObject();
    message.getHeaders().entrySet().forEach(e -> {
      try {
        final String key = e.getKey();
        final Object value = e.getValue();
        if (value instanceof Object[]) {
          setHeader(headers, key, Arrays.asList((Object[]) value));
        } else if (value instanceof Iterable) {
          setHeader(headers, key, (Iterable<?>) value);
        } else {
          if (key.equals("Subject")) {
            try {
              headers.set(key, MimeUtility.decodeText((String)e.getValue()));
            } catch (UnsupportedEncodingException e1) {
              headers.set(key, e.getValue(), false);
            }
          } else {
            headers.set(key, e.getValue(), false);
          }
        }
      } catch (IllegalArgumentException ex) {
        logger.info("--------------------------------------------------> {}={}",
            e.getKey(), e.getValue().getClass());
      }
    });

    final JtonObject attachments = new JtonObject();
    message.getAttachments().entrySet().forEach(a -> {
      final JtonObject _attachment = new JtonObject();
      final DataHandler attachment = a.getValue();
      _attachment.set("name", attachment.getName());
      _attachment.set("contentType", attachment.getContentType());
      attachments.set(a.getKey(), _attachment);
    });
    
    final JtonObject msg = new JtonObject()
        .set("headers", headers)
        .set("topic", MimeUtility.decodeText(headers.getAsString("Subject", "")))
        .set("from", headers.get("From"));

    final Object body = message.getBody();
    if (body instanceof MimeMultipart) {
      msg.set("body", handleMultipart(msg, (MimeMultipart) body));
    } else {
      logger.info("body--------------------------------------------------> {}",
          body.getClass());
    }

    send(msg);
  }

  private JtonObject handleMultipart(JtonObject msg, MimeMultipart multipart)
      throws MessagingException, IOException {
    final JtonObject _multipart = new JtonObject();
    final JtonObject _bodyParts = new JtonObject();
    _multipart.set("contentType", multipart.getContentType());
    for (int i = 0, n = multipart.getCount(); i < n; i++) {
      final JtonObject _part = new JtonObject();
      final MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
      final String contentType = part.getContentType();
      final Object content = part.getContent();
      final String contentID = part.getContentID();
      _part.set("contentType", contentType);
      if (content instanceof String) {
        _part.set("content", (String) content);
        final String ct = contentType.toLowerCase();
        if (ct.contains("text/plain")) {
          msg.set("payload", (String) content);
        } else if (ct.contains("text/html")) {
          msg.set("html", (String) content);
        }
      } else if (content instanceof MimeMultipart) {
        _part.set("content", handleMultipart(msg, (MimeMultipart) content));
      } else if (content instanceof InputStream) {
        final String disposition = part.getDisposition();
        if (disposition != null) {
          if (disposition.toUpperCase().equals("INLINE")) {
            _part.set("content", IOUtils.toByteArray((InputStream) content));
          } else {
            _part.set("content", content, true);
          }
        }
        _part.set("disposition", disposition);
        _part.set("fileName", part.getFileName());
        _part.set("md5", part.getContentMD5());
      } else {
        logger.info("--------------------{}: {}", contentType, content.getClass());
      }
      _bodyParts.set(contentID != null ? contentID : "part-" + i, _part);
    }
    _multipart.set("bodyParts", _bodyParts);
    return _multipart;
  }

  private void setHeader(JtonObject headers, String key, Iterable<?> values) {
    final JtonArray _values = new JtonArray();
    for (Object v : values) {
      try {
        _values.push(JtonPrimitive.create(v));
      } catch (IllegalArgumentException e) {
        logger.info("--------------------x------------------------------> {}={}",
            key, v.getClass());
      }
    }
    headers.set(key, _values);
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    send(msg);
  }
}

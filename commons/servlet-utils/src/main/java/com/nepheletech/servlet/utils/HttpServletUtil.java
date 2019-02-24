package com.nepheletech.servlet.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonNull;
import com.nepheletech.json.JsonParseException;
import com.nepheletech.json.JsonParser;

/**
 * HttpServlet utility.
 */
public final class HttpServletUtil {
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(HttpServletUtil.class);

  private static final String HTTP_HEADER_ACCEPT = "Accept";

  public static final String APPLICATION_JSON = "application/json";
  private static final String APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8";

  public static final String APPLICATION_XML = "application/html";
  private static final String APPLICATION_XML_UTF8 = "application/html; charset=UTF-8";

  public static final String TEXT_HTML = "text/html";
  private static final String TEXT_HTML_UTF8 = "text/html; charset=UTF-8";
  
  public static final String TEXT_PLAIN = "text/plain";
  @SuppressWarnings("unused")
  private static final String TEXT_PLAIN_UTF8 = "text/plain; charset=UTF-8";
  
  public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String MULTIPART_FORM_DATA = "multipart/form-data";

  /**
   * 
   * @param req
   * @param defaultValue
   * @return
   */
  public static String getContentType(HttpServletRequest req, String defaultValue) {
    final String contentType = req.getContentType();
    return contentType != null ? contentType.toLowerCase() : defaultValue;
  }

  /**
   * TODO
   * 
   * @param req
   * @return
   */
  public static boolean acceptsJSON(HttpServletRequest req) {
    final String v = req.getHeader(HTTP_HEADER_ACCEPT);
    return (v != null) ? v.toLowerCase().startsWith(APPLICATION_JSON) : false;
  }

  /**
   * TODO
   * 
   * @param req
   * @return
   */
  public static boolean acceptsHTML(HttpServletRequest req) {
    final String v = req.getHeader(HTTP_HEADER_ACCEPT);
    return (v != null) ? v.toLowerCase().startsWith(TEXT_HTML) : false;
  }

  /**
   * TODO
   * 
   * @param req
   * @return
   */
  public static boolean acceptsXML(HttpServletRequest req) {
    final String v = req.getHeader(HTTP_HEADER_ACCEPT);
    return (v != null) ? v.toLowerCase().startsWith(APPLICATION_XML) : false;
  }

  /**
   * 
   * @param res
   * @param json
   * @throws IOException
   */
  public static void sendJSON(HttpServletResponse res, JsonElement json) throws IOException {
    send(res, APPLICATION_JSON_UTF8, json.toString());
  }

  /**
   * 
   * @param res
   * @param html
   * @throws IOException
   */
  public static void sendHTML(HttpServletResponse res, String html) throws IOException {
    send(res, TEXT_HTML_UTF8, html);
  }

  /**
   * 
   * @param res
   * @param xml
   * @throws IOException
   */
  public static void sendXML(HttpServletResponse res, String xml) throws IOException {
    send(res, APPLICATION_XML_UTF8, xml);
  }
  
  /**
   * 
   * @param res
   * @param contentType
   * @param string
   */
  public static void send(HttpServletResponse res, String contentType, String string) throws IOException {
    final byte[] content = string.getBytes(StandardCharsets.UTF_8);
    res.setContentLength(content.length);
    res.setContentType(contentType);
    // res.setCharacterEncoding("UTF-8");
    IOUtils.write(content, res.getOutputStream());
  }

  /**
   * Retrieves the body of the request as character data using a
   * {@code BufferedReader}. The reader translates the character data according to
   * the character encoding used on the body. Either this method or
   * {@link #getInputStream} may be called to read the body, not both.
   * 
   * @param req the request
   * 
   * @return the body of the request
   *
   * @throws UnsupportedEncodingException if the character set encoding used is
   *                                      not supported and the text cannot be
   *                                      decoded
   * @throws IllegalStateException        if {@link #getInputStream} method has
   *                                      been called on this request
   * @throws IOException                  if an input or output exception occurred
   */
  public static String getBody(HttpServletRequest req) throws IOException {
    try (final Reader input = req.getReader()) {
      return IOUtils.toString(input);
    }
  }

  /**
   * Retrieves the body of the request as character data using a
   * {@code BufferedReader}. The reader translates the character data according to
   * the character encoding used on the body. Either this method or
   * {@link #getInputStream} may be called to read the body, not both.
   * 
   * @param req the request
   * 
   * @return the body of the request
   *
   * @throws UnsupportedEncodingException if the character set encoding used is
   *                                      not supported and the text cannot be
   *                                      decoded
   * @throws IllegalStateException        if {@link #getInputStream} method has
   *                                      been called on this request
   * @throws IOException                  if an input or output exception occurred
   */
  public static JsonElement getJSONBody(HttpServletRequest req) throws IOException {
    try (final Reader input = req.getReader()) {
      return JsonParser.parse(input);
    } catch (JsonParseException e) {
      // TODO log the error
      return JsonNull.INSTANCE;
    }
  }

  // ---

  private HttpServletUtil() {
    // hidden
  }
}

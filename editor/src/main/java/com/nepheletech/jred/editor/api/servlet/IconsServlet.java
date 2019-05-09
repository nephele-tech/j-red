package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.jton.JsonParser;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/icons" })
public class IconsServlet extends HttpServlet {
  private static final long serialVersionUID = 7211868867121781035L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (HttpServletUtil.acceptsJSON(req)) {
      HttpServletUtil.sendJSON(res, JsonParser.parse("{\n" +
          "    \"node-red\": [\n" +
          "    \"alert.png\",\n" +
          "    \"arduino.png\",\n" +
          "    \"arrow-in.png\",\n" +
          "    \"batch.png\",\n" +
          "    \"bluetooth.png\",\n" +
          "    \"bridge-dash.png\",\n" +
          "    \"bridge.png\",\n" +
          "    \"cog.png\",\n" +
          "    \"comment.png\",\n" +
          "    \"db.png\",\n" +
          "    \"debug.png\",\n" +
          "    \"envelope.png\",\n" +
          "    \"feed.png\",\n" +
          "    \"file.png\",\n" +
          "    \"function.png\",\n" +
          "    \"hash.png\",\n" +
          "    \"inject.png\",\n" +
          "    \"join.png\",\n" +
          "    \"leveldb.png\",\n" +
          "    \"light.png\",\n" +
          "    \"link-out.png\",\n" +
          "    \"mongodb.png\",\n" +
          "    \"mouse.png\",\n" +
          "    \"node-changed.png\",\n" +
          "    \"node-error.png\",\n" +
          "    \"parser-csv.png\",\n" +
          "    \"parser-html.png\",\n" +
          "    \"parser-json.png\",\n" +
          "    \"parser-xml.png\",\n" +
          "    \"parser-yaml.png\",\n" +
          "    \"range.png\",\n" +
          "    \"redis.png\",\n" +
          "    \"rpi.png\",\n" +
          "    \"serial.png\",\n" +
          "    \"sort.png\",\n" +
          "    \"split.png\",\n" +
          "    \"subflow.png\",\n" +
          "    \"swap.png\",\n" +
          "    \"switch.png\",\n" +
          "    \"template.png\",\n" +
          "    \"timer.png\",\n" +
          "    \"trigger.png\",\n" +
          "    \"twitter.png\",\n" +
          "    \"watch.png\",\n" +
          "    \"white-globe.png\"\n" +
          "  ],\n" +
          "  \"node-red-node-rbe\": [\n" +
          "    \"rbe.png\"\n" +
          "  ]\n" +
          "}"));
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

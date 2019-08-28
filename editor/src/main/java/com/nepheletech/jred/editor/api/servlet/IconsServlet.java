/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED project.
 *
 * J-RED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J-RED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jton.JsonParser;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/icons" })
public class IconsServlet extends HttpServlet {
  private static final long serialVersionUID = 7211868867121781035L;
  
  private static final Logger logger = LoggerFactory.getLogger(IconsServlet.class);

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doGet: {}", req.getRequestURI());
    
    if (HttpServletUtil.acceptsJSON(req)) {
      HttpServletUtil.sendJSON(res, JsonParser.parse("{\n" +
          "    \"node-red\": [\n" +
          "    \"alert.svg\",\n" +
          "    \"arduino.png\",\n" +
          "    \"arrow-in.svg\",\n" +
          "    \"batch.svg\",\n" +
          "    \"bluetooth.png\",\n" +
          "    \"bridge-dash.svg\",\n" +
          "    \"bridge.svg\",\n" +
          "    \"cog.svg\",\n" +
          "    \"comment.svg\",\n" +
          "    \"db.svg\",\n" +
          "    \"debug.svg\",\n" +
          "    \"envelope.svg\",\n" +
          "    \"feed.svg\",\n" +
          "    \"file.svg\",\n" +
          "    \"function.svg\",\n" +
          "    \"hash.svg\",\n" +
          "    \"inject.svg\",\n" +
          "    \"join.svg\",\n" +
          "    \"leveldb.svg\",\n" +
          "    \"light.svg\",\n" +
          "    \"link-out.svg\",\n" +
          "    \"mongodb.png\",\n" +
          "    \"mouse.png\",\n" +
          "    \"node-changed.svg\",\n" +
          "    \"node-error.svg\",\n" +
          "    \"parser-csv.svg\",\n" +
          "    \"parser-html.svg\",\n" +
          "    \"parser-json.svg\",\n" +
          "    \"parser-xml.svg\",\n" +
          "    \"parser-yaml.svg\",\n" +
          "    \"range.svg\",\n" +
          "    \"redis.png\",\n" +
          "    \"rpi.svg\",\n" +
          "    \"serial.svg\",\n" +
          "    \"sort.svg\",\n" +
          "    \"split.svg\",\n" +
          "    \"subflow.svg\",\n" +
          "    \"swap.svg\",\n" +
          "    \"switch.svg\",\n" +
          "    \"template.svg\",\n" +
          "    \"timer.svg\",\n" +
          "    \"trigger.svg\",\n" +
          "    \"twitter.svg\",\n" +
          "    \"watch.svg\",\n" +
          "    \"white-globe.svg\"\n" +
          "  ],\n" +
          "  \"node-red-node-rbe\": [\n" +
          "    \"rbe.svg\"\n" +
          "  ]\n" +
          "}"));
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

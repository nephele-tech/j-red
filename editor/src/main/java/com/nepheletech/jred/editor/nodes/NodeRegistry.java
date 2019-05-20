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
package com.nepheletech.jred.editor.nodes;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.io.Resources;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jred.editor.Constants;

public final class NodeRegistry {
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);

  private static final Pattern pattern = Pattern.compile("<script ([^>]*)data-template-name=['\"]([^'\"]*)['\"]");

  private final JtonArray nodeList;
  private final String nodeConfigs;

  public NodeRegistry() {
    final Predicate<String> filter = new FilterBuilder().include(".*\\.html");
    final Reflections r = new Reflections(new ConfigurationBuilder()
        .filterInputsBy(filter)
        .setScanners(new ResourcesScanner())
        .setUrls(ClasspathHelper.forPackage(Node.class.getPackage().getName())));
    final Set<String> resourceSet = r.getResources(Pattern.compile(".*"));
    final String[] resources = resourceSet.toArray(new String[resourceSet.size()]);
    Arrays.sort(resources);

    final JtonArray nodeList = new JtonArray();
    final StringBuilder sb = new StringBuilder();

    final String module = "node-red";
    final String version = Constants.NODE_RED_VERSION;

    for (String rc : resources) {
      try {
        final String name = rc.replaceAll("^.+-|.html$", "");
        final String id = module + "/" + name;

        final JtonArray types = new JtonArray();
        final URL url = Resources.getResource(rc);
        final String content = Resources.toString(url, StandardCharsets.UTF_8);
        final Matcher m = pattern.matcher(content);
        while (m.find()) {
          types.push(m.group(2));
        }

        if (types.size() > 0) {
          final JtonObject node = new JtonObject()
              .set("id", id)
              .set("name", name)
              .set("types", types)
              .set("enabled", true)
              .set("local", false)
              .set("module", module)
              .set("version", version);
          nodeList.push(node);
          sb.append(content);
        }

        // logger.info(content);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    this.nodeList = nodeList;
    this.nodeConfigs = sb.toString();
  }

  public JtonArray getNodeList() { return nodeList.deepCopy(); }

  public String getNodeConfigs() { return nodeConfigs; }
}

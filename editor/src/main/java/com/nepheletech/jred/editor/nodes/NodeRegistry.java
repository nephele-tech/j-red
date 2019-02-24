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
import com.nepheletech.jred.editor.Constants;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;

public final class NodeRegistry {
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);

  private static final Pattern pattern = Pattern.compile("<script ([^>]*)data-template-name=['\"]([^'\"]*)['\"]");

  private final JsonArray nodeList;
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

    final JsonArray nodeList = new JsonArray();
    final StringBuilder sb = new StringBuilder();

    final String module = "node-red";
    final String version = Constants.NODE_RED_VERSION;

    for (String rc : resources) {
      try {
        final String name = rc.replaceAll("^.+-|.html$", "");
        final String id = module + "/" + name;

        final JsonArray types = new JsonArray();
        final URL url = Resources.getResource(rc);
        final String content = Resources.toString(url, StandardCharsets.UTF_8);
        final Matcher m = pattern.matcher(content);
        while (m.find()) {
          types.push(m.group(2));
        }

        if (types.size() > 0) {
          final JsonObject node = new JsonObject()
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

  public JsonArray getNodeList() { return nodeList.deepCopy(); }

  public String getNodeConfigs() { return nodeConfigs; }
}

/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Runtime project.
 *
 * J-RED Runtime is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Runtime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Runtime; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.storage.util.Crypto;
import com.nepheletech.jton.JsonParseException;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;

public class LocalFileSystemStorage implements Storage {
  private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemStorage.class);

  private final String baseDir;

  private final Path flowsFile;
  private final Path flowsBackupFile;

  private final Path credentialsFile;
  private final Path credentialsBackupFile;

  private final Path libDir;

  public LocalFileSystemStorage(String baseDir) {
    this.baseDir = baseDir;

    flowsFile = new File(baseDir, "flows.json").toPath();
    flowsBackupFile = new File(baseDir, "flows.backup").toPath();

    credentialsFile = new File(baseDir, "credentials.json").toPath();
    credentialsBackupFile = new File(baseDir, "credentials.backup").toPath();

    final File libDir = new File(baseDir, "lib");

    if (!libDir.exists()) {
      try {
        final Path path = Files.createDirectories(libDir.toPath());
        // logger.log(INFO, "created folder: {0}", path.toAbsolutePath());
      } catch (IOException e) {
        // logger.log(SEVERE, e, () -> format("failed to create folder %s",
        // libDir.getAbsolutePath()));
      }
    }

    this.libDir = libDir.toPath();
  }

  public String getBaseDir() { return baseDir; }

  @Override
  public JtonObject getFlows() {
    logger.trace(">>> getFlows:");
    final JtonElement flows = readJSONFile(flowsFile, flowsBackupFile);
    logger.debug("flows: {}", flows);
    final JtonElement credentials = readJSONFile(credentialsFile, credentialsBackupFile);
    final JtonObject result = new JtonObject()
        .set("flows", flows.isJtonArray() ? flows : new JtonArray())
        .set("credentials", credentials.isJtonObject() ? credentials : new JtonObject());
    result.set("rev", Crypto.createHashOf(result.toString()));
    return result;
  }

  @Override
  public String saveFlows(JtonObject config) throws IOException {
    final JtonArray flows = config.get("flows").asJtonArray();
    final JtonObject credentials = config.get("credentials").asJtonObject();
    final boolean credentialsDirty = config.remove("credentialsDirty").asBoolean();

    if (credentialsDirty) {
      saveCredentials(credentials);
    }

    saveFlows(flows);

    return Crypto.createHashOf(config.toString());
  }

  private void saveFlows(JtonArray flows) throws IOException {
    if (flowsFile.toFile().exists()) {
      Files.move(flowsFile, flowsBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }
    writeFile(flowsFile, flows.toString(" "));
  }

  @Override
  public JtonObject getCredentials() {
    final JtonObject credentials = readJSONFile(credentialsFile, credentialsBackupFile).asJtonObject();
    return credentials.isJtonObject() ? credentials : new JtonObject();
  }

  @Override
  public void saveCredentials(JtonObject credentials) throws IOException {
    if (credentialsFile.toFile().exists()) {
      Files.move(credentialsFile, credentialsBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }
    writeFile(credentialsFile, credentials.toString(" "));
  }

  @Override
  public JtonObject getSettings() { // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void saveSettings(JtonObject settings) {
    // TODO Auto-generated method stub

  }

  @Override
  public Object getLibraryEntry(String type, String path) throws IOException {
    final Path fn = Paths.get(libDir.toString(), path);
        
        logger.info("---------------------------------{}", fn);

    if (fn.toFile().isFile()) {
      if ("flows".equals(type)) {
        try {
          return JsonParser.parse(new String(Files.readAllBytes(fn), "UTF-8"));
        } catch (JsonParseException e) {
          throw new RuntimeException(e);
        }
      } else {
        return readFile(fn);
      }
    } else {
      final JtonArray entries = new JtonArray();
      final File parent = fn.toFile();
      if (parent.isDirectory()) {
        final File[] files = fn.toFile().listFiles();
        if (files != null) {
          Arrays.sort(files);

          for (File dir : files) {
            if (dir.isDirectory()) {
              entries.push(dir.getName());
            }
          }

          for (File file : files) {
            if (file.isFile()) {
              entries.push(getFileMeta(file.getAbsolutePath()));
            }
          }
        }
      }
      return entries;
    }
  }

  private static JtonObject getFileMeta(final String path) throws IOException {
    final Path fn = Paths.get(path);
    final JtonObject meta = new JtonObject();
    final String[] lines = new String(Files.readAllBytes(fn), "UTF-8").split("\n");
    for (String line : lines) {
      if (path.endsWith(".jsp")) {
        if (line.startsWith("<%-- meta:") && line.endsWith(" --%>")) {
          final String[] pair = line.substring(10, line.length() - 5).split(":");
          if (pair.length == 2) {
            meta.set(pair[0].trim(), pair[1].trim());
            continue;
          }
        }
        break;
      } else {
        if (line.startsWith("// meta:")) {
          final String[] pair = line.substring(8).split(":");
          if (pair.length == 2) {
            meta.set(pair[0].trim(), pair[1].trim());
            continue;
          }
        }
        break;
      }
    }
    meta.set("fn", fn.toFile().getName());
    return meta;
  }

  @Override
  public void saveLibraryEntry(String type, String path, JtonObject meta, String text) throws IOException {
    logger.trace(">>> saveLibraryEntry: type={}, path={}, meta={}", type, path, meta);
    
    final Path fn;
    if ("flows".equals(type) && !path.endsWith(".json")) {
      fn = Paths.get(libDir.toString(), path + ".json");
    } else {
      fn = Paths.get(libDir.toString(), path);
    }

    Files.createDirectories(fn.getParent());
    
    logger.info("-----------------------{}", fn);

    final StringBuilder sb = new StringBuilder();
    for (Entry<String, JtonElement> entry : meta.entrySet()) { // headers
      final String key = entry.getKey();
      final String value = entry.getValue().asString();
      if (path.endsWith(".jsp")) {
        sb.append("<%-- meta:").append(key)
            .append(':')
            .append(value)
            .append(" --%>\n");
      } else {
        sb.append("// meta:").append(key)
            .append(':')
            .append(value)
            .append('\n');
      }
    }
    sb.append(text);

    writeFile(fn, sb.toString());
  }

  @Override
  public JtonObject listFlows(String path) throws IOException {
    logger.trace(">>> listFlows: path={}", path);
    final Path fn = Paths.get(libDir.toString(), path);
    return listFlows(fn);
  }

  private JtonObject listFlows(Path fn) {
    final JtonObject result = new JtonObject();
    final File parent = fn.toFile();
    if (parent.isDirectory()) {
      final File[] files = fn.toFile().listFiles();
      if (files != null) {
        Arrays.sort(files);

        final JtonObject d = new JtonObject();
        for (File dir : files) {
          if (dir.isDirectory()) {
            d.set(dir.getName(), listFlows(dir.toPath()));
          }
        }

        if (d.size() > 0) {
          result.set("d", d);
        }

        final JtonArray f = new JtonArray();
        for (File file : files) {
          if (file.isFile()) {
            f.push(file.getName().replaceAll(".json$", ""));
          }
        }

        if (f.size() > 0) {
          result.set("f", f);
        }
      }
    }

    return result;
  }

  // ---

  private static JtonElement readJSONFile(Path path, Path backupPath) {
    String data = readFile(path, backupPath);
    if (data != null) {
      try {
        return JsonParser.parse(data);
      } catch (JsonParseException e1) {
        logger.debug(e1.getMessage(), e1);
        // try again with backup file only
        data = readFile(backupPath);
        try {
          return JsonParser.parse(data);
        } catch (JsonParseException e2) {
          logger.debug(e1.getMessage(), e2);
          return JtonNull.INSTANCE;
        }
      }
    } else {
      return JtonNull.INSTANCE;
    }
  }

  private static String readFile(Path path, Path backupPath) {
    String data = StringUtils.trimToNull(readFile(path));

    if (data == null) {
      // read backup file
      data = StringUtils.trimToNull(readFile(backupPath));
    }

    return data;
  }

  private static String readFile(Path path) {
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.warn("Failed to open file: {}", path);
      return null;
    }
  }

  private static void writeFile(Path path, String data) throws IOException {
    Files.write(path, data.getBytes("UTF-8"),
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}

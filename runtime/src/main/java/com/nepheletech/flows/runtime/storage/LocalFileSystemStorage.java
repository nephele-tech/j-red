package com.nepheletech.flows.runtime.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.nepheletech.flows.runtime.storage.Storage;
import com.nepheletech.flows.runtime.storage.util.Crypto;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonNull;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParseException;
import com.nepheletech.json.JsonParser;

public class LocalFileSystemStorage implements Storage {
  private static final Logger log = Logger.getLogger(LocalFileSystemStorage.class.getName());

  private final String baseDir;

  private final Path flowsFile;
  private final Path flowsBackupFile;

  private final Path credentialsFile;
  private final Path credentialsBackupFile;

  @SuppressWarnings("unused")
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
  public JsonObject getFlows() {
    final JsonElement flows = readJSONFile(flowsFile, flowsBackupFile);
    final JsonElement credentials = readJSONFile(credentialsFile, credentialsBackupFile);
    final JsonObject result = new JsonObject()
        .set("flows", flows.isJsonArray() ? flows : new JsonArray())
        .set("credentials", credentials.isJsonObject() ? credentials : new JsonObject());
    result.set("rev", Crypto.createHashOf(result.toString()));
    return result;
  }

  @Override
  public String saveFlows(JsonObject config) throws IOException {
    final JsonArray flows = config.get("flows").asJsonArray();
    final JsonObject credentials = config.get("credentials").asJsonObject();
    final boolean credentialsDirty = config.remove("credentialsDirty").asBoolean();

    if (credentialsDirty) {
      saveCredentials(credentials);
    }

    saveFlows(flows);

    return Crypto.createHashOf(config.toString());
  }

  private void saveFlows(JsonArray flows) throws IOException {
    if (flowsFile.toFile().exists()) {
      Files.move(flowsFile, flowsBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }
    writeFile(flowsFile, flows.toString(" "));
  }

  @Override
  public JsonObject getCredentials() {
    final JsonObject credentials = readJSONFile(credentialsFile, credentialsBackupFile).asJsonObject();
    return credentials.isJsonObject() ? credentials : new JsonObject();
  }

  @Override
  public void saveCredentials(JsonObject credentials) throws IOException {
    if (credentialsFile.toFile().exists()) {
      Files.move(credentialsFile, credentialsBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }
    writeFile(credentialsFile, credentials.toString(" "));
  }

  @Override
  public JsonObject getSettings() { // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void saveSettings(JsonObject settings) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getLibraryEntry(String type, String path) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setLibraryEntry(String type, String path, JsonObject meta, String body) {
    // TODO Auto-generated method stub

  }

  // ---

  private static JsonElement readJSONFile(Path path, Path backupPath) {
    String data = readFile(path, backupPath);
    if (data != null) {
      try {
        return JsonParser.parse(data);
      } catch (JsonParseException e1) {
        data = readFile(backupPath);
        try {
          return JsonParser.parse(data);
        } catch (JsonParseException e2) {
          return JsonNull.INSTANCE;
        }
      }
    } else {
      return JsonNull.INSTANCE;
    }
  }

  private static String readFile(Path path, Path backupPath) {
    String data = readFile(path);

    // read backup
    if (StringUtils.trimToNull(data) == null) {
      data = readFile(backupPath);
    }

    return StringUtils.trimToNull(null);
  }

  private static String readFile(Path path) {
    try {
      return new String(Files.readAllBytes(path), "UTF-8");
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to open file: {0}", path);
      return null;
    }
  }

  private static void writeFile(Path path, String data) throws IOException {
    Files.write(path, data.getBytes("UTF-8"), 
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}

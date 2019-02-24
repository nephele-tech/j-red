package com.nepheletech.jred.runtime.storage;

import java.io.IOException;

import com.nepheletech.json.JsonObject;

public interface Storage {

  JsonObject getFlows();

  String saveFlows(JsonObject config) throws IOException;

  JsonObject getCredentials();

  void saveCredentials(JsonObject credentials) throws IOException;

  JsonObject getSettings();

  void saveSettings(JsonObject settings);

  // Sessions???

  /* Library Functions */

  String getLibraryEntry(String type, String path);

  void setLibraryEntry(String type, String path, JsonObject meta, String body);
}
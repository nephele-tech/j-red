package com.nepheletech.jred.runtime.storage;

import java.io.IOException;

import com.nepheletech.jton.JtonObject;

public interface Storage {

  JtonObject getFlows();

  String saveFlows(JtonObject config) throws IOException;

  JtonObject getCredentials();

  void saveCredentials(JtonObject credentials) throws IOException;

  JtonObject getSettings();

  void saveSettings(JtonObject settings);

  // Sessions???

  /* Library Functions */

  String getLibraryEntry(String type, String path);

  void setLibraryEntry(String type, String path, JtonObject meta, String body);
}
/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
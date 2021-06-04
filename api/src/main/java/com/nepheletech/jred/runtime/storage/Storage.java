/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED API project.
 *
 * J-RED API is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED API; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

  Object getLibraryEntry(String type, String path) throws IOException;

  void saveLibraryEntry(String type, String path, JtonObject meta, String text) throws IOException;

  JtonObject listFlows(String path) throws IOException;
}
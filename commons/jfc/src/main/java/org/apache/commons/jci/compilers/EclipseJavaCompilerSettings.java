/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Commons project.
 *
 * J-RED Commons is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Commons is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Commons; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.apache.commons.jci.compilers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Native Eclipse compiler settings
 * 
 * @author tcurdt
 */
public final class EclipseJavaCompilerSettings extends JavaCompilerSettings {

  final private Map<String, String> defaultEclipseSettings = new HashMap<String, String>();

  public EclipseJavaCompilerSettings() {
    defaultEclipseSettings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
    defaultEclipseSettings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
    defaultEclipseSettings.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
    defaultEclipseSettings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
  }

  public EclipseJavaCompilerSettings(final JavaCompilerSettings pSettings) {
    super(pSettings);

    if (pSettings instanceof EclipseJavaCompilerSettings) {
      defaultEclipseSettings.putAll(((EclipseJavaCompilerSettings) pSettings).toNativeSettings());
    }
  }

  public EclipseJavaCompilerSettings(final Map<String, String> pMap) {
    defaultEclipseSettings.putAll(pMap);
  }

  private static Map<String, String> nativeVersions = new HashMap<String, String>() {
    private static final long serialVersionUID = 1L;
    {
      put("1.1", CompilerOptions.VERSION_1_1);
      put("1.2", CompilerOptions.VERSION_1_2);
      put("1.3", CompilerOptions.VERSION_1_3);
      put("1.4", CompilerOptions.VERSION_1_4);
      put("1.5", CompilerOptions.VERSION_1_5);
      put("1.6", CompilerOptions.VERSION_1_6);
      put("1.7", CompilerOptions.VERSION_1_7);
      put("1.8", CompilerOptions.VERSION_1_8);
      put("9", CompilerOptions.VERSION_9);
      put("10", CompilerOptions.VERSION_10);
      put("11", CompilerOptions.VERSION_11);
    }
  };

  private String toNativeVersion(final String pVersion) {
    final String nativeVersion = nativeVersions.get(pVersion);

    if (nativeVersion == null) {
      throw new RuntimeException("unknown version " + pVersion);
    }

    return nativeVersion;
  }

  Map<String, String> toNativeSettings() {
    final Map<String, String> map = new HashMap<String, String>(defaultEclipseSettings);

    map.put(CompilerOptions.OPTION_SuppressWarnings, isWarnings() ? CompilerOptions.GENERATE : CompilerOptions.DO_NOT_GENERATE);
    map.put(CompilerOptions.OPTION_ReportDeprecation, isDeprecations() ? CompilerOptions.GENERATE : CompilerOptions.DO_NOT_GENERATE);
    map.put(CompilerOptions.OPTION_TargetPlatform, toNativeVersion(getTargetVersion()));
    map.put(CompilerOptions.OPTION_Source, toNativeVersion(getSourceVersion()));
    map.put(CompilerOptions.OPTION_Encoding, getSourceEncoding());

    return map;
  }

  @Override
  public String toString() {
    return toNativeSettings().toString();
  }
}

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
package com.nepheletech.jfc;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.nepheletech.jfc.JFunctionScriptEngine.JavaCompiledScript;

public final class ScriptEvaluator<T> {
  private static final AtomicLong nextClassNum = new AtomicLong();

  @SuppressWarnings("unused")
  private final String[] parameterNames;
  private final Class<?>[] parameterTypes;
  private final String packageName;
  private final String className;
  private final String returnTypeName;

  private final String script;

  private JavaCompiledScript compiledScript;

  public ScriptEvaluator(String[] javaImports, String javaCodeBlock, Class<T> returnType,
      String[] parameterNames, Class<?>[] parameterTypes, String parseLocation) {
    this(javaImports, javaCodeBlock, returnType, parameterNames, parameterTypes, new Class[0], parseLocation);
  }

  public ScriptEvaluator(String[] javaImports, String javaCodeBlock, Class<T> returnType,
      String[] parameterNames, Class<?>[] parameterTypes, Class<?>[] throwTypes, String parseLocation) {

    if (parameterNames == null) {
      parameterNames = new String[0];
    }

    this.parameterNames = parameterNames;

    if (parameterTypes == null) {
      parameterTypes = new Class<?>[0];
    }

    this.parameterTypes = parameterTypes;

    if (parameterNames != null && parameterTypes != null && parameterNames.length != parameterTypes.length) {
      throw new IllegalArgumentException("Lengths of parameterNames (" + parameterNames.length
          + ") and parameterTypes (" + parameterTypes.length + ") do not match");
    }

    this.packageName = getClass().getPackage().getName() + ".scripts";
    this.className = "JFunction" + nextClassNum.incrementAndGet();
    this.returnTypeName = (returnType == Void.class ? "void" : returnType.getCanonicalName());

    this.script = createJavaClass(className, packageName, javaImports,
        javaCodeBlock, returnTypeName, parameterNames, parameterTypes, throwTypes);
  }

  public void compile() throws ScriptException {
    final JFunctionScriptEngine engine = new JFunctionScriptEngine();
    final ScriptContext context = engine.getContext();
    final String fileName = File.separator 
        + packageName.replace('.', File.separatorChar) + File.separator + className + ".java";
    context.setAttribute(ScriptEngine.FILENAME, fileName, ScriptContext.ENGINE_SCOPE);
    context.setAttribute("className", packageName + '.' + className, ScriptContext.ENGINE_SCOPE);
    context.setAttribute("parameterTypes", parameterTypes, ScriptContext.ENGINE_SCOPE);
    compiledScript = (JavaCompiledScript) engine.compile(script);
  }

  public String getScript() { return script; }

  @SuppressWarnings("unchecked")
  public T evaluate(Object... params) throws ScriptException {
    if (compiledScript != null) {
      return (T) compiledScript.eval(params);
    } else {
      throw new ScriptException("compiledScript is null");
    }
  }

  private String createJavaClass(String className, String packageName, String[] javaImports,
      String javaCodeBlock, String returnTypeName, String[] parameterNames, Class<?>[] parameterTypes, Class<?>[] throwTypes) {
    final StringBuilder sb = new StringBuilder();
    sb.append("package ").append(packageName).append(";\n");
    if (javaImports != null && javaImports.length > 0) {
      sb.append("\n");
      for (String javaImport : javaImports) {
        sb.append("import ").append(javaImport).append(";\n");
      }
    }
    sb.append("\n");
    sb.append("public final class ").append(className).append(" {\n");
    sb.append("\tpublic static ").append(returnTypeName).append(" eval(");
    if (parameterNames.length > 0) {
      int i = 0, n = parameterNames.length - 1;
      for (; i < n; i++) {
        sb.append("final " + parameterTypes[i].getCanonicalName()).append(" ").append(parameterNames[i]).append(", ");
      }
      sb.append("final " + parameterTypes[i].getCanonicalName()).append(" ").append(parameterNames[i]);
    }
    sb.append(") ");
    if (throwTypes != null && throwTypes.length > 0) {
      sb.append("throws ");
      int i = 0, n = throwTypes.length - 1;
      for (; i < n; i++) {
        sb.append(throwTypes[i].getCanonicalName()).append(", ");
      }
      sb.append(throwTypes[i].getCanonicalName()).append(" ");
    }
    sb.append("{\n// --- BEGIN --------------------\n")
        .append(javaCodeBlock)
        .append("\n// --- END ----------------------\n\t}\n");
    sb.append("}\n"); // end of class
    return sb.toString();
  }

  public static void main(String[] args) throws ScriptException {
    ScriptEvaluator<String> e = new ScriptEvaluator<>(null,
        "for (String s: new String[] {\"1\", \"2\", \"3\"}) {\n" +
            "      System.out.println(s);\n" +
            "}\n" +
            "return \"Hello, World!!!\";",
        String.class, null, null, "math");
    e.compile();
    System.out.println(e.evaluate());
  }
}

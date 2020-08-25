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

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.problems.CompilationProblem;
import org.apache.commons.jci.readers.MemoryResourceReader;
import org.apache.commons.jci.stores.MemoryResourceStore;
import org.apache.commons.jci.stores.ResourceStore;
import org.apache.commons.jci.stores.ResourceStoreClassLoader;

public class JFunctionScriptEngine extends AbstractScriptEngine implements Compilable {
  private final JavaCompiler compiler;
  private final JavaCompilerSettings settings;

  public JFunctionScriptEngine() {
    // compiler = new JavaCompilerFactory().createCompiler("jsr199");
    compiler = new JavaCompilerFactory().createCompiler("eclipse");
    settings = compiler.createDefaultSettings();
    settings.setSourceVersion("11");
    settings.setTargetVersion("11");
  }

  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Bindings createBindings() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScriptEngineFactory getFactory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompiledScript compile(String script) throws ScriptException {
    final String fileName = getFileName(context);
    final String className = getClassName(context);
    final Class<?>[] parameterTypes = getParameterTypes(context);
    final ClassLoader parentClassLoader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
        .orElse(JFunctionScriptEngine.class.getClassLoader());

    System.out.println("fileName: " + fileName);
    System.out.println("className: " + className);

    // provide access to resource like e.g. source code
    final MemoryResourceReader src = new MemoryResourceReader();
    
    try {
      src.add(fileName, script.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      src.add(fileName, script.getBytes());
    }

    // where the compilers are storing the results
    final MemoryResourceStore dst = new MemoryResourceStore();

    // class loader
    final ResourceStoreClassLoader classLoader = new ResourceStoreClassLoader(parentClassLoader,
        new ResourceStore[] { dst });

    final CompilationResult result = compiler
        .compile(new String[] { fileName }, src, dst, classLoader, settings);

    if (result.getErrors().length > 0) {
      for (CompilationProblem error : result.getErrors()) {
        System.err.println(String.format("ERROR: %s", error.toString()));
      }

      String[] lines = script.split("\n");
      for (int i = 0, n = lines.length; i < n; i++) {
        System.err.println(String.format("%6d: %s", i + 1, lines[i]));
      }

      final CompilationProblem firstError = result.getErrors()[0];
      throw new ScriptException(firstError.getMessage(),
          firstError.getFileName(), firstError.getStartLine(), firstError.getStartColumn());
    }

    for (CompilationProblem warning : result.getWarnings()) {
      System.err.println(String.format("WARNING: %s", warning.toString()));
    }

    try {
      return new JavaCompiledScript(classLoader.loadClass(className), "eval", parameterTypes);
    } catch (ClassNotFoundException e) {
      throw new ScriptException(e);
    }
  }

  private static String getFileName(ScriptContext context) {
    final int scope = context.getAttributesScope(ScriptEngine.FILENAME);
    if (-1 != scope) {
      return context.getAttribute(ScriptEngine.FILENAME, scope).toString();
    }
    return null;
  }

  private static String getClassName(ScriptContext context) {
    final int scope = context.getAttributesScope("className");
    if (-1 != scope) {
      return (String) context.getAttribute("className", scope);
    }
    return null;
  }

  private static Class<?>[] getParameterTypes(ScriptContext context) {
    final int scope = context.getAttributesScope("parameterTypes");
    if (-1 != scope) {
      return (Class<?>[]) context.getAttribute("parameterTypes", scope);
    }
    return null;
  }

  @Override
  public CompiledScript compile(Reader script) throws ScriptException {
    try {
      return compile(IOUtils.toString(script));
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  // Custom implementation for CompiledScript
  public final class JavaCompiledScript extends CompiledScript {
    private final Method targetMethod;

    JavaCompiledScript(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws ScriptException {
      try {
        this.targetMethod = clazz.getMethod(methodName, parameterTypes);
      } catch (NoSuchMethodException e) {
        throw new ScriptException(e);
      }
      int modifiers = this.targetMethod.getModifiers();
      if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
        throw new ScriptException("Cannot find public static method: " + methodName);
      }
    }

    public Object eval(Object[] params) throws ScriptException {
      try {
        return targetMethod.invoke(null, params);
      } catch (Exception e) {
        throw new ScriptException(e);
      }
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
      throw new UnsupportedOperationException();
    }

    @Override
    public ScriptEngine getEngine() {
      return JFunctionScriptEngine.this;
    }
  }
}
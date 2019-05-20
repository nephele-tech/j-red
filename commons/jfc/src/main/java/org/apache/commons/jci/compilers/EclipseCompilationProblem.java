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

import org.apache.commons.jci.problems.CompilationProblem;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Wrapping an Eclipse compiler problem
 * 
 * @author tcurdt
 */
public final class EclipseCompilationProblem implements CompilationProblem {

  private final IProblem problem;

  public EclipseCompilationProblem(final IProblem pProblem) {
    problem = pProblem;
  }

  public boolean isError() {
    return problem.isError();
  }

  public String getFileName() {
    return new String(problem.getOriginatingFileName());
  }

  public int getStartLine() {
    return problem.getSourceLineNumber();
  }

  public int getStartColumn() {
    return problem.getSourceStart();
  }

  public int getEndLine() {
    return getStartLine();
  }

  public int getEndColumn() {
    return problem.getSourceEnd();
  }

  public String getMessage() {
    return problem.getMessage();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getFileName()).append(" (");
    sb.append(getStartLine());
    sb.append(":");
    sb.append(getStartColumn());
    sb.append(") : ");
    sb.append(getMessage());
    return sb.toString();
  }

  public int getId() {
    return problem.getID();
  }

}

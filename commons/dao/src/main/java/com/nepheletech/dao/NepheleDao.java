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
package com.nepheletech.dao;

import static com.nepheletech.jton.JsonUtil.getProperty;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;

import com.nepheletech.dao.domain.Page;
import com.nepheletech.dao.domain.PageImpl;
import com.nepheletech.dao.domain.Pageable;
import com.nepheletech.dao.transformer.JsonResultTransformer;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;

public class NepheleDao {

  private final EntityManager entityManager;
  private final ResultTransformer transformer;

  protected NepheleDao(EntityManager entityManager) {
    this(entityManager, new JsonResultTransformer());
  }

  protected NepheleDao(EntityManager entityManager, ResultTransformer transformer) {
    this.entityManager = entityManager;
    this.transformer = transformer;
  }

  public EntityManager getEntityManager() { return entityManager; }

  public JtonArray nativeQuery(String query) {
    return query((Query<?>) entityManager.createNativeQuery(query));
  }

  public JtonArray nativeQuery(String query, JtonObject namedParams) {
    return query(applyNamedParameters((Query<?>) entityManager.createNativeQuery(query), namedParams));
  }

  public Page<JtonElement> nativeQuery(String query, Pageable pageable) {
    return query((Query<?>) entityManager.createNativeQuery(query), pageable);
  }

  public Page<JtonElement> nativeQuery(String query, JtonObject namedParams, Pageable pageable) {
    return query(applyNamedParameters((Query<?>) entityManager.createNativeQuery(query), namedParams), pageable);
  }

  @SuppressWarnings("deprecation")
  protected JtonArray query(Query<?> query) {
    query.setResultTransformer(transformer);
    return (JtonArray) query.getResultList();
  }

  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  protected Page<JtonElement> query(Query<?> query, Pageable pageable) {
    query.setResultTransformer(transformer);

    // Get the total number of rows
    final int total = getTotalRowNumber(query);

    // Pagination
    query.setFirstResult((int) pageable.getOffset()); // XXX
    query.setMaxResults(pageable.getPageSize());

    final List<?> content = query.getResultList();

    // Create the page and return it
    return new PageImpl(content, pageable, total);
  }
  
  // ---

  public int nativeExec(String query) {
    return exec((Query<?>) entityManager.createNativeQuery(query));
  }

  public int nativeExec(String query, JtonObject namedParams) {
    return exec(applyNamedParameters((Query<?>) entityManager.createNativeQuery(query), namedParams));
  }
  
  protected int exec(Query<?> query) {
    return query.executeUpdate();
  }
  
  // ---

  protected int getTotalRowNumber(Query<?> query) {
    try (final ScrollableResults resultScroll = query.scroll(ScrollMode.FORWARD_ONLY)) {
      resultScroll.last();
      return resultScroll.getRowNumber() + 1;
    }
  }

  protected Query<?> applyNamedParameters(final Query<?> query, final JtonObject namedParams) {
    for (String name : query.getParameterMetadata().getNamedParameterNames()) {
      final JtonElement value = getProperty(namedParams, name);
      if (value.isJtonNull()) {
        query.setParameter(name, null);
      } else {
        query.setParameter(name, value.asJtonPrimitive().getValue());
      }
    }
    return query;
  }

  public void close() {
    entityManager.close();
  }

  public boolean isOpen() { return entityManager.isOpen(); }
}

/*
 * Copyright 2019 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

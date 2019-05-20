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
package com.nepheletech.dao.domain;

import java.util.Optional;

/**
 * Abstract interface for pagination information.
 *
 * @author Oliver Gierke (springframework)
 * @author ggeorg
 */
public interface Pageable {

  /**
   * Returns a {@link Pageable} instance representing no pagination setup.
   *
   * @return
   */
  static Pageable unpaged() {
    return Unpaged.INSTANCE;
  }

  /**
   * Returns whether the current {@link Pageable} contains pagination information.
   *
   * @return
   */
  default boolean isPaged() {
    return true;
  }

  /**
   * Returns whether the current {@link Pageable} does not contain pagination information.
   *
   * @return
   */
  default boolean isUnpaged() {
    return !isPaged();
  }

  /**
   * Returns the page to be returned.
   *
   * @return the page to be returned.
   */
  int getPageNumber();

  /**
   * Returns the number of items to be returned.
   *
   * @return the number of items of that page
   */
  int getPageSize();

  /**
   * Returns the offset to be taken according to the underlying page and page size.
   *
   * @return the offset to be taken
   */
  long getOffset();

  /**
   * Returns the sorting parameters.
   *
   * @return
   */
  Sort getSort();

  /**
   * Returns the current {@link Sort} or the given one if the current one is unsorted.
   *
   * @param sort must not be {@literal null}.
   * @return
   */
  default Sort getSortOr(Sort sort) {

    //Assert.notNull(sort, "Fallback Sort must not be null!");

    return getSort().isSorted() ? getSort() : sort;
  }

  /**
   * Returns the {@link Pageable} requesting the next {@link Page}.
   *
   * @return
   */
  Pageable next();

  /**
   * Returns the previous {@link Pageable} or the first {@link Pageable} if the current one already is the first one.
   *
   * @return
   */
  Pageable previousOrFirst();

  /**
   * Returns the {@link Pageable} requesting the first page.
   *
   * @return
   */
  Pageable first();

  /**
   * Returns whether there's a previous {@link Pageable} we can access from the current one. Will return
   * {@literal false} in case the current {@link Pageable} already refers to the first page.
   *
   * @return
   */
  boolean hasPrevious();

  /**
   * Returns an {@link Optional} so that it can easily be mapped on.
   *
   * @return
   */
  default Optional<Pageable> toOptional() {
    return isUnpaged() ? Optional.empty() : Optional.of(this);
  }
}

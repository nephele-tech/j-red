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

import java.util.List;
import java.util.function.Function;

/**
 * A slice of data that indicates whether there's a next or previous slice available. Allows to obtain a
 * {@link Pageable} to request a previous or next {@link Slice}.
 *
 * @author Oliver Gierke (springframework)
 * @author ggeorg
 */
public interface Slice<T> /*extends Streamable<T>*/ {

	/**
	 * Returns the number of the current {@link Slice}. Is always non-negative.
	 *
	 * @return the number of the current {@link Slice}.
	 */
	int getNumber();

	/**
	 * Returns the size of the {@link Slice}.
	 *
	 * @return the size of the {@link Slice}.
	 */
	int getSize();

	/**
	 * Returns the number of elements currently on this {@link Slice}.
	 *
	 * @return the number of elements currently on this {@link Slice}.
	 */
	int getNumberOfElements();

	/**
	 * Returns the page content as {@link List}.
	 *
	 * @return
	 */
	List<T> getContent();

	/**
	 * Returns whether the {@link Slice} has content at all.
	 *
	 * @return
	 */
	boolean hasContent();

	/**
	 * Returns the sorting parameters for the {@link Slice}.
	 *
	 * @return
	 */
	Sort getSort();

	/**
	 * Returns whether the current {@link Slice} is the first one.
	 *
	 * @return
	 */
	boolean isFirst();

	/**
	 * Returns whether the current {@link Slice} is the last one.
	 *
	 * @return
	 */
	boolean isLast();

	/**
	 * Returns if there is a next {@link Slice}.
	 *
	 * @return if there is a next {@link Slice}.
	 */
	boolean hasNext();

	/**
	 * Returns if there is a previous {@link Slice}.
	 *
	 * @return if there is a previous {@link Slice}.
	 */
	boolean hasPrevious();

	/**
	 * Returns the {@link Pageable} that's been used to request the current {@link Slice}.
	 *
	 * @return
	 * @since 2.0
	 */
	default Pageable getPageable() {
		return PageRequest.of(getNumber(), getSize(), getSort());
	}

	/**
	 * Returns the {@link Pageable} to request the next {@link Slice}. Can be {@link Pageable#unpaged()} in case the
	 * current {@link Slice} is already the last one. Clients should check {@link #hasNext()} before calling this method.
	 *
	 * @return
	 */
	Pageable nextPageable();

	/**
	 * Returns the {@link Pageable} to request the previous {@link Slice}. Can be {@link Pageable#unpaged()} in case the
	 * current {@link Slice} is already the first one. Clients should check {@link #hasPrevious()} before calling this
	 * method.
	 *
	 * @return
	 */
	Pageable previousPageable();

	/**
	 * Returns a new {@link Slice} with the content of the current one mapped by the given {@link Converter}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return a new {@link Slice} with the content of the current one mapped by the given {@link Converter}.
	 * @since 1.10
	 */
	<U> Slice<U> map(Function<? super T, ? extends U> converter);
}

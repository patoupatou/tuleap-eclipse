/*******************************************************************************
 * Copyright (c) 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.tuleap.mylyn.task.internal.core.client.rest;

import java.util.Iterator;

/**
 * Iterable rest operation, typically a GET operation on a resource that returns arrays of elements should
 * implement this interface.
 * 
 * @param <T>
 *            The type of iterated elements.
 * @author <a href="mailto:laurent.delaigue@obeo.fr">Laurent Delaigue</a>
 */
public interface IRestIterableOperation<T> extends IRestOperation {

	/**
	 * Iterator.
	 * 
	 * @return An iterator over the elements returned by this operation.
	 */
	Iterator<T> iterator();
}

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

import org.restlet.data.Method;

/**
 * PUT operation on a REST resource.
 * 
 * @author <a href="mailto:laurent.delaigue@obeo.fr">Laurent Delaigue</a>
 */
public class RestOpPut extends AbstractRestOperation {

	/**
	 * Constructor.
	 * 
	 * @param fullUrl
	 *            The resource's URL.
	 * @param connector
	 *            the connector to use to "task to" the server.
	 */
	public RestOpPut(String fullUrl, IRestConnector connector) {
		super(fullUrl, connector);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.tuleap.mylyn.task.internal.core.client.rest.IRestOperation#getMethodName()
	 */
	public String getMethodName() {
		return Method.PUT.getName();
	}

}

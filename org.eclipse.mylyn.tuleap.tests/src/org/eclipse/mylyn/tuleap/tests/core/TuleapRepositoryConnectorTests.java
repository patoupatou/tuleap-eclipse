/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.tuleap.tests.core;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.tuleap.core.repository.TuleapRepositoryConnector;

/**
 * Tests for the {@link TuleapRepositoryConnector}.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapRepositoryConnectorTests extends TestCase {

	/**
	 * Test the retrieval of the repository url from a given task url.
	 */
	public void testGetRepositoryUrlFromTaskUrl() {
		final String taskUrl = "https://demo.tuleap.net/plugins/tracker/?tracker=409&aid=453"; //$NON-NLS-1$

		TuleapRepositoryConnector connector = new TuleapRepositoryConnector();
		String repositoryUrl = connector.getRepositoryUrlFromTaskUrl(taskUrl);
		assertEquals("https://demo.tuleap.net/plugins/tracker/?tracker=409", repositoryUrl); //$NON-NLS-1$
	}

	/**
	 * Test the retrieval of the task id from the url of the task.
	 */
	public void testGetTaskIdFromTaskUrl() {
		final String taskUrl = "https://demo.tuleap.net/plugins/tracker/?tracker=409&aid=217"; //$NON-NLS-1$

		TuleapRepositoryConnector connector = new TuleapRepositoryConnector();
		String taskId = connector.getTaskIdFromTaskUrl(taskUrl);
		assertEquals("217", taskId); //$NON-NLS-1$
	}

	/**
	 * Test the creation of the task url from the repository url and the task id.
	 */
	public void testGetTaskUrl() {
		final String repositoryUrl = "https://demo.tuleap.net/plugins/tracker/?tracker=409"; //$NON-NLS-1$
		final String taskId = "821"; //$NON-NLS-1$

		TuleapRepositoryConnector connector = new TuleapRepositoryConnector();
		String taskUrl = connector.getTaskUrl(repositoryUrl, taskId);
		assertEquals("https://demo.tuleap.net/plugins/tracker/?tracker=409&aid=821", taskUrl); //$NON-NLS-1$
	}

}
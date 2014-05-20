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
package org.eclipse.mylyn.internal.tuleap.ui.repository;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

/**
 * The Tuleap validator will be used inside of the repository setting page in orde rto check the
 * login/password and the tracker configuration.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapValidator {

	/**
	 * The task repository.
	 */
	private TaskRepository taskRepository;

	/**
	 * The constructor.
	 * 
	 * @param repository
	 *            The Mylyn task repository
	 */
	public TuleapValidator(TaskRepository repository) {
		this.taskRepository = repository;
	}

	/**
	 * Validate the settings of the repository.
	 * 
	 * @param monitor
	 *            The progress monitor
	 * @throws CoreException
	 *             In case of error with the configuration of the repository
	 */
	public void validate(IProgressMonitor monitor) throws CoreException {
		// TODO Check connection, login/password
		AbstractWebLocation location = new TaskRepositoryLocationFactory().createWebLocation(taskRepository);

		// Return an Eclipse status?
	}
}

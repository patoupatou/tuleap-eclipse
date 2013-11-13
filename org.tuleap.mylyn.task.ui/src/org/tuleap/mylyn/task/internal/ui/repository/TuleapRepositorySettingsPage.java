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
package org.tuleap.mylyn.task.internal.ui.repository;

import java.net.Proxy;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;
import org.tuleap.mylyn.task.internal.core.client.rest.ITuleapAPIVersions;
import org.tuleap.mylyn.task.internal.core.client.rest.RestResourceFactory;
import org.tuleap.mylyn.task.internal.core.client.rest.TuleapRestClient;
import org.tuleap.mylyn.task.internal.core.client.rest.TuleapRestConnector;
import org.tuleap.mylyn.task.internal.core.client.soap.TuleapSoapClient;
import org.tuleap.mylyn.task.internal.core.client.soap.TuleapSoapConnector;
import org.tuleap.mylyn.task.internal.core.client.soap.TuleapSoapParser;
import org.tuleap.mylyn.task.internal.core.client.soap.TuleapSoapSerializer;
import org.tuleap.mylyn.task.internal.core.parser.TuleapJsonParser;
import org.tuleap.mylyn.task.internal.core.parser.TuleapJsonSerializer;
import org.tuleap.mylyn.task.internal.core.util.ITuleapConstants;
import org.tuleap.mylyn.task.internal.ui.TuleapTasksUIPlugin;
import org.tuleap.mylyn.task.internal.ui.util.TuleapMylynTasksUIMessages;

/**
 * The wizard page displaying the settings of the Tuleap repository.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 0.7
 */
public class TuleapRepositorySettingsPage extends AbstractRepositorySettingsPage {

	/**
	 * The constructor.
	 * 
	 * @param taskRepository
	 *            the Mylyn task repository.
	 */
	public TuleapRepositorySettingsPage(TaskRepository taskRepository) {
		super(TuleapMylynTasksUIMessages.getString("TuleapRepositorySettingsPage.Name"), //$NON-NLS-1$
				TuleapMylynTasksUIMessages.getString("TuleapRepositorySettingsPage.Description"), //$NON-NLS-1$
				taskRepository);
		this.setNeedsAnonymousLogin(true);
		this.setNeedsValidateOnFinish(true);
		this.setNeedsHttpAuth(true);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#getConnectorKind()
	 */
	@Override
	public String getConnectorKind() {
		return ITuleapConstants.CONNECTOR_KIND;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#createAdditionalControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createAdditionalControls(Composite parent) {
		// nothing yet (consider supporting multiple versions of Tuleap later)
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#isValidUrl(java.lang.String)
	 */
	@Override
	protected boolean isValidUrl(String url) {
		// The Tuleap tracker that the repository will respect the following pattern
		// "https://<domainName>"
		boolean isValid = super.isValidUrl(url);
		return isValid && url.matches("https://.*"); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#isMissingCredentials()
	 */
	@Override
	protected boolean isMissingCredentials() {
		boolean isMissingCredentials = super.isMissingCredentials();
		if (!isMissingCredentials) {
			isMissingCredentials = "".equals(repositoryPasswordEditor.getStringValue().trim()); //$NON-NLS-1$
		}
		return isMissingCredentials;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#applyTo(org.eclipse.mylyn.tasks.core.TaskRepository)
	 */
	@Override
	public void applyTo(TaskRepository taskRepository) {
		super.applyTo(taskRepository);
		taskRepository
				.setProperty(IRepositoryConstants.PROPERTY_CATEGORY, IRepositoryConstants.CATEGORY_BUGS);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#getValidator(org.eclipse.mylyn.tasks.core.TaskRepository)
	 */
	@Override
	protected Validator getValidator(TaskRepository taskRepository) {
		return new TuleapRepositoryValidator(taskRepository);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage#validateSettings()
	 */
	@Override
	protected void validateSettings() {
		if (repository != null) {
			AuthenticationCredentials repoCredentials = repository
					.getCredentials(AuthenticationType.REPOSITORY);
			AuthenticationCredentials proxyCredentials = repository.getCredentials(AuthenticationType.PROXY);
			AuthenticationCredentials httpCredentials = repository.getCredentials(AuthenticationType.HTTP);

			super.validateSettings();

			repository.setCredentials(AuthenticationType.REPOSITORY, repoCredentials, repository
					.getSavePassword(AuthenticationType.REPOSITORY));
			repository.setCredentials(AuthenticationType.HTTP, httpCredentials, repository
					.getSavePassword(AuthenticationType.HTTP));
			repository.setCredentials(AuthenticationType.PROXY, proxyCredentials, repository
					.getSavePassword(AuthenticationType.PROXY));
		} else {
			super.validateSettings();
		}
	}

	/**
	 * The validator used to check the configuration of the repository.
	 * 
	 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
	 * @since 0.7
	 */
	private class TuleapRepositoryValidator extends Validator {

		/**
		 * The validation engine.
		 */
		private TuleapValidator tuleapValidator;

		/**
		 * The constructor.
		 * 
		 * @param taskRepository
		 *            The Mylyn task repository
		 */
		public TuleapRepositoryValidator(TaskRepository taskRepository) {
			final AbstractWebLocation location = new TaskRepositoryLocationFactory()
					.createWebLocation(taskRepository);

			ILog logger = Platform.getLog(Platform.getBundle(TuleapTasksUIPlugin.PLUGIN_ID));
			TuleapSoapParser tuleapSoapParser = new TuleapSoapParser();
			TuleapSoapSerializer tuleapSoapSerializer = new TuleapSoapSerializer();
			TuleapSoapConnector tuleapSoapConnector = new TuleapSoapConnector(location);

			TuleapSoapClient tuleapSoapClient = new TuleapSoapClient(taskRepository, tuleapSoapConnector,
					tuleapSoapParser, tuleapSoapSerializer, logger);

			TuleapJsonParser jsonParser = new TuleapJsonParser();
			TuleapJsonSerializer jsonSerializer = new TuleapJsonSerializer();
			// TODO Temporary hack to access a different REST server
			AbstractWebLocation webLocationRest = new AbstractWebLocation(taskRepository.getRepositoryLabel()) {

				@Override
				public Proxy getProxyForHost(String host, String proxyType) {
					return location.getProxyForHost(host, proxyType);
				}

				@Override
				public AuthenticationCredentials getCredentials(AuthenticationType type) {
					return location.getCredentials(type);
				}
			};
			TuleapRestConnector tuleapRestConnector = new TuleapRestConnector(location, logger);
			RestResourceFactory resourceFactory = new RestResourceFactory(location.getUrl(),
					ITuleapAPIVersions.BEST_VERSION, tuleapRestConnector);
			TuleapRestClient tuleapRestClient = new TuleapRestClient(resourceFactory, jsonParser,
					jsonSerializer, taskRepository, logger);

			this.tuleapValidator = new TuleapValidator(location, tuleapSoapClient, tuleapRestClient,
					taskRepository);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage.Validator#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			IStatus validationStatus = this.tuleapValidator.validate(monitor);
			this.setStatus(validationStatus);
		}

	}
}

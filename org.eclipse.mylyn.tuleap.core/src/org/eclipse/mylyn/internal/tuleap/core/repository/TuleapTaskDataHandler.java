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
package org.eclipse.mylyn.internal.tuleap.core.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.internal.tuleap.core.client.ITuleapClient;
import org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapDynamicField;
import org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapField;
import org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapFormElement;
import org.eclipse.mylyn.internal.tuleap.core.model.TuleapArtifact;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapFileUpload;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapMultiSelectBox;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapSelectBox;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapSelectBoxItem;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapString;
import org.eclipse.mylyn.internal.tuleap.core.model.field.dynamic.TuleapArtifactId;
import org.eclipse.mylyn.internal.tuleap.core.model.field.dynamic.TuleapLastUpdateDate;
import org.eclipse.mylyn.internal.tuleap.core.model.field.dynamic.TuleapSubmittedBy;
import org.eclipse.mylyn.internal.tuleap.core.model.field.dynamic.TuleapSubmittedOn;
import org.eclipse.mylyn.internal.tuleap.core.model.permission.ITuleapDefaultPermissionGroups;
import org.eclipse.mylyn.internal.tuleap.core.model.permission.TuleapAccessPermission;
import org.eclipse.mylyn.internal.tuleap.core.model.workflow.TuleapWorkflow;
import org.eclipse.mylyn.internal.tuleap.core.util.ITuleapConstants;
import org.eclipse.mylyn.internal.tuleap.core.util.TuleapMylynTasksMessages;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;

/**
 * This class is in charge of the publication and retrieval of the tasks data to and from the repository.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapTaskDataHandler extends AbstractTaskDataHandler {

	/**
	 * The Tuleap repository connector.
	 */
	private ITuleapRepositoryConnector connector;

	/**
	 * The constructor.
	 * 
	 * @param repositoryConnector
	 *            The Tuleap repository connector.
	 */
	public TuleapTaskDataHandler(ITuleapRepositoryConnector repositoryConnector) {
		this.connector = repositoryConnector;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler#postTaskData(org.eclipse.mylyn.tasks.core.TaskRepository,
	 *      org.eclipse.mylyn.tasks.core.data.TaskData, java.util.Set,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData,
			Set<TaskAttribute> oldAttributes, IProgressMonitor monitor) throws CoreException {
		RepositoryResponse response = null;

		TuleapArtifact artifact = TuleapTaskDataHandler.getTuleapArtifact(repository, taskData);
		ITuleapClient client = this.connector.getClientManager().getClient(repository);
		if (taskData.isNew()) {
			int artifactId = client.createArtifact(artifact, monitor);
			response = new RepositoryResponse(ResponseKind.TASK_CREATED, Integer.valueOf(artifactId)
					.toString());
		} else {
			client.updateArtifact(artifact, monitor);
			response = new RepositoryResponse(ResponseKind.TASK_UPDATED, Integer.valueOf(artifact.getId())
					.toString());
		}

		return response;
	}

	/**
	 * Returns a Tuleap artifact representing the given Mylyn task data.
	 * 
	 * @param repository
	 *            The Mylyn task repository
	 * @param taskData
	 *            The Mylyn task data
	 * @return A Tuleap artifact representing the given Mylyn task data.
	 */
	private static TuleapArtifact getTuleapArtifact(TaskRepository repository, TaskData taskData) {
		// Create the artifact
		TuleapArtifact artifact = null;
		if (taskData.isNew()) {
			artifact = new TuleapArtifact();
		} else {
			artifact = new TuleapArtifact(TuleapRepositoryConnector.getArtifactId(taskData.getTaskId()));
		}

		Collection<TaskAttribute> attributes = taskData.getRoot().getAttributes().values();
		for (TaskAttribute taskAttribute : attributes) {
			// Ignore Tuleap internal attributes
			if (!TuleapAttributeMapper.isInternalAttribute(taskAttribute)) {
				artifact.putValue(taskAttribute.getId(), taskAttribute.getValue());
			}
		}

		// TODO Handle the other attributes (date to parse, status to set, etc...)

		return artifact;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler#initializeTaskData(org.eclipse.mylyn.tasks.core.TaskRepository,
	 *      org.eclipse.mylyn.tasks.core.data.TaskData, org.eclipse.mylyn.tasks.core.ITaskMapping,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean initializeTaskData(TaskRepository repository, TaskData taskData,
			ITaskMapping initializationData, IProgressMonitor monitor) throws CoreException {
		if (taskData.isNew()) {
			TuleapRepositoryConfiguration repositoryConfiguration = connector.getRepositoryConfiguration(
					repository, false, monitor);
			if (repositoryConfiguration != null) {
				// Update the available attributes for the tasks
				ITuleapClient tuleapClient = this.connector.getClientManager().getClient(repository);
				tuleapClient.updateAttributes(monitor, false);
				this.createDefaultAttributes(taskData, tuleapClient, false);

				// Sets the creation date and last modification date.
				if (this.connector instanceof AbstractRepositoryConnector
						&& ((AbstractRepositoryConnector)this.connector).getTaskMapping(taskData) instanceof TaskMapper) {
					ITaskMapping taskMapping = ((AbstractRepositoryConnector)this.connector)
							.getTaskMapping(taskData);
					TaskMapper taskMapper = (TaskMapper)taskMapping;
					taskMapper.setCreationDate(new Date());
					taskMapper.setModificationDate(new Date());
					taskMapper.setSummary(TuleapMylynTasksMessages.getString(
							"TuleapTaskDataHandler.DefaultNewTitle", repositoryConfiguration.getItemName())); //$NON-NLS-1$

					this.createOperations(taskData, tuleapClient, taskMapper.getStatus());
					this.createPersons(taskData, tuleapClient);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Creates the default Tuleap related attributes.
	 * 
	 * @param taskData
	 *            The task data
	 * @param tuleapClient
	 *            The Tuleap client
	 * @param existingTask
	 *            Indicates if we are manipulating a newly created task
	 */
	private void createDefaultAttributes(TaskData taskData, ITuleapClient tuleapClient, boolean existingTask) {
		TuleapRepositoryConfiguration configuration = tuleapClient.getRepositoryConfiguration();
		if (configuration == null) {
			return;
		}
		// The kind of the task
		TaskAttribute attribute = taskData.getRoot().createAttribute(TaskAttribute.TASK_KIND);
		String name = configuration.getName();
		if (name != null) {
			attribute.setValue(name);
		}

		// Creation date
		attribute = taskData.getRoot().createAttribute(TaskAttribute.DATE_CREATION);
		TaskAttributeMetaData metaData = attribute.getMetaData();
		metaData.setLabel(TuleapMylynTasksMessages.getString("TuleapTaskDataHandler.CreationDate")); //$NON-NLS-1$
		metaData.setType(TaskAttribute.TYPE_DATE);

		// Last modification date
		attribute = taskData.getRoot().createAttribute(TaskAttribute.DATE_MODIFICATION);
		metaData = attribute.getMetaData();
		metaData.setLabel(TuleapMylynTasksMessages.getString("TuleapTaskDataHandler.LastModificationDate")); //$NON-NLS-1$
		metaData.setType(TaskAttribute.TYPE_DATE);

		// New comment
		attribute = taskData.getRoot().createAttribute(TaskAttribute.COMMENT_NEW);
		metaData = attribute.getMetaData();
		metaData.setLabel(TuleapMylynTasksMessages.getString("TuleapTaskDataHandler.NewComment")); //$NON-NLS-1$
		metaData.setType(TaskAttribute.TYPE_LONG_RICH_TEXT);

		// Default attributes
		List<AbstractTuleapFormElement> formElements = configuration.getFormElements();
		for (AbstractTuleapFormElement abstractTuleapStructuralElement : formElements) {
			List<AbstractTuleapField> fields = TuleapRepositoryConfiguration
					.getFields(abstractTuleapStructuralElement);
			for (AbstractTuleapField abstractTuleapField : fields) {
				if (shouldCreateAttributeFor(abstractTuleapField)) {
					this.createAttribute(taskData, abstractTuleapField);
				}
			}
		}
	}

	/**
	 * Indicates if we should create a mylyn attribute for the given tuleap field.
	 * 
	 * @param abstractTuleapField
	 *            the tuleap field
	 * @return <code>true</code> if we should create a Mylyn attribute for the Tuleap field,
	 *         <code>false</code> otherwise.
	 */
	private boolean shouldCreateAttributeFor(AbstractTuleapField abstractTuleapField) {
		boolean shouldCreateAttributeFor = true;
		shouldCreateAttributeFor = shouldCreateAttributeFor
				&& !(abstractTuleapField instanceof TuleapFileUpload);
		shouldCreateAttributeFor = shouldCreateAttributeFor
				&& !(abstractTuleapField instanceof TuleapArtifactId);
		shouldCreateAttributeFor = shouldCreateAttributeFor
				&& !(abstractTuleapField instanceof TuleapLastUpdateDate);
		shouldCreateAttributeFor = shouldCreateAttributeFor
				&& !(abstractTuleapField instanceof TuleapSubmittedOn);
		shouldCreateAttributeFor = shouldCreateAttributeFor
				&& !(abstractTuleapField instanceof TuleapSubmittedBy);
		return shouldCreateAttributeFor;
	}

	/**
	 * Creates the attribute representing the Tuleap field in the given Mylyn task data.
	 * 
	 * @param taskData
	 *            The Mylyn task data
	 * @param tuleapField
	 *            The Tuleap field
	 */
	private void createAttribute(TaskData taskData, AbstractTuleapField tuleapField) {
		TaskAttribute attribute = this.instantiateAttribute(taskData, tuleapField);
		TaskAttributeMetaData attributeMetaData = attribute.getMetaData();

		// Set the kind if we do not have a semantic field
		boolean isTitle = tuleapField instanceof TuleapString
				&& ((TuleapString)tuleapField).isSemanticTitle();
		boolean isStatus = tuleapField instanceof TuleapSelectBox
				&& ((TuleapSelectBox)tuleapField).isSemanticStatus();
		boolean isContributor = tuleapField instanceof TuleapSelectBox
				&& ((TuleapSelectBox)tuleapField).isSemanticContributor();

		if (!isTitle && !isStatus && !isContributor) {
			attributeMetaData.setKind(tuleapField.getMetadataKind());
		}

		String group = ITuleapDefaultPermissionGroups.ALL_USERS;
		if (!tuleapField.getPermissions().canSubmit(group)
				&& !(TuleapAccessPermission.UPDATE == tuleapField.getPermissions().getAccessPermission(group))) {
			// TODO Compute the group of the user
			// attributeMetadata.setReadOnly(true);
		}

		// Dynamic fields are read only
		if (tuleapField instanceof AbstractTuleapDynamicField) {
			attributeMetaData.setReadOnly(true);
		}

		// Possible values
		Map<String, String> options = tuleapField.getOptions();
		for (Entry<String, String> entry : options.entrySet()) {
			attribute.putOption(entry.getKey(), entry.getValue());
		}

		// Default values
		Object defaultValue = tuleapField.getDefaultValue();
		if (defaultValue instanceof String) {
			attribute.setValue((String)defaultValue);
		} else if (defaultValue instanceof List<?>) {
			List<?> list = (List<?>)defaultValue;

			List<String> strList = new ArrayList<String>();
			for (Object object : list) {
				if (object instanceof String) {
					strList.add((String)object);
				}
			}

			attribute.setValues(strList);
		}
	}

	/**
	 * Instantiate the attribute.
	 * 
	 * @param taskData
	 *            The task data
	 * @param tuleapField
	 *            The tuleap field
	 * @return The attribute instantiated to represent the given tuleap field in the given task data.
	 */
	private TaskAttribute instantiateAttribute(TaskData taskData, AbstractTuleapField tuleapField) {
		TaskAttribute attribute = null;

		// Semantic support
		if (tuleapField instanceof TuleapString && ((TuleapString)tuleapField).isSemanticTitle()) {
			attribute = taskData.getRoot().createAttribute(TaskAttribute.SUMMARY);
			// Attributes
			TaskAttributeMetaData attributeMetadata = attribute.getMetaData();
			attributeMetadata.setType(tuleapField.getMetadataType());
			attributeMetadata.setLabel(tuleapField.getLabel());
		} else if (tuleapField instanceof TuleapSelectBox) {
			TuleapSelectBox selectBox = (TuleapSelectBox)tuleapField;
			if (selectBox.isSemanticStatus()) {
				// Create an attribute for the assigned person
				attribute = taskData.getRoot().createAttribute(TaskAttribute.STATUS);
				TaskAttributeMetaData metaData = attribute.getMetaData();
				metaData.setLabel(TuleapMylynTasksMessages.getString("TuleapTaskDataHandler.Status")); //$NON-NLS-1$
				metaData.setType(TaskAttribute.TYPE_SINGLE_SELECT);
			} else if (selectBox.isSemanticContributor()) {
				// Create an attribute for the assigned person
				attribute = taskData.getRoot().createAttribute(TaskAttribute.USER_ASSIGNED);
				TaskAttributeMetaData metaData = attribute.getMetaData();
				metaData.setLabel(TuleapMylynTasksMessages.getString("TuleapTaskDataHandler.AssignedToLabel")); //$NON-NLS-1$
				metaData.setType(TaskAttribute.TYPE_PERSON);
			} else {
				attribute = taskData.getRoot().createAttribute(tuleapField.getIdentifier());
				// Attributes
				TaskAttributeMetaData attributeMetadata = attribute.getMetaData();
				attributeMetadata.setType(tuleapField.getMetadataType());
				attributeMetadata.setLabel(tuleapField.getLabel());
			}
		} else {
			attribute = taskData.getRoot().createAttribute(tuleapField.getIdentifier());
			// Attributes
			TaskAttributeMetaData attributeMetadata = attribute.getMetaData();
			attributeMetadata.setType(tuleapField.getMetadataType());
			attributeMetadata.setLabel(tuleapField.getLabel());
		}

		return attribute;
	}

	/**
	 * Creates the operations available on the task data thanks to the configuration of the client while using
	 * the given current status.
	 * 
	 * @param taskData
	 *            The task data
	 * @param tuleapClient
	 *            The tuleap client used to get the configuration of the tracker
	 * @param currentStatus
	 *            The current status
	 */
	private void createOperations(TaskData taskData, ITuleapClient tuleapClient, String currentStatus) {
		TuleapSelectBox statusSelectBox = null;

		// Create operations from the status semantic
		TuleapRepositoryConfiguration configuration = tuleapClient.getRepositoryConfiguration();
		List<AbstractTuleapFormElement> formElements = configuration.getFormElements();
		for (AbstractTuleapFormElement abstractTuleapStructuralElement : formElements) {
			List<AbstractTuleapField> fields = TuleapRepositoryConfiguration
					.getFields(abstractTuleapStructuralElement);
			for (AbstractTuleapField abstractTuleapField : fields) {
				if (abstractTuleapField instanceof TuleapSelectBox) {
					TuleapSelectBox selectBox = (TuleapSelectBox)abstractTuleapField;
					if (selectBox.isSemanticStatus()) {
						statusSelectBox = selectBox;
					}
				}
			}
		}

		if (statusSelectBox != null) {
			// Create the default attribute for the operation
			TaskAttribute operationAttribute = taskData.getRoot().getAttribute(TaskAttribute.OPERATION);
			if (operationAttribute == null) {
				operationAttribute = taskData.getRoot().createAttribute(TaskAttribute.OPERATION);
			}
			TaskOperation.applyTo(operationAttribute, TaskAttribute.STATUS, TuleapMylynTasksMessages
					.getString("TuleapTaskDataHandler.MarkAs")); //$NON-NLS-1$

			List<String> tuleapStatus = new ArrayList<String>();

			TuleapWorkflow workflow = statusSelectBox.getWorkflow();
			if (workflow.getTransitions().size() > 0) {
				List<TuleapSelectBoxItem> items = statusSelectBox.getItems();
				for (TuleapSelectBoxItem tuleapSelectBoxItem : items) {
					if (tuleapSelectBoxItem.getLabel().equals(currentStatus)) {
						// Only support the reachable state from the current status
						tuleapStatus.addAll(workflow.accessibleStates(tuleapSelectBoxItem.getIdentifier()));
					}
				}
			} else {
				List<TuleapSelectBoxItem> items = statusSelectBox.getItems();
				for (TuleapSelectBoxItem tuleapSelectBoxItem : items) {
					tuleapStatus.add(tuleapSelectBoxItem.getLabel());
				}
			}

			TaskAttribute attrResolvedInput = taskData.getRoot().createAttribute(
					TaskAttribute.PREFIX_OPERATION + TaskAttribute.STATUS);
			attrResolvedInput.getMetaData().setType(TaskAttribute.TYPE_SINGLE_SELECT);
			attrResolvedInput.getMetaData().setKind(TaskAttribute.KIND_OPERATION);
			for (String status : tuleapStatus) {
				attrResolvedInput.putOption(status, status);
			}
			TaskOperation.applyTo(attrResolvedInput, TaskAttribute.STATUS, TuleapMylynTasksMessages
					.getString("TuleapTaskDataHandler.MarkAs")); //$NON-NLS-1$

			attrResolvedInput.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID,
					TaskAttribute.STATUS);
		}
	}

	/**
	 * Creates the attributes to manage the persons to which the task is assigned.
	 * 
	 * @param taskData
	 *            The task data
	 * @param tuleapClient
	 *            The tuleap client used to get the configuration of the tracker
	 */
	private void createPersons(TaskData taskData, ITuleapClient tuleapClient) {
		AbstractTuleapField personsSelectBox = null;

		TuleapRepositoryConfiguration configuration = tuleapClient.getRepositoryConfiguration();
		List<AbstractTuleapFormElement> formElements = configuration.getFormElements();
		for (AbstractTuleapFormElement abstractTuleapStructuralElement : formElements) {
			List<AbstractTuleapField> fields = TuleapRepositoryConfiguration
					.getFields(abstractTuleapStructuralElement);
			for (AbstractTuleapField abstractTuleapField : fields) {
				if (abstractTuleapField instanceof TuleapSelectBox) {
					TuleapSelectBox selectBox = (TuleapSelectBox)abstractTuleapField;
					if (selectBox.isSemanticStatus()) {
						personsSelectBox = selectBox;
					}
				} else if (abstractTuleapField instanceof TuleapMultiSelectBox) {
					TuleapMultiSelectBox selectBox = (TuleapMultiSelectBox)abstractTuleapField;
					if (selectBox.isSemanticStatus()) {
						personsSelectBox = selectBox;
					}
				}
			}
		}

		if (personsSelectBox instanceof TuleapSelectBox) {
			// Only one possible assignee
			TuleapSelectBox tuleapSelectBox = (TuleapSelectBox)personsSelectBox;

			TaskAttribute attribute = taskData.getRoot().createAttribute(TaskAttribute.USER_ASSIGNED);
			TaskAttributeMetaData metaData = attribute.getMetaData();
			metaData.setKind(TaskAttribute.KIND_PEOPLE);
			metaData.setType(TaskAttribute.TYPE_SINGLE_SELECT);

			List<TuleapSelectBoxItem> items = tuleapSelectBox.getItems();
			for (TuleapSelectBoxItem tuleapSelectBoxItem : items) {
				attribute.putOption(tuleapSelectBoxItem.getLabel(), tuleapSelectBoxItem.getLabel());
			}
		} else if (personsSelectBox instanceof TuleapMultiSelectBox) {
			// Multiple assignee supported
			TuleapMultiSelectBox tuleapSelectBox = (TuleapMultiSelectBox)personsSelectBox;

			TaskAttribute attribute = taskData.getRoot().createAttribute(TaskAttribute.USER_ASSIGNED);
			TaskAttributeMetaData metaData = attribute.getMetaData();
			metaData.setKind(TaskAttribute.KIND_PEOPLE);
			metaData.setType(TaskAttribute.TYPE_MULTI_SELECT);

			List<TuleapSelectBoxItem> items = tuleapSelectBox.getItems();
			for (TuleapSelectBoxItem tuleapSelectBoxItem : items) {
				attribute.putOption(tuleapSelectBoxItem.getLabel(), tuleapSelectBoxItem.getLabel());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler#getAttributeMapper(org.eclipse.mylyn.tasks.core.TaskRepository)
	 */
	@Override
	public TaskAttributeMapper getAttributeMapper(TaskRepository repository) {
		return new TuleapAttributeMapper(repository, connector);
	}

	/**
	 * Returns the Mylyn task data for the task matching the given task id in the given Mylyn task repository.
	 * 
	 * @param taskRepository
	 *            the Mylyn task repository
	 * @param taskId
	 *            the id of the task
	 * @param monitor
	 *            the progress monitor
	 * @return The Mylyn task data for the task matching the given task id in the given Mylyn task repository.
	 * @throws CoreException
	 *             If repository file configuration is not accessible
	 */
	public TaskData getTaskData(TaskRepository taskRepository, String taskId, IProgressMonitor monitor)
			throws CoreException {
		return downloadTaskData(taskRepository, TuleapRepositoryConnector.getArtifactId(taskId), monitor);
	}

	/**
	 * Downloads the Tuleap artifact and convert it to a Mylyn task data.
	 * 
	 * @param taskRepository
	 *            The Mylyn tasks repository
	 * @param taskId
	 *            The ID of the task to download
	 * @param monitor
	 *            The progress monitor
	 * @return The Mylyn task data with the information obtained from the Tuleap artifact matching the given
	 *         task ID obtained from the Tuleap client.
	 * @throws CoreException
	 *             If repository file configuration is not accessible
	 */
	public TaskData downloadTaskData(TaskRepository taskRepository, int taskId, IProgressMonitor monitor)
			throws CoreException {
		ITuleapClient tuleapClient = this.connector.getClientManager().getClient(taskRepository);

		tuleapClient.updateAttributes(monitor, false);
		TuleapArtifact tuleapArtifact = tuleapClient.getArtifact(taskId, monitor);
		TaskData taskData = this.createTaskDataFromArtifact(tuleapClient, taskRepository, tuleapArtifact,
				monitor);

		return taskData;
	}

	/**
	 * Creates the Mylyn task data from the Tuleap artifact obtained from the Tuleap tracker.
	 * 
	 * @param tuleapClient
	 *            The Tuleap client
	 * @param taskRepository
	 *            The Mylyn tasks repository
	 * @param tuleapArtifact
	 *            The Tuleap Artifact
	 * @param monitor
	 *            The progress monitor
	 * @return The Mylyn task data computed from the Tuleap artifact.
	 */
	private TaskData createTaskDataFromArtifact(ITuleapClient tuleapClient, TaskRepository taskRepository,
			TuleapArtifact tuleapArtifact, IProgressMonitor monitor) {
		// Create the default attributes
		TaskData taskData = new TaskData(this.getAttributeMapper(taskRepository),
				ITuleapConstants.CONNECTOR_KIND, taskRepository.getRepositoryUrl(), Integer.valueOf(
						tuleapArtifact.getId()).toString());

		tuleapClient.updateAttributes(monitor, false);
		this.createDefaultAttributes(taskData, tuleapClient, false);
		this.createOperations(taskData, tuleapClient, tuleapArtifact.getValue(TaskAttribute.STATUS));

		// Convert Tuleap artifact to Mylyn task data
		Set<String> keys = tuleapArtifact.getKeys();
		for (String key : keys) {
			String value = tuleapArtifact.getValue(key);
			TaskAttribute attribute = taskData.getRoot();
			Map<String, TaskAttribute> attributes = attribute.getAttributes();
			TaskAttribute taskAttribute = attributes.get(key);
			if (taskAttribute != null && value != null) {
				taskAttribute.setValue(value);
			}
		}
		return taskData;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler#canGetMultiTaskData(org.eclipse.mylyn.tasks.core.TaskRepository)
	 */
	@Override
	public boolean canGetMultiTaskData(TaskRepository repository) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler#canInitializeSubTaskData(org.eclipse.mylyn.tasks.core.TaskRepository,
	 *      org.eclipse.mylyn.tasks.core.ITask)
	 */
	@Override
	public boolean canInitializeSubTaskData(TaskRepository repository, ITask task) {
		return false;
	}

}

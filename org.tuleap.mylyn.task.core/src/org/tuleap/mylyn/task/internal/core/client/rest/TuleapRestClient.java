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

// By design, tThis class should have no dependency to
// - com.google.json
// - org.restlet
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.tuleap.mylyn.task.internal.core.TuleapCoreActivator;
import org.tuleap.mylyn.task.internal.core.model.TuleapAttachmentDescriptor;
import org.tuleap.mylyn.task.internal.core.model.TuleapProjectConfiguration;
import org.tuleap.mylyn.task.internal.core.model.TuleapServerConfiguration;
import org.tuleap.mylyn.task.internal.core.model.agile.TuleapBacklogItem;
import org.tuleap.mylyn.task.internal.core.model.agile.TuleapBacklogItemType;
import org.tuleap.mylyn.task.internal.core.model.agile.TuleapMilestone;
import org.tuleap.mylyn.task.internal.core.model.agile.TuleapMilestoneType;
import org.tuleap.mylyn.task.internal.core.model.agile.TuleapTopPlanning;
import org.tuleap.mylyn.task.internal.core.model.tracker.TuleapTrackerConfiguration;
import org.tuleap.mylyn.task.internal.core.parser.TuleapBacklogItemDeserializer;
import org.tuleap.mylyn.task.internal.core.parser.TuleapBacklogItemTypeDeserializer;
import org.tuleap.mylyn.task.internal.core.parser.TuleapJsonParser;
import org.tuleap.mylyn.task.internal.core.parser.TuleapJsonSerializer;
import org.tuleap.mylyn.task.internal.core.parser.TuleapMilestoneDeserializer;
import org.tuleap.mylyn.task.internal.core.parser.TuleapMilestoneTypeDeserializer;

/**
 * This class will be used to communicate with the server with a higher level of abstraction than raw HTTP
 * connections. This class will not have any relation with the framework used to parse the JSON nor with the
 * framework used to communicate with the server in order to be easily testable.
 * <p>
 * All the operations that can be used from the editor will throw {@link CoreException} in order to let the
 * editor catch these exceptions. All the operations that cannot be used from an editor will instead log their
 * errors using the logger.
 * </p>
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 */
public class TuleapRestClient {

	/**
	 * The version number of the best api supported.
	 */
	public static final String BEST_SUPPORTED_API_VERSION = ITuleapAPIVersions.V1_0;

	/**
	 * Utility class used to communicate using HTTP with the server.
	 */
	private final TuleapRestConnector tuleapRestConnector;

	/**
	 * The JSON parser.
	 */
	private final TuleapJsonParser jsonParser;

	/**
	 * The JSON serializer.
	 */
	private final TuleapJsonSerializer jsonSerializer;

	/**
	 * The task repository.
	 */
	private final TaskRepository taskRepository;

	/**
	 * The task repository.
	 */
	private final ICredentials credentials;

	/**
	 * The logger.
	 */
	private ILog logger;

	/**
	 * The constructor.
	 * 
	 * @param tuleapRestConnector
	 *            The connector is used for HTTP communication
	 * @param jsonParser
	 *            The Tuleap JSON parser
	 * @param jsonSerializer
	 *            The Tuleap JSON serializer
	 * @param taskRepository
	 *            The task repository
	 * @param logger
	 *            The logger
	 */
	public TuleapRestClient(TuleapRestConnector tuleapRestConnector, TuleapJsonParser jsonParser,
			TuleapJsonSerializer jsonSerializer, TaskRepository taskRepository, ILog logger) {
		this.tuleapRestConnector = tuleapRestConnector;
		this.jsonParser = jsonParser;
		this.jsonSerializer = jsonSerializer;
		this.taskRepository = taskRepository;
		this.logger = logger;
		this.credentials = new TaskRepositoryCredentials(taskRepository);
	}

	/**
	 * Tests that we can use the repository by validating that the server provides a compatible version of the
	 * API and then trying to log in using the credentials from the task repository, checking the user
	 * session, and logging out.
	 * 
	 * @param monitor
	 *            Used to monitor the progress
	 * @return A status indicating if the server support a version of the API compatible with the one expected
	 *         by the connector and if we can successfully log in and log out from the server
	 * @throws CoreException
	 *             In case of error during the log in or log out process
	 */
	public IStatus validateConnection(IProgressMonitor monitor) throws CoreException {
		// Test the connection first
		RestResources restResources = tuleapRestConnector.resources(credentials);

		restResources.user().checkGet(Collections.<String, String> emptyMap());

		// If we made it here without exception, everything is fine since all the potential errors in the
		// validation process would be considered as critical, they would throw a CoreException
		return Status.OK_STATUS;
	}

	/**
	 * Returns the configuration of the Tuleap server.
	 * 
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The configuration of the server
	 * @throws CoreException
	 *             In case of error during the retrieval of the configuration
	 */
	public TuleapServerConfiguration getTuleapServerConfiguration(IProgressMonitor monitor)
			throws CoreException {
		// Test the connection
		RestResources restResources = tuleapRestConnector.resources(credentials);

		TuleapServerConfiguration tuleapServerConfiguration = null;

		// Check that we can get the list of projects
		RestProjects restProjects = restResources.projects();
		restProjects.checkGet(Collections.<String, String> emptyMap());

		// Retrieve the projects and create the configuration of each project
		// TODO Pagination on projects?
		ServerResponse projectsGetServerResponse = restProjects.get(Collections.<String, String> emptyMap());
		if (ITuleapServerStatus.OK == projectsGetServerResponse.getStatus()) {
			String projectsGetResponseBody = projectsGetServerResponse.getBody();
			List<TuleapProjectConfiguration> projectConfigurations = this.jsonParser
					.getProjectConfigurations(projectsGetResponseBody);

			// For each project that has the tracker service
			for (TuleapProjectConfiguration projectConfig : projectConfigurations) {
				// TODO Comment the tracker part while there is no REST API for trackers
				if (projectConfig.hasService("trackers")) { //$NON-NLS-1$
					// Check that we can get the list of trackers for this project
					RestProjectsTrackers restTrackers = restResources.projectsTrackers(projectConfig
							.getIdentifier());

					// Retrieve the trackers and create the configuration of each tracker
					ServerResponse projectTrackersGetServerResponse = restTrackers.get(Collections
							.<String, String> emptyMap());
					// TODO Pagination on trackers?
					if (ITuleapServerStatus.OK == projectTrackersGetServerResponse.getStatus()) {
						String projectTrackersGetResponseBody = projectTrackersGetServerResponse.getBody();
						List<TuleapTrackerConfiguration> trackerConfigurations = this.jsonParser
								.getTrackerConfigurations(projectTrackersGetResponseBody);
						// Put the configuration of the trackers in their containing project's
						// configuration
						for (TuleapTrackerConfiguration trackerConfig : trackerConfigurations) {
							projectConfig.addTracker(trackerConfig);
						}

						// adding the properties parent/children to trackers
						JsonParser trackersParser = new JsonParser();
						JsonArray trackersArray = trackersParser.parse(projectTrackersGetResponseBody)
								.getAsJsonArray();

						for (int i = 0; i < trackersArray.size(); i++) {
							JsonObject tracker = (JsonObject)trackersArray.get(i);
							int trackerId = tracker.get("id").getAsInt(); //$NON-NLS-1$
							JsonObject hierarchy = tracker.get("hierarchy").getAsJsonObject(); //$NON-NLS-1$
							int parentTrackerId = hierarchy.get("parent_tracker_id").getAsInt(); //$NON-NLS-1$

							projectConfig.getTrackerConfiguration(trackerId).setParentTracker(
									projectConfig.getTrackerConfiguration(parentTrackerId));
						}

					} else {
						// Invalid login? server error?
						String message = this.jsonParser.getErrorMessage(projectTrackersGetServerResponse
								.getBody());
						throw new CoreException(new Status(IStatus.ERROR, TuleapCoreActivator.PLUGIN_ID,
								message));
					}
				}

				// Retrieve Milestone types for the project
				List<TuleapMilestoneType> milestoneTypes = getMilestoneTypes(projectConfig.getIdentifier(),
						null);
				for (TuleapMilestoneType tuleapMilestoneType : milestoneTypes) {
					projectConfig.addMilestoneType(tuleapMilestoneType);
				}

				// Retrieve BacklogItem types for the project
				List<TuleapBacklogItemType> backLogItemTypes = getBacklogitemTypes(projectConfig
						.getIdentifier(), null);
				for (TuleapBacklogItemType tuleapBacklogItemType : backLogItemTypes) {
					projectConfig.addBacklogItemType(tuleapBacklogItemType);
				}

			}
		} else {
			// Invalid login? server error?
			String message = this.jsonParser.getErrorMessage(projectsGetServerResponse.getBody());
			throw new CoreException(new Status(IStatus.ERROR, TuleapCoreActivator.PLUGIN_ID, message));
		}

		return tuleapServerConfiguration;
	}

	// TODO get user groups of a project

	// TODO get users of a group

	/**
	 * Retrieves the artifact from the server with the given artifact id.
	 * 
	 * @param artifactId
	 *            The identifier of the artifact
	 * @param connector
	 *            The tuleap repository connector
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The task data of the artifact
	 * @throws CoreException
	 *             In case of error during the retrieval of the artifact
	 */
	/*
	 * public TaskData getArtifact(int artifactId, ITuleapRepositoryConnector connector, IProgressMonitor
	 * monitor) throws CoreException { // TODO [SBE] See if the parameter connector should be a class
	 * attribute instead of a parameter // Test the connection RestResources restResources =
	 * tuleapRestConnector.resources(credentials); // Send a request with OPTIONS to ensure that we can and
	 * have the right to retrieve the artifact RestArtifacts restArtifacts =
	 * restResources.artifacts(artifactId); restArtifacts.checkGet(Collections.<String, String> emptyMap());
	 * // Retrieve the artifact ServerResponse response = restArtifacts.get(Collections.<String, String>
	 * emptyMap()); if (ITuleapServerStatus.OK != response.getStatus()) { // Invalid login? server error?
	 * String message = this.jsonParser.getErrorMessage(response.getBody()); throw new CoreException(new
	 * Status(IStatus.ERROR, TuleapCoreActivator.PLUGIN_ID, message)); } // Create the task data String json =
	 * response.getBody(); return jsonParser.parseArtifact(taskRepository, connector, json); }
	 */

	/**
	 * Updates the artifact represented by the given task data with the given task data.
	 * 
	 * @param taskData
	 *            The task data
	 * @param monitor
	 *            Used to monitor the progress
	 * @throws CoreException
	 *             In case of error during the update of the artifact
	 */
	/*
	 * public void updateArtifact(TaskData taskData, IProgressMonitor monitor) throws CoreException { // Test
	 * the connection // Try to log in // Send a request with OPTIONS to ensure that we can and have the right
	 * to update the artifact // Compute the change to send // Send the update to the server for the attribute
	 * of the task // See if we need to update additional tasks (cardwall, planning, etc) // Send the update
	 * of the other artifacts // Try to log out }
	 */

	/**
	 * Creates the artifact on the server.
	 * 
	 * @param taskData
	 *            The task data of the artifact to create
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The identifier of the artifact created
	 * @throws CoreException
	 *             In case of error during the creation of the artifact
	 */
	/*
	 * public int createArtifact(TaskData taskData, IProgressMonitor monitor) throws CoreException { // Test
	 * the connection // Try to log in // Send a request with OPTIONS to ensure that we can and have the right
	 * to create the artifact // Create the artifact // See if we need to update additional tasks (cardwall,
	 * planning, etc) // Send the update of the other artifacts // Try to log out return -1; }
	 */

	/**
	 * Returns all the reports of the tracker with the given tracker identifier.
	 * 
	 * @param trackerId
	 *            The identifier of the tracker
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The list of all the reports of the tracker
	 * @throws CoreException
	 *             In case of error during the retrieval of the reports
	 */
	/*
	 * public List<TuleapTrackerReport> getReports(int trackerId, IProgressMonitor monitor) throws
	 * CoreException { // Test the connection // Try to log in // Send a request with OPTIONS to ensure that
	 * we can and have the right to retrieve the reports // Retrieve the reports // Try to log out return
	 * null; }
	 */

	/**
	 * Runs the report on the server for the tracker with the given tracker identifier.
	 * 
	 * @param trackerId
	 *            The identifier of the tracker
	 * @param reportId
	 *            The identifier of the report
	 * @param collector
	 *            The task data collector
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The number of artifact retrieved
	 * @throws CoreException
	 *             In case of error during the report execution
	 */
	/*
	 * public int executeReport(int trackerId, int reportId, TaskDataCollector collector, IProgressMonitor
	 * monitor) throws CoreException { // Test the connection // Try to log in // Send a request with OPTIONS
	 * to ensure that we can and have the right to run a report // Run the report // Create the task data from
	 * the result // Put them in the task data collector // Try to log out return -1; }
	 */

	/**
	 * Runs the query on the server for the tracker with the given tracker identifier.
	 * 
	 * @param trackerId
	 *            The identifier of the tracker
	 * @param criteras
	 *            The criteras of the query
	 * @param collector
	 *            The task data collector
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The number of artifact retrieved
	 * @throws CoreException
	 *             In case of error during the query execution
	 */
	/*
	 * public int executeQuery(int trackerId, Map<String, String> criteras, TaskDataCollector collector,
	 * IProgressMonitor monitor) throws CoreException { // Test the connection // Try to log in // Send a
	 * request with OPTIONS to ensure that we can and have the right to run a query // Run the report //
	 * Create the task data from the result // Put them in the task data collector // Try to log out return
	 * -1; }
	 */

	/**
	 * Retrieve the content of the attachment with the given attachment identifier.
	 * 
	 * @param attachmentId
	 *            The identifier of the attachment
	 * @param monitor
	 *            Used to monitor the progress
	 * @return The content of the attachment
	 * @throws CoreException
	 *             In case of error during the attachment content retrieval
	 */
	public byte[] getAttachmentContent(int attachmentId, IProgressMonitor monitor) throws CoreException {
		// Test the connection

		// Try to log in

		// Send a request with OPTIONS to ensure that we can and have the right to retrieve the file

		// Retrieve the details of the file from the OPTIONS request (size)

		// Download a chunk of the file until completion

		// Assemble the whole file in an array of byte

		// Try to log out

		return null;
	}

	/**
	 * Uploads an attachment to the server for the given artifact.
	 * 
	 * @param artifactId
	 *            The identifier of the artifact
	 * @param attachmentFieldId
	 *            The identifier of the file field in the artifact
	 * @param tuleapAttachmentDescriptor
	 *            The descriptor of the attachment
	 * @param comment
	 *            The comment
	 * @param monitor
	 *            Used to monitor the progress
	 * @throws CoreException
	 *             In case of error during the attachment upload
	 */
	public void uploadAttachment(int artifactId, int attachmentFieldId,
			TuleapAttachmentDescriptor tuleapAttachmentDescriptor, String comment, IProgressMonitor monitor)
			throws CoreException {
		// Test the connection

		// Try to log in

		// Send a request with OPTIONS to ensure that we can and have the right to upload a file

		// Send the first chunk of data to create a temporary file

		// Retrieve the upload id as a response

		// Send the remaining chunk using the upload id

		// Send a request with POST in order to complete the upload of the temporary file

		// Send a request with OPTIONS to ensure that we can and have the right to update the artifact

		// Update the artifact with a new attachment

		// Try to log out
	}

	/**
	 * Retrieves a milestone from its id.
	 * 
	 * @param milestoneId
	 *            milestone id
	 * @param monitor
	 *            progress monitor to use
	 * @return a milestone
	 * @throws CoreException
	 *             If anything goes wrong.
	 */
	public TuleapMilestone getMilestone(int milestoneId, IProgressMonitor monitor) throws CoreException {
		RestResources restResources = tuleapRestConnector.resources(credentials);
		RestMilestones restMilestones = restResources.milestone(milestoneId);
		restMilestones.checkGet(Collections.<String, String> emptyMap());

		ServerResponse milestoneResponse = restMilestones.get(Collections.<String, String> emptyMap());
		JsonParser parser = new JsonParser();
		String jsonMilestoneId = milestoneResponse.getBody();
		JsonElement milestoneElement = parser.parse(jsonMilestoneId);
		TuleapMilestone milestone = new TuleapMilestoneDeserializer().deserialize(milestoneElement,
				TuleapMilestone.class, null);

		return milestone;
	}

	/**
	 * Retrieves the list of the project's BacklogItem Types from the server with the given project id.
	 * 
	 * @param projectId
	 *            the project identifier
	 * @param monitor
	 *            Used to monitor the progress
	 * @return the list of the project's BacklogItem Types
	 * @throws CoreException
	 *             In case of error during the retrieval of the BacklogItem Types
	 */
	public List<TuleapBacklogItemType> getBacklogitemTypes(int projectId, IProgressMonitor monitor)
			throws CoreException {
		// Test the connection
		RestResources restResources = tuleapRestConnector.resources(credentials);

		// Send a request with OPTIONS to ensure that we can and have the right to retrieve the
		// backlogItemType

		RestProjectsBacklogItemTypes restProjectsBacklogItemTypes = restResources
				.projectsBacklogItemTypes(projectId);
		restProjectsBacklogItemTypes.checkGet(Collections.<String, String> emptyMap());

		ServerResponse response = restProjectsBacklogItemTypes.get(Collections.<String, String> emptyMap());

		if (ITuleapServerStatus.OK != response.getStatus()) {
			// Invalid login? server error?
			String message = this.jsonParser.getErrorMessage(response.getBody());
			throw new CoreException(new Status(IStatus.ERROR, TuleapCoreActivator.PLUGIN_ID, message));
		}

		// Analyze the server response
		String jsonResponse = response.getBody();
		JsonParser theJsonParser = new JsonParser();
		JsonArray jsonArray = theJsonParser.parse(jsonResponse).getAsJsonArray();

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(TuleapBacklogItemType.class, new TuleapBacklogItemTypeDeserializer());

		List<TuleapBacklogItemType> result = Lists.newArrayList();
		for (JsonElement jsonElement : jsonArray) {
			Gson gson = gsonBuilder.create();
			TuleapBacklogItemType tuleapBacklogItemType = gson.fromJson(jsonElement,
					TuleapBacklogItemType.class);

			result.add(tuleapBacklogItemType);
		}
		return result;
	}

	/**
	 * Retrieves the list of the project's Milestone Types from the server with the given project id.
	 * 
	 * @param projectId
	 *            the project identifier
	 * @param monitor
	 *            Used to monitor the progress
	 * @return the list of the project's Milestone Types
	 * @throws CoreException
	 *             In case of error during the retrieval of the Milestone Types
	 */
	public List<TuleapMilestoneType> getMilestoneTypes(int projectId, IProgressMonitor monitor)
			throws CoreException {
		// Test the connection
		RestResources restResources = tuleapRestConnector.resources(credentials);

		// Send a request with OPTIONS to ensure that we can and have the right to retrieve the
		// backlogItemType

		RestProjectsMilestoneTypes restProjectsMilestoneTypes = restResources
				.projectsMilestoneTypes(projectId);
		restProjectsMilestoneTypes.checkGet(Collections.<String, String> emptyMap());

		ServerResponse response = restProjectsMilestoneTypes.get(Collections.<String, String> emptyMap());

		if (ITuleapServerStatus.OK != response.getStatus()) {
			// Invalid login? server error?
			String message = this.jsonParser.getErrorMessage(response.getBody());
			throw new CoreException(new Status(IStatus.ERROR, TuleapCoreActivator.PLUGIN_ID, message));
		}

		// Analyze the server response
		String jsonResponse = response.getBody();
		JsonParser theJsonParser = new JsonParser();
		JsonArray jsonArray = theJsonParser.parse(jsonResponse).getAsJsonArray();

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(TuleapMilestoneType.class, new TuleapMilestoneTypeDeserializer());

		List<TuleapMilestoneType> result = Lists.newArrayList();
		for (JsonElement jsonElement : jsonArray) {
			Gson gson = gsonBuilder.create();
			TuleapMilestoneType tuleapMilestoneType = gson.fromJson(jsonElement, TuleapMilestoneType.class);

			result.add(tuleapMilestoneType);
		}
		return result;
	}

	/**
	 * Retrieve the top planning(s) of a given project.
	 * 
	 * @param projectId
	 *            The project id
	 * @param monitor
	 *            The monitor to use
	 * @return A list of the top plannings of the project.
	 * @throws CoreException
	 *             If anything goes wrong.
	 */
	public List<TuleapTopPlanning> getTopPlannings(int projectId, IProgressMonitor monitor)
			throws CoreException {
		List<TuleapTopPlanning> result = Lists.newArrayList();
		RestResources restResources = tuleapRestConnector.resources(credentials);

		// 1- Retrieve the list of top planning ids
		RestProjectsTopPlannings projectTopPlannings = restResources.projectsTopPlannings(projectId);
		projectTopPlannings.checkGet(Collections.<String, String> emptyMap());
		ServerResponse topPlanningsResponse = projectTopPlannings
				.get(Collections.<String, String> emptyMap());
		// Contains a JSON array of integers
		JsonParser parser = new JsonParser();
		String jsonTopPlanningIds = topPlanningsResponse.getBody();
		JsonArray topPlanningIds = parser.parse(jsonTopPlanningIds).getAsJsonArray();
		for (JsonElement element : topPlanningIds) {
			int topPlanningId = element.getAsInt();
			TuleapTopPlanning topPlanning = new TuleapTopPlanning(topPlanningId);

			// 2- Retrieve the milestones of this top planning
			RestTopPlanningsMilestones restMilestones = restResources.topPlanningsMilestones(topPlanningId);
			restMilestones.checkGet(Collections.<String, String> emptyMap());
			ServerResponse milestonesResponse = restMilestones.get(Collections.<String, String> emptyMap());
			// TODO Pagination
			String jsonMilestones = milestonesResponse.getBody();
			// Contains a JSON array of milestones
			JsonArray milestonesArray = parser.parse(jsonMilestones).getAsJsonArray();
			for (JsonElement milestoneElement : milestonesArray) {
				TuleapMilestone milestone = new TuleapMilestoneDeserializer().deserialize(milestoneElement,
						TuleapMilestone.class, null);
				topPlanning.addMilestone(milestone);
			}

			// 3- Retrieve the backlog items of this top planning
			RestTopPlanningsBacklogItems restBacklogItems = restResources
					.topPlanningsBacklogItems(topPlanningId);
			restBacklogItems.checkGet(Collections.<String, String> emptyMap());
			ServerResponse backlogItemsResponse = restBacklogItems.get(Collections
					.<String, String> emptyMap());
			// TODO Pagination
			String jsonBacklogItems = backlogItemsResponse.getBody();
			// Contains a JSON array of backlog items
			JsonArray backlogItemsArray = parser.parse(jsonBacklogItems).getAsJsonArray();
			for (JsonElement backlogItemElement : backlogItemsArray) {
				TuleapBacklogItem backlogItem = new TuleapBacklogItemDeserializer().deserialize(
						backlogItemElement, TuleapBacklogItem.class, null);
				topPlanning.addBacklogItem(backlogItem);
			}
			result.add(topPlanning);
		}

		return result;
	}

	/**
	 * Retrieve the backlog items of a given milestone.
	 * 
	 * @param milestoneId
	 *            The milestone id
	 * @param monitor
	 *            The monitor to use
	 * @return A list of backlog items of the milestone.
	 * @throws CoreException
	 *             If anything goes wrong.
	 */
	public List<TuleapBacklogItem> getBacklogItems(int milestoneId, IProgressMonitor monitor)
			throws CoreException {
		List<TuleapBacklogItem> result = Lists.newArrayList();
		RestResources restResources = tuleapRestConnector.resources(credentials);

		// 1- Retrieve the list of backlog items ids
		RestMilestonesBacklogItems milestoneBacklogItems = restResources.milestonesBacklogItems(milestoneId);
		milestoneBacklogItems.checkGet(Collections.<String, String> emptyMap());
		ServerResponse backlogItemsResponse = milestoneBacklogItems.get(Collections
				.<String, String> emptyMap());
		// Contains a JSON array of integers
		JsonParser parser = new JsonParser();
		String jsonBacklogItemIds = backlogItemsResponse.getBody();
		JsonArray backlogItemIds = parser.parse(jsonBacklogItemIds).getAsJsonArray();
		for (JsonElement backlogItemId : backlogItemIds) {
			TuleapBacklogItemDeserializer deserializer = new TuleapBacklogItemDeserializer();
			TuleapBacklogItem backlogItem = deserializer.deserialize(backlogItemId, TuleapBacklogItem.class,
					null);
			result.add(backlogItem);
		}
		return result;
	}

	/**
	 * Retrieve the backlog items of a given milestone.
	 * 
	 * @param milestoneId
	 *            The milestone id
	 * @param monitor
	 *            The monitor to use
	 * @return A list of backlog items of the milestone.
	 * @throws CoreException
	 *             If anything goes wrong.
	 */
	public List<TuleapMilestone> getSubMilestones(int milestoneId, IProgressMonitor monitor)
			throws CoreException {
		List<TuleapMilestone> result = Lists.newArrayList();
		RestResources restResources = tuleapRestConnector.resources(credentials);

		// 1- Retrieve the list of milestone ids
		RestMilestonesSubmilestones milestoneSubmilestones = restResources
				.milestonesSubmilestones(milestoneId);
		milestoneSubmilestones.checkGet(Collections.<String, String> emptyMap());
		ServerResponse milestonesResponse = milestoneSubmilestones.get(Collections
				.<String, String> emptyMap());
		// Contains a JSON array of integers
		JsonParser parser = new JsonParser();
		String jsonMilestoneIds = milestonesResponse.getBody();
		JsonArray milestoneIds = parser.parse(jsonMilestoneIds).getAsJsonArray();
		for (JsonElement currentMilestoneId : milestoneIds) {
			TuleapMilestoneDeserializer deserializer = new TuleapMilestoneDeserializer();
			TuleapMilestone milestone = deserializer.deserialize(currentMilestoneId, TuleapMilestone.class,
					null);
			result.add(milestone);
		}
		return result;
	}

}
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
package org.eclipse.mylyn.tuleap.core.tests.internal.repository;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tuleap.core.internal.client.ITuleapQueryConstants;
import org.eclipse.mylyn.tuleap.core.internal.client.TuleapClientManager;
import org.eclipse.mylyn.tuleap.core.internal.client.rest.RestResourceFactory;
import org.eclipse.mylyn.tuleap.core.internal.client.rest.TuleapRestClient;
import org.eclipse.mylyn.tuleap.core.internal.client.rest.TuleapRestConnector;
import org.eclipse.mylyn.tuleap.core.internal.data.TuleapArtifactMapper;
import org.eclipse.mylyn.tuleap.core.internal.data.TuleapTaskId;
import org.eclipse.mylyn.tuleap.core.internal.model.config.TuleapProject;
import org.eclipse.mylyn.tuleap.core.internal.model.config.TuleapServer;
import org.eclipse.mylyn.tuleap.core.internal.model.config.TuleapTracker;
import org.eclipse.mylyn.tuleap.core.internal.model.config.field.TuleapSelectBox;
import org.eclipse.mylyn.tuleap.core.internal.model.config.field.TuleapSelectBoxItem;
import org.eclipse.mylyn.tuleap.core.internal.model.data.ArtifactReference;
import org.eclipse.mylyn.tuleap.core.internal.model.data.TuleapArtifact;
import org.eclipse.mylyn.tuleap.core.internal.model.data.TuleapElementComment;
import org.eclipse.mylyn.tuleap.core.internal.model.data.TuleapReference;
import org.eclipse.mylyn.tuleap.core.internal.model.data.agile.TuleapBacklogItem;
import org.eclipse.mylyn.tuleap.core.internal.model.data.agile.TuleapMilestone;
import org.eclipse.mylyn.tuleap.core.internal.model.data.agile.TuleapStatus;
import org.eclipse.mylyn.tuleap.core.internal.parser.TuleapGsonProvider;
import org.eclipse.mylyn.tuleap.core.internal.repository.TuleapRepositoryConnector;
import org.eclipse.mylyn.tuleap.core.internal.repository.TuleapTaskDataCollector;
import org.eclipse.mylyn.tuleap.core.internal.util.ITuleapConstants;
import org.eclipse.mylyn.tuleap.core.tests.internal.TestLogger;
import org.junit.Before;
import org.junit.Test;
import org.tuleap.mylyn.task.agile.core.data.planning.MilestonePlanningWrapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the {@link TuleapRepositoryConnector}.
 *
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 0.7
 */
public class TuleapRepositoryConnectorTests {

	private Gson gson;

	private AbstractWebLocation location = new AbstractWebLocation("") {
		@Override
		public Proxy getProxyForHost(String host, String proxyType) {
			return null;
		}

		@Override
		public AuthenticationCredentials getCredentials(AuthenticationType type) {
			return null;
		}
	};

	/**
	 * Test the retrieval of the repository url from a given task url.
	 */
	@Test
	public void testGetRepositoryUrlFromTaskUrl() {
		final String taskUrl = "https://tuleap.net/plugins/tracker/?group_id=409&tracker=55&aid=453"; //$NON-NLS-1$

		TuleapRepositoryConnector connector = new TuleapRepositoryConnector();
		String repositoryUrl = connector.getRepositoryUrlFromTaskUrl(taskUrl);
		assertEquals("https://tuleap.net", repositoryUrl); //$NON-NLS-1$
	}

	/**
	 * Test the retrieval of the task id from the url of the task.
	 */
	@Test
	public void testGetTaskIdFromTaskUrl() {
		final String taskUrl = "https://demo.tuleap.net/plugins/tracker/?group_id=409&tracker=55&aid=217"; //$NON-NLS-1$

		TuleapRepositoryConnector connector = new TuleapRepositoryConnector();
		String taskId = connector.getTaskIdFromTaskUrl(taskUrl);
		assertEquals(TuleapTaskId.forArtifact(409, 55, 217).toString(), taskId);
	}

	/**
	 * Test the creation of the task url from the repository url and the task id.
	 */
	@Test
	public void testGetTaskUrl() {
		final String repositoryUrl = "https://tuleap.net"; //$NON-NLS-1$
		final String taskId = TuleapTaskId.forArtifact(409, 31, 821).toString();

		TuleapRepositoryConnector connector = new TuleapRepositoryConnector();
		String taskUrl = connector.getTaskUrl(repositoryUrl, taskId);
		assertEquals("https://tuleap.net/plugins/tracker/?group_id=409&tracker=31&aid=821", taskUrl); //$NON-NLS-1$
	}

	/**
	 * Test if the task has changed.
	 */
	@Test
	public void testHasTaskChanged() {
		TuleapRepositoryConnector tuleapRepositoryConnector = new TuleapRepositoryConnector();

		Date dateModified = new Date();

		ITask task = new LocalTask("https://tuleap.net", "123:456#789");
		task.setModificationDate(dateModified);

		TaskData taskData = new TaskData(new TaskAttributeMapper(new TaskRepository(
				ITuleapConstants.CONNECTOR_KIND, "https://tuleap.net")), ITuleapConstants.CONNECTOR_KIND,
				"https://tuleap.net", "123:456#789");

		TaskMapper mapper = new TaskMapper(taskData, true);
		mapper.setModificationDate(dateModified);

		boolean hasTaskChanged = tuleapRepositoryConnector.hasTaskChanged(null, task, taskData);
		assertThat(hasTaskChanged, is(false));

		TaskData taskData2 = new TaskData(new TaskAttributeMapper(new TaskRepository(
				ITuleapConstants.CONNECTOR_KIND, "https://tuleap.net")), ITuleapConstants.CONNECTOR_KIND,
				"https://tuleap.net", "123:456#789");

		mapper = new TaskMapper(taskData, true);
		mapper.setModificationDate(new Date());

		hasTaskChanged = tuleapRepositoryConnector.hasTaskChanged(null, task, taskData2);
		assertThat(hasTaskChanged, is(true));
	}

	/**
	 * Test the execution of a query to retrieve all the artifacts from a given tracker.
	 */
	@Test
	public void testPerformQueryReport() {
		TuleapReference projectRef = new TuleapReference(979, "projects/979");
		TuleapReference trackerRef = new TuleapReference(42, "trackers/42");
		int artifactId = 121;

		final TuleapServer tuleapServer = new TuleapServer("https://tuleap.net");

		TuleapProject tuleapProject = new TuleapProject("", projectRef.getId());
		TuleapTracker trackerConfiguration = new TuleapTracker(trackerRef.getId(), null, null, null, null,
				new Date());
		tuleapProject.addTracker(trackerConfiguration);

		tuleapServer.addProject(tuleapProject);

		final TuleapArtifact tuleapArtifact = new TuleapArtifact(artifactId, projectRef, "", "", "",
				new Date(), new Date());
		tuleapArtifact.setTracker(trackerRef);

		final TuleapClientManager tuleapClientManager = new TuleapClientManager() {
			@Override
			public TuleapRestClient getRestClient(TaskRepository taskRepository) {
				return new TuleapRestClient(new RestResourceFactory("v3.14", new TuleapRestConnector(
						location, new TestLogger()), gson, new TestLogger()), gson, taskRepository) {
					@Override
					public List<TuleapArtifact> getTrackerReportArtifacts(int trackerReportId,
							IProgressMonitor monitor) {
						return Lists.newArrayList(tuleapArtifact);
					}
				};
			}
		};

		TuleapRepositoryConnector tuleapRepositoryConnector = new TuleapRepositoryConnector() {
			@Override
			public TuleapServer getServer(TaskRepository repo) {
				return tuleapServer;
			}

			@Override
			public TuleapTracker refreshTracker(TaskRepository taskRepository, TuleapTracker tracker,
					IProgressMonitor monitor) throws CoreException {
				return tracker;
			}

			@Override
			public TuleapClientManager getClientManager() {
				return tuleapClientManager;
			}
		};

		TaskRepository taskRepository = new TaskRepository(ITuleapConstants.CONNECTOR_KIND,
				"https://tuleap.net");

		// All from tracker
		TuleapTaskDataCollector collector = new TuleapTaskDataCollector();

		IRepositoryQuery query = new RepositoryQuery(ITuleapConstants.CONNECTOR_KIND, "");
		query.setAttribute(ITuleapQueryConstants.QUERY_KIND, ITuleapQueryConstants.QUERY_KIND_REPORT);
		query.setAttribute(ITuleapQueryConstants.QUERY_TRACKER_ID, String.valueOf(trackerRef.getId()));
		query.setAttribute(ITuleapQueryConstants.QUERY_REPORT_ID, "100");

		tuleapRepositoryConnector.performQuery(taskRepository, query, collector, null,
				new NullProgressMonitor());

		assertThat(collector.getTaskData().size(), is(1));
		assertThat(collector.getTaskData().iterator().next().getTaskId(), is(TuleapTaskId.forArtifact(
				projectRef.getId(), trackerRef.getId(), artifactId).toString()));
	}

	/**
	 * Test the execution of a query to retrieve all the artifacts from a given tracker.
	 */
	@Test
	public void testPerformQueryCustom() {
		TuleapReference projectRef = new TuleapReference(979, "projects/979");
		TuleapReference trackerRef = new TuleapReference(42, "trackers/42");
		int artifactId = 121;

		final TuleapServer tuleapServer = new TuleapServer("https://tuleap.net");

		TuleapProject tuleapProject = new TuleapProject("", projectRef.getId());
		TuleapTracker trackerConfiguration = new TuleapTracker(trackerRef.getId(), null, null, null, null,
				new Date());
		tuleapProject.addTracker(trackerConfiguration);

		tuleapServer.addProject(tuleapProject);

		final TuleapArtifact tuleapArtifact = new TuleapArtifact(artifactId, projectRef, "", "", "",
				new Date(), new Date());
		tuleapArtifact.setTracker(trackerRef);

		final TuleapClientManager tuleapClientManager = new TuleapClientManager() {
			@Override
			public TuleapRestClient getRestClient(TaskRepository taskRepository) {
				return new TuleapRestClient(null, null, null) {

					@Override
					public List<TuleapArtifact> getArtifactsFromQuery(IRepositoryQuery query,
							TuleapTracker tracker, IProgressMonitor monitor) throws CoreException {
						return Arrays.asList(tuleapArtifact);
					}

					@Override
					public List<TuleapElementComment> getArtifactComments(int artId, TuleapServer server,
							IProgressMonitor monitor) throws CoreException {
						return Collections.emptyList();
					}
				};
			}
		};

		TuleapRepositoryConnector tuleapRepositoryConnector = new TuleapRepositoryConnector() {
			@Override
			public TuleapServer getServer(TaskRepository repo) {
				return tuleapServer;
			}

			@Override
			public TuleapTracker refreshTracker(TaskRepository taskRepository, TuleapTracker tracker,
					IProgressMonitor monitor) throws CoreException {
				return tracker;
			}

			@Override
			public TuleapClientManager getClientManager() {
				return tuleapClientManager;
			}
		};

		TaskRepository taskRepository = new TaskRepository(ITuleapConstants.CONNECTOR_KIND,
				"https://tuleap.net");

		// All from tracker
		TuleapTaskDataCollector collector = new TuleapTaskDataCollector();

		IRepositoryQuery query = new RepositoryQuery(ITuleapConstants.CONNECTOR_KIND, "");
		query.setAttribute(ITuleapQueryConstants.QUERY_KIND, ITuleapQueryConstants.QUERY_KIND_CUSTOM);
		query.setAttribute(ITuleapQueryConstants.QUERY_TRACKER_ID, String.valueOf(trackerRef.getId()));

		tuleapRepositoryConnector.performQuery(taskRepository, query, collector, null,
				new NullProgressMonitor());

		assertThat(collector.getTaskData().size(), is(1));
		assertThat(collector.getTaskData().iterator().next().getTaskId(), is(TuleapTaskId.forArtifact(
				projectRef.getId(), trackerRef.getId(), artifactId).toString()));
	}

	/**
	 * Test the execution of a query to retrieve all the artifacts from a given tracker.
	 */
	@Test
	public void testPerformQueryTopLevelPlanning() {
		final TuleapReference projectRef = new TuleapReference(979, "projects/979");

		final TuleapServer tuleapServer = new TuleapServer("https://tuleap.net");

		TuleapProject tuleapProject = new TuleapProject("", projectRef.getId());
		tuleapServer.addProject(tuleapProject);

		// MockListRestConnector connector = new MockListRestConnector();

		final TaskRepository taskRepository = new TaskRepository(ITuleapConstants.CONNECTOR_KIND,
				"https://tuleap.net");

		// final RestResourceFactory resourceFactory = new RestResourceFactory("", "", connector);

		final TuleapClientManager tuleapClientManager = new TuleapClientManager() {
			@Override
			public TuleapRestClient getRestClient(TaskRepository repository) {
				return new TuleapRestClient(null, null, null) {
					@Override
					public List<TuleapBacklogItem> getProjectBacklog(int projectId, IProgressMonitor monitor)
							throws CoreException {
						if (projectId == projectRef.getId()) {
							TuleapBacklogItem item = new TuleapBacklogItem();
							item.setId(123);
							item.setArtifact(new ArtifactReference(123, "artifacts/123", new TuleapReference(
									12, "trackers/12")));
							item.setLabel("Backlog item");
							item.setProject(projectRef);
							item.setStatus(TuleapStatus.valueOf("Closed"));
							return Collections.singletonList(item);
						}
						return Collections.emptyList();
					}

					@Override
					public List<TuleapMilestone> getProjectMilestones(int projectId, IProgressMonitor monitor)
							throws CoreException {
						if (projectId == projectRef.getId()) {
							TuleapMilestone milestone = new TuleapMilestone(100, projectRef);
							milestone.setLabel("Milestone");
							return Collections.singletonList(milestone);
						}
						return Collections.emptyList();
					}

					@Override
					public List<TuleapBacklogItem> getMilestoneContent(int milestoneId,
							IProgressMonitor monitor) throws CoreException {
						if (milestoneId == 100) {
							TuleapBacklogItem item = new TuleapBacklogItem();
							item.setId(333);
							item.setArtifact(new ArtifactReference(123, "artifacts/333", new TuleapReference(
									12, "trackers/12")));
							item.setLabel("Backlog item");
							item.setProject(projectRef);
							item.setStatus(TuleapStatus.valueOf("Closed"));
							return Collections.singletonList(item);
						}
						return Collections.emptyList();
					}
				};
			}
		};

		TuleapRepositoryConnector tuleapRepositoryConnector = new TuleapRepositoryConnector() {
			@Override
			public TuleapServer getServer(TaskRepository repo) {
				return tuleapServer;
			}

			@Override
			public TuleapClientManager getClientManager() {
				return tuleapClientManager;
			}
		};

		// All from tracker
		TuleapTaskDataCollector collector = new TuleapTaskDataCollector();

		IRepositoryQuery query = new RepositoryQuery(ITuleapConstants.CONNECTOR_KIND, "");
		query.setAttribute(ITuleapQueryConstants.QUERY_KIND,
				ITuleapQueryConstants.QUERY_KIND_TOP_LEVEL_PLANNING);
		query.setAttribute(ITuleapQueryConstants.QUERY_PROJECT_ID, String.valueOf(projectRef.getId()));

		tuleapRepositoryConnector.performQuery(taskRepository, query, collector, null,
				new NullProgressMonitor());

		assertThat(collector.getTaskData().size(), is(1));
		TaskData taskData = collector.getTaskData().iterator().next();
		assertThat(collector.getTaskData().iterator().next().getTaskId(), is(TuleapTaskId.forArtifact(
				projectRef.getId(), TuleapTaskId.IRRELEVANT_ID, projectRef.getId()).toString()));
		MilestonePlanningWrapper wrapper = new MilestonePlanningWrapper(taskData.getRoot());
		assertEquals(2, wrapper.getAllBacklogItems().size());
		assertEquals(1, wrapper.getSubMilestones().size());
	}

	/**
	 * Test the update of the task from the given task data.
	 */
	@Test
	public void testUpdateTaskFromTaskData() {
		TaskRepository taskRepository = new TaskRepository(ITuleapConstants.CONNECTOR_KIND,
				"https://tuleap.net");

		int projectId = 789;
		int configurationId = 456;
		int elementId = 123;
		String taskId = TuleapTaskId.forArtifact(projectId, configurationId, elementId).toString();

		ITask task = new LocalTask(taskId, "");

		TaskData taskData = new TaskData(new TaskAttributeMapper(taskRepository),
				ITuleapConstants.CONNECTOR_KIND, "https://tuleap.net", taskId);

		int openStatusId = 0;
		int closedStatusId = 1;

		final TuleapServer tuleapServer = new TuleapServer("https://tuleap.net");
		TuleapProject tuleapProject = new TuleapProject(null, projectId);

		TuleapTracker trackerConfiguration = new TuleapTracker(configurationId, null, null, null, null,
				new Date());

		TuleapSelectBox tuleapSelectBox = new TuleapSelectBox(0);
		TuleapSelectBoxItem item = new TuleapSelectBoxItem(0);
		item.setLabel("OPEN");
		tuleapSelectBox.addItem(item);
		tuleapSelectBox.getOpenStatus().add(item);

		item = new TuleapSelectBoxItem(1);
		item.setLabel("CLOSED");
		tuleapSelectBox.addItem(item);

		trackerConfiguration.addField(tuleapSelectBox);

		tuleapProject.addTracker(trackerConfiguration);
		tuleapServer.addProject(tuleapProject);

		TuleapArtifactMapper mapper = new TuleapArtifactMapper(taskData, trackerConfiguration);
		mapper.initializeEmptyTaskData();
		mapper.setStatus(openStatusId);

		TuleapRepositoryConnector tuleapRepositoryConnector = new TuleapRepositoryConnector() {
			@Override
			public TuleapServer getServer(TaskRepository repo) {
				return tuleapServer;
			}
		};
		tuleapRepositoryConnector.updateTaskFromTaskData(taskRepository, task, taskData);
		assertThat(task.isCompleted(), is(false));
		assertThat(task.getCompletionDate(), nullValue());

		mapper.setModificationDate(new Date());
		mapper.setStatus(closedStatusId);

		tuleapRepositoryConnector.updateTaskFromTaskData(taskRepository, task, taskData);
		assertThat(task.isCompleted(), is(true));
		assertThat(task.getCompletionDate(), notNullValue());
	}

	@Before
	public void setUp() {
		gson = TuleapGsonProvider.defaultGson();
	}
}

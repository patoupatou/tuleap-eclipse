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
package org.eclipse.mylyn.internal.tuleap.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.internal.tuleap.core.client.ITuleapClient;
import org.eclipse.mylyn.internal.tuleap.core.client.ITuleapClientManager;
import org.eclipse.mylyn.internal.tuleap.core.repository.ITuleapRepositoryConnector;
import org.eclipse.mylyn.internal.tuleap.ui.util.TuleapMylynTasksUIMessages;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * This page will be used when a new task is created in order to let the user select the tracker for which the
 * task will be created.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapTrackerPage extends WizardPage {

	/**
	 * The hinted height of the viewer containing the list of the tracker.
	 */
	private static final int HEIGHT_HINT = 250;

	/**
	 * The task repository.
	 */
	private TaskRepository repository;

	/**
	 * The widget where the available trackers will be displayed.
	 */
	private FilteredTree trackerTree;

	/**
	 * The constructor.
	 * 
	 * @param taskRepository
	 *            The task repository
	 */
	public TuleapTrackerPage(TaskRepository taskRepository) {
		super(TuleapMylynTasksUIMessages.getString("TuleapTrackerPage.PageName")); //$NON-NLS-1$
		this.setTitle(TuleapMylynTasksUIMessages.getString("TuleapTrackerPage.PageTitle")); //$NON-NLS-1$
		this.setDescription(TuleapMylynTasksUIMessages.getString("TuleapTrackerPage.PageDescription")); //$NON-NLS-1$
		this.repository = taskRepository;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());

		trackerTree = new FilteredTree(composite, SWT.SINGLE | SWT.BORDER, new PatternFilter(), true);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.heightHint = HEIGHT_HINT;
		gridData.widthHint = SWT.DEFAULT;

		trackerTree.setLayoutData(gridData);
		TreeViewer viewer = trackerTree.getViewer();
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(new TuleapTrackerContentProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				String trackerSelected = TuleapTrackerPage.this.getTrackerSelected();
				if (trackerSelected == null) {
					TuleapTrackerPage.this.setErrorMessage(TuleapMylynTasksUIMessages
							.getString("TuleapTrackerPage.SelectATracker")); //$NON-NLS-1$
				} else {
					TuleapTrackerPage.this.setErrorMessage(null);
					TuleapTrackerPage.this.setMessage(null);
				}
				IWizard wizard = TuleapTrackerPage.this.getWizard();
				wizard.getContainer().updateButtons();
			}
		});

		Button updateButton = new Button(composite, SWT.LEFT | SWT.PUSH);
		updateButton.setText(TuleapMylynTasksUIMessages.getString("TuleapTrackerPage.UpdateTrackersList")); //$NON-NLS-1$
		updateButton.setLayoutData(new GridData());
		updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TuleapTrackerPage.this.updateTrackersList(true);
			}
		});

		Dialog.applyDialogFont(composite);

		setControl(composite);
	}

	/**
	 * Update the list of the tracker that should be displayed in the widget.
	 * 
	 * @param forceRefresh
	 *            Indicates if we should force the refresh of the list of the tracker.
	 */
	protected void updateTrackersList(final boolean forceRefresh) {
		String connectorKind = this.repository.getConnectorKind();
		AbstractRepositoryConnector repositoryConnector = TasksUi.getRepositoryManager()
				.getRepositoryConnector(connectorKind);
		if (repositoryConnector instanceof ITuleapRepositoryConnector) {
			ITuleapRepositoryConnector connector = (ITuleapRepositoryConnector)repositoryConnector;
			ITuleapClientManager clientManager = connector.getClientManager();
			final ITuleapClient client = clientManager.getClient(this.repository);

			IRunnableWithProgress runnable = new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					List<String> trackersList = client.getTrackerList(forceRefresh, monitor);
					TuleapTrackerPage.this.trackerTree.getViewer().setInput(trackersList);
				}
			};

			try {
				this.getContainer().run(false, true, runnable);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				this.getWizard().getContainer().updateButtons();
			}
		}
	}

	/**
	 * Returns the tracker where the new task should be created.
	 * 
	 * @return The tracker where the new task should be created.
	 */
	public String getTrackerSelected() {
		IStructuredSelection selection = (IStructuredSelection)this.trackerTree.getViewer().getSelection();
		return (String)selection.getFirstElement();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		return this.getTrackerSelected() != null;
	}
}
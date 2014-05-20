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
package org.eclipse.mylyn.internal.tuleap.ui.wizards.query;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.internal.tuleap.ui.TuleapTasksUIPlugin;
import org.eclipse.mylyn.internal.tuleap.ui.util.ITuleapUIConstants;
import org.eclipse.mylyn.internal.tuleap.ui.util.TuleapMylynTasksUIMessages;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * The first page of the Tuleap query wizard.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapQueryPage extends WizardPage {
	/**
	 * The custom query page.
	 */
	private final TuleapCustomQueryPage customQueryPage;

	/**
	 * The default queries page.
	 */
	private final TuleapDefaultQueriesPage defaultQueriesPage;

	/**
	 * The group displaying the queries.
	 */
	private Group queriesGroup;

	/**
	 * The button to search the artifacts of a selected project.
	 */
	private Button defaultQueriesButton;

	/**
	 * The button to select the search with a custom query.
	 */
	private Button customQueryButton;

	/**
	 * the constructor.
	 * 
	 * @param taskRepository
	 *            The Mylyn tasks repository.
	 */
	public TuleapQueryPage(TaskRepository taskRepository) {
		super(TuleapMylynTasksUIMessages.getString("TuleapQueryPage.Name")); //$NON-NLS-1$
		this.setTitle(TuleapMylynTasksUIMessages.getString("TuleapQueryPage.Name")); //$NON-NLS-1$
		this.setDescription(TuleapMylynTasksUIMessages.getString("TuleapQueryPage.Description")); //$NON-NLS-1$
		this.customQueryPage = new TuleapCustomQueryPage(taskRepository);
		this.defaultQueriesPage = new TuleapDefaultQueriesPage(taskRepository);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessVerticalSpace = false;
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout(1, false));

		this.queriesGroup = new Group(composite, SWT.NONE);
		this.queriesGroup.setText(TuleapMylynTasksUIMessages.getString("TuleapQueryPage.QueriesGroup.Name")); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessVerticalSpace = false;
		this.queriesGroup.setLayoutData(gridData);
		this.queriesGroup.setLayout(new GridLayout(1, false));

		Composite groupComposite = new Composite(queriesGroup, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessVerticalSpace = false;
		groupComposite.setLayoutData(gridData);
		groupComposite.setLayout(new GridLayout(1, false));

		defaultQueriesButton = new Button(groupComposite, SWT.RADIO);
		defaultQueriesButton.setText(TuleapMylynTasksUIMessages
				.getString("TuleapQueryPage.ProjectQueryButton.Name")); //$NON-NLS-1$
		defaultQueriesButton.setSelection(true);

		customQueryButton = new Button(groupComposite, SWT.RADIO);
		customQueryButton.setText(TuleapMylynTasksUIMessages
				.getString("TuleapQueryPage.CustomQueryButton.Name")); //$NON-NLS-1$

		setPageComplete(true);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getImage()
	 */
	@Override
	public Image getImage() {
		return TuleapTasksUIPlugin.getDefault().getImage(ITuleapUIConstants.Icons.WIZARD_TULEAP_LOGO_48);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage() {
		if (this.defaultQueriesButton.getSelection()) {
			this.defaultQueriesPage.setWizard(this.getWizard());
			return this.defaultQueriesPage;
		}
		this.customQueryPage.setWizard(this.getWizard());
		return this.customQueryPage;
	}
}

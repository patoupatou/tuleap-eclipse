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
package org.eclipse.mylyn.internal.tuleap.core.model.field;

import org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapField;
import org.eclipse.mylyn.internal.tuleap.core.model.TuleapPermission;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 * The Tuleap string field.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapString extends AbstractTuleapField {

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = -2298403602439489298L;

	/**
	 * The maximum length of the string.
	 */
	private int size;

	/**
	 * The constructor.
	 * 
	 * @param formElementName
	 *            The name of the form element
	 * @param formElementLabel
	 *            The label of the form element
	 * @param formElementDescription
	 *            The description of the form element
	 * @param isRequired
	 *            Indicates if the form element is required
	 * @param formElementPermission
	 *            The permission of the form element
	 * @param formElementIdentifier
	 *            The identifier of the form element
	 */
	public TuleapString(String formElementName, String formElementLabel, String formElementDescription,
			boolean isRequired, TuleapPermission formElementPermission, String formElementIdentifier) {
		super(formElementName, formElementLabel, formElementDescription, isRequired, formElementPermission,
				formElementIdentifier);
	}

	/**
	 * Sets the maximum size of the strings.
	 * 
	 * @param maximumSize
	 *            The maximum size of the strings.
	 */
	public void setSize(int maximumSize) {
		this.size = maximumSize;
	}

	/**
	 * Returns the maximum size of the strings.
	 * 
	 * @return The maximum size of the strings.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapField#getMetadataKind()
	 */
	@Override
	public String getMetadataKind() {
		return TaskAttribute.KIND_DEFAULT;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapField#getMetadataType()
	 */
	@Override
	public String getMetadataType() {
		return TaskAttribute.TYPE_SHORT_RICH_TEXT;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapField#getDefaultValue()
	 */
	@Override
	public Object getDefaultValue() {
		return ""; //$NON-NLS-1$
	}
}

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
package org.tuleap.mylyn.task.core.internal.util;

/**
 * This interface is a container of constants used accross the Mylyn tasks Tuleap connector.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 0.7
 */
public interface ITuleapConstants {
	/**
	 * The kind of Mylyn tasks connector.
	 */
	String CONNECTOR_KIND = "tuleap"; //$NON-NLS-1$

	/**
	 * The identifier of the none binding.
	 */
	int CONFIGURABLE_FIELD_NONE_BINDING_ID = 100;

	/**
	 * The attributes query separator.
	 */
	String QUERY_ATTRIBUTES_SEPARATOR = ","; //$NON-NLS-1$

	/**
	 * The constant used to specify the name of the field to use to upload new files.
	 */
	String ATTACHMENT_FIELD_NAME = "tuleap_attachment_field_name"; //$NON-NLS-1$

	/**
	 * The constant used to specify the label of the field to use to upload new files.
	 */
	String ATTACHMENT_FIELD_LABEL = "tuleap_attachment_field_label"; //$NON-NLS-1$

	/**
	 * The identifier of an anonymous user.
	 */
	int ANONYMOUS_USER_INFO_IDENTIFIER = 0;

	/**
	 * The username of an anonymous user.
	 */
	String ANONYMOUS_USER_INFO_USERNAME = "anonymous"; //$NON-NLS-1$

	/**
	 * The real name of an anonymous user.
	 */
	String ANONYMOUS_USER_INFO_REAL_NAME = "Anonymous User"; //$NON-NLS-1$

	/**
	 * The email address of an anonymous user.
	 */
	String ANONYMOUS_USER_INFO_EMAIL = "anonymous@tuleap.net"; //$NON-NLS-1$

	/**
	 * The LDAP identifier of an anonymous user.
	 */
	String ANONYMOUS_USER_INFO_LDAP_IDENTIFIER = ""; //$NON-NLS-1$

	/**
	 * The key used for the id of the POJO.
	 */
	String ID = "id"; //$NON-NLS-1$

	/**
	 * The key used for the label of the POJO.
	 */
	String LABEL = "label"; //$NON-NLS-1$

	/**
	 * The key used for the URL of the POJO.
	 * 
	 * @deprecated
	 */
	@Deprecated
	String URL = "url"; //$NON-NLS-1$

	/**
	 * The key used for the REST URI of the POJO.
	 */
	String URI = "uri"; //$NON-NLS-1$

	/**
	 * The key used for the HTML URL of the POJO.
	 */
	String HTML_URL = "html_url"; //$NON-NLS-1$

	/**
	 * The key used for the field values of the POJO.
	 */
	String VALUES = "values"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the field identifier of the POJO.
	 */
	String FIELD_ID = "field_id"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the field value of the POJO.
	 */
	String FIELD_VALUE = "value"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the field value of the POJO.
	 */
	String FIELD_LINKS = "links"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the bind value identifier.
	 */
	String FIELD_BIND_VALUE_ID = "bind_value_id"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the list of bind value identifiers.
	 */
	String FIELD_BIND_VALUE_IDS = "bind_value_ids"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the list of file descriptions.
	 */
	String FILE_DESCRIPTIONS = "file_descriptions"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the person that submit the file.
	 */
	String SUBMITTED_BY = "submitted_by"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the date of creation.
	 */
	String SUBMITTED_ON = "submitted_on"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the date of last modification.
	 */
	String LAST_MODIFIED_DATE = "last_modified_date"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file description.
	 */
	String DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file name.
	 */
	String NAME = "name"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file size.
	 */
	String SIZE = "size"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file type.
	 */
	String MIME_TYPE = "mimetype"; //$NON-NLS-1$

	/**
	 * The key used to retrieve a start date.
	 */
	String START_DATE = "start_date"; //$NON-NLS-1$

	/**
	 * The key used to retrieve a duration.
	 */
	String END_DATE = "end_date"; //$NON-NLS-1$

	/**
	 * The key used to retrieve a comment.
	 */
	String COMMENT = "comment"; //$NON-NLS-1$

	/**
	 * The key used to retrieve a body.
	 */

	String BODY = "body"; //$NON-NLS-1$

	/**
	 * The key used for the project ID of an element possessed by a project.
	 */
	String JSON_TRACKER = "tracker"; //$NON-NLS-1$

	/**
	 * The key used for the project ref of an element possessed by a project.
	 */
	String JSON_PROJECT = "project"; //$NON-NLS-1$

	/**
	 * The key used for the artifact ref of an element possessed by a project.
	 */
	String JSON_ARTIFACT = "artifact"; //$NON-NLS-1$

	/**
	 * The key used for a parent.
	 */
	String JSON_PARENT = "parent"; //$NON-NLS-1$

	/**
	 * The key used for a Tuleap status ("Open" or "Closed").
	 */
	String JSON_STATUS = "status"; //$NON-NLS-1$

	/**
	 * The identifier of the Tuleap preference node.
	 */
	String TULEAP_PREFERENCE_NODE = "tuleap_preference_node"; //$NON-NLS-1$

	/**
	 * The identifier of the Tuleap preference for the activation of the debug mode.
	 */
	String TULEAP_PREFERENCE_DEBUG_MODE = "tuleap_preference_debug_mode"; //$NON-NLS-1$

	/**
	 * The key used for the Tuleap tracker reference.
	 */
	String TRACKER = "tracker"; //$NON-NLS-1$

	/**
	 * The key used for the comment format.
	 */
	String FORMAT = "format"; //$NON-NLS-1$

	/**
	 * The key used for the last comment.
	 */
	String LAST_COMMENT = "last_comment"; //$NON-NLS-1$

	/**
	 * The key used for the user email.
	 */
	String EMAIL = "email"; //$NON-NLS-1$

	/**
	 * The key used for the file content.
	 */
	String CONTENT = "content"; //$NON-NLS-1$

	/**
	 * The key used for the file offset.
	 */
	String OFFSET = "offset"; //$NON-NLS-1$

}

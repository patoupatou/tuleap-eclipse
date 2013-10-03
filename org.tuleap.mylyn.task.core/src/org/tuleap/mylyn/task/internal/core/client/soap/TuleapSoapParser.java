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
package org.tuleap.mylyn.task.internal.core.client.soap;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.tuleap.mylyn.task.internal.core.data.AbstractFieldValue;
import org.tuleap.mylyn.task.internal.core.data.AttachmentFieldValue;
import org.tuleap.mylyn.task.internal.core.data.AttachmentValue;
import org.tuleap.mylyn.task.internal.core.data.BoundFieldValue;
import org.tuleap.mylyn.task.internal.core.data.LiteralFieldValue;
import org.tuleap.mylyn.task.internal.core.data.TuleapTaskIdentityUtil;
import org.tuleap.mylyn.task.internal.core.model.AbstractTuleapField;
import org.tuleap.mylyn.task.internal.core.model.TuleapElementComment;
import org.tuleap.mylyn.task.internal.core.model.TuleapPerson;
import org.tuleap.mylyn.task.internal.core.model.TuleapServerConfiguration;
import org.tuleap.mylyn.task.internal.core.model.field.TuleapFileUpload;
import org.tuleap.mylyn.task.internal.core.model.field.TuleapMultiSelectBox;
import org.tuleap.mylyn.task.internal.core.model.field.TuleapSelectBox;
import org.tuleap.mylyn.task.internal.core.model.tracker.TuleapArtifact;
import org.tuleap.mylyn.task.internal.core.model.tracker.TuleapTrackerConfiguration;
import org.tuleap.mylyn.task.internal.core.repository.TuleapUrlUtil;
import org.tuleap.mylyn.task.internal.core.wsdl.soap.v2.Artifact;
import org.tuleap.mylyn.task.internal.core.wsdl.soap.v2.ArtifactFieldValue;
import org.tuleap.mylyn.task.internal.core.wsdl.soap.v2.FieldValueFileInfo;
import org.tuleap.mylyn.task.internal.core.wsdl.soap.v2.TrackerFieldBindValue;

/**
 * Utility class used to transform the the data structure used by the SOAP API in {@link TuleapArtifact}.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 */
public class TuleapSoapParser {

	/**
	 * Parse the SOAP data and return a {@link TuleapArtifact} representing the artifact.
	 * 
	 * @param tuleapTrackerConfiguration
	 *            The configuration of the Tuleap tracker
	 * @param commentedArtifact
	 *            The SOAP artifact to parse + the related comments.
	 * @return The TuleapArtifact representing the artifact
	 */
	public TuleapArtifact parseArtifact(TuleapTrackerConfiguration tuleapTrackerConfiguration,
			CommentedArtifact commentedArtifact) {
		Artifact artifactToParse = commentedArtifact.getArtifact();
		int artifactId = artifactToParse.getArtifact_id();
		int trackerId = artifactToParse.getTracker_id();

		// Useless for regular artifacts (agile only)
		String label = null;
		String url = null;

		String repositoryUrl = tuleapTrackerConfiguration.getTuleapProjectConfiguration()
				.getServerConfiguration().getUrl();

		String taskId = TuleapTaskIdentityUtil.getTaskDataId(tuleapTrackerConfiguration
				.getTuleapProjectConfiguration().getIdentifier(), trackerId, artifactId);
		String htmlUrl = TuleapUrlUtil.getTaskUrlFromTaskId(repositoryUrl, taskId);

		int submittedOn = artifactToParse.getSubmitted_on();
		Date creationDate = this.getDateFromTimestamp(submittedOn);

		int lastUpdateDate = artifactToParse.getLast_update_date();
		Date lastModificationDate = this.getDateFromTimestamp(lastUpdateDate);

		TuleapArtifact tuleapArtifact = new TuleapArtifact(artifactId, trackerId, label, url, htmlUrl,
				creationDate, lastModificationDate);

		ArtifactFieldValue[] value = artifactToParse.getValue();
		for (ArtifactFieldValue artifactFieldValue : value) {
			Collection<AbstractTuleapField> fields = tuleapTrackerConfiguration.getFields();
			for (AbstractTuleapField abstractTuleapField : fields) {
				if (artifactFieldValue.getField_name().equals(abstractTuleapField.getName())) {
					AbstractFieldValue abstractFieldValue = null;
					if (abstractTuleapField instanceof TuleapSelectBox) {
						// Select box?
						int bindValueId = -1;

						TrackerFieldBindValue[] bindValue = artifactFieldValue.getField_value()
								.getBind_value();
						if (bindValue.length > 0) {
							bindValueId = bindValue[0].getBind_value_id();
						}

						abstractFieldValue = new BoundFieldValue(abstractTuleapField.getIdentifier(), Lists
								.newArrayList(Integer.valueOf(bindValueId)));
					} else if (abstractTuleapField instanceof TuleapMultiSelectBox) {
						// Multi-select box?
						List<Integer> bindValueIds = new ArrayList<Integer>();

						TrackerFieldBindValue[] bindValue = artifactFieldValue.getField_value()
								.getBind_value();
						for (TrackerFieldBindValue trackerFieldBindValue : bindValue) {
							bindValueIds.add(Integer.valueOf(trackerFieldBindValue.getBind_value_id()));
						}

						abstractFieldValue = new BoundFieldValue(abstractTuleapField.getIdentifier(),
								bindValueIds);
					} else if (abstractTuleapField instanceof TuleapFileUpload) {
						// File attachment?
						List<AttachmentValue> attachments = new ArrayList<AttachmentValue>();

						FieldValueFileInfo[] fileInfo = artifactFieldValue.getField_value().getFile_info();
						TuleapServerConfiguration serverConfiguration = tuleapTrackerConfiguration
								.getTuleapProjectConfiguration().getServerConfiguration();
						// Yes, this array can be null.
						if (fileInfo != null) {
							for (FieldValueFileInfo fieldValueFileInfo : fileInfo) {
								int submitterId = fieldValueFileInfo.getSubmitted_by();
								TuleapPerson submitter = serverConfiguration.getUser(submitterId);
								attachments.add(new AttachmentValue(fieldValueFileInfo.getId(),
										fieldValueFileInfo.getFilename(), submitter, fieldValueFileInfo
												.getFilesize(), fieldValueFileInfo.getDescription(),
										fieldValueFileInfo.getFiletype()));
							}
						}

						abstractFieldValue = new AttachmentFieldValue(abstractTuleapField.getIdentifier(),
								attachments);
					} else {
						// Literal
						abstractFieldValue = new LiteralFieldValue(abstractTuleapField.getIdentifier(),
								artifactFieldValue.getField_value().getValue());
					}
					tuleapArtifact.addFieldValue(abstractFieldValue);
				}
			}
		}

		for (TuleapElementComment comment : commentedArtifact.getComments()) {
			tuleapArtifact.addComment(comment);
		}

		return tuleapArtifact;
	}

	/**
	 * Creates the date from a timestamp.
	 * 
	 * @param timestamp
	 *            The timestamp
	 * @return The date created from the timestamp
	 */
	private Date getDateFromTimestamp(int timestamp) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(Long.valueOf(timestamp).longValue() * 1000);
		return calendar.getTime();
	}
}

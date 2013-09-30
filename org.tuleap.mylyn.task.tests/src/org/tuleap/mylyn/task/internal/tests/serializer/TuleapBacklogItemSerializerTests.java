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
package org.tuleap.mylyn.task.internal.tests.serializer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tuleap.mylyn.task.internal.core.data.AttachmentFieldValue;
import org.tuleap.mylyn.task.internal.core.data.AttachmentValue;
import org.tuleap.mylyn.task.internal.core.data.BoundFieldValue;
import org.tuleap.mylyn.task.internal.core.data.LiteralFieldValue;
import org.tuleap.mylyn.task.internal.core.model.TuleapPerson;
import org.tuleap.mylyn.task.internal.core.model.agile.TuleapBacklogItem;
import org.tuleap.mylyn.task.internal.core.serializer.TuleapBacklogItemSerializer;

import static org.junit.Assert.assertEquals;

/**
 * Tests the serialization of the Tuleap BacklogItem.
 * 
 * @author <a href="mailto:firas.bacha@obeo.fr">Firas Bacha</a>
 */
public class TuleapBacklogItemSerializerTests {
	/**
	 * the BacklogItem.
	 */
	private static TuleapBacklogItem backlogItem;

	/**
	 * The generated JSON object.
	 */
	private static JsonObject jsonObject;

	/**
	 * The key used for the id of the POJO.
	 */
	private static final String ID = "id"; //$NON-NLS-1$

	/**
	 * The key used for the label of the POJO.
	 */
	private static final String LABEL = "label"; //$NON-NLS-1$

	/**
	 * The key used for the URL of the POJO.
	 */
	private static final String URL = "url"; //$NON-NLS-1$

	/**
	 * The key used for the HTML URL of the POJO.
	 */
	private static final String HTML_URL = "html_url"; //$NON-NLS-1$

	/**
	 * The key used for the field values of the POJO.
	 */
	private static final String VALUES = "values"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the id of a field of the POJO.
	 */
	private static final String FIELD_ID = "field_id"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the value of a field of the POJO.
	 */
	private static final String FIELD_VALUE = "value"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the list of bind value ids.
	 */
	private static final String FIELD_BIND_VALUE_IDS = "bind_value_ids"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the type id of the backlogItem.
	 */
	private static final String TYPE_ID = "backlog_item_type_id"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the list of file descriptions.
	 */
	private static final String FILE_DESCRIPTIONS = "file_descriptions"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file identifier.
	 */
	private static final String FILE_ID = "file_id"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the person that submit the file.
	 */
	private static final String SUBMITTED_BY = "submitted_by"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file description.
	 */
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file name.
	 */
	private static final String NAME = "name"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file size.
	 */
	private static final String SIZE = "size"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the file type.
	 */
	private static final String TYPE = "type"; //$NON-NLS-1$

	/**
	 * The key used to retrieve the initial effort of the backlogItem.
	 */
	private static final String INITIAL_EFFORT = "initial_effort"; //$NON-NLS-1$

	/**
	 * The pattern used to format date following the ISO8601 standard.
	 */
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); //$NON-NLS-1$

	/**
	 * Loads the json files from the server data project into the appropriate variables.
	 * 
	 * @throws ParseException
	 *             the exception
	 */
	@BeforeClass
	public static void staticSetUp() throws ParseException {
		backlogItem = new TuleapBacklogItem(
				200,
				901,
				"the first backlog item", "/backlogItemss/200", "/backlogItemss?id=200&group_id=3", dateFormat.parse("2013-09-23T11:44:18.963Z"), //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
				dateFormat.parse("2013-09-24T11:44:18.963Z")); //$NON-NLS-1$
		backlogItem.setAssignedMilestoneId(Integer.valueOf(500));
		backlogItem.setInitialEffort(Float.valueOf(10));

		List<Integer> valueIds = new ArrayList<Integer>();
		valueIds.add(new Integer(1));
		valueIds.add(new Integer(2));
		valueIds.add(new Integer(3));

		LiteralFieldValue fieldValue1 = new LiteralFieldValue(1000, "300, 301, 302");
		BoundFieldValue fieldValue2 = new BoundFieldValue(2000, valueIds);

		TuleapPerson person = new TuleapPerson("Adam Smith", "SAdam", 513, "adam.smith@gmail.com");
		AttachmentValue attachementvalue = new AttachmentValue("10000", "Attachement name", person, 50000,
				"the description", "Attachement type");
		List<AttachmentValue> attachementlist = new ArrayList<AttachmentValue>();
		attachementlist.add(attachementvalue);
		AttachmentFieldValue fieldValue3 = new AttachmentFieldValue(3000, attachementlist);

		backlogItem.addFieldValue(fieldValue1);
		backlogItem.addFieldValue(fieldValue2);
		backlogItem.addFieldValue(fieldValue3);

		com.google.gson.Gson gson = new GsonBuilder().registerTypeAdapter(TuleapBacklogItem.class,
				new TuleapBacklogItemSerializer()).create();

		jsonObject = (JsonObject)gson.toJsonTree(backlogItem);
		System.out.print(gson.toJson(jsonObject));
	}

	/**
	 * Test the serialization of the simple attributes of a backlogItem.
	 * 
	 * @throws ParseException
	 *             the exception
	 * @throws IOException
	 *             the IO exception
	 */
	@Test
	public void firstTest() throws ParseException, IOException {

		int id = jsonObject.get(ID).getAsInt();
		String label = jsonObject.get(LABEL).getAsString();
		String url = jsonObject.get(URL).getAsString();
		String htmlUrl = jsonObject.get(HTML_URL).getAsString();
		int backlogItemType = jsonObject.get(TYPE_ID).getAsInt();
		int initialEffort = jsonObject.get(INITIAL_EFFORT).getAsInt();

		assertEquals(200, id);
		assertEquals("the first backlog item", label); //$NON-NLS-1$
		assertEquals("/backlogItemss/200", url); //$NON-NLS-1$
		assertEquals("/backlogItemss?id=200&group_id=3", htmlUrl); //$NON-NLS-1$
		assertEquals(901, backlogItemType);
		assertEquals(10, initialEffort);
	}

	/**
	 * Test the serialization of the values attributes of a backlogItem.
	 * 
	 * @throws ParseException
	 *             the exception
	 * @throws IOException
	 *             the IO exception
	 */
	@Test
	public void backlogItemValuesTest() throws ParseException, IOException {

		JsonArray fields = jsonObject.get(VALUES).getAsJsonArray();

		Iterator<JsonElement> valuesIterator = fields.iterator();

		JsonElement firstField = valuesIterator.next();
		JsonElement secondField = valuesIterator.next();
		JsonElement thirdField = valuesIterator.next();

		int firstFieldId = firstField.getAsJsonObject().get(FIELD_ID).getAsInt();
		int secondFieldId = secondField.getAsJsonObject().get(FIELD_ID).getAsInt();
		int thirdFieldId = thirdField.getAsJsonObject().get(FIELD_ID).getAsInt();

		assertEquals(1000, firstFieldId);
		assertEquals(2000, secondFieldId);
		assertEquals(3000, thirdFieldId);

		// The literal field Value
		String firstFieldValue = firstField.getAsJsonObject().get(FIELD_VALUE).getAsString();
		assertEquals("300, 301, 302", firstFieldValue); //$NON-NLS-1$

		// The bind field values
		JsonArray bindValues = secondField.getAsJsonObject().get(FIELD_BIND_VALUE_IDS).getAsJsonArray();

		Iterator<JsonElement> bindValuesIterator = bindValues.iterator();

		int firstBindFieldId = bindValuesIterator.next().getAsInt();
		int secondBindFieldId = bindValuesIterator.next().getAsInt();
		int thirdBindFieldId = bindValuesIterator.next().getAsInt();

		assertEquals(1, firstBindFieldId);
		assertEquals(2, secondBindFieldId);
		assertEquals(3, thirdBindFieldId);

		// The file descriptions
		JsonArray fileDescriptions = thirdField.getAsJsonObject().get(FILE_DESCRIPTIONS).getAsJsonArray();

		Iterator<JsonElement> filedescriptionIterator = fileDescriptions.iterator();

		JsonObject fileDescription = filedescriptionIterator.next().getAsJsonObject();

		int fileId = fileDescription.get(FILE_ID).getAsInt();
		int submittedBy = fileDescription.get(SUBMITTED_BY).getAsInt();
		String description = fileDescription.get(DESCRIPTION).getAsString();
		String name = fileDescription.get(NAME).getAsString();
		String type = fileDescription.get(TYPE).getAsString();
		int size = fileDescription.get(SIZE).getAsInt();

		assertEquals(10000, fileId);
		assertEquals(513, submittedBy);
		assertEquals(50000, size);
		assertEquals("the description", description); //$NON-NLS-1$
		assertEquals("Attachement name", name); //$NON-NLS-1$
		assertEquals("Attachement type", type); //$NON-NLS-1$

	}
}
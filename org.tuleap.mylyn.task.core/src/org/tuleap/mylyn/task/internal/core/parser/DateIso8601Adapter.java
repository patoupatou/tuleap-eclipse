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
package org.tuleap.mylyn.task.internal.core.parser;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Date JSON TypeAdapter for dates in ISO8601.
 * 
 * @author <a href="mailto:laurent.delaigue@obeo.fr">Laurent Delaigue</a>
 */
public class DateIso8601Adapter implements JsonDeserializer<Date>, JsonSerializer<Date> {

	/**
	 * Date format ISO-8601 with milliseconds when using a timezone with a sign and 4 digits separated by a
	 * colon.
	 */
	private static final String DATE_FORMAT_MILLIS_WITH_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; //$NON-NLS-1$

	/**
	 * Date format ISO-8601 with milliseconds when using the default timezone.
	 */
	private static final String DATE_FORMAT_MILLIS_WITH_DEFAULT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; //$NON-NLS-1$

	/**
	 * Date format ISO-8601 when using a timezone with a sign and 4 digits separated by a colon.
	 */
	private static final String DATE_FORMAT_WITH_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ssZ"; //$NON-NLS-1$

	/**
	 * Date format ISO-8601 when using the default timezone.
	 */
	private static final String DATE_FORMAT_WITH_DEFAULT_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss'Z'"; //$NON-NLS-1$

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type,
	 *      com.google.gson.JsonSerializationContext)
	 */
	public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
		String date = new SimpleDateFormat(DATE_FORMAT_WITH_TIMEZONE).format(src).replaceFirst(
				"(\\d\\d)$", ":$1"); //$NON-NLS-1$//$NON-NLS-2$
		return new JsonPrimitive(date);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type,
	 *      com.google.gson.JsonDeserializationContext)
	 */
	public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (json.isJsonPrimitive()) {
			try {
				return parseIso8601Date(json.getAsString());
			} catch (ParseException e) {
				throw new JsonParseException(e);
			}
		}
		throw new JsonParseException("Impossible to parse date for " + json.toString());
	}

	/**
	 * Parses a date in ISO 8601 format, without Joda time...
	 * 
	 * @param dateIso8601
	 *            Date to parse.
	 * @throws ParseException
	 *             if the given date is not in the right format.
	 * @return The parsed date.
	 */
	private Date parseIso8601Date(String dateIso8601) throws ParseException {
		SimpleDateFormat format;
		if (dateIso8601.endsWith("Z")) { //$NON-NLS-1$
			if (dateIso8601.indexOf('.') > 0) {
				format = new SimpleDateFormat(DATE_FORMAT_MILLIS_WITH_DEFAULT_TIMEZONE);
			} else {
				format = new SimpleDateFormat(DATE_FORMAT_WITH_DEFAULT_TIMEZONE);
			}
			format.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
			return format.parse(dateIso8601);
		}
		if (dateIso8601.indexOf('.') > 0) {
			format = new SimpleDateFormat(DATE_FORMAT_MILLIS_WITH_TIMEZONE);
		} else {
			format = new SimpleDateFormat(DATE_FORMAT_WITH_TIMEZONE);
		}
		return format.parse(dateIso8601.replaceFirst(":(\\d\\d)$", "$1")); //$NON-NLS-1$//$NON-NLS-2$
	}

}
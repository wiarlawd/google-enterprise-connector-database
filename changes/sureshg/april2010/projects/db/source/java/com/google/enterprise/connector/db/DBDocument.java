// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.db;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.SimpleProperty;
import com.google.enterprise.connector.spi.Value;

/**
 * An implementation of Document for database records. Each row in the database
 * represents a {@link DBDocument}.
 */
public class DBDocument implements Document {

	private final Map<String, List<Value>> properties = new HashMap<String, List<Value>>();
	public static final String ROW_CHECKSUM = "dbconnector:checksum";

	/**
	 * Constructs a document with no properties.
	 */
	public DBDocument() {
	}

	/* @Override */
	public Property findProperty(String name) {
		List<Value> property = properties.get(name);
		return (property == null) ? null : new SimpleProperty(property);
	}

	/**
	 * Returns all the property names.
	 */
	/* @Override */
	public Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(properties.keySet());
	}

	/**
	 * Set a property for this document. If propertyValue is null this does
	 * nothing.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
	public void setProperty(String propertyName, String propertyValue) {
		if (propertyValue != null) {
			properties.put(propertyName, Collections.singletonList(Value.getStringValue(propertyValue)));
		}
	}

	/**
	 * This method adds the last modified date property to the DB Document
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
	public void setLastModifiedDate(String propertyName, Object propertyValue) {
		Timestamp time = (Timestamp) propertyValue;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time.getTime());

		if (propertyValue == null) {
			return;
		}
		properties.put(propertyName, Collections.singletonList(Value.getDateValue(cal)));

	}

	/**
	 * In case of BLOB data iBATIS returns binary array for BLOB data-type. This
	 * method sets the "binary array" as a content of DB Document.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
	public void setBinaryContent(String propertyName, Object propertyValue) {
		if (propertyValue == null) {
			return;
		}
		properties.put(propertyName, Collections.singletonList(Value.getBinaryValue((byte[]) propertyValue)));
	}
}

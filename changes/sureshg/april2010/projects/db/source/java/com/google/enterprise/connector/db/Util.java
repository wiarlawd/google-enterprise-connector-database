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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants;

/**
 * Utility class for database connector.
 */
public class Util {
	public static final String NO_TIMESTAMP = "NO_TIMESTAMP";
	public static final String NO_DOCID = "NO_DOCID";
	private static final Logger LOG = Logger.getLogger(Util.class.getName());
	public static final String PRIMARY_KEYS_SEPARATOR = ",";
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	private static final String CHECKSUM_ALGO = "SHA1";
	private static final String MIMETYPE = "text/html";
	private static final String DBCONNECTOR_PROTOCOL = "dbconnector://";
	private static final String DATABASE_TITLE_PREFIX = "Database Connector Result";

	// This class should not be initialized.
	private Util() {
	}

	/**
	 * Converts a row to document. Docid is the checksum of primary keys values,
	 * concatenated with a comma. Content is the xml representation of a row. It
	 * also adds the checksum of the contents.
	 * 
	 * @param row row of a table.
	 * @return doc
	 * @throws DBException
	 */
	public static DBDocument rowToDoc(String dbName, String[] primaryKeys,
			Map<String, Object> row, String hostname, String xslt)
			throws DBException {
		// TODO(meghna): Look into what other document properties can be added.
		DBDocument doc = new DBDocument();
		String xmlRow = XmlUtils.getXMLRow(dbName, row, primaryKeys, xslt);
		doc.setProperty(SpiConstants.PROPNAME_CONTENT, xmlRow);
		String docId = generateDocId(primaryKeys, row);
		doc.setProperty(SpiConstants.PROPNAME_DOCID, docId);
		doc.setProperty(SpiConstants.PROPNAME_ACTION, SpiConstants.ActionType.ADD.toString());
		// TODO(meghna): Look into which encoding/charset to use for getBytes()
		doc.setProperty(DBDocument.ROW_CHECKSUM, getChecksum(xmlRow.getBytes()));
		doc.setProperty(SpiConstants.PROPNAME_MIMETYPE, MIMETYPE);
		doc.setProperty(SpiConstants.PROPNAME_DISPLAYURL, getDisplayUrl(hostname, dbName, docId));
		return doc;
	}

	private static String getDisplayUrl(String hostname, String dbName,
			String docId) {
		// displayurl is of the form -
		// dbconnector://meghna-linux.corp.google.com/mysql/2a61639c96ed45ec8f6e3d4e1ab79944cd1d1923
		String displayUrl = String.format("%s%s/%s/%s", DBCONNECTOR_PROTOCOL, hostname, dbName, docId);
		return displayUrl;
	}

	/**
	 * Generates the title of the DB document.
	 * 
	 * @param primaryKeys primary keys of the database.
	 * @param row row corresponding to the document.
	 * @return title String.
	 */
	public static String getTitle(String[] primaryKeys, Map<String, Object> row)
			throws DBException {
		StringBuilder title = new StringBuilder();
		title.append(DATABASE_TITLE_PREFIX).append(" ");

		if (row != null && (primaryKeys != null && primaryKeys.length > 0)) {
			Set<String> keySet = row.keySet();
			for (String primaryKey : primaryKeys) {
				/*
				 * Primary key value is mapped to the value of key of map row
				 * before getting record. We need to do this because GSA admin
				 * may entered primary key value which differed in case from
				 * column name.
				 */
				for (String key : keySet) {
					if (primaryKey.equalsIgnoreCase(key)) {
						primaryKey = key;
						break;
					}
				}
				if (!keySet.contains(primaryKey)) {
					String msg = "Primary Key does not match with any of the coulmn names";
					LOG.info(msg);
					throw new DBException(msg);
				}
				Object keyValue = row.get(primaryKey);
				String strKeyValue;
				if (keyValue == null
						|| keyValue.toString().trim().length() == 0) {
					strKeyValue = "";
				} else {
					strKeyValue = keyValue.toString();
				}
				title.append(primaryKey).append("=");
				title.append(strKeyValue).append(" ");
			}
		} else {
			String msg = "";
			if (row != null && (primaryKeys != null && primaryKeys.length > 0)) {
				msg = "row is null and primary key array is empty";
			} else if (row != null) {
				msg = "hash map row is null";
			} else {
				msg = "primary key array is empty or null";
			}
			LOG.info(msg);
			throw new DBException(msg);
		}
		return title.toString();
	}

	/**
	 * Generates the docId for a DB row. If the primary keys are id and lastName
	 * and their corresponding values are 1 and last_01, then the docId would be
	 * the SHA1 checksum of (1,7)1last_01. The key values are concatenated and
	 * is prepended with their lengths in parentheses.
	 * 
	 * @return docId checksum generated using the primary key values.
	 */
	public static String generateDocId(String[] primaryKeys,
			Map<String, Object> row) throws DBException {
		StringBuilder length = new StringBuilder();
		StringBuilder primaryKeyValues = new StringBuilder();
		length.append("(");
		if (row != null && (primaryKeys != null && primaryKeys.length > 0)) {
			Set<String> keySet = row.keySet();

			for (String primaryKey : primaryKeys) {
				/*
				 * Primary key value is mapped to the value of key of map row
				 * before getting record. We need to do this because GSA admin
				 * may entered primary key value which differed in case from
				 * column name.
				 */

				for (String key : keySet) {
					if (primaryKey.equalsIgnoreCase(key)) {
						primaryKey = key;
						break;
					}
				}
				if (!keySet.contains(primaryKey)) {
					String msg = "Primary Key does not match with any of the coulmn names";
					LOG.info(msg);
					throw new DBException(msg);
				}
				Object keyValue = row.get(primaryKey);
				if (null == keyValue) {
					length.append("-1" + PRIMARY_KEYS_SEPARATOR);
				} else {
					String keyValueStr = keyValue.toString();
					length.append(keyValueStr.length() + PRIMARY_KEYS_SEPARATOR);
					primaryKeyValues.append(keyValueStr);
				}
			}
		} else {
			String msg = "";
			if (row != null && (primaryKeys != null && primaryKeys.length > 0)) {
				msg = "row is null and primary key array is empty";
			} else if (row != null) {
				msg = "hash map row is null";
			} else {
				msg = "primary key array is empty or null";
			}
			LOG.info(msg);
			throw new DBException(msg);
		}
		length.deleteCharAt(length.length() - 1);
		length.append(")");
		length.append(primaryKeyValues.toString());
		LOG.info("Primary key values concatenated string : "
				+ length.toString());
		String docId = getChecksum(length.toString().getBytes());
		LOG.info("DocId : " + docId);
		return docId;
	}

	/**
	 * Generates the SHA1 checksum.
	 * 
	 * @param buf
	 * @return checksum string.
	 */
	private static String getChecksum(byte[] buf) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(CHECKSUM_ALGO);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Could not get a message digest for "
					+ CHECKSUM_ALGO);
		}
		digest.update(buf);
		return asHex(digest.digest());
	}

	/**
	 * Utility method to convert a byte[] to hex string.
	 * 
	 * @param buf
	 * @return hex string.
	 */
	private static String asHex(byte[] buf) {
		char[] chars = new char[2 * buf.length];
		for (int i = 0; i < buf.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}

	/**
	 * Creates a checkpoint string of the form (date_time_string)docId.
	 * 
	 * @param dt
	 * @param doc
	 * @return checkpoint string.
	 * @throws RepositoryException
	 */
	public static String getCheckpointString(DateTime dt, DBDocument doc)
			throws RepositoryException {
		StringBuilder str = new StringBuilder();
		str.append("(");
		if (null == dt) {
			str.append(NO_TIMESTAMP);
		} else {
			str.append(dt.toString());
		}
		str.append(")");
		if (null == doc) {
			str.append(NO_DOCID);
		} else {
			str.append(doc.findProperty(SpiConstants.PROPNAME_DOCID).nextValue().toString());
		}
		return str.toString();
	}

	/**
	 * This method convert given row into equivalent Metadata-URL feed. Value of
	 * dbconn_url column is used as URL or Doc_Id of document and value of other
	 * columns is used as metadata(except doc_id colunm values). If the column
	 * name is dbconn_last_mod , it is used as last modified date. MIME type of
	 * the document is the value of "mime_type" column. It is assumed that
	 * connector admin will use the required alias for columns in SQL query.
	 * 
	 * @param dbName
	 * @param primaryKeys
	 * @param row
	 * @param hostname
	 * @param dbClient
	 * @return DBDocument
	 * @throws DBException
	 */
	public static DBDocument generateURLMetaFeed(String dbName,
			String[] primaryKeys, Map<String, Object> row, String hostname,
			String baseURL) throws DBException {

		DBDocument doc = new DBDocument();
		// get doc id from primary key values
		String docId = generateDocId(primaryKeys, row);

		String xmlRow = XmlUtils.getXMLRow(dbName, row, primaryKeys, null);
		/*
		 * skipColumns maintain the list of column which needs to skip while
		 * indexing as they are not part of metadata or they already considered
		 * for indexing. For example document_id column, MIME type column, URL
		 * columns.
		 */
		List<String> skipColumns = new ArrayList<String>();

		// get the value of URL or doc_id field from row
		String urlValue = (String) row.get(ApplicationConstants.DOC_COLUMN);

		if (urlValue != null && urlValue.trim().length() > 0) {

			// if the value of base URL is not empty, append the value of URL
			// field column at the end of base URL to get complete path of the
			// document.
			if (baseURL != null && baseURL.trim().length() > 0) {
				urlValue = baseURL.trim() + urlValue;
			}
			doc.setProperty(SpiConstants.PROPNAME_SEARCHURL, urlValue);
			doc.setProperty(SpiConstants.PROPNAME_DISPLAYURL, urlValue);
		}

		// set doc id
		doc.setProperty(SpiConstants.PROPNAME_DOCID, docId);
		// get MIME type of this document from "mime_type" column.
		Object mimeType = row.get(ApplicationConstants.MIME_TYPE_COLUMN);
		if (mimeType != null) {
			doc.setProperty(SpiConstants.PROPNAME_MIMETYPE, row.get(ApplicationConstants.MIME_TYPE_COLUMN).toString());
		}
		doc.setProperty(DBDocument.ROW_CHECKSUM, getChecksum(xmlRow.getBytes()));
		// add action as add
		doc.setProperty(SpiConstants.PROPNAME_ACTION, SpiConstants.ActionType.ADD.toString());

		// set last modified
		Object lastModified = row.get(ApplicationConstants.LAST_MOD_COLUMN);
		if (lastModified != null) {
			doc.setLastModified(SpiConstants.PROPNAME_LASTMODIFIED, lastModified);
			skipColumns.add(ApplicationConstants.LAST_MOD_COLUMN);
		}

		skipColumns.add((String) row.get(ApplicationConstants.MIME_TYPE_COLUMN));
		skipColumns.add(ApplicationConstants.DOC_COLUMN);
		skipColumns.addAll(Arrays.asList(primaryKeys));
		setMetaInfo(doc, row, skipColumns);

		return doc;
	}

	/**
	 * This method will add value of each column as metadata to Database
	 * document except the values of column in skipColumns list.
	 * 
	 * @param doc
	 * @param row
	 * @param skipColumns list of columns needs to ignore while indexing
	 */
	private static void setMetaInfo(DBDocument doc, Map<String, Object> row,
			List<String> skipColumns) {
		// get all column names as key set
		Set<String> keySet = row.keySet();
		for (String key : keySet) {
			// set column value as metadata and column name as meta-name.
			if (!skipColumns.contains(key)) {
				Object value = row.get(key);
				if (value != null)
					doc.setProperty(key, value.toString());
			} else {
				LOG.info("skipping metadata indexing of column " + key);
			}
		}
	}
}

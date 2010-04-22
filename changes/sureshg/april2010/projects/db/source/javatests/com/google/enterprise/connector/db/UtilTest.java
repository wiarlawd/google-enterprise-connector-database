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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants;

public class UtilTest extends TestCase {
	private static final Logger LOG = Logger.getLogger(UtilTest.class.getName());

	/**
	 * Test for generating the docId.
	 */
	public final void testGenerateDocId() {
		Map<String, Object> rowMap = null;
		String primaryKeys[] = null;

		try {
			// generateDocId(primaryKeys, rowMap) should throw an exception as
			// primary key array and rowMai is null
			String docId = Util.generateDocId(primaryKeys, rowMap);
			fail();
		} catch (DBException e1) {
			LOG.info("NullPointerException is handle in generateDocId method");
		}

		try {
			rowMap = TestUtils.getStandardDBRow();
			primaryKeys = TestUtils.getStandardPrimaryKeys();
			assertEquals("6fd5643953e6e60188c93b89c71bc1808eb7edc2", Util.generateDocId(primaryKeys, rowMap));
		} catch (DBException e) {
			fail("Exception while generating doc id" + e.toString());
		}
	}

	/**
	 * Test for converting DB row to DB Doc.
	 */
	public final void testRowToDoc() {
		Map<String, Object> rowMap = TestUtils.getStandardDBRow();
		String[] primaryKeys = TestUtils.getStandardPrimaryKeys();
		try {
			DBDocument doc = Util.rowToDoc("testdb_", primaryKeys, rowMap, "localhost", null);
			for (String propName : doc.getPropertyNames()) {
				Property prop = doc.findProperty(propName);
				LOG.info(propName + ":    " + prop.nextValue().toString());
			}
			assertEquals("6fd5643953e6e60188c93b89c71bc1808eb7edc2", doc.findProperty(SpiConstants.PROPNAME_DOCID).nextValue().toString());
			assertEquals("eb476c046da8b3e83081e3195923aba1dd9c6045", doc.findProperty(DBDocument.ROW_CHECKSUM).nextValue().toString());
		} catch (DBException e) {
			fail("Could not generate DB document from row.");
		} catch (RepositoryException e) {
			fail("Could not generate DB document from row.");
		}
	}

	public final void testGetCheckpointString() throws DBException {
		Map<String, Object> rowMap = TestUtils.getStandardDBRow();
		DBDocument doc = Util.rowToDoc("testdb_", TestUtils.getStandardPrimaryKeys(), rowMap, "localhost", null);
		try {
			String checkpointStr = Util.getCheckpointString(null, null);
			assertEquals("(NO_TIMESTAMP)NO_DOCID", checkpointStr);
			DateTime dt = new DateTime();
			checkpointStr = Util.getCheckpointString(dt, doc);
			assertTrue(checkpointStr.contains(dt.toString()));
			assertTrue(checkpointStr.contains("6fd5643953e6e60188c93b89c71bc1808eb7edc2"));
			LOG.info(checkpointStr);
		} catch (RepositoryException e) {
			fail("Unexpected exception" + e.toString());
		}
	}

	/**
	 * test case for generateURLMetaFeed()
	 */
	public final void testGenerateURLMetaFeed() {
		Map<String, Object> rowMap = new HashMap<String, Object>();
		Map<String, Object> rowMapWithBaseURL = new HashMap<String, Object>();

		String primaryKeyColumn = "id";
		String[] primaryKeys = { primaryKeyColumn };
		String documentURL = "http://myhost/app/welcome.html";
		String baseURL = "http://myhost/app/";
		String docId = "index123.html";
		String versionColumn = "version";
		String versionValue = "2.3.4";
		// add primary key in row
		rowMap.put(primaryKeyColumn, 1);
		// add document URL in row
		rowMap.put(DBConnectorConstants.DOC_COLUMN, documentURL);
		// add version column in row
		rowMap.put(versionColumn, versionValue);
		try {
			DBDocument doc = Util.generateMetadataURLFeed("testdb_", primaryKeys, rowMap, "localhost", "");
			// test:- column "version" value as metadata
			assertEquals(versionValue, doc.findProperty(versionColumn).nextValue().toString());
			// test:- display URL will be same as the actual URL of the
			// document
			assertEquals("http://myhost/app/welcome.html", doc.findProperty(SpiConstants.PROPNAME_DISPLAYURL).nextValue().toString());

			// test scenario: when base URL is not empty, the display URL is
			// generated by concatenating document id with base URL.

			rowMapWithBaseURL.put(primaryKeyColumn, 2);
			rowMapWithBaseURL.put(DBConnectorConstants.DOC_COLUMN, docId);
			rowMapWithBaseURL.put(versionColumn, versionValue);
			DBDocument docWithBaseURL = Util.generateMetadataURLFeed("testdb_", primaryKeys, rowMapWithBaseURL, "localhost", baseURL);

			// test:- column "version" value as metadata
			assertEquals(versionValue, docWithBaseURL.findProperty(versionColumn).nextValue().toString());
			// test: display URL of the document
			assertEquals(baseURL + docId, docWithBaseURL.findProperty(SpiConstants.PROPNAME_DISPLAYURL).nextValue().toString());

		} catch (DBException e) {
			fail("Unexpected exception" + e.toString());
		} catch (RepositoryException e) {
			fail("Unexpected exception" + e.toString());
		}

	}

	/**
	 * test case for largeObjectToDoc() method
	 */
	public final void testLargeObjectToDoc() {
		Map<String, Object> rowMap = new HashMap<String, Object>();
		Map<String, Object> rowMapWithBaseURL = new HashMap<String, Object>();
		// define common test data
		String primaryKeyColumn = "id";
		String[] primaryKeys = { primaryKeyColumn };
		String versionColumn = "version";
		String versionValue = "2.3.4";
		String title = "Welcome Page";
		// add primary key in row
		rowMap.put(primaryKeyColumn, 1);
		// add version column in row
		rowMap.put(versionColumn, versionValue);
		// add document title for this document. Use alias "dbconn_title" for
		// column used to store document title.
		rowMap.put(DBConnectorConstants.TITLE_COLUMN, title);

		// Test scenarios for CLOB data types
		testCLOBDataScenarios(rowMap, primaryKeys);
		// remove CLOB entry from row map which was added in
		// testCLOBDataScenarios().
		rowMap.remove(DBConnectorConstants.CLOB_COLUMN);
		// Test scenarios for BLOB data types
		testBLOBDataScenarios(rowMap, primaryKeys);
	}

	/**
	 * test scenarios for CLOB data types
	 * 
	 * @param rowMap
	 * @param primaryKeys
	 */
	private void testCLOBDataScenarios(Map<String, Object> rowMap,
			String[] primaryKeys) {

		LOG.info("Testing largeObjectToDoc() for CLOB data");
		// In iBATIS CLOB data is represented as String or char array.
		// Define CLOB data for this test case
		String clobContent = "This IS CLOB Text";
		// set CLOB content. Use alias "dbconn_clob" for column used for storing
		// CLOB data.
		rowMap.put(DBConnectorConstants.CLOB_COLUMN, clobContent);

		try {
			DBDocument clobDoc = Util.largeObjectToDoc("testdb_", primaryKeys, rowMap, "localhost", DBConnectorConstants.CLOB_COLUMN);
			assertNotNull(clobDoc);
			// test scenario:- this doc will have column name "version" as
			// metadata key and value will be "2.3.4"
			assertEquals("2.3.4", clobDoc.findProperty("version").nextValue().toString());
			// test scenario:- the content of this document will be same as the
			// content of CLOB column(dbconn_clob).
			assertEquals(clobContent, clobDoc.findProperty(SpiConstants.PROPNAME_CONTENT).nextValue().toString());
			// test scenario:- primary key column should be excluded while
			// indexing external metadata
			assertNull(clobDoc.findProperty("id"));

			// test document title
			assertEquals("Welcome Page", clobDoc.findProperty(SpiConstants.PROPNAME_TITLE).nextValue().toString());

		} catch (DBException e) {
			fail("Unexpected exception" + e.toString());
		} catch (RepositoryException e) {
			fail("Unexpected exception" + e.toString());
		}
	}

	/**
	 * test scenarios for clob
	 * 
	 * @param rowMap
	 * @param primaryKeys
	 */

	private void testBLOBDataScenarios(Map<String, Object> rowMap,
			String[] primaryKeys) {
		LOG.info("Testing largeObjectToDoc for BLOB data");
		// In iBATIS binary content(BLOB) is represented as byte array.
		// Define BLOB data for this test case
		byte[] blobContent = "SOME BINARY DATA".getBytes();
		// set BLOB content
		rowMap.put(DBConnectorConstants.BLOB_COLUMN, blobContent);

		// Define for fetching BLOB content
		String fetchURL = "http://myhost:8030/app?dpc_id=120";
		// use alias "dbconn_lob_url" for the column user for storing URL to
		// BLOB data
		rowMap.put(DBConnectorConstants.LOB_URL, fetchURL);

		try {
			DBDocument blobDoc = Util.largeObjectToDoc("testdb_", primaryKeys, rowMap, "localhost", DBConnectorConstants.CLOB_COLUMN);
			assertNotNull(blobDoc);
			// test scenario:- this doc will have column name "version" as
			// metadata key and value will be "2.3.4"
			assertEquals("2.3.4", blobDoc.findProperty("version").nextValue().toString());

			// test scenario:- primary key column should be excluded while
			// indexing external metadata
			assertNull(blobDoc.findProperty("id"));

			// test document title
			assertEquals("Welcome Page", blobDoc.findProperty(SpiConstants.PROPNAME_TITLE).nextValue().toString());

			// if one of the column holds the URL for fetching BLOB data. It
			// will be used as display URL in feed.
			assertEquals(fetchURL, blobDoc.findProperty(SpiConstants.PROPNAME_DISPLAYURL).nextValue().toString());

		} catch (DBException e) {
			fail("Unexpected exception" + e.toString());
		} catch (RepositoryException e) {
			fail("Unexpected exception" + e.toString());
		}
	}
}

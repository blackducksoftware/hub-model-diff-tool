/**
 * hub-model-diff-tool
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hubdiff;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

public class HubDiffTest {
	private File expectedFile;
	private File actualFile;
	private File resultsFile;
	private File swaggerDocFile1;
	private File swaggerDocFile2;
	private SwaggerDoc doc1;
	private SwaggerDoc doc2;
	
	@Before
	public void setup() throws IOException, URISyntaxException {
		URI basePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
		expectedFile = new File(basePath.resolve("expected.csv"));
		actualFile = new File(basePath.resolve("actual.csv"));
		resultsFile = new File(basePath.resolve("results.txt"));
		swaggerDocFile1 = new File(basePath.resolve("api-docs-3.4.2-test.json"));
		swaggerDocFile2 = new File(basePath.resolve("api-docs-3.5.0-test.json"));
		doc1 = new SwaggerDoc(FileUtils.readFileToString(swaggerDocFile1, StandardCharsets.UTF_8), "3.4.2");
		doc2 = new SwaggerDoc(FileUtils.readFileToString(swaggerDocFile2, StandardCharsets.UTF_8), "3.5.0");
	}
	
	@Test
	public void getDiff() throws JSONException, IOException {
		String expected = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
		HubDiff hubDiff = new HubDiff(doc1, doc2);
		assertEquals(expected, hubDiff.getDiff());
	}
	
	@Test
	public void csvTest() throws IOException, IllegalArgumentException, EncryptionException, HubIntegrationException, JSONException {
		HubDiff hubDiff = new HubDiff(doc1, doc2);
		
		hubDiff.writeDiffAsCSV(actualFile);
		
		CSVParser expectedParser = new CSVParser(new FileReader(expectedFile), CSVFormat.EXCEL);
		CSVParser actualParser = new CSVParser(new FileReader(actualFile), CSVFormat.EXCEL);
		List<CSVRecord> expectedRecords = expectedParser.getRecords();
		List<CSVRecord> actualRecords = actualParser.getRecords();
		
		assertEquals(expectedRecords.size(), actualRecords.size());
		
		for (int i = 0; i < expectedRecords.size(); i++) {
			String expected = expectedRecords.get(i).toString();
			String actual = actualRecords.get(i).toString();
			assertEquals(expected, actual);
		}
		
		expectedParser.close();
		actualParser.close();
	}
}

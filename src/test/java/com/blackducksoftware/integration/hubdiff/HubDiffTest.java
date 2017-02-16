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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hubdiff.HubDiff;
import com.blackducksoftware.integration.hubdiff.SwaggerDoc;

public class HubDiffTest {
	private final String resources = "src/test/resources/";
	private File file1;
	private File file2;
	
	// @Before
	public void setup() {
		file1 = new File(resources + "api-docs-3.4.2-test.json");
		file2 = new File(resources + "api-docs-3.5.0-test.json");
	}
	
	// @Test
	public void getDiffFile() throws IOException, JSONException {
		
		File results = new File(resources + "results.txt");
		
		String doc1 = FileUtils.readFileToString(file1, StandardCharsets.UTF_8);
		String doc2 = FileUtils.readFileToString(file2, StandardCharsets.UTF_8);
		String expected = FileUtils.readFileToString(results, StandardCharsets.UTF_8);
		
		SwaggerDoc swaggerDoc1 = new SwaggerDoc(doc1, "3.4.2");
		SwaggerDoc swaggerDoc2 = new SwaggerDoc(doc2, "3.5.0");
		HubDiff hubDiff = new HubDiff(swaggerDoc1, swaggerDoc2);
		
		assertEquals(expected, hubDiff.getDiff());
	}
	
	// @Test
	public void csvTest() throws IOException, IllegalArgumentException, EncryptionException, HubIntegrationException, JSONException {
		String doc1 = FileUtils.readFileToString(file1, StandardCharsets.UTF_8);
		String doc2 = FileUtils.readFileToString(file2, StandardCharsets.UTF_8);
		
		SwaggerDoc swaggerDoc1 = new SwaggerDoc(doc1, "3.4.2");
		SwaggerDoc swaggerDoc2 = new SwaggerDoc(doc2, "3.5.0");
		
		HubDiff hubDiff = new HubDiff(swaggerDoc1, swaggerDoc2);
		
		String expected = FileUtils.readFileToString(new File(resources + "expected.csv"), StandardCharsets.UTF_8);
		File actual = new File(resources + "actual.csv");
		assertEquals(expected, hubDiff.writeDiffAsCSV(actual));
	}
}

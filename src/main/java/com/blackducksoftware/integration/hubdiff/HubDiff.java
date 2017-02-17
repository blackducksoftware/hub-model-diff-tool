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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.FieldComparisonFailure;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.request.HubRequest;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.rest.RestConnection;

public class HubDiff {
	private static final Logger log = LoggerFactory.getLogger("HubDiff");
	
	public static void main(String[] args) throws IOException, IllegalArgumentException, EncryptionException, HubIntegrationException, JSONException {
		Options options = new Options();
		Option optUrl1 = new Option("h1", "hub-url-1", true, "the base url to the hub. Example: http://int-hub01.dc1.lan:8080");
		Option optUsername1 = new Option("u1", "username-1", true, "the username for your hub instance");
		Option optPassword1 = new Option("p1", "password-1", true, "the password for your hub instance and username");
		Option optUrl2 = new Option("h2", "hub-url-2", true, "the base url to the hub. Example: http://int-auto.dc1.lan:9000");
		Option optUsername2 = new Option("u2", "username-2", true, "the username for your hub instance");
		Option optPassword2 = new Option("p2", "password-2", true, "the password for your hub instance and username");
		Option optOutputFile = new Option("o", "output", true, "the file path to your output file");
		
		optUrl1.setRequired(true);
		optUsername1.setRequired(true);
		optPassword1.setRequired(true);
		optUsername2.setRequired(true);
		optUrl1.setRequired(true);
		optPassword2.setRequired(true);
		optOutputFile.setRequired(false);
		
		// Add options to collection
		options.addOption(optUrl1);
		options.addOption(optUsername1);
		options.addOption(optPassword1);
		options.addOption(optUrl2);
		options.addOption(optUsername2);
		options.addOption(optPassword2);
		options.addOption(optOutputFile);
		
		// Parse the arguments array for the options
		CommandLineParser cliParser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine optionParser;
		try {
			optionParser = cliParser.parse(options, args);
		} catch (ParseException e) {
			formatter.printHelp("hub-model-generator", options);
			log.error(e.getMessage());
			System.exit(1);
			return;
		}
		
		// Read arguments
		String url1 = optionParser.getOptionValue("hub-url-1");
		String username1 = optionParser.getOptionValue("username-1");
		String password1 = optionParser.getOptionValue("password-1");
		String url2 = optionParser.getOptionValue("hub-url-2");
		String username2 = optionParser.getOptionValue("username-2");
		String password2 = optionParser.getOptionValue("password-2");
		String outputFilePath = optionParser.getOptionValue("output");
		
		HubServerConfigBuilder configBuilder = new HubServerConfigBuilder();
		configBuilder.setHubUrl(url1);
		configBuilder.setUsername(username1);
		configBuilder.setPassword(password1);
		HubServerConfig config1 = configBuilder.build();
		
		HubServerConfigBuilder configBuilder2 = new HubServerConfigBuilder();
		configBuilder2.setHubUrl(url2);
		configBuilder2.setUsername(username2);
		configBuilder2.setPassword(password2);
		HubServerConfig config2 = configBuilder2.build();
		
		HubDiff hubDiff = new HubDiff(config1, config2);
		hubDiff.printDiff(System.out);
		
		if (outputFilePath != null) {
			File outputFile = new File(outputFilePath);
			hubDiff.writeDiffAsCSV(outputFile);
		}
	}
	
	private SwaggerDoc swaggerDoc1;
	private SwaggerDoc swaggerDoc2;
	private JSONCompareResult results;
	
	public HubDiff(HubServerConfig config1, HubServerConfig config2) throws IllegalArgumentException, EncryptionException, JSONException, HubIntegrationException {
		RestConnection connection1 = new CredentialsRestConnection(config1);
		RestConnection connection2 = new CredentialsRestConnection(config2);
		connection1.connect();
		connection2.connect();
		
		String serverVersion1 = fetchHubVersion(connection1);
		String serverVersion2 = fetchHubVersion(connection2);
		swaggerDoc1 = new SwaggerDoc(fetch(connection1), serverVersion1);
		swaggerDoc2 = new SwaggerDoc(fetch(connection2), serverVersion2);
		
		try {
			saveFiles(swaggerDoc1);
			saveFiles(swaggerDoc2);
		} catch (HubIntegrationException | IOException | URISyntaxException e) {
			System.out.println("Failed to save files");
			e.printStackTrace();
		}
		
		results = swaggerDoc1.getDifference(swaggerDoc2);
	}
	
	public HubDiff(SwaggerDoc swaggerDoc1, SwaggerDoc swaggerDoc2) throws JSONException {
		this.swaggerDoc1 = swaggerDoc1;
		this.swaggerDoc2 = swaggerDoc2;
		results = swaggerDoc1.getDifference(swaggerDoc2);
	}
	
	public void saveFiles(SwaggerDoc swaggerDoc) throws HubIntegrationException, IOException, URISyntaxException {
		URL basePath = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		
		File swaggerFile = new File(basePath.toURI().resolve("api-docs-" + swaggerDoc.getVersion() + ".json"));
		if (!swaggerFile.exists()) {
			FileUtils.write(swaggerFile, swaggerDoc.getSwaggerDoc(), StandardCharsets.UTF_8);
			log.info("No doc for version [{}]. Creating one now.", swaggerDoc.getVersion());
		}
	}
	
	public String fetch(RestConnection rest) throws HubIntegrationException {
		String url = rest.getBaseUrl() + "/api/v2/api-docs.json";
		HubRequest docsRequest = new HubRequest(rest);
		docsRequest.setUrl(url);
		log.info("Making GET request to hub: {}", url);
		return docsRequest.executeGetForResponseString();
	}
	
	public String fetchHubVersion(RestConnection rest) throws HubIntegrationException {
		String url = rest.getBaseUrl() + "/api/v1/current-version";
		HubRequest versionRequest = new HubRequest(rest);
		versionRequest.setUrl(url);
		log.info("Making GET request to hub: {}", url);
		return versionRequest.executeGetForResponseString().replaceAll("\"", "");
		
	}
	
	public String getDiff() {
		String diff = "";
		for (FieldComparisonFailure added : results.getFieldUnexpected()) {
			diff += ("[ADDED] " + added.getActual() + " TO " + added.getField());
			diff += ("\n");
		}
		for (FieldComparisonFailure changed : results.getFieldFailures()) {
			diff += ("[CHANGED] " + changed.getField() + " FROM " + changed.getExpected() + " TO " + changed.getActual());
			diff += ("\n");
		}
		for (FieldComparisonFailure removed : results.getFieldMissing()) {
			diff += ("[REMOVED] " + removed.getExpected() + " FROM " + removed.getField());
			diff += ("\n");
		}
		return diff;
	}
	
	public void printDiff(OutputStream out) {
		try {
			out.write(getDiff().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeDiff(File file) {
		try {
			FileUtils.write(file, getDiff(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String writeDiffAsCSV(File file) throws IOException {
		if (!file.exists()) {
			file.createNewFile();
		}
		
		CSVPrinter printer = new CSVPrinter(new PrintStream(file), CSVFormat.EXCEL);
		
		printer.printRecord("HubVersions", swaggerDoc1.getVersion(), " -> ", swaggerDoc2.getVersion());
		printer.printRecord("Operation", "Expected", "Actual", "Field");
		
		// Log all additions to the API
		for (FieldComparisonFailure added : results.getFieldUnexpected()) {
			printer.printRecord("ADDED", added.getExpected(), added.getActual(), added.getField());
		}
		// Log all changes made to the API
		for (FieldComparisonFailure changed : results.getFieldFailures()) {
			printer.printRecord("CHANGED", changed.getExpected(), changed.getActual(), changed.getField());
		}
		// Log all deletions made to the API
		for (FieldComparisonFailure removed : results.getFieldMissing()) {
			printer.printRecord("REMOVED", removed.getExpected(), removed.getActual(), removed.getField());
		}
		
		printer.close();
		return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
	}
}

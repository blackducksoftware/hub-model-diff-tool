package com.blackducksoftware.integration.hub_common_diff;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
	private static final String resources = "src/main/resources/";
	
	public static void main(String[] args) throws IOException, IllegalArgumentException, EncryptionException, HubIntegrationException, JSONException {
		Options options = new Options();
		Option optUrl1 = new Option("h1", "hub-url-1", true, "the base url to the hub. Example: http://int-hub01.dc1.lan:8080");
		Option optUsername1 = new Option("u1", "username-1", true, "the username for your hub instance");
		Option optPassword1 = new Option("p1", "password-1", true, "the password for your hub instance and username");
		Option optUrl2 = new Option("h2", "hub-url-2", true, "the base url to the hub. Example: http://int-auto.dc1.lan:9000");
		Option optUsername2 = new Option("u2", "username-2", true, "the username for your hub instance");
		Option optPassword2 = new Option("p2", "password-2", true, "the password for your hub instance and username");
		
		// Add options to collection
		options.addOption(optUrl1);
		options.addOption(optUsername1);
		options.addOption(optPassword1);
		options.addOption(optUrl2);
		options.addOption(optUsername2);
		options.addOption(optPassword2);
		
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
		hubDiff.getDiff();
	}
	
	private SwaggerDoc swaggerDoc1;
	private SwaggerDoc swaggerDoc2;
	private JSONCompareResult results;
	
	public HubDiff(HubServerConfig config1, HubServerConfig config2) throws IllegalArgumentException, EncryptionException, HubIntegrationException, IOException, JSONException {
		RestConnection connection1 = new CredentialsRestConnection(config1);
		RestConnection connection2 = new CredentialsRestConnection(config1);
		connection1.connect();
		connection2.connect();
		
		String serverVersion1 = fetchHubVersion(connection1);
		String serverVersion2 = fetchHubVersion(connection2);
		swaggerDoc1 = new SwaggerDoc(fetch(connection1), serverVersion1);
		swaggerDoc2 = new SwaggerDoc(fetch(connection2), serverVersion2);
		
		saveFiles(swaggerDoc1);
		saveFiles(swaggerDoc2);
		
		results = swaggerDoc1.getDifference(swaggerDoc2);
	}
	
	public void saveFiles(SwaggerDoc swaggerDoc) throws HubIntegrationException, IOException {
		File swaggerFile = new File(resources + "api-docs-" + swaggerDoc.getVersion() + ".json");
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
	
	public void writeDiffAsCSV(File file) {
		String output = "Operation,Expected,Actual,Field";
		// Log all additions to the API
		for (FieldComparisonFailure added : results.getFieldUnexpected()) {
			output += ("ADDED," + added.getExpected() + "," + added.getActual() + "," + added.getField());
			output += ("\n");
		}
		// Log all changes made to the API
		for (FieldComparisonFailure changed : results.getFieldFailures()) {
			output += ("CHANGED," + changed.getExpected() + "," + changed.getActual() + "," + changed.getField());
			output += ("\n");
		}
		// Log all deletions made to the API
		for (FieldComparisonFailure removed : results.getFieldMissing()) {
			output += ("REMOVED," + removed.getExpected() + "," + removed.getActual() + "," + removed.getField());
			output += ("\n");
		}
		
		try {
			FileUtils.write(file, output, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

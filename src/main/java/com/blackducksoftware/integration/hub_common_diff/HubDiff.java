package com.blackducksoftware.integration.hub_common_diff;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.FieldComparisonFailure;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class HubDiff {
	private static final String HUB_URL = "http://int-auto01.dc1.lan:9000";
	// private static final String HUB_URL = "http://int-hub01.dc1.lan:8080";
	private static final String HUB_USERNAME = "sysadmin";
	private static final String HUB_PASSWORD = "blackduck";
	
	private static final Logger log = LoggerFactory.getLogger("HubDiff");
	private static final String resources = "src/main/resources/";
	
	public static void main(String[] args) throws IOException, IllegalArgumentException, EncryptionException, HubIntegrationException, JSONException {
		HubServerConfigBuilder configBuilder = new HubServerConfigBuilder();
		configBuilder.setHubUrl(HUB_URL);
		configBuilder.setUsername(HUB_USERNAME);
		configBuilder.setPassword(HUB_PASSWORD);
		HubServerConfig config = configBuilder.build();
		
		HubDiff hubDiff = new HubDiff(config);
		hubDiff.compare();
	}
	
	private RestConnection restConnection;
	private String baseUrl;
	private String hubVersion;
	private String localVersion;
	private String swaggerDoc;
	
	public HubDiff(HubServerConfig config) throws IllegalArgumentException, EncryptionException, HubIntegrationException, IOException, JSONException {
		baseUrl = config.getHubUrl().toString();
		restConnection = new CredentialsRestConnection(config);
		restConnection.connect();
		hubVersion = fetchHubVersion();
		swaggerDoc = fetch();
		validateFiles();
		localVersion = FileUtils.readFileToString(new File(resources + "hubVersion.txt"), StandardCharsets.UTF_8);
	}
	
	public void validateFiles() throws HubIntegrationException, IOException {
		File swaggerFile = new File(resources + "api-docs-" + hubVersion + ".json");
		if (!swaggerFile.exists()) {
			writeJSON(swaggerDoc, hubVersion);
			log.info("No doc for version [{}]. Creating one now.", hubVersion);
		}
		
		File versionFile = new File(resources + "hubVersion.txt");
		if (!versionFile.exists()) {
			log.info("No hub version file. Creating one now.");
			FileUtils.write(versionFile, hubVersion, StandardCharsets.UTF_8);
		}
	}
	
	public String fetch() throws HubIntegrationException {
		String url = baseUrl + "/api/v2/api-docs.json";
		HubRequest docsRequest = new HubRequest(restConnection);
		docsRequest.setUrl(url);
		log.info("Making GET request to hub: {}", url);
		return docsRequest.executeGetForResponseString();
	}
	
	public String fetchHubVersion() throws HubIntegrationException {
		String url = baseUrl + "/api/v1/current-version";
		HubRequest versionRequest = new HubRequest(restConnection);
		versionRequest.setUrl(url);
		log.info("Making GET request to hub: {}", url);
		return versionRequest.executeGetForResponseString().replaceAll("\"", "");
		
	}
	
	public void compare() throws JSONException {
		compare(localVersion, hubVersion, swaggerDoc);
	}
	
	public void compare(String yourVersion, String hubVersion, String fetchedSwagger) throws JSONException {
		if (!yourVersion.equals(hubVersion)) {
			String current = readJSON(yourVersion);
			log.info("The current hub version is {}. Your version is {}", hubVersion, yourVersion);
			JSONCompareResult results = JSONCompare.compareJSON(current, fetchedSwagger, JSONCompareMode.STRICT);
			// Log all additions to the API
			for (FieldComparisonFailure unexcpected : results.getFieldUnexpected()) {
				log.info("[ADDED] {} TO {}", unexcpected.getActual(), unexcpected.getField());
			}
			// Log all changes made to the API
			for (FieldComparisonFailure changed : results.getFieldFailures()) {
				log.info("[CHANGED] {} FROM {} TO {}", changed.getField(), changed.getExpected(), changed.getActual());
			}
			// Log all deletions made to the API
			for (FieldComparisonFailure removed : results.getFieldMissing()) {
				log.info("[REMOVED] {} FROM {}", removed.getExpected(), removed.getField());
			}
		}
	}
	
	public static String formatJSON(String json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement elem = gson.fromJson(json, JsonElement.class);
		json = gson.toJson(elem);
		return json;
	}
	
	public static String readJSON(String version) {
		File file = new File(resources + "api-docs-" + version + ".json");
		try {
			return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to read from file {}", file.getPath());
			return null;
		}
	}
	
	public boolean writeJSON(String json, String version) {
		File file = new File(resources + "api-docs-" + version + ".json");
		try {
			FileUtils.write(file, version, StandardCharsets.UTF_8);
			return true;
		} catch (IOException e) {
			log.error("Failed to write to file {}", file.getPath());
			return false;
		}
	}
}

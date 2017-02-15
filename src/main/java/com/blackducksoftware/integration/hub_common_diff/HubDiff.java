package com.blackducksoftware.integration.hub_common_diff;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.RepositoryBuilder;
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
	private static final String HUB_URL = "http://int-auto01.dc1.lan:9000";
	// private static final String HUB_URL = "http://int-hub01.dc1.lan:8080";
	private static final String HUB_USERNAME = "sysadmin";
	private static final String HUB_PASSWORD = "blackduck";
	
	private static final Logger log = LoggerFactory.getLogger("HubDiff");
	private final String resources = "src/main/resources/";
	
	public static void main(String[] args) throws IOException, IllegalArgumentException, EncryptionException, HubIntegrationException {
		HubServerConfigBuilder configBuilder = new HubServerConfigBuilder();
		configBuilder.setHubUrl(HUB_URL);
		configBuilder.setUsername(HUB_USERNAME);
		configBuilder.setPassword(HUB_PASSWORD);
		HubServerConfig config = configBuilder.build();
		
		HubDiff hubDiff = new HubDiff(config);
		
	}
	
	private RestConnection restConnection;
	private String baseUrl;
	
	public HubDiff(HubServerConfig config) throws IllegalArgumentException, EncryptionException, HubIntegrationException, IOException {
		baseUrl = config.getHubUrl().toString();
		restConnection = new CredentialsRestConnection(config);
		restConnection.connect();
		
		String hubVersion = getHubVersion();
		String swaggerDoc = fetch();
		
		File swaggerFile = new File(resources + "api-docs-" + hubVersion + ".json");
		if (!swaggerFile.exists()) {
			FileUtils.write(swaggerFile, swaggerDoc, StandardCharsets.UTF_8);
			log.info("No doc for version [{}]. Creating one now.", hubVersion);
		}
		
		File versionFile = new File(resources + "hubVersion.txt");
		if (!versionFile.exists()) {
			FileUtils.write(versionFile, hubVersion, StandardCharsets.UTF_8);
			log.info("No hub version file. Creating one now.");
		}
		
		String version = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
		if (!version.equals(hubVersion)) {
			log.info("The current hub version is {}. Your version is {}", hubVersion, version);
			RepositoryBuilder builder = new RepositoryBuilder();
			builder.setGitDir(null);
		}
	}
	
	public String fetch() throws HubIntegrationException {
		String url = baseUrl + "/api/v2/api-docs.json";
		HubRequest docsRequest = new HubRequest(restConnection);
		docsRequest.setUrl(url);
		log.info("Making GET request to hub: {}", url);
		return docsRequest.executeGetForResponseString();
	}
	
	public String getHubVersion() throws HubIntegrationException {
		String url = baseUrl + "/api/v1/current-version";
		HubRequest versionRequest = new HubRequest(restConnection);
		versionRequest.setUrl(url);
		log.info("Making GET request to hub: {}", url);
		return versionRequest.executeGetForResponseString().replaceAll("\"", "");
		
	}
	
}

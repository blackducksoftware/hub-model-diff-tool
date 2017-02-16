package com.blackducksoftware.integration.hub_common_diff;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;

public class HubDiffTest {
	
	public void getDiffFromHubTest() {
		HubServerConfigBuilder oldHubConfig = new HubServerConfigBuilder();
		oldHubConfig.setHubUrl("http://int-hub01.dc1.lan:8080");
		oldHubConfig.setUsername("sysadmin");
		oldHubConfig.setPassword("blackduck");
		HubServerConfig oldHub = oldHubConfig.build();
		
		HubServerConfigBuilder newHubConfig = new HubServerConfigBuilder();
		newHubConfig.setHubUrl("http://int-auto01.dc1.lan:9000");
		newHubConfig.setUsername("sysadmin");
		newHubConfig.setPassword("blackduck");
		HubServerConfig newHub = newHubConfig.build();
		HubDiff hubDiff = null;
		try {
			hubDiff = new HubDiff(oldHub, newHub);
		} catch (IllegalArgumentException | EncryptionException | HubIntegrationException | JSONException e) {
			e.printStackTrace();
			Assert.fail("Failed to start test");
		}
		System.out.println(hubDiff.getDiff());
	}
	
	@Test
	public void getDiffFile() throws IOException, JSONException {
		String resources = "src/main/resources/tests/";
		File file1 = new File(resources + "api-docs-3.4.2-test.json");
		File file2 = new File(resources + "api-docs-3.5.0-test.json");
		File results = new File(resources + "results.txt");
		
		String doc1 = FileUtils.readFileToString(file1, StandardCharsets.UTF_8);
		String doc2 = FileUtils.readFileToString(file2, StandardCharsets.UTF_8);
		String expected = FileUtils.readFileToString(results, StandardCharsets.UTF_8);
		
		SwaggerDoc swaggerDoc1 = new SwaggerDoc(doc1, "3.4.2");
		SwaggerDoc swaggerDoc2 = new SwaggerDoc(doc2, "3.5.0");
		HubDiff hubDiff = new HubDiff(swaggerDoc1, swaggerDoc2);
		
		assertEquals(expected, hubDiff.getDiff());
	}
}

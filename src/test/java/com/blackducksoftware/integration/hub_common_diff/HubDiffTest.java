package com.blackducksoftware.integration.hub_common_diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;

public class HubDiffTest {
	private final String resources = "src/main/resources/tests/";
	private HubServerConfig oldHub;
	private HubServerConfig newHub;
	private File file1;
	private File file2;
	
	@Before
	public void setup() {
		HubServerConfigBuilder oldHubConfig = new HubServerConfigBuilder();
		oldHubConfig.setHubUrl("http://int-hub01.dc1.lan:8080");
		oldHubConfig.setUsername("sysadmin");
		oldHubConfig.setPassword("blackduck");
		oldHub = oldHubConfig.build();
		
		HubServerConfigBuilder newHubConfig = new HubServerConfigBuilder();
		newHubConfig.setHubUrl("http://int-auto01.dc1.lan:9000");
		newHubConfig.setUsername("sysadmin");
		newHubConfig.setPassword("blackduck");
		newHub = newHubConfig.build();
		
		file1 = new File(resources + "api-docs-3.4.2-test.json");
		file2 = new File(resources + "api-docs-3.5.0-test.json");
	}
	
	@Test
	public void getDiffFromHubTest() {
		HubDiff hubDiff = null;
		try {
			hubDiff = new HubDiff(oldHub, newHub);
		} catch (IllegalArgumentException | EncryptionException | HubIntegrationException | JSONException e) {
			e.printStackTrace();
			Assert.fail("Failed to start test");
		}
		System.out.println(hubDiff.getDiff());
		assertTrue(hubDiff.getDiff() != "" || hubDiff.getDiff() != null);
	}
	
	@Test
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
	
	@Test
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

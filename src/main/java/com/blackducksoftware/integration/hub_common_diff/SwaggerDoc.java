package com.blackducksoftware.integration.hub_common_diff;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class SwaggerDoc {
	private JsonElement swaggerDoc;
	private String version;
	private Gson gson;
	
	public SwaggerDoc(String swaggerDoc, String version) {
		gson = new GsonBuilder().setPrettyPrinting().create();
		setSwaggerDoc(swaggerDoc);
		setVersion(version);
	}
	
	public void setSwaggerDoc(String swaggerDoc) {
		this.swaggerDoc = gson.fromJson(swaggerDoc, JsonElement.class);
	}
	
	public String getSwaggerDoc() {
		return gson.toJson(swaggerDoc);
	}
	
	public void setGsonSwaggerDoc(JsonElement swaggerDoc) {
		this.swaggerDoc = swaggerDoc;
	}
	
	public JsonElement getGsonSwaggerDoc() {
		return this.swaggerDoc;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public JSONCompareResult getDifference(SwaggerDoc other) throws JSONException {
		return JSONCompare.compareJSON(this.getSwaggerDoc(), other.getSwaggerDoc(), JSONCompareMode.STRICT);
	}
	
}

package com.xebia.incubator.xebium;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class VisualAnalyzer {
	
	private static final Logger LOG = LoggerFactory.getLogger(VisualAnalyzer.class);
	private static final String DEFAULT_HOSTNAME = "nohost";
	private static final String DEFAULT_PORT = "7000";
	private static String hostname = DEFAULT_HOSTNAME;
	private static String port = DEFAULT_PORT;
	private static String project;
	private static String suite;
	private static int runId;
	private String browserName = "chrome";
	private static String platformName = "Windows7";
	private static String resolution = "unkown x unknown";
	private static String browserVersion = "45.0 (64-bit)";
	

	/**
	   * Creates a new run on the VisualReview server with the given project name and suite name.
	   *
	   * @return the new run's RunID, which can be used to upload screenshots to
	   * @throws IOException
	   */
	  public void createRun() throws IOException {
		if (DEFAULT_HOSTNAME.equalsIgnoreCase(hostname)) {
			LOG.info("The hostname is 'nohost' VisualAnalyzer acts if it is not configured and will ignore commands.");
			return;
		}
	    CloseableHttpClient httpclient = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("http://" + hostname + ":" + port + "/api/runs");
	    StringEntity input = new StringEntity("{\"projectName\":\"" + project + "\",\"suiteName\":\"" + suite + "\"}");
	    input.setContentType("application/json");
	    LOG.info("httpPost when creating run: " + "http://" + hostname + ":" + port + "/api/runs");
	 
	    httpPost.setEntity(input);
	    CloseableHttpResponse response = httpclient.execute(httpPost);

	    try {
	      LOG.info("response from server when creating run: " + response.getStatusLine());
	      HttpEntity responseEntity = response.getEntity();

	      JsonFactory factory = new JsonFactory();
	      JsonParser parser = factory.createParser(response.getEntity().getContent());
	      if (parser.nextToken() != JsonToken.START_OBJECT) {
	        throw new IOException("Expected data to start with an Object");
	      }

	      while (parser.nextToken() != JsonToken.END_OBJECT) {
	        String fieldName = parser.getCurrentName();
	        parser.nextToken(); // moves to value
	        if (fieldName != null && fieldName.equals("id")) {
	          runId = Integer.parseInt(parser.getValueAsString());
	        }
	      }
	      EntityUtils.consume(responseEntity);
	    } finally {
	      response.close();
	    }
	    if (runId == 0) {
	    	throw new RuntimeException("something went wrong while creating suite.. The runId was still zero");
	    }
	  }
	  
	  public void takeAndSendScreenshot(String screenshotName, String output) throws IOException {
	    if (runId == 0) {
	    	return;
	    }
	  	byte[] screenshotData = Base64.decodeBase64(output);
	  	
	    JsonFactory factory = new JsonFactory();
	    StringWriter jsonString = new StringWriter();
	    JsonGenerator generator = factory.createGenerator(jsonString);
	    generator.writeStartObject();
	    generator.writeStringField("browser", browserName);
	    generator.writeStringField("platform", platformName);
	    generator.writeStringField("resolution", resolution);
	    generator.writeStringField("version", browserVersion);
	    generator.writeEndObject();
	    generator.flush();

	    CloseableHttpClient httpclient = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("http://" + hostname + ":" + port + "/api/runs/" + runId + "/screenshots");

	    HttpEntity input = MultipartEntityBuilder.create()
	        .addBinaryBody("file", screenshotData, ContentType.parse("image/png"), "file.png")
	        .addTextBody("screenshotName", screenshotName, ContentType.TEXT_PLAIN)
	        .addTextBody("properties", jsonString.toString(), ContentType.APPLICATION_JSON)
	        .addTextBody("meta", "{}", ContentType.APPLICATION_JSON)
	        .build();

	    httpPost.setEntity(input);
	    CloseableHttpResponse response = httpclient.execute(httpPost);

	    try {
	    	LOG.info("response from server when uploading screenshot: " + response.getStatusLine());
	    } finally {
	      response.close();
	    }
	  }	  
	  
	public void setProject(String project) {
		VisualAnalyzer.project = project;
	}

	public void setSuite(String suite) {
		VisualAnalyzer.suite = suite;
	}

	public void setHost(String hostname) {
		VisualAnalyzer.hostname = hostname;
	}

	public void setPort(String port) {
		VisualAnalyzer.port = port;
	}

	public boolean isInitialized(String project, String suite) {
		return VisualAnalyzer.runId != 0 && project.equals(VisualAnalyzer.project) && suite.equals(VisualAnalyzer.suite);
	}

	public void setBrowser(String browser) {
		this.browserName = browser;
		
	}

	public void setSize(Dimension size) {
		resolution = size.getHeight() + " x " + size.getWidth();
	}
}

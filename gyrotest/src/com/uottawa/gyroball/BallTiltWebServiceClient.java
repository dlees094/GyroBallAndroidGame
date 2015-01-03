package com.uottawa.gyroball;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class BallTiltWebServiceClient {

	private RestTemplate restTemplate = new RestTemplate();
	private String baseUrl = "http://137.122.91.175:8080/";
	private static BallTiltWebServiceClient singleton;

	private BallTiltWebServiceClient() {
		// do nothing.
	}

	public static BallTiltWebServiceClient getInstance() {
		if(singleton == null) {
			singleton = new BallTiltWebServiceClient();
			singleton.getRestTemplate().getMessageConverters().add(new GsonHttpMessageConverter());
			return singleton;
		} else {
			return singleton; 
		}
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public Integer submitScore(int score) {
		String url = baseUrl + "score/" + score;
		Map<String, Integer> vars = new HashMap<String, Integer>();
		vars.put("score", score);
		Rank result;
		try {
			result = restTemplate.getForObject(url, Rank.class, vars);
			return result.rank;
		} catch (RestClientException e) {
			// TODO Auto-generated catch block
			System.out.println("url: " + url);
			System.out.println("vars: " + vars);
			e.printStackTrace();
		}
		return -1;

	}
}
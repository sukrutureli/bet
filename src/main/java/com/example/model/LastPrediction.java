package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class LastPrediction {
	private String homeTeam;
	private String awayTeam;
	private List<String> predictions;
	
	public LastPrediction(String homeTeam, String awayTeam) {
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
		this.predictions = new ArrayList<String>();
	}
	public String getHomeTeam() {
		return homeTeam;
	}
	public void setHomeTeam(String homeTeam) {
		this.homeTeam = homeTeam;
	}
	public String getAwayTeam() {
		return awayTeam;
	}
	public void setAwayTeam(String awayTeam) {
		this.awayTeam = awayTeam;
	}
	public List<String> getPredictions() {
		return predictions;
	}
	public void setPredictions(List<String> predictions) {
		this.predictions = predictions;
	}
	
	public String preditionsToString() {
		String result = "";
		
		for (String s:predictions) {
			if (result.equals("")) {
				result += s;
			} else {
				result += " | " + s;
			}
		}
		
		return result;
	}
}

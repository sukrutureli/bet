package com.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

// Tek maç sonucu
public class MatchResult {
    private String homeTeam;
    private String awayTeam;
    private int homeScore;
    private int awayScore;
    private String matchDate;
    private String tournament;
    private String season;
    private String matchType; // "rekabet-gecmisi" veya "son-maclar"
    private String originalUrl;
    private String status; // "finished", "cancelled", "postponed" etc.
    
    public MatchResult(String homeTeam, String awayTeam, int homeScore, int awayScore, 
                      String matchDate, String tournament, String matchType, String originalUrl) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.matchDate = matchDate;
        this.tournament = tournament;
        this.matchType = matchType;
        this.originalUrl = originalUrl;
        this.status = "finished";
    }
    
    // Getters
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public String getMatchDate() { return matchDate; }
    public String getTournament() { return tournament; }
    public String getSeason() { return season; }
    public String getMatchType() { return matchType; }
    public String getOriginalUrl() { return originalUrl; }
    public String getStatus() { return status; }
    
    // Setters
    public void setSeason(String season) { this.season = season; }
    public void setStatus(String status) { this.status = status; }
    
    // Utility methods
    public String getResult() {
        if (homeScore > awayScore) return "H"; // Home win
        if (awayScore > homeScore) return "A"; // Away win
        return "D"; // Draw
    }
    
    public int getTotalGoals() {
        return homeScore + awayScore;
    }
    
    public String getScoreString() {
        return homeScore + "-" + awayScore;
    }
    
    public String getMatchString() {
        return homeTeam + " vs " + awayTeam;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s %d-%d %s [%s] (%s)", 
            matchDate, homeTeam, homeScore, awayScore, awayTeam, tournament, matchType);
    }
    
    // JSON-like string representation
    public String toJsonString() {
        return String.format(
            "{\"homeTeam\":\"%s\",\"awayTeam\":\"%s\",\"homeScore\":%d,\"awayScore\":%d," +
            "\"matchDate\":\"%s\",\"tournament\":\"%s\",\"matchType\":\"%s\",\"result\":\"%s\"}",
            homeTeam, awayTeam, homeScore, awayScore, matchDate, tournament, matchType, getResult()
        );
    }
}
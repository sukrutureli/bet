package com.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

// Tek maç sonucu
class MatchResult {
    private String homeTeam;
    private String awayTeam;
    private int homeScore;
    private int awayScore;
    private LocalDate matchDate;
    private String tournament;
    private String season;
    private String matchType; // "rekabet-gecmisi" veya "son-maclar"
    private String originalUrl;
    private String status; // "finished", "cancelled", "postponed" etc.
    
    public MatchResult(String homeTeam, String awayTeam, int homeScore, int awayScore, 
                      LocalDate matchDate, String tournament, String matchType, String originalUrl) {
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
    public LocalDate getMatchDate() { return matchDate; }
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

// Bir takımın tüm maç sonuçlarını tutan class
class TeamMatchHistory {
    private String teamName;
    private String originalMatchUrl;
    private List<MatchResult> rekabetGecmisi;
    private List<MatchResult> sonMaclar;
    private LocalDateTime lastUpdated;
    
    public TeamMatchHistory(String teamName, String originalMatchUrl) {
        this.teamName = teamName;
        this.originalMatchUrl = originalMatchUrl;
        this.rekabetGecmisi = new ArrayList<>();
        this.sonMaclar = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Add methods
    public void addRekabetGecmisiMatch(MatchResult match) {
        rekabetGecmisi.add(match);
    }
    
    public void addSonMacMatch(MatchResult match) {
        sonMaclar.add(match);
    }
    
    // Getters
    public String getTeamName() { return teamName; }
    public String getOriginalMatchUrl() { return originalMatchUrl; }
    public List<MatchResult> getRekabetGecmisi() { return rekabetGecmisi; }
    public List<MatchResult> getSonMaclar() { return sonMaclar; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    // Utility methods
    public int getTotalMatches() {
        return rekabetGecmisi.size() + sonMaclar.size();
    }
    
    public List<MatchResult> getAllMatches() {
        List<MatchResult> allMatches = new ArrayList<>();
        allMatches.addAll(rekabetGecmisi);
        allMatches.addAll(sonMaclar);
        return allMatches;
    }
    
    public int getWinCount() {
        return (int) getAllMatches().stream()
            .filter(match -> {
                String result = match.getResult();
                return (match.getHomeTeam().contains(teamName) && result.equals("H")) ||
                       (match.getAwayTeam().contains(teamName) && result.equals("A"));
            })
            .count();
    }
    
    public int getDrawCount() {
        return (int) getAllMatches().stream()
            .filter(match -> match.getResult().equals("D"))
            .count();
    }
    
    public int getLossCount() {
        return getTotalMatches() - getWinCount() - getDrawCount();
    }
    
    public double getWinRate() {
        if (getTotalMatches() == 0) return 0.0;
        return (double) getWinCount() / getTotalMatches() * 100;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %d maç (Rekabet: %d, Son Maçlar: %d) - G:%d B:%d M:%d (%.1f%%)",
            teamName, getTotalMatches(), rekabetGecmisi.size(), sonMaclar.size(),
            getWinCount(), getDrawCount(), getLossCount(), getWinRate());
    }
}

// Tüm takım geçmişlerini yöneten ana class
class MatchHistoryManager {
    private List<TeamMatchHistory> teamHistories;
    private LocalDateTime createdAt;
    
    public MatchHistoryManager() {
        this.teamHistories = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }
    
    public void addTeamHistory(TeamMatchHistory teamHistory) {
        teamHistories.add(teamHistory);
    }
    
    public List<TeamMatchHistory> getTeamHistories() {
        return teamHistories;
    }
    
    public TeamMatchHistory getTeamHistory(String teamName) {
        return teamHistories.stream()
            .filter(th -> th.getTeamName().equals(teamName))
            .findFirst()
            .orElse(null);
    }
    
    public int getTotalTeams() {
        return teamHistories.size();
    }
    
    public int getTotalMatches() {
        return teamHistories.stream()
            .mapToInt(TeamMatchHistory::getTotalMatches)
            .sum();
    }
    
    public List<MatchResult> getAllMatches() {
        List<MatchResult> allMatches = new ArrayList<>();
        for (TeamMatchHistory teamHistory : teamHistories) {
            allMatches.addAll(teamHistory.getAllMatches());
        }
        return allMatches;
    }
    
    // CSV export için
    public String toCsvString() {
        StringBuilder csv = new StringBuilder();
        csv.append("HomeTeam,AwayTeam,HomeScore,AwayScore,MatchDate,Tournament,MatchType,Result\n");
        
        for (MatchResult match : getAllMatches()) {
            csv.append(String.format("%s,%s,%d,%d,%s,%s,%s,%s\n",
                match.getHomeTeam(), match.getAwayTeam(), match.getHomeScore(), match.getAwayScore(),
                match.getMatchDate(), match.getTournament(), match.getMatchType(), match.getResult()));
        }
        
        return csv.toString();
    }
    
    // JSON export için
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"createdAt\": \"").append(createdAt).append("\",\n");
        json.append("  \"totalTeams\": ").append(getTotalTeams()).append(",\n");
        json.append("  \"totalMatches\": ").append(getTotalMatches()).append(",\n");
        json.append("  \"teams\": [\n");
        
        for (int i = 0; i < teamHistories.size(); i++) {
            TeamMatchHistory team = teamHistories.get(i);
            json.append("    {\n");
            json.append("      \"teamName\": \"").append(team.getTeamName()).append("\",\n");
            json.append("      \"totalMatches\": ").append(team.getTotalMatches()).append(",\n");
            json.append("      \"winRate\": ").append(String.format("%.1f", team.getWinRate())).append(",\n");
            json.append("      \"matches\": [\n");
            
            List<MatchResult> matches = team.getAllMatches();
            for (int j = 0; j < matches.size(); j++) {
                json.append("        ").append(matches.get(j).toJsonString());
                if (j < matches.size() - 1) json.append(",");
                json.append("\n");
            }
            
            json.append("      ]\n");
            json.append("    }");
            if (i < teamHistories.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    @Override
    public String toString() {
        return String.format("MatchHistoryManager: %d takım, %d maç (Oluşturma: %s)",
            getTotalTeams(), getTotalMatches(), createdAt);
    }
}
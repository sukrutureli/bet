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

// Bir takımın tüm maç sonuçlarını tutan class
class TeamMatchHistory {
    private String teamName;
    private String teamEv;
    private String teamDep;
    private String originalMatchUrl;
    private List<MatchResult> rekabetGecmisi;
    private List<MatchResult> sonMaclarHome;
    private List<MatchResult> sonMaclarAway;
    private LocalDateTime lastUpdated;
    
    public TeamMatchHistory(String teamName,String teamEv, String teamDep, String originalMatchUrl) {
        this.teamName = teamName;
        this.teamEv = teamEv;
        this.teamDep = teamDep;
        this.originalMatchUrl = originalMatchUrl;
        this.rekabetGecmisi = new ArrayList<>();
        this.sonMaclarHome = new ArrayList<>();
        this.sonMaclarAway = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Add methods
    public void addRekabetGecmisiMatch(MatchResult match) {
        rekabetGecmisi.add(match);
    }
    
    public void addSonMacMatch(MatchResult match, int homeOrAway) {
        if (homeOrAway == 1) {
            sonMaclarHome.add(match);
        } else if (homeOrAway == 2) {
            sonMaclarAway.add(match);
        } 
    }
    
    // Getters
    public String getTeamName() { return teamName; }
    public String getOriginalMatchUrl() { return originalMatchUrl; }
    public List<MatchResult> getRekabetGecmisi() { return rekabetGecmisi; }
    public List<MatchResult> getSonMaclar(int homeOrAway) { 
        if (homeOrAway == 1) {
            return sonMaclarHome;
        } else if (homeOrAway == 2) {
            return sonMaclarAway;
        }  
        return null;
    }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    // Utility methods
    public int getTotalMatches() {
        return rekabetGecmisi.size() + sonMaclarHome.size() + sonMaclarAway.size();
    }
    
    public List<MatchResult> getAllMatches() {
        List<MatchResult> allMatches = new ArrayList<>();
        allMatches.addAll(rekabetGecmisi);
        allMatches.addAll(sonMaclarHome);
        allMatches.addAll(sonMaclarAway);
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

    public int getTotalMatchesIn10() {
        return Math.min(10, rekabetGecmisi.size()) + 
                    Math.min(10, sonMaclarHome.size()) +
                    Math.min(10, sonMaclarAway.size());
    }

    public int getMs1() {
        int ms1 = 0;
        for (int i = 0; i < Math.min(10, rekabetGecmisi.size()); i++) {
            if (rekabetGecmisi.get(i).getHomeTeam().contains(teamEv) && rekabetGecmisi.get(i).getResult() == "H") {
                ms1++;
            } else if (rekabetGecmisi.get(i).getAwayTeam().contains(teamEv) && rekabetGecmisi.get(i).getResult() == "A") {
                ms1++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarHome.size()); i++) {
            if (sonMaclarHome.get(i).getHomeTeam().contains(teamEv) && sonMaclarHome.get(i).getResult() == "H") {
                ms1++;
            } else if (sonMaclarHome.get(i).getAwayTeam().contains(teamEv) && sonMaclarHome.get(i).getResult() == "A") {
                ms1++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarAway.size()); i++) {
            if (sonMaclarAway.get(i).getHomeTeam().contains(teamDep) && sonMaclarAway.get(i).getResult() == "A") {
                ms1++;
            } else if (sonMaclarAway.get(i).getAwayTeam().contains(teamDep) && sonMaclarAway.get(i).getResult() == "H") {
                ms1++;
            } 
        }
        return ms1;
    }

    public int getMs2() {
        int ms2 = 0;
        for (int i = 0; i < Math.min(10, rekabetGecmisi.size()); i++) {
            if (rekabetGecmisi.get(i).getHomeTeam().contains(teamDep) && rekabetGecmisi.get(i).getResult() == "H") {
                ms2++;
            } else if (rekabetGecmisi.get(i).getAwayTeam().contains(teamDep) && rekabetGecmisi.get(i).getResult() == "A") {
                ms2++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarHome.size()); i++) {
            if (sonMaclarHome.get(i).getHomeTeam().contains(teamEv) && sonMaclarHome.get(i).getResult() == "A") {
                ms2++;
            } else if (sonMaclarHome.get(i).getAwayTeam().contains(teamEv) && sonMaclarHome.get(i).getResult() == "H") {
                ms2++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarAway.size()); i++) {
            if (sonMaclarAway.get(i).getHomeTeam().contains(teamDep) && sonMaclarAway.get(i).getResult() == "H") {
                ms2++;
            } else if (sonMaclarAway.get(i).getAwayTeam().contains(teamDep) && sonMaclarAway.get(i).getResult() == "A") {
                ms2++;
            } 
        }

        return ms2;
    }

    public int getMs0() {
        return getTotalMatchesIn10() - getMs1() - getMs2();
    }

    public int getVar() {
        int var = 0;

        for (int i = 0; i < Math.min(10, rekabetGecmisi.size()); i++) {
            if (rekabetGecmisi.get(i).getHomeScore() > 0 &&
                 rekabetGecmisi.get(i).getAwayScore() > 0) {
                var++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarHome.size()); i++) {
            if (sonMaclarHome.get(i).getHomeScore() > 0 &&
                 sonMaclarHome.get(i).getAwayScore() > 0) {
                var++;
            }  
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarAway.size()); i++) {
            if (sonMaclarAway.get(i).getHomeScore() > 0 &&
                 sonMaclarAway.get(i).getAwayScore() > 0) {
                var++;
            } 
        }
        return var;
    }

    public int getYok() {
        return getTotalMatchesIn10() - getVar();
    }

    public int getUst() {
        int ust = 0;

        for (int i = 0; i < Math.min(10, rekabetGecmisi.size()); i++) {
            if ((rekabetGecmisi.get(i).getHomeScore() + rekabetGecmisi.get(i).getAwayScore()) > 2) {
                ust++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarHome.size()); i++) {
            if ((sonMaclarHome.get(i).getHomeScore() + sonMaclarHome.get(i).getAwayScore()) > 2) {
                ust++;
            } 
        }
        
        for (int i = 0; i < Math.min(10, sonMaclarAway.size()); i++) {
            if ((sonMaclarAway.get(i).getHomeScore() + sonMaclarAway.get(i).getAwayScore()) > 2) {
                ust++;
            } 
        }
        return ust;
    }

    public int getAlt() {
        return getTotalMatchesIn10() - getUst();
    }

    public String toStringAsPercentage(int value, String type) {
        return type + " : %" + ((int)(((value * 1.0) / getTotalMatchesIn10()) * 100));
    }

    public String getStyle(int value, String type) {
        String color = "background-color: #c8facc;";

        if (type.startsWith("MS")) {
            if (value >= 50) {
                return color;
            }
        } else {
            if (value >= 70) {
                return color;
            }
        }
        return "";
    }
    
    @Override
    public String toString() {
        return String.format("%s: %d maç (Rekabet: %d, Son Maçlar Home: %d, Son Maçlar Away: %d) - G:%d B:%d M:%d (%.1f%%)",
            teamName, getTotalMatches(), rekabetGecmisi.size(), sonMaclarHome.size(), sonMaclarAway.size(),
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
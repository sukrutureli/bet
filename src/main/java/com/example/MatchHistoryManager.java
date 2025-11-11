package com.example;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.model.MatchResult;
import com.example.model.TeamMatchHistory;

public class MatchHistoryManager {
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
		return teamHistories.stream().filter(th -> th.getTeamName().equals(teamName)).findFirst().orElse(null);
	}

	public int getTotalTeams() {
		return teamHistories.size();
	}

	public int getTotalMatches() {
		return teamHistories.stream().mapToInt(TeamMatchHistory::getTotalMatches).sum();
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
			csv.append(String.format("%s,%s,%d,%d,%s,%s,%s,%s\n", match.getHomeTeam(), match.getAwayTeam(),
					match.getHomeScore(), match.getAwayScore(), match.getMatchDate(), match.getTournament(),
					match.getMatchType(), match.getResult()));
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
				if (j < matches.size() - 1)
					json.append(",");
				json.append("\n");
			}

			json.append("      ]\n");
			json.append("    }");
			if (i < teamHistories.size() - 1)
				json.append(",");
			json.append("\n");
		}

		json.append("  ]\n");
		json.append("}");

		return json.toString();
	}

	@Override
	public String toString() {
		return String.format("MatchHistoryManager: %d takım, %d maç (Oluşturma: %s)", getTotalTeams(),
				getTotalMatches(), createdAt);
	}
}

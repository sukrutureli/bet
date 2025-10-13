package com.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.model.TeamStats;

public class TeamMatchHistory {
	private String teamName;
	private String teamEv;
	private String teamDep;
	private String originalMatchUrl;
	private List<MatchResult> rekabetGecmisi;
	private List<MatchResult> sonMaclarHome;
	private List<MatchResult> sonMaclarAway;
	private LocalDateTime lastUpdated;

	public TeamMatchHistory(String teamName, String teamEv, String teamDep, String originalMatchUrl) {
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
	public String getTeamName() {
		return teamName;
	}

	public String getOriginalMatchUrl() {
		return originalMatchUrl;
	}

	public List<MatchResult> getRekabetGecmisi() {
		return rekabetGecmisi;
	}

	public List<MatchResult> getSonMaclar(int homeOrAway) {
		if (homeOrAway == 1) {
			return sonMaclarHome;
		} else if (homeOrAway == 2) {
			return sonMaclarAway;
		}
		return null;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

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
		return (int) getAllMatches().stream().filter(match -> {
			String result = match.getResult();
			return (match.getHomeTeam().contains(teamName) && result.equals("H"))
					|| (match.getAwayTeam().contains(teamName) && result.equals("A"));
		}).count();
	}

	public int getDrawCount() {
		return (int) getAllMatches().stream().filter(match -> match.getResult().equals("D")).count();
	}

	public int getLossCount() {
		return getTotalMatches() - getWinCount() - getDrawCount();
	}

	public double getWinRate() {
		if (getTotalMatches() == 0)
			return 0.0;
		return (double) getWinCount() / getTotalMatches() * 100;
	}

	public int getTotalMatchesIn10() {
		return Math.min(10, rekabetGecmisi.size()) + Math.min(10, sonMaclarHome.size())
				+ Math.min(10, sonMaclarAway.size());
	}

	public int getMs1() {
		int ms1 = 0;
		for (int i = 0; i < Math.min(10, rekabetGecmisi.size()); i++) {
			if (rekabetGecmisi.get(i).getHomeTeam().contains(teamEv) && rekabetGecmisi.get(i).getResult() == "H") {
				ms1++;
			} else if (rekabetGecmisi.get(i).getAwayTeam().contains(teamEv)
					&& rekabetGecmisi.get(i).getResult() == "A") {
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
			} else if (sonMaclarAway.get(i).getAwayTeam().contains(teamDep)
					&& sonMaclarAway.get(i).getResult() == "H") {
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
			} else if (rekabetGecmisi.get(i).getAwayTeam().contains(teamDep)
					&& rekabetGecmisi.get(i).getResult() == "A") {
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
			} else if (sonMaclarAway.get(i).getAwayTeam().contains(teamDep)
					&& sonMaclarAway.get(i).getResult() == "A") {
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
			if (rekabetGecmisi.get(i).getHomeScore() > 0 && rekabetGecmisi.get(i).getAwayScore() > 0) {
				var++;
			}
		}

		for (int i = 0; i < Math.min(10, sonMaclarHome.size()); i++) {
			if (sonMaclarHome.get(i).getHomeScore() > 0 && sonMaclarHome.get(i).getAwayScore() > 0) {
				var++;
			}
		}

		for (int i = 0; i < Math.min(10, sonMaclarAway.size()); i++) {
			if (sonMaclarAway.get(i).getHomeScore() > 0 && sonMaclarAway.get(i).getAwayScore() > 0) {
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
		return type + " : %" + ((int) (((value * 1.0) / getTotalMatchesIn10()) * 100));
	}

	public String getStyle(int value, String type) {
		String color = "background-color: #c8facc;";
		int percentage = ((int) (((value * 1.0) / getTotalMatchesIn10()) * 100));

		if (type.startsWith("MS")) {
			if (percentage >= 50) {
				return color;
			}
		} else {
			if (percentage >= 70) {
				return color;
			}
		}
		return "";
	}

	public Match createMatch(MatchInfo pMatch) {
		Match currentMatch = new Match(teamEv, teamDep);
		currentMatch.setOdds(pMatch.getOdds());

		TeamStats homeStats = new TeamStats();
		homeStats.setLast5Points(getPoints(sonMaclarHome, teamEv));
		homeStats.setLast5Count(Math.min(5, sonMaclarHome.size()));
		homeStats.setAvgGF(getGFandGA(sonMaclarHome, teamEv)[0]);
		homeStats.setAvgGA(getGFandGA(sonMaclarHome, teamEv)[1]);
		homeStats.setRating100(getRating(homeStats.getAvgGF(), homeStats.getAvgGA(), homeStats.getLast5Points()));
		homeStats.setH2hCount(Math.min(5, rekabetGecmisi.size()));
		homeStats.setH2hWins(getWinCount(rekabetGecmisi, teamEv));

		TeamStats awayStats = new TeamStats();
		awayStats.setLast5Points(getPoints(sonMaclarAway, teamDep));
		awayStats.setLast5Count(Math.min(5, sonMaclarAway.size()));
		awayStats.setAvgGF(getGFandGA(sonMaclarAway, teamDep)[0]);
		awayStats.setAvgGA(getGFandGA(sonMaclarAway, teamDep)[1]);
		awayStats.setRating100(getRating(awayStats.getAvgGF(), awayStats.getAvgGA(), awayStats.getLast5Points()));
		awayStats.setH2hCount(Math.min(5, rekabetGecmisi.size()));
		awayStats.setH2hWins(getWinCount(rekabetGecmisi, teamDep));

		currentMatch.setHomeStats(homeStats);
		currentMatch.setAwayStats(awayStats);

		return currentMatch;
	}

	private int getWinCount(List<MatchResult> macResult, String teamName) {
		int winCount = 0;

		for (int i = 0; i < Math.min(5, macResult.size()); i++) {
			if (macResult.get(i).getHomeTeam().contains(teamName) && macResult.get(i).getResult() == "H") {
				winCount++;
			} else if (macResult.get(i).getAwayTeam().contains(teamName) && macResult.get(i).getResult() == "A") {
				winCount++;
			}
		}

		return winCount;
	}

	public int getPoints(List<MatchResult> macResult, String teamName) {
		int points = 0;

		for (int i = 0; i < Math.min(5, macResult.size()); i++) {
			if (macResult.get(i).getHomeTeam().contains(teamName) && macResult.get(i).getResult() == "H") {
				points += 3;
			} else if (macResult.get(i).getAwayTeam().contains(teamName) && macResult.get(i).getResult() == "A") {
				points += 3;
			} else if (macResult.get(i).getResult() == "D") {
				points += 1;
			}
		}

		return points;
	}

	public double[] getGFandGA(List<MatchResult> macResult, String teamName) {
		double[] goals = { 0.0, 0.0 };

		int size = Math.min(10, macResult.size());

		for (int i = 0; i < size; i++) {
			if (macResult.get(i).getHomeTeam().contains(teamName)) {
				goals[0] += macResult.get(i).getHomeScore();
				goals[1] += macResult.get(i).getAwayScore();
			} else if (macResult.get(i).getAwayTeam().contains(teamName)) {
				goals[1] += macResult.get(i).getHomeScore();
				goals[0] += macResult.get(i).getAwayScore();
			}
		}

		if (size != 0) {
			goals[0] /= size;
			goals[1] /= size;
		}

		return goals;
	}

	public int getRating(double gf, double ga, double ppg) {
		double share = gf + ga > 0 ? (gf / (gf + ga)) : 0.5;
		double rating = (ppg / 3.0) * 60.0 + share * 40.0;

		return (int) Math.round(clamp(rating, 0, 100));
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	@Override
	public String toString() {
		return String.format(
				"%s: %d maç (Rekabet: %d, Son Maçlar Home: %d, Son Maçlar Away: %d) - G:%d B:%d M:%d (%.1f%%)",
				teamName, getTotalMatches(), rekabetGecmisi.size(), sonMaclarHome.size(), sonMaclarAway.size(),
				getWinCount(), getDrawCount(), getLossCount(), getWinRate());
	}
}

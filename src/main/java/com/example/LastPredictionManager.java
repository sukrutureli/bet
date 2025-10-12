package com.example;

import java.util.ArrayList;
import java.util.List;

import com.example.model.LastPrediction;
import com.example.model.MatchInfo;
import com.example.model.PredictionResult;
import com.example.model.TeamMatchHistory;

public class LastPredictionManager {
	private List<LastPrediction> lastPrediction;
	private MatchHistoryManager historyManager;
	private List<PredictionResult> predictionResults;
	private List<MatchInfo> matchInfo;

	public LastPredictionManager(MatchHistoryManager historyManager, List<PredictionResult> predictionResults,
			List<MatchInfo> matchInfo) {
		this.lastPrediction = new ArrayList<LastPrediction>();
		this.historyManager = historyManager;
		this.predictionResults = predictionResults;
		this.matchInfo = matchInfo;
	}

	public void fillPredictions() {
		for (int i = 0; i < historyManager.getTeamHistories().size(); i++) {
			String[] splitTeamNames = historyManager.getTeamHistories().get(i).getTeamName().split("-");
			if (splitTeamNames[0].trim().equals("") || splitTeamNames[1].trim().equals("")) {
				continue;
			}
			LastPrediction tempLastPrediction = new LastPrediction(splitTeamNames[0].trim(), splitTeamNames[1].trim());
			String[] tahminListesi = { "MS1", "MS2", "Üst", "Alt", "Var", "Yok" };
			
			for (String s:tahminListesi) {
				if (calculatePrediction(historyManager.getTeamHistories().get(i), predictionResults.get(i),
						matchInfo.get(i), s) != null) {
					tempLastPrediction.getPredictions().add(s);
				}
			}
			
			if (!tempLastPrediction.getPredictions().isEmpty()) {
				lastPrediction.add(tempLastPrediction);
			}
		}
	}

	private String calculatePrediction(TeamMatchHistory h, PredictionResult pr, MatchInfo matchInfo, String tahmin) {
		double percentageH = 0;
		double percentagePR = 0;

		if (h.getRekabetGecmisi().size() == 0 || h.getSonMaclar(1).size() == 0 || h.getRekabetGecmisi().size() == 0) {
			return null;
		}

		if (tahmin.equals("MS1")) {
			percentageH = ((h.getMs1() * 1.0) / h.getTotalMatchesIn10()) * 100;
			percentagePR = pr.getpHome() * 100;
			if (matchInfo.getOdds().getMs1() > 1.30 && percentageH > 50 && percentagePR > 50
					&& isScoreOk(pr.getScoreline(), "MS1")) {
				return "MS1";
			}
		} else if (tahmin.equals("MS2")) {
			percentageH = ((h.getMs2() * 1.0) / h.getTotalMatchesIn10()) * 100;
			percentagePR = pr.getpAway() * 100;
			if (matchInfo.getOdds().getMs2() > 1.30 && percentageH > 50 && percentagePR > 50
					&& isScoreOk(pr.getScoreline(), "MS2")) {
				return "MS2";
			}
		} else if (tahmin.equals("Üst")) {
			percentageH = ((h.getUst() * 1.0) / h.getTotalMatchesIn10()) * 100;
			percentagePR = pr.getpOver25() * 100;
			if (percentageH > 70 && percentagePR > 70 && isScoreOk(pr.getScoreline(), "Üst")) {
				return "Üst";
			}
			if (percentagePR > 80 && isScoreOk(pr.getScoreline(), "Üst")) {
				return "Üst";
			}
		} else if (tahmin.equals("Alt")) {
			percentageH = ((h.getAlt() * 1.0) / h.getTotalMatchesIn10()) * 100;
			percentagePR = (1 - pr.getpOver25()) * 100;
			if (percentageH > 70 && percentagePR > 70 && isScoreOk(pr.getScoreline(), "Alt")) {
				return "Alt";
			}
			if (percentagePR > 80 && isScoreOk(pr.getScoreline(), "Alt")) {
				return "Alt";
			}
		} else if (tahmin.equals("Var")) {
			percentageH = ((h.getVar() * 1.0) / h.getTotalMatchesIn10()) * 100;
			percentagePR = pr.getpBttsYes() * 100;
			if (matchInfo.getOdds().getMs1() > 1.50 && matchInfo.getOdds().getMs2() > 1.50) {
				if (percentageH > 70 && percentagePR > 70 && isScoreOk(pr.getScoreline(), "Var")) {
					return "Var";
				}
				if (percentagePR > 80 && isScoreOk(pr.getScoreline(), "Var")) {
					return "Var";
				}
			}
		} else if (tahmin.equals("Yok")) {
			percentageH = ((h.getYok() * 1.0) / h.getTotalMatchesIn10()) * 100;
			percentagePR = (1 - pr.getpBttsYes()) * 100;
			if (percentageH > 70 && percentagePR > 70 && isScoreOk(pr.getScoreline(), "Yok")) {
				return "Yok";
			}
			if (percentagePR > 80 && isScoreOk(pr.getScoreline(), "Yok")) {
				return "Yok";
			}
		}

		return null;
	}

	private boolean isScoreOk(String score, String tahmin) {
		String[] splitScore = score.split("-");
		int home = Integer.valueOf(splitScore[0].trim());
		int away = Integer.valueOf(splitScore[1].trim());

		if (tahmin.equals("MS1") && home > away) {
			return true;
		} else if (tahmin.equals("MS2") && home < away) {
			return true;
		} else if (tahmin.equals("Alt") && (home + away) < 3) {
			return true;
		} else if (tahmin.equals("Üst") && (home + away) > 2) {
			return true;
		} else if (tahmin.equals("Var") && home > 0 && away > 0) {
			return true;
		} else if (tahmin.equals("Yok") && (home == 0 || away == 0)) {
			return true;
		} else {
			return false;
		}

	}

	public List<LastPrediction> getLastPrediction() {
		return lastPrediction;
	}

}

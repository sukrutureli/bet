package com.example;

import java.util.ArrayList;
import java.util.List;

import com.example.model.LastPrediction;
import com.example.model.MatchInfo;
import com.example.model.PredictionData;
import com.example.model.PredictionResult;
import com.example.model.TeamMatchHistory;

public class LastPredictionManager {
	private List<LastPrediction> lastPrediction;
	private MatchHistoryManager historyManager;
	private List<PredictionResult> predictionResults;
	private List<MatchInfo> matchInfo;
	private List<PredictionData> predictionData;

	public LastPredictionManager(MatchHistoryManager historyManager, List<PredictionResult> predictionResults,
			List<MatchInfo> matchInfo) {
		this.lastPrediction = new ArrayList<LastPrediction>();
		this.historyManager = historyManager;
		this.predictionResults = predictionResults;
		this.matchInfo = matchInfo;
		this.predictionData = new ArrayList<PredictionData>();
	}

	public void fillPredictions() {
		for (int i = 0; i < historyManager.getTeamHistories().size(); i++) {
			TeamMatchHistory th = historyManager.getTeamHistories().get(i);

			LastPrediction tempLastPrediction = new LastPrediction(matchInfo.get(i).getName(),
					matchInfo.get(i).getTime());
			String[] tahminListesi = { "MS1", "MS2", "Üst", "Alt", "Var", "Yok" };

			tempLastPrediction.setScore(predictionResults.get(i).getScoreline());
			tempLastPrediction.setMbs(matchInfo.get(i).getOdds().getMbs());

			for (String s : tahminListesi) {
				if (calculatePrediction(th, predictionResults.get(i), matchInfo.get(i), s) != null) {
					String withOdd = s + getOddsAndPercentage(s, matchInfo.get(i), predictionResults.get(i));
					tempLastPrediction.getPredictions().add(withOdd);
				}
			}

			if (!tempLastPrediction.getPredictions().isEmpty()) {
				lastPrediction.add(tempLastPrediction);

				String homeTeam = tempLastPrediction.getName().split("-")[0].trim();
				String awayTeam = tempLastPrediction.getName().split("-")[1].trim();
				PredictionData tempPredictionData = new PredictionData(homeTeam, awayTeam,
						tempLastPrediction.getPredictions());
				predictionData.add(tempPredictionData);
			}
		}
	}

	private String calculatePrediction(TeamMatchHistory h, PredictionResult pr, MatchInfo matchInfo, String tahmin) {
		double percentageH = 0;
		double percentagePR = 0;

		if (!h.isInfoEnough()) {
			return null;
		}

		if (tahmin.equals("MS1")) {
			percentageH = h.getMs1() * 100;
			percentagePR = pr.getpHome() * 100;
			if (matchInfo.getOdds().getMs1() > 0.0 && percentageH >= 50 && percentagePR >= 55
					&& isScoreOk(pr.getScoreline(), "MS1")) {
				return "MS1";
			}
		} else if (tahmin.equals("MS2")) {
			percentageH = h.getMs2() * 100;
			percentagePR = pr.getpAway() * 100;
			if (matchInfo.getOdds().getMs2() > 0.0 && percentageH >= 50 && percentagePR >= 55
					&& isScoreOk(pr.getScoreline(), "MS2")) {
				return "MS2";
			}
		} else if (tahmin.equals("Üst")) {
			percentageH = h.getUst() * 100;
			percentagePR = pr.getpOver25() * 100;
			if (matchInfo.getOdds().getOver25() > 0.0) {
				if (percentageH >= 60 && percentagePR >= 65 && isScoreOk(pr.getScoreline(), "Üst")) {
					return "Üst";
				}
			}
		} else if (tahmin.equals("Alt")) {
			percentageH = h.getAlt() * 100;
			percentagePR = (1 - pr.getpOver25()) * 100;
			if (matchInfo.getOdds().getUnder25() > 0.0) {
				if (percentageH >= 60 && percentagePR >= 65 && isScoreOk(pr.getScoreline(), "Alt")) {
					return "Alt";
				}
			}
		} else if (tahmin.equals("Var")) {
			percentageH = h.getVar() * 100;
			percentagePR = pr.getpBttsYes() * 100;
			if (matchInfo.getOdds().getBttsYes() > 0.0) {
				if (matchInfo.getOdds().getMs1() > 1.5 && matchInfo.getOdds().getMs2() > 1.5) {
					if (percentageH >= 60 && percentagePR >= 65 && isScoreOk(pr.getScoreline(), "Var")) {
						return "Var";
					}
				}
			}
		} else if (tahmin.equals("Yok")) {
			percentageH = h.getYok() * 100;
			percentagePR = (1 - pr.getpBttsYes()) * 100;
			if (matchInfo.getOdds().getBttsNo() > 0.0) {
				if (percentageH >= 60 && percentagePR >= 65 && isScoreOk(pr.getScoreline(), "Yok")) {
					return "Yok";
				}
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

	private String getOddsAndPercentage(String tahmin, MatchInfo match, PredictionResult pr) {
		if (tahmin.equals("MS1")) {
			return " (" + String.valueOf(match.getOdds().getMs1()) + " - %" + ((int) (pr.getpHome() * 100)) + ")";
		} else if (tahmin.equals("MS2")) {
			return " (" + String.valueOf(match.getOdds().getMs2()) + " - %" + ((int) (pr.getpAway() * 100)) + ")";
		} else if (tahmin.equals("Alt")) {
			return " (" + String.valueOf(match.getOdds().getUnder25()) + " - %" + ((int) ((1 - pr.getpOver25()) * 100))
					+ ")";
		} else if (tahmin.equals("Üst")) {
			return " (" + String.valueOf(match.getOdds().getOver25()) + " - %" + ((int) (pr.getpOver25() * 100)) + ")";
		} else if (tahmin.equals("Var")) {
			return " (" + String.valueOf(match.getOdds().getBttsYes()) + " - %" + ((int) (pr.getpBttsYes() * 100))
					+ ")";
		} else if (tahmin.equals("Yok")) {
			return " (" + String.valueOf(match.getOdds().getBttsNo()) + " - %" + ((int) ((1 - pr.getpBttsYes()) * 100))
					+ ")";
		}

		return null;
	}

	public List<LastPrediction> getLastPrediction() {
		return lastPrediction;
	}

	public List<PredictionData> getPredictionData() {
		return predictionData;
	}

}

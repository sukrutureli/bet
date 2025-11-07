package com.example;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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

			tempLastPrediction.setScore(predictionResults.get(i).getScoreline());
			tempLastPrediction.setMbs(matchInfo.get(i).getOdds().getMbs());

			if (calculatePrediction(th, predictionResults.get(i), matchInfo.get(i),
					predictionResults.get(i).getPick()) != null) {
				String withOdd = predictionResults.get(i).getPick() + getOddsAndPercentage(
						predictionResults.get(i).getPick(), matchInfo.get(i), predictionResults.get(i));
				tempLastPrediction.getPredictions().add(withOdd);
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
		if (!h.isInfoEnough() && !h.isInfoEnoughWithoutRekabet()) {
			return null;
		}
		
		if (tahmin.equals("MS1")) {
			if (matchInfo.getOdds().getMs1() > 1.0 && h.getMs1() > 0.55
					&& isScoreOk(pr.getScoreline(), tahmin)) {
				return tahmin;
			}
		}
		else if (tahmin.equals("MS2")) {
			if (matchInfo.getOdds().getMs2() > 1.0 && h.getMs2() > 0.55
					&& isScoreOk(pr.getScoreline(), tahmin)) {
				return tahmin;
			}
		}
		else if (tahmin.equals("Üst")) {
			if (matchInfo.getOdds().getOver25() > 1.0 && h.getUst() > 0.7
					&& isScoreOk(pr.getScoreline(), tahmin)) {
				return tahmin;
			}
		}
		else if (tahmin.equals("Alt")) {
			if (matchInfo.getOdds().getUnder25() > 1.0 && h.getAlt() > 0.7
					&& isScoreOk(pr.getScoreline(), tahmin)) {
				return tahmin;
			}
		}
		else if (tahmin.equals("Var")) {
			if (matchInfo.getOdds().getBttsYes() > 1.0 && h.getVar() > 0.7
					&& isScoreOk(pr.getScoreline(), tahmin)) {
				return tahmin;
			}
		}
		else if (tahmin.equals("Yok")) {
			if (matchInfo.getOdds().getBttsNo() > 1.0 && h.getYok() > 0.7
					&& isScoreOk(pr.getScoreline(), tahmin)) {
				return tahmin;
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
		} else if (tahmin.equals("MSX")) {
			return " (" + String.valueOf(match.getOdds().getMsX()) + " - %" + ((int) (pr.getpDraw() * 100)) + ")";
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

	public boolean isPercentageOK(double h, double pr, String type) {
		boolean result = false;
		double hWdMs = 55;
		double hWeMs = 60;
		double prWdMs = 60;
		double prWeMs = 65;

		double hWdOther = 65;
		double hWeOther = 70;
		double prWdOther = 70;
		double prWeOther = 75;

		LocalDate now = LocalDate.now(ZoneId.of("Europe/Istanbul"));
		boolean isWeekEnd = now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY;

		if (isWeekEnd) {
			if (type.contains("MS")) {
				if (h >= hWeMs && pr >= prWeMs) {
					result = true;
				}
			} else {
				if (h >= hWeOther && pr >= prWeOther) {
					result = true;
				}
			}
		} else {
			if (type.equals("MS")) {
				if (h >= hWdMs && pr >= prWdMs) {
					result = true;
				}
			} else {
				if (h >= hWdOther && pr >= prWdOther) {
					result = true;
				}
			}
		}

		return result;
	}
}

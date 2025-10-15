package com.example.algo;

import java.util.Optional;
import com.example.model.*;

public class SimpleHeuristicModel implements BettingAlgorithm {

	@Override
	public String name() {
		return "SimpleHeuristicModel";
	}

	@Override
	public double weight() {
		return 0.3;
	}

	@Override
	public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
		TeamStats h = match.getHomeStats();
		TeamStats a = match.getAwayStats();
		if (h == null || a == null || h.isEmpty() || a.isEmpty())
			return neutralResult(match);

		try {
			// --- 1. Dinamik normalize edilen farklar ---
			double formDiff = safeDiv((h.getAvgPointsPerMatch() - a.getAvgPointsPerMatch()), 2.5);
			double goalDiff = safeDiv(((h.getAvgGF() - h.getAvgGA()) - (a.getAvgGF() - a.getAvgGA())), 3.0);
			double ratingDiff = safeDiv((h.getRating100() - a.getRating100()), 120.0);
			double h2hDiff = h.getH2hCount() > 0 ? safeDiv((h.getH2hWinRate() - 0.5), 0.5) : 0.0;

			// --- 2. Kombine güç skoru ---
			double s = 0.45 * formDiff + 0.25 * goalDiff + 0.20 * ratingDiff + 0.10 * h2hDiff;

			// --- 3. Softmax tabanlı sonuç olasılıkları ---
			double hadv = 0.12;
			double Lh = s + hadv;
			double Ld = -Math.abs(s) * 0.9;
			double La = -s;

			double eh = Math.exp(Lh), ed = Math.exp(Ld), ea = Math.exp(La);
			double Z = eh + ed + ea;
			double pHome = eh / Z;
			double pDraw = ed / Z;
			double pAway = ea / Z;

			// --- 4. Toplam gol & BTTS ---
			double totalGoals = Math.max(0.4, h.getAvgGF() + a.getAvgGF());
			double pOver25 = clamp(0.25 + 0.18 * (totalGoals - 2.3), 0.10, 0.90);
			double pBttsYes = clamp(0.25 + 0.25 * (h.getAvgGF() * a.getAvgGF()), 0.10, 0.90);

			// --- 5. Mantıklı skor tahmini ---
			String score = "";
			if (totalGoals >= 0.5 && totalGoals <= 5.0) {
				if (Math.abs(pHome - pAway) > 0.05)
					score = (pHome > pAway) ? "2-1" : "1-2";
				else
					score = "1-1";
			}

			// --- 6. Tahmin çıktısı ---
			double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
			String pick = (maxRes == pHome) ? "MS1" : (maxRes == pDraw ? "MSX" : "MS2");
			double confidence = Math.round(maxRes * 100.0) / 100.0;

			return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), safeProb(pHome),
					safeProb(pDraw), safeProb(pAway), safeProb(pOver25), safeProb(pBttsYes), pick, confidence, score);

		} catch (Exception e) {
			System.out.println("SimpleHeuristicModel hata: " + e.getMessage());
			return neutralResult(match);
		}
	}

	// --- Yardımcılar ---
	private double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	private double safeDiv(double a, double b) {
		return (b == 0) ? 0 : a / b;
	}

	private double safeProb(double v) {
		return Double.isFinite(v) ? Math.min(0.99, Math.max(0.01, v)) : 0.33;
	}

	private PredictionResult neutralResult(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.33, 0.34, 0.33, 0.50, 0.50, "MSX", 0.33,
				"");
	}
}

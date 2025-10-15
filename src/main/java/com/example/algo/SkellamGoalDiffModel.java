package com.example.algo;

import java.util.*;
import com.example.model.*;
import com.example.util.MathUtils;

public class SkellamGoalDiffModel implements BettingAlgorithm {

	@Override
	public String name() {
		return "SkellamGoalDiffModel";
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
			double baseHomeAdv = 1.1;
			double lambdaH = baseHomeAdv * (0.6 * h.getAvgGF() + 0.4 * a.getAvgGA());
			double lambdaA = (0.6 * a.getAvgGF() + 0.4 * h.getAvgGA());

			// Skellam: D = X - Y
			double pHome = 0, pDraw = 0, pAway = 0;
			double pOver25 = 0, pBtts = 0;

			int maxG = 6;
			for (int d = -maxG; d <= maxG; d++) {
				double prob = skellam(lambdaH, lambdaA, d);
				if (d > 0)
					pHome += prob;
				else if (d == 0)
					pDraw += prob;
				else
					pAway += prob;
			}

			// Alt-Üst tahmini (yaklaşık)
			double expGoals = lambdaH + lambdaA;
			pOver25 = MathUtils.sigmoid(expGoals - 2.5);
			pBtts = MathUtils.sigmoid(expGoals - 2.2);

			double maxP = Math.max(pHome, Math.max(pDraw, pAway));
			String pick = (maxP == pHome) ? "MS1" : (maxP == pDraw ? "MSX" : "MS2");

			return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), pHome, pDraw, pAway, pOver25,
					pBtts, pick, maxP, "");

		} catch (Exception e) {
			System.out.println("SkellamGoalDiffModel hata: " + e.getMessage());
			return neutralResult(match);
		}
	}

	// Skellam olasılık fonksiyonu
	private double skellam(double mu1, double mu2, int k) {
		double ratio = Math.pow(mu1 / mu2, k / 2.0);
		double bessel = MathUtils.besselI(Math.abs(k), 2 * Math.sqrt(mu1 * mu2));
		return Math.exp(-(mu1 + mu2)) * ratio * bessel;
	}

	private PredictionResult neutralResult(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.33, 0.34, 0.33, 0.5, 0.5, "MSX", 0.33,
				"");
	}
}

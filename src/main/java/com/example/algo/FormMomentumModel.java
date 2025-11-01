package com.example.algo;

import java.util.*;
import com.example.model.*;

public class FormMomentumModel implements BettingAlgorithm {
	@Override
	public String name() {
		return "FormMomentumModel";
	}

	public double[] weight() {
		return new double[]{ 0.2, 0.0 };
	}

	@Override
	public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
		TeamStats h = match.getHomeStats();
		TeamStats a = match.getAwayStats();
		
		if (h == null || a == null || h.isEmpty() || a.isEmpty())
			return neutralResult(match);

		double momentum = (h.getLast5Points() / h.getLast5Count()) - (a.getLast5Points() / a.getLast5Count());
		double pHome = 0.5 + momentum * 0.6;
		double pAway = 0.5 - momentum * 0.6;
		double pDraw = 1 - (pHome + pAway);

		String pick = (pHome > pAway) ? "MS1" : "MS2";
		double conf = Math.max(pHome, pAway);

		return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), clamp(pHome), clamp(pDraw),
				clamp(pAway), 0.5, 0.5, pick, conf, "");
	}

	private double clamp(double v) {
		return Math.max(0.01, Math.min(0.99, v));
	}

	private PredictionResult neutralResult(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.33, 0.34, 0.33, 0.50, 0.50, "MSX", 0.33,
				"");
	}
}

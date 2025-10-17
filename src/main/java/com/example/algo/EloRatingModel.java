package com.example.algo;

import java.util.*;
import com.example.model.*;

public class EloRatingModel implements BettingAlgorithm {
	@Override
	public String name() {
		return "EloRatingModel";
	}

	@Override
	public double[] weight() {
		return new double[]{ 0.2, 0.0 };
	}

	@Override
	public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
		TeamStats h = match.getHomeStats();
		TeamStats a = match.getAwayStats();

		double diff = (h.getRating100() - a.getRating100()) / 400.0;
		double expectedHome = 1 / (1 + Math.pow(10, -diff));

		double pHome = expectedHome;
		double pAway = 1 - expectedHome;
		double pDraw = 0.25 * (1 - Math.abs(pHome - pAway));

		String pick = (pHome > pAway) ? "MS1" : "MS2";
		double conf = Math.max(pHome, pAway);

		return new PredictionResult(name(), h.getTeamName(), a.getTeamName(), pHome, pDraw, pAway, 0.5, 0.5, pick, conf,
				"");
	}
}

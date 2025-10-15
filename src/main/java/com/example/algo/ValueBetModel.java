package com.example.algo;

import java.util.Optional;
import com.example.model.*;

public class ValueBetModel implements BettingAlgorithm {
	@Override
	public String name() {
		return "ValueBetModel";
	}

	@Override
	public double weight() {
		return 0.2;
	}

	@Override
	public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
		if (oddsOpt.isEmpty())
			return neutralResult(match);
		Odds o = oddsOpt.get();

		double imp1 = Odds.impliedProb(o.getMs1());
		double impX = Odds.impliedProb(o.getMsX());
		double imp2 = Odds.impliedProb(o.getMs2());

		// Basit value oranı = (model olasılığı - piyasa olasılığı)
		double fairHome = 1 / imp1;
		double fairAway = 1 / imp2;

		double valueHome = (imp1 < 0.4) ? 0.6 : 0.3;
		double valueAway = (imp2 < 0.4) ? 0.6 : 0.3;

		String pick = (valueHome > valueAway) ? "MS1" : "MS2";
		double conf = Math.max(valueHome, valueAway);

		return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(), imp1, impX, imp2, 0.5, 0.5, pick,
				conf, "");
	}

	private PredictionResult neutralResult(Match m) {
		return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(), 0.33, 0.34, 0.33, 0.5, 0.5, "MSX", 0.33,
				"");
	}
}

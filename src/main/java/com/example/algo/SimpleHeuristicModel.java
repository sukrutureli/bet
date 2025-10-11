package com.example.algo;

import java.util.Optional;
import com.example.model.*;

public class SimpleHeuristicModel implements BettingAlgorithm {

    @Override
    public String name() { return "SimpleHeuristicModel"; }
    @Override
    public double weight() { return 0.4; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        TeamStats h = match.getHomeStats();
        TeamStats a = match.getAwayStats();
        if (h == null || a == null) {
            return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(),
                    0.33, 0.34, 0.33, 0.50, 0.50, "MSX", 0.33, "1-1");
        }

        // normalize edilmiş temel farklar
        double formDiff = (h.getLast5Points() - a.getLast5Points()) / 15.0;
        double goalDiff = ((h.getAvgGF() - h.getAvgGA()) - (a.getAvgGF() - a.getAvgGA())) / 3.0;
        double ratingDiff = (h.getRating100() - a.getRating100()) / 100.0;

        // genel güç skoru
        double s = 0.5 * formDiff + 0.3 * goalDiff + 0.2 * ratingDiff;

        // softmax tahmini (normalize edilmiş logitler)
        double hadv = 0.15;
        double Lh = s + hadv;
        double Ld = -Math.abs(s) * 1.0;
        double La = -s;

        double eh = Math.exp(Lh), ed = Math.exp(Ld), ea = Math.exp(La);
        double Z = eh + ed + ea;
        double pHome = eh / Z;
        double pDraw = ed / Z;
        double pAway = ea / Z;

        // Gol beklentisine göre alt/üst ve BTTS
        double muH = Math.max(0.2, h.getAvgGF());
        double muA = Math.max(0.2, a.getAvgGF());
        double totalGoals = muH + muA;

        double pOver25 = clamp(0.25 + 0.20 * (totalGoals - 2.4), 0.10, 0.90);
        double pBttsYes = clamp(0.20 + 0.25 * (muH * muA), 0.05, 0.90);

        String pick;
        double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
        if (maxRes == pHome) pick = "MS1";
        else if (maxRes == pDraw) pick = "MSX";
        else pick = "MS2";

        String score = (totalGoals >= 2.7 ? (pHome > pAway ? "2-1" : "1-2") : "1-1");
        double confidence = Math.round(maxRes * 100.0) / 100.0;

        return new PredictionResult(
                name(), match.getHomeTeam(), match.getAwayTeam(),
                pHome, pDraw, pAway, pOver25, pBttsYes, pick, confidence, score);
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

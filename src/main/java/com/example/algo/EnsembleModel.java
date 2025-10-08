package com.example.algo;

import java.util.*;

import com.example.model.Match;
import com.example.model.Odds;
import com.example.model.PredictionResult;

public class EnsembleModel implements BettingAlgorithm {
    private final List<BettingAlgorithm> models;

    public EnsembleModel(List<BettingAlgorithm> models) {
        this.models = models;
    }

    @Override
    public String name() { return "EnsembleModel"; }

    @Override
    public double weight() { return 1.0; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> odds) {
        double pH=0, pD=0, pA=0, pO=0, pB=0;
        String bestScore = "1-0";
        String bestPick = "MS1";
        double conf = 0;
        double totW = 0;

        for (BettingAlgorithm m : models) {
            double w = m.weight();
            PredictionResult r = m.predict(match, odds);
            pH += w*r.getpHome();
            pD += w*r.getpDraw();
            pA += w*r.getpAway();
            pO += w*r.getpOver25();
            pB += w*r.getpBttsYes();
            if (w > conf) { // skor/pick için en yüksek ağırlıklı modeli referans al
                bestScore = r.getScoreline();
                bestPick = r.getPick();
                conf = w;
            }
            totW += w;
        }

        if (totW == 0) totW = 1;
        pH/=totW; pD/=totW; pA/=totW; pO/=totW; pB/=totW;

        // En yüksek olasılığa göre pick güncelle
        double maxRes = Math.max(pH, Math.max(pD, pA));
        String pick = (maxRes == pH) ? "MS1" : (maxRes == pD ? "MSX" : "MS2");
        double confidence = maxRes;

        return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(),
                pH, pD, pA, pO, pB, pick, confidence, bestScore);
    }
}

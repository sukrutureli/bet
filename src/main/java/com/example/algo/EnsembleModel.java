package com.example.algo;

import java.util.*;
import com.example.model.*;

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
        double pH = 0, pD = 0, pA = 0;  // Maç sonucu
        double pO = 0, pB = 0;          // Üst ve BTTS
        double totW = 0;

        String bestScore = "";
        String bestPick = "MSX";
        double bestWeight = -1;

        for (BettingAlgorithm m : models) {
            PredictionResult r = m.predict(match, odds);
            if (r == null) continue;

            double w = m.weight();
            if (Double.isNaN(w) || w <= 0) continue;

            pH += w * safe(r.getpHome());
            pD += w * safe(r.getpDraw());
            pA += w * safe(r.getpAway());
            pO += w * safe(r.getpOver25());
            pB += w * safe(r.getpBttsYes());
            totW += w;

            // skor doluysa ve model ağır basıyorsa onu referans al
            if (w > bestWeight && r.getScoreline() != null && !r.getScoreline().isBlank()) {
                bestScore = r.getScoreline();
                bestPick = r.getPick();
                bestWeight = w;
            }
        }

        if (totW == 0) totW = 1;
        pH /= totW; pD /= totW; pA /= totW; pO /= totW; pB /= totW;

        // --- 1️⃣ Maç Sonucu Olasılığı ---
        double msMax = Math.max(pH, Math.max(pD, pA));
        String msPick = (msMax == pH) ? "MS1" : (msMax == pD ? "MSX" : "MS2");

        // --- 2️⃣ Alt/Üst Olasılığı ---
        double pUnder = 1.0 - pO;
        String ouPick = (pO > 0.55) ? "ÜST" : (pUnder > 0.55 ? "ALT" : "");

        // --- 3️⃣ Var/Yok Olasılığı ---
        double pNoBtts = 1.0 - pB;
        String bttsPick = (pB > 0.55) ? "VAR" : (pNoBtts > 0.55 ? "YOK" : "");

        // --- 4️⃣ En güçlü olasılığı seç (dinamik strateji) ---
        Map<String, Double> candidates = new LinkedHashMap<>();
        candidates.put(msPick, msMax);
        if (!ouPick.isEmpty()) candidates.put(ouPick, Math.max(pO, pUnder));
        if (!bttsPick.isEmpty()) candidates.put(bttsPick, Math.max(pB, pNoBtts));

        String finalPick = msPick;
        double finalConf = msMax;

        for (Map.Entry<String, Double> e : candidates.entrySet()) {
            if (e.getValue() > finalConf) {
                finalConf = e.getValue();
                finalPick = e.getKey();
            }
        }

        finalConf = Math.round(finalConf * 100.0) / 100.0;

        return new PredictionResult(
                name(), match.getHomeTeam(), match.getAwayTeam(),
                safe(pH), safe(pD), safe(pA),
                safe(pO), safe(pB),
                finalPick, finalConf, bestScore);
    }

    private double safe(Double v) {
        if (v == null || Double.isNaN(v) || v < 0 || v > 1) return 0.33;
        return v;
    }
}

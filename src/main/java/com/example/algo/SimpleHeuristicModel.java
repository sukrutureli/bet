package com.example.algo;

import java.util.Locale;
import java.util.Optional;

import com.example.model.Match;
import com.example.model.Odds;
import com.example.model.PredictionResult;
import com.example.model.TeamStats;

public class SimpleHeuristicModel implements BettingAlgorithm {

    // mvn/Java çalıştırırken -Dab.debug=true dersen kapat/aç
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("ab.debug", "true"));

    @Override
    public String name() { return "SimpleHeuristicModel"; }

    @Override
    public double weight() { return 0.35; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        TeamStats h = match.getHomeStats();
        TeamStats a = match.getAwayStats();

        // Güvenli koruma
        if (h == null || a == null) {
            if (DEBUG) {
                System.out.printf("[HEUR DEBUG] %s vs %s | TeamStats eksik, nÃ¶tr dağıtım dÃ¶ndü.%n",
                        match.getHomeTeam(), match.getAwayTeam());
            }
            return new PredictionResult(
                    name(), match.getHomeTeam(), match.getAwayTeam(),
                    0.33, 0.34, 0.33, 0.50, 0.50, "MSX", 0.34, "1-1"
            );
        }

        // --- Özellikler (normalize) ---
        // form: son 5 maç toplam puanı (0..15) -> fark /15 ile [-1..1]
        double formH = clamp(h.getLast5Points(), 0, 15);
        double formA = clamp(a.getLast5Points(), 0, 15);
        double formDiff = (formH - formA) / 15.0; // [-1..1]

        // gol farkı: (GF-GA) farkı, aşırı etkilenmesin diye [-2..2] clamp, sonra /2
        double gdiffH = h.getAvgGF() - h.getAvgGA();
        double gdiffA = a.getAvgGF() - a.getAvgGA();
        double goalDiff = clamp(gdiffH - gdiffA, -2.0, 2.0) / 2.0; // ~[-1..1]

        // rating farkı: [-1..1]
        double ratingDiff = (h.getRating100() - a.getRating100()) / 100.0;

        // tek skora topla (küçük ağırlıklar; aşırı büyümesin)
        double s = 0.55 * formDiff + 0.30 * goalDiff + 0.15 * ratingDiff;

        // 3 sınıf için logitâ€™ler: ev avantajı hadv ekle,
        // beraberliğin logiti yakın maçlarda yükselsin: -|s|*beta
        double hadv = 0.18;
        double beta = 1.1;

        double Lh = s + hadv;
        double La = -s;
        double Ld = -Math.abs(s) * beta;

        // sıcaklık (overconfidenceâ€™ı düşürmek için)
        double temp = 1.6;
        Lh /= temp; Ld /= temp; La /= temp;

        // sayısal stabil softmax
        double max = Math.max(Lh, Math.max(Ld, La));
        double eh = Math.exp(Lh - max);
        double ed = Math.exp(Ld - max);
        double ea = Math.exp(La - max);
        double Z  = eh + ed + ea;
        double pHome = eh / Z;
        double pDraw = ed / Z;
        double pAway = ea / Z;

        // Over/BTTS (sakinleştirilmiş)
        double muH = Math.max(0.2, h.getAvgGF()*0.9 + Math.max(0, gdiffH)*0.25 + 0.25);
        double muA = Math.max(0.2, a.getAvgGF()*0.9 + Math.max(0, gdiffA)*0.20);
        double goals = muH + muA;

        double pOver25  = clamp(0.20 + (goals - 2.2)*0.25, 0.05, 0.90);
        double pBttsYes = clamp(0.15 + (muH * muA)*0.18,   0.05, 0.85);

        String pick;
        double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
        if (maxRes == pHome)      pick = "MS1";
        else if (maxRes == pDraw) pick = "MSX";
        else                      pick = "MS2";

        double confidence = maxRes;
        String score = (goals >= 2.6 ? (pHome > pAway ? "2-1" : "1-2") : "1-1");

        if (DEBUG) {
            System.out.printf(
                Locale.US,
                "[HEUR DEBUG] %s vs %s | formH=%d formA=%d diff=%.2f | gdiffH=%.2f gdiffA=%.2f Î”goal=%.2f | " +
                "rateH=%d rateA=%d Î”rate=%.2f | s=%.2f | Lh=%.2f Ld=%.2f La=%.2f | pH=%.2f pD=%.2f pA=%.2f | pick=%s conf=%.2f | muH=%.2f muA=%.2f over25=%.2f btts=%.2f%n",
                match.getHomeTeam(), match.getAwayTeam(),
                (int)formH, (int)formA, formDiff,
                gdiffH, gdiffA, goalDiff,
                h.getRating100(), a.getRating100(), ratingDiff,
                s, Lh, Ld, La,
                pHome, pDraw, pAway,
                pick, confidence,
                muH, muA, pOver25, pBttsYes
            );
        }

        return new PredictionResult(
                name(), match.getHomeTeam(), match.getAwayTeam(),
                pHome, pDraw, pAway, pOver25, pBttsYes, pick, confidence, score
        );
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

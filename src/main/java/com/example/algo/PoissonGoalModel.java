package com.example.algo;

import java.util.Optional;

import com.example.model.Match;
import com.example.model.Odds;
import com.example.model.PredictionResult;
import com.example.model.TeamStats;
import com.example.util.MathUtils;

public class PoissonGoalModel implements BettingAlgorithm {

    @Override
    public String name() { return "PoissonGoalModel"; }

    @Override
    public double weight() { return 0.65; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        TeamStats h = match.getHomeStats();
        TeamStats a = match.getAwayStats();

        // Beklenen goller (basit aproks.): son N ortalamaları ve rakip GA/GF etkisi
        double homeAdv = 1.10; // ev sahibi avantaj çarpanı
        double lambdaH = homeAdv * (0.6 * h.getAvgGF() + 0.4 * a.getAvgGA());
        double lambdaA = (0.6 * a.getAvgGF() + 0.4 * h.getAvgGA());

        // 0..6 gol aralığında Poisson
        int maxG = 6;
        double[] pH = MathUtils.poissonDist(lambdaH, maxG);
        double[] pA = MathUtils.poissonDist(lambdaA, maxG);

        // Skor matrisi
        double pHome = 0, pDraw = 0, pAway = 0, pOver25 = 0, pBttsYes = 0;
        double bestScoreP = -1; String bestScore = "1-0";
        for (int i = 0; i <= maxG; i++) {
            for (int j = 0; j <= maxG; j++) {
                double pij = pH[i] * pA[j]; // bağımsız varsayımı
                if (i > j) pHome += pij;
                else if (i == j) pDraw += pij;
                else pAway += pij;
                if (i + j >= 3) pOver25 += pij;
                if (i > 0 && j > 0) pBttsYes += pij;
                if (pij > bestScoreP) {
                    bestScoreP = pij; bestScore = i + "-" + j;
                }
            }
        }

        // Öneri: En yüksek ihtimalli 1X2 seçimi + güven
        double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
        String pick = (maxRes == pHome) ? "MS1" : (maxRes == pDraw ? "MSX" : "MS2");
        double confidence = maxRes;

        // Odds ile kalibrasyon (varsa küçük bir itme; Örn: implied prob ortalaması ile harmanla)
        if (oddsOpt.isPresent()) {
            Odds o = oddsOpt.get();
            double imp1 = Odds.impliedProb(o.getMs1());
            double impx = Odds.impliedProb(o.getMsX());
            double imp2 = Odds.impliedProb(o.getMs2());
            if (!Double.isNaN(imp1) && !Double.isNaN(impx) && !Double.isNaN(imp2)) {
                double sum = imp1 + impx + imp2;
                if (sum > 0) {
                    // Bookmaker margin normalize
                    imp1 /= sum; impx /= sum; imp2 /= sum;
                    pHome = 0.8*pHome + 0.2*imp1;
                    pDraw = 0.8*pDraw + 0.2*impx;
                    pAway = 0.8*pAway + 0.2*imp2;
                    // pick & confidence yeniden değerlendir
                    maxRes = Math.max(pHome, Math.max(pDraw, pAway));
                    pick = (maxRes == pHome) ? "MS1" : (maxRes == pDraw ? "MSX" : "MS2");
                    confidence = maxRes;
                }
            }
        }

        return new PredictionResult(name(), match.getHomeTeam(), match.getAwayTeam(),
                pHome, pDraw, pAway, pOver25, pBttsYes, pick, confidence, bestScore);
    }
}

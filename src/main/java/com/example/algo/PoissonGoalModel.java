package com.example.algo;

import java.util.Optional;
import com.example.model.*;
import com.example.util.MathUtils;

public class PoissonGoalModel implements BettingAlgorithm {

    @Override
    public String name() { return "PoissonGoalModel"; }
    @Override
    public double weight() { return 0.6; }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        TeamStats h = match.getHomeStats();
        TeamStats a = match.getAwayStats();

        // Beklenen goller: hem son ortalama hem rakip savunma etkisi
        double homeAdv = 1.12; // ev sahibi avantajı
        double lambdaH = homeAdv * (0.55 * h.getAvgGF() + 0.45 * a.getAvgGA());
        double lambdaA = (0.55 * a.getAvgGF() + 0.45 * h.getAvgGA());

        // 0–7 arası gol dağılımı
        int maxG = 7;
        double[] pH = MathUtils.poissonDist(lambdaH, maxG);
        double[] pA = MathUtils.poissonDist(lambdaA, maxG);

        double pHome = 0, pDraw = 0, pAway = 0;
        double pOver25 = 0, pUnder25 = 0;
        double pBttsYes = 0, pBttsNo = 0;
        double bestP = -1; String bestScore = "1-1";

        for (int i = 0; i <= maxG; i++) {
            for (int j = 0; j <= maxG; j++) {
                double pij = pH[i] * pA[j];
                if (i > j) pHome += pij;
                else if (i == j) pDraw += pij;
                else pAway += pij;

                if (i + j >= 3) pOver25 += pij;
                else pUnder25 += pij;

                if (i > 0 && j > 0) pBttsYes += pij;
                else pBttsNo += pij;

                if (pij > bestP) {
                    bestP = pij;
                    bestScore = i + "-" + j;
                }
            }
        }

        // Odds kalibrasyonu
        if (oddsOpt.isPresent()) {
            Odds o = oddsOpt.get();
            double imp1 = Odds.impliedProb(o.getMs1());
            double impx = Odds.impliedProb(o.getMsX());
            double imp2 = Odds.impliedProb(o.getMs2());
            double sum = imp1 + impx + imp2;
            if (sum > 0) {
                imp1 /= sum; impx /= sum; imp2 /= sum;
                pHome = 0.8 * pHome + 0.2 * imp1;
                pDraw = 0.8 * pDraw + 0.2 * impx;
                pAway = 0.8 * pAway + 0.2 * imp2;
            }
        }

        double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
        String pick = (maxRes == pHome) ? "MS1" : (maxRes == pDraw ? "MSX" : "MS2");
        double confidence = Math.round(maxRes * 100.0) / 100.0;

        return new PredictionResult(
                name(), match.getHomeTeam(), match.getAwayTeam(),
                pHome, pDraw, pAway,
                pOver25, pBttsYes,
                pick, confidence, bestScore
        );
    }
}

package com.example.algo;

import java.util.Optional;
import com.example.model.*;
import com.example.util.MathUtils;

/**
 * Geliştirilmiş PoissonGoalModel:
 * - Ligsiz global μ (EMA) tabanı
 * - Tanh ile normalize edilmiş rating & form farkı
 * - Dixon–Coles düzeltmesi (ρ)
 * - Piyasa karışımı (%15)
 * - Beklenen skor tahmini
 * - Over/Under ve BTTS olasılıkları
 */
public class PoissonGoalModel implements BettingAlgorithm {

    // --- Global seviye parametreleri ---
    private double mu = Math.log(1.35);   // başlangıç: takım başına ~1.35 gol
    private double muAlpha = 0.97;        // EMA faktörü
    private double rho = 0.10;            // Dixon–Coles düzeltme katsayısı

    @Override
    public String name() {
        return "PoissonGoalModel";
    }

    @Override
    public double[] weight() {
        // MS düşük, Alt/Üst baskın model
        return new double[]{ 0.2, 0.7 };
    }

    /**
     * Global gol seviyesini güncelle (isteğe bağlı çağrılabilir)
     */
    public void updateMu(double goalsHome, double goalsAway) {
        double perTeam = (goalsHome + goalsAway) / 2.0;
        double muNew = Math.log(Math.max(0.2, perTeam));
        mu = muAlpha * mu + (1 - muAlpha) * muNew;
    }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        TeamStats h = match.getHomeStats();
        TeamStats a = match.getAwayStats();

        if (h == null || a == null || h.isEmpty() || a.isEmpty())
            return neutralResult(match);

        try {
            // --- 1. Normalize edilmiş rating & form farkı ---
            double ratingAdj = 1.0 + Math.tanh((h.getRating100() - a.getRating100()) / 800.0);
            double formAdj = 1.0 + 0.3 * Math.tanh((h.getAvgPointsPerMatch() - a.getAvgPointsPerMatch()));

            // --- 2. Ev avantajı ---
            double baseHomeAdv = 1.10; // sabit ev bonusu
            double homeAdv = baseHomeAdv * ratingAdj * formAdj;

            // --- 3. Beklenen goller ---
            double formFactorH = 1.0 + 0.1 * (h.getAvgPointsPerMatch() - 1.0);
            double formFactorA = 1.0 + 0.1 * (a.getAvgPointsPerMatch() - 1.0);

            double logLambdaH = mu + Math.log(homeAdv)
                    + Math.log(0.55 * Math.max(0.1, h.getAvgGF()) * formFactorH + 0.45 * Math.max(0.1, a.getAvgGA()));
            double logLambdaA = mu
                    + Math.log(0.55 * Math.max(0.1, a.getAvgGF()) * formFactorA + 0.45 * Math.max(0.1, h.getAvgGA()));

            double lambdaH = Math.exp(logLambdaH);
            double lambdaA = Math.exp(logLambdaA);

            // --- 4. Skor dağılımları ---
            int maxG = 6;
            double[] pH = MathUtils.poissonDist(lambdaH, maxG);
            double[] pA = MathUtils.poissonDist(lambdaA, maxG);

            double pHome = 0, pDraw = 0, pAway = 0;
            double pOver25 = 0, pBttsYes = 0;

            for (int i = 0; i <= maxG; i++) {
                for (int j = 0; j <= maxG; j++) {
                    double pij = pH[i] * pA[j] * dcAdjust(i, j); // DC düzeltme
                    if (i > j) pHome += pij;
                    else if (i == j) pDraw += pij;
                    else pAway += pij;

                    if (i + j >= 3) pOver25 += pij;
                    if (i > 0 && j > 0) pBttsYes += pij;
                }
            }

            // --- 5. Normalize (küçük yuvarlama hatalarını temizle) ---
            double sum = pHome + pDraw + pAway;
            if (sum > 0) {
                pHome /= sum; pDraw /= sum; pAway /= sum;
            }

            // --- 6. Piyasa karışımı (%15) ---
            if (oddsOpt.isPresent()) {
                Odds o = oddsOpt.get();
                double sH = 1.0 / o.getMs1();
                double sD = 1.0 / o.getMsX();
                double sA = 1.0 / o.getMs2();
                double R = sH + sD + sA;
                double mH = sH / R, mD = sD / R, mA = sA / R;
                double w = 0.15;
                pHome = (1 - w) * pHome + w * mH;
                pDraw = (1 - w) * pDraw + w * mD;
                pAway = (1 - w) * pAway + w * mA;
            }

            // --- 7. Tahmini skor (beklenen gol sayısına göre) ---
            String bestScore = Math.round(lambdaH) + "-" + Math.round(lambdaA);

            // --- 8. Nihai karar ---
            double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
            String pick = (maxRes == pHome) ? "MS1" : (maxRes == pDraw ? "MSX" : "MS2");
            double confidence = Math.round(maxRes * 100.0) / 100.0;

            String meta = String.format("λH=%.2f λA=%.2f ρ=%.2f μ=%.2f", lambdaH, lambdaA, rho, Math.exp(mu));

            return new PredictionResult(name(),
                    match.getHomeTeam(), match.getAwayTeam(),
                    safeProb(pHome), safeProb(pDraw), safeProb(pAway),
                    safeProb(pOver25), safeProb(pBttsYes),
                    pick, confidence, bestScore);

        } catch (Exception e) {
            System.out.println("PoissonGoalModel hata: " + e.getMessage());
            return neutralResult(match);
        }
    }

    // --- Dixon–Coles local correction ---
    private double dcAdjust(int i, int j) {
        if (i == 0 && j == 0) return 1 - rho;
        if (i == 0 && j == 1) return 1 - rho;
        if (i == 1 && j == 0) return 1 - rho;
        if (i == 1 && j == 1) return 1 + rho;
        return 1.0;
    }

    private double safeProb(double v) {
        return Double.isFinite(v) ? Math.min(0.99, Math.max(0.01, v)) : 0.33;
    }

    private PredictionResult neutralResult(Match m) {
        return new PredictionResult(name(), m.getHomeTeam(), m.getAwayTeam(),
                0.33, 0.34, 0.33, 0.50, 0.50, "MSX", 0.33, "");
    }
}

package com.example.algo;

import java.util.Optional;
import com.example.model.*;
import com.example.util.MathUtils;

/**
 * PoissonGoalModel (tutarlı dağılımlı, ligsiz)
 * - Global μ (EMA) tabanı, μ=ln(1.40) ≈ 1.40 takım başına gol
 * - GF/GA global ortalamaya göre normalize (double-count engeli)
 * - Log-linear (geometrik) harman
 * - Yumuşak toplam-gol ankrajı (β=0.40)
 * - Küçük rating/form log-terimleri (±0.15*tanh)
 * - Dixon–Coles (ρ=0.10) + %15 piyasa karışımı
 * - 0–0'dan 4–2'ye kadar doğal skorlar; 5+ nadir ama olası
 */
public class PoissonGoalModel implements BettingAlgorithm {

    // --- Global seviye ---
    private double mu = Math.log(1.40);   // takım başına ortalama gol ≈ 1.40 (toplam ≈ 2.8)
    private double muAlpha = 0.97;        // EMA faktörü (istersen maç sonrasında updateMu çağır)
    private double rho = 0.10;            // Dixon–Coles düzeltmesi

    // GF/GA global ortalamaları (istersen sezonluk güncelleyebilirsin)
    private double meanGF = 1.40;
    private double meanGA = 1.40;

    @Override
    public String name() { return "PoissonGoalModel"; }

    @Override
    public double[] weight() {
        // MS düşük, Alt/Üst yüksek katkı (ensemble için mantıklı)
        return new double[]{ 0.2, 0.7 };
    }

    /** Maç sonrasında ortalama gol seviyesini güncellemek için opsiyonel kanca */
    public void updateMu(double goalsHome, double goalsAway) {
        double perTeam = (goalsHome + goalsAway) / 2.0;
        double muNew = Math.log(Math.max(0.2, perTeam));
        mu = muAlpha * mu + (1 - muAlpha) * muNew;
    }

    /** Global ortalamaları dışarıdan güncellemek istersen */
    public void updateMeans(double newMeanGF, double newMeanGA) {
        if (newMeanGF > 0.2) meanGF = newMeanGF;
        if (newMeanGA > 0.2) meanGA = newMeanGA;
    }

    @Override
    public PredictionResult predict(Match match, Optional<Odds> oddsOpt) {
        TeamStats h = match.getHomeStats();
        TeamStats a = match.getAwayStats();
        if (h == null || a == null || h.isEmpty() || a.isEmpty())
            return neutralResult(match);

        try {
            // --- 1) GF/GA'yı global ortalamalara göre normalize et ---
            double rGF_H = Math.max(0.2, h.getAvgGF()) / meanGF;
            double rGA_A = Math.max(0.2, a.getAvgGA()) / meanGA;
            double rGF_A = Math.max(0.2, a.getAvgGF()) / meanGF;
            double rGA_H = Math.max(0.2, h.getAvgGA()) / meanGA;

            // --- 2) Küçük log-terimleri (şişirmez, kontrollü etki) ---
            double ratingTerm = 0.15 * Math.tanh((h.getRating100() - a.getRating100()) / 800.0);
            double formDiff   = (h.getAvgPointsPerMatch() - a.getAvgPointsPerMatch());
            double formTerm   = 0.15 * Math.tanh(formDiff);

            // --- 3) Ev avantajı log-uzayında (≈ %14) ---
            double Hlog = 0.13;

            // --- 4) Log-linear (geometrik) harman ile λ'lar ---
            // λ_home = exp( μ + H + 0.55*log rGF_H + 0.45*log rGA_A + ratingTerm + formTerm )
            // λ_away = exp( μ      + 0.55*log rGF_A + 0.45*log rGA_H - ratingTerm - formTerm )
            double logLamH = mu + Hlog
                    + 0.55 * Math.log(rGF_H) + 0.45 * Math.log(rGA_A)
                    + ratingTerm + formTerm;

            double logLamA = mu
                    + 0.55 * Math.log(rGF_A) + 0.45 * Math.log(rGA_H)
                    - ratingTerm - formTerm;

            double lambdaH = Math.exp(logLamH);
            double lambdaA = Math.exp(logLamA);

            // --- 5) Yumuşak toplam-gol ankrajı (esnek, ama taşırmaz) ---
            double targetTotal = 2.0 * Math.exp(mu);     // ≈ global toplam gol
            double totalNow = lambdaH + lambdaA;
            if (totalNow > 0) {
                double beta = 0.40;                      // 0: kapalı, 1: tam eşitle (esnek tut)
                double scale = Math.pow(targetTotal / totalNow, beta);
                lambdaH *= scale;
                lambdaA *= scale;
            }

            // --- 6) Güvenlik sınırları (doğal dağılım için geniş band) ---
            lambdaH = Math.max(0.10, Math.min(3.20, lambdaH));
            lambdaA = Math.max(0.10, Math.min(3.20, lambdaA));

            // --- 7) Skor dağılımları ---
            int maxG = 7; // biraz geniş tuttuk (4-2, 5-3 gibi skorlar mümkün)
            double[] pH = MathUtils.poissonDist(lambdaH, maxG);
            double[] pA = MathUtils.poissonDist(lambdaA, maxG);

            double pHome = 0, pDraw = 0, pAway = 0;
            double pOver25 = 0, pBttsYes = 0;

            for (int i = 0; i <= maxG; i++) {
                for (int j = 0; j <= maxG; j++) {
                    double pij = pH[i] * pA[j] * dcAdjust(i, j); // DC
                    if (i > j) pHome += pij;
                    else if (i == j) pDraw += pij;
                    else pAway += pij;

                    if (i + j >= 3) pOver25 += pij;
                    if (i > 0 && j > 0) pBttsYes += pij;
                }
            }
            // Normalize (sayısal toparlama)
            double sum = pHome + pDraw + pAway;
            if (sum > 0) { pHome /= sum; pDraw /= sum; pAway /= sum; }

            // --- 8) %15 piyasa karışımı (MS 1-X-2) ---
            if (oddsOpt.isPresent()) {
                Odds o = oddsOpt.get();
                double sH = 1.0 / o.getMs1();
                double sD = 1.0 / o.getMsX();
                double sA = 1.0 / o.getMs2();
                double R = sH + sD + sA;
                if (R > 0) {
                    double mH = sH / R, mD = sD / R, mA = sA / R;
                    double w = 0.15;
                    pHome = (1 - w) * pHome + w * mH;
                    pDraw = (1 - w) * pDraw + w * mD;
                    pAway = (1 - w) * pAway + w * mA;
                }
            }

            // --- 9) Tahmini skor (beklenen goller) ---
            String bestScore = Math.round(lambdaH) + "-" + Math.round(lambdaA);

            // --- 10) Nihai karar ---
            double maxRes = Math.max(pHome, Math.max(pDraw, pAway));
            String pick = (maxRes == pHome) ? "MS1" : (maxRes == pDraw ? "MSX" : "MS2");
            double confidence = Math.round(maxRes * 100.0) / 100.0;

            String meta = String.format(
                "λH=%.2f λA=%.2f μ=%.2f ρ=%.2f tot=%.2f tgt=%.2f",
                lambdaH, lambdaA, Math.exp(mu), rho, (lambdaH + lambdaA), 2.0 * Math.exp(mu)
            );

            return new PredictionResult(
                name(),
                match.getHomeTeam(), match.getAwayTeam(),
                safeProb(pHome), safeProb(pDraw), safeProb(pAway),
                safeProb(pOver25), safeProb(pBttsYes),
                pick, confidence, bestScore
            );

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
        return new PredictionResult(
            name(), m.getHomeTeam(), m.getAwayTeam(),
            0.33, 0.34, 0.33, 0.50, 0.50, "MSX", 0.33, ""
        );
    }
}

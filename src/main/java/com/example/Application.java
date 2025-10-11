package com.example;

import com.example.model.*;
import com.example.report.HtmlReportGenerator;
import com.example.algo.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Application {
    public static void main(String[] args) {
        ZoneId ist = ZoneId.of("Europe/Istanbul");
        MatchScraper scraper = null;

        try {
            System.out.println("=== İddaa Scraper Başlatıldı ===");
            System.out.println("Zaman: " + LocalDateTime.now(ist));

            scraper = new MatchScraper();

            System.out.println("\n1️⃣ Ana sayfa maçları çekiliyor...");
            List<MatchInfo> matches = scraper.scrapeMainPage();
            System.out.println("Toplam " + matches.size() + " maç bulundu.");

            List<Match> matchStats = new ArrayList<>();
            MatchHistoryManager histManager = new MatchHistoryManager();

            int count = 0;
            for (MatchInfo m : matches) {
                if (!m.hasDetailUrl()) continue;
                count++;
                System.out.println("🔍 (" + count + "/" + matches.size() + ") " + m.getName());

                try {
                    TeamMatchHistory hist = scraper.scrapeTeamHistory(m.getDetailUrl(), m.getName());
                    if (hist != null) {
                        histManager.addTeamHistory(hist);
                        matchStats.add(hist.createMatch(m));
                    }
                } catch (Exception e) {
                    System.out.println("Geçmiş çekme hatası: " + e.getMessage());
                }

                if (count % 10 == 0) System.out.println("→ " + count + " maç işlendi.");
            }

            // Basit modeller
            BettingAlgorithm poisson = new PoissonGoalModel();
            BettingAlgorithm heur = new SimpleHeuristicModel();
            EnsembleModel ensemble = new EnsembleModel(List.of(poisson, heur));

            List<PredictionResult> results = new ArrayList<>();
            for (Match m : matchStats) {
                results.add(ensemble.predict(m, Optional.empty()));
            }

            HtmlReportGenerator.generateHtml(matches, histManager, matchStats, results, "futbol.html");
            System.out.println("✅ Rapor üretildi: public/futbol.html");

        } catch (Exception e) {
            System.out.println("GENEL HATA: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scraper != null) scraper.close();
        }
    }
}

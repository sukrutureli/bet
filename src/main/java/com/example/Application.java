package com.example;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.model.Match;
import com.example.model.MatchInfo;
import com.example.model.TeamMatchHistory;
import com.example.report.HtmlReportGenerator;
import com.example.algo.*;
import com.example.model.PredictionResult;

public class Application {
    public static void main(String[] args) {
        MatchScraper scraper = null;
        MatchHistoryManager historyManager = new MatchHistoryManager();
        List<MatchInfo> matches = null;
        List<Match> matchStats = new ArrayList<Match>();
        ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");
        
        try {
            System.out.println("=== İddaa Scraper Başlatılıyor ===");
            System.out.println("Zaman: " + LocalDateTime.now(istanbulZone));
            
            // Scraper'ı başlat
            scraper = new MatchScraper();
            
            // Ana sayfa verilerini çek
            System.out.println("\n1. Ana sayfa maçları çekiliyor...");
            matches = scraper.scrapeMainPage();
            
            System.out.println("Ana sayfadan " + matches.size() + " maç çekildi");
            
            for (int i = 0; i < matches.size(); i++) {
                MatchInfo match = matches.get(i);
                 
                // Detay URL'si varsa geçmiş verilerini çek
                if (match.hasDetailUrl()) {
                    System.out.println("Geçmiş çekiliyor " + (i+1) + "/" + matches.size() + ": " + match.getName());
                    
                    try {
                        TeamMatchHistory teamHistory = scraper.scrapeTeamHistory(match.getDetailUrl(), match.getName());
                        
                        if (teamHistory != null) {
                            historyManager.addTeamHistory(teamHistory);
                            matchStats.add(teamHistory.createMatch(match));
                        } 
                        
                        // Rate limiting - 3 saniye bekle
                        Thread.sleep(1000);

                        if ((i + 1) % 5 == 0) {
                            System.gc(); // Garbage collection tetikle
                        }
                        
                    } catch (Exception e) {
                        System.out.println("Geçmiş çekme hatası: " + e.getMessage());
                    }
                }
                
                // Her 20 maçta bir progress yazdır
                if ((i + 1) % 20 == 0) {
                    System.out.println("İşlendi: " + (i + 1) + "/" + matches.size());
                }
            }
            
            BettingAlgorithm poisson = new PoissonGoalModel();
            BettingAlgorithm heur = new SimpleHeuristicModel();
            EnsembleModel ensemble = new EnsembleModel(List.of(poisson, heur));

            List<PredictionResult> results = new ArrayList<>();
            for (Match m : matchStats) {
                results.add(ensemble.predict(m, Optional.empty()));
            }
            
            HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "futboldeneme2.html");
            
            System.out.println("futbol.html oluşturuldu.");
            
        } catch (Exception e) {
            System.out.println("GENEL HATA: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scraper != null) {
                scraper.close();
            }
        }
    }
}
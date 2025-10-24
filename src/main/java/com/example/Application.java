package com.example;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.model.Match;
import com.example.model.MatchInfo;
import com.example.model.TeamMatchHistory;
import com.example.prediction.PredictionSaver;
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
			matches = scraper.fetchMatches();

			System.out.println("Ana sayfadan " + matches.size() + " maç çekildi");

//			for (int i = 0; i < matches.size(); i++) {
//				MatchInfo match = matches.get(i);
//
//				// Detay URL'si varsa geçmiş verilerini çek
//				if (match.hasDetailUrl()) {
//					System.out.println("Geçmiş çekiliyor " + (i + 1) + "/" + matches.size() + ": " + match.getName());
//
//					try {
//						String url = match.getDetailUrl();
//						if (url == null || !url.startsWith("http")) {
//							System.out.println("⚠️ Geçersiz URL: " + url);
//							continue;
//						}
//
//						TeamMatchHistory teamHistory = scraper.scrapeTeamHistory(match.getDetailUrl(), match.getName());
//
//						if (teamHistory != null) {
//							historyManager.addTeamHistory(teamHistory);
//							matchStats.add(teamHistory.createMatch(match));
//						} else {
//							System.out.println("⚠️ Veri yok veya boş döndü: " + match.getName());
//						}
//
//						Thread.sleep(1500);
//						if ((i + 1) % 5 == 0)
//							System.gc();
//
//					} catch (Exception e) {
//						System.out.println("Geçmiş çekme hatası: " + e.getMessage());
//					}
//				}
//
//				if ((i + 1) % 20 == 0) {
//					System.out.println("İşlendi: " + (i + 1) + "/" + matches.size());
//				}
//			}
//
//			BettingAlgorithm poisson = new PoissonGoalModel();
//			BettingAlgorithm heur = new SimpleHeuristicModel();
//			BettingAlgorithm elo = new EloRatingModel();
//			BettingAlgorithm formMomentum = new FormMomentumModel();
//			EnsembleModel ensemble = new EnsembleModel(List.of(poisson, heur, elo, formMomentum));
//
//			List<PredictionResult> results = new ArrayList<>();
//			for (Match m : matchStats) {
//				results.add(ensemble.predict(m, Optional.ofNullable(m.getOdds())));
//			}
//
//			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "futbol.html");
//			System.out.println("futbol.html oluşturuldu.");
//
//			LastPredictionManager lastPredictionManager = new LastPredictionManager(historyManager, results, matches);
//			lastPredictionManager.fillPredictions();
//
//			HtmlReportGenerator.generateHtmlForSublist(lastPredictionManager.getLastPrediction(), "futboltahmin.html");
//			System.out.println("futboltahmin.html oluşturuldu.");
//			
//			PredictionSaver.saveTodayPredictions(lastPredictionManager.getPredictionData());

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
package com.example.prediction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.model.PredictionData;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PredictionUpdater {

	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * GitHub Pages üzerindeki JSON'u indirir, skorları günceller, güncel
	 * versiyonunu "data/2025-10-16-updated.json" olarak kaydeder.
	 */
	public static void updateFromGithub(Map<String, String> updatedScores) throws IOException {
		// 🔹 dünün tarihini bul
		//String yesterday = LocalDate.now(ZoneId.of("Europe/Istanbul")).minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String yesterday = LocalDate.now(ZoneId.of("Europe/Istanbul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		// 🔹 GitHub Pages URL'si
		String url = "https://sukrutureli.github.io/bettingsukru/data/" + yesterday + ".json";
		System.out.println("📥 JSON indiriliyor: " + url);

		// 🔹 JSON’u indir
		List<PredictionData> predictions;
		try (InputStream in = new URL(url).openStream()) {
			predictions = mapper.readerForListOf(PredictionData.class).readValue(in);
		} catch (Exception e) {
			System.out.println("❌ JSON indirilemedi: " + e.getMessage());
			return;
		}

		// 🔹 Güncelle
		for (PredictionData p : predictions) {
			String key = (p.getHomeTeam() + " - " + p.getAwayTeam()).trim();
			System.out.println(key);
			if (updatedScores.containsKey(key)) {
				String score = updatedScores.get(key);
				p.setScore(score);
				evaluatePredictions(p, score);
			}
		}

		// 🔹 Lokale kaydet (örnek: data/2025-10-15-updated.json)
		File outDir = new File("public/data");
		if (!outDir.exists())
			outDir.mkdirs();

		File outFile = new File(outDir, yesterday + ".json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, predictions);

		System.out.println("✅ Güncellenmiş dosya: " + outFile.getAbsolutePath());
	}

	/**
	 * Skora göre "won/lost/pending" durumu belirler
	 */
	private static void evaluatePredictions(PredictionData p, String score) {
		try {
			String[] parts = score.split("-");
			int home = Integer.parseInt(parts[0].trim());
			int away = Integer.parseInt(parts[1].trim());

			for (String pick : p.getPicks()) {
				String result = evaluatePick(pick, home, away);
				p.getStatuses().put(pick, result);
			}

		} catch (Exception e) {
			System.err.println("⚠️ Skor formatı hatalı: " + score);
		}
	}

	private static String evaluatePick(String pick, int home, int away) {
		if (pick.equalsIgnoreCase("MS1"))
			return home > away ? "won" : "lost";
		if (pick.equalsIgnoreCase("MS2"))
			return away > home ? "won" : "lost";
		if (pick.equalsIgnoreCase("MSX"))
			return away == home ? "won" : "lost";

		if (pick.toLowerCase().contains("üst"))
			return (home + away) > 2.5 ? "won" : "lost";
		if (pick.toLowerCase().contains("alt"))
			return (home + away) < 2.5 ? "won" : "lost";
		
		if (pick.equalsIgnoreCase("var"))
			return (home > 0 && away > 0) ? "won" : "lost";
		if (pick.toLowerCase().contains("yok"))
			return (home == 0 || away == 0) ? "won" : "lost";
			

		return "pending";
	}
}

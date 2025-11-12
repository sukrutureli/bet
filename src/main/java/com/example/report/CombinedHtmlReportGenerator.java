package com.example.report;

import com.example.MatchHistoryManager;
import com.example.model.LastPrediction;
import com.example.model.Match;
import com.example.model.MatchInfo;
import com.example.model.PredictionResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * İki raporu tek HTML altında birleştirir:
 *  - Üstte: Hazır Kupon (generateHtmlForSublist)
 *  - Altta: Detaylı Futbol Tahminleri (generateHtml)
 */
public class CombinedHtmlReportGenerator {

	public static void generateCombinedHtml(
			List<LastPrediction> sublistPredictions,
			List<MatchInfo> matches,
			MatchHistoryManager historyManager,
			List<Match> matchStats,
			List<PredictionResult> results,
			String fileName) {

		try {
			File dir = new File("public");
			if (!dir.exists()) dir.mkdirs();

			File file = new File(dir, fileName);
			try (FileWriter fw = new FileWriter(file)) {

				// === 1️⃣ HTML başlangıcı ===
				fw.write("<!DOCTYPE html>\n<html lang='tr'>\n<head>\n<meta charset='UTF-8'>\n");
				fw.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
				fw.write("<title>⚽ Futbol Tahminleri ve 💰 Hazır Kupon</title>\n");
				fw.write("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css'>\n");
				fw.write("<style>\n");
				// --- Ortak CSS ---
				fw.write("body { font-family: 'Segoe UI', Roboto, Arial, sans-serif; background:#f3f6fa; margin:0; padding:0; color:#222; }\n");
				fw.write("section { margin: 30px auto; padding: 20px; max-width: 1200px; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.05); }\n");
				fw.write("h1 { text-align:center; margin-top:20px; }\n");
				fw.write("hr { border:none; border-top:3px solid #0077cc; margin:40px 0; }\n");
				fw.write("</style>\n</head>\n<body>\n");

				// === 2️⃣ Üst kısım: Hazır Kupon ===
				fw.write("<section id='sublist'>\n");
				fw.write("<h1>💰 Hazır Kupon</h1>\n");
				fw.write("<p style='text-align:center; color:#555;'>Sistemin oluşturduğu öneri kuponu</p>\n");

				// generateHtmlForSublist içeriğini çağırmadan, aynı HTML gövdesini inline üretelim:
				fw.write("<table style='width:100%; border-collapse:collapse; background:#fff; border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,0.1); overflow:hidden;'>\n");
				fw.write("<thead><tr style='background:#0077cc; color:white;'>"
						+ "<th>🕒 Saat</th><th>MBS</th><th>⚽ Maç</th><th>🎯 Tahmin</th><th>📊 Skor Tahmini</th></tr></thead>\n<tbody>\n");

				for (LastPrediction p : sublistPredictions) {
					String mbsClass = "match-mbs-" + p.getMbs();
					fw.write("<tr style='border-bottom:1px solid #eee;'>");
					fw.write("<td style='padding:8px;'>" + p.getTime() + "</td>");
					fw.write("<td style='text-align:center;'><span class='" + mbsClass + "'>" + p.getMbs() + "</span></td>");
					fw.write("<td style='padding:8px; font-weight:bold;'>" + p.getName() + "</td>");
					fw.write("<td style='padding:8px;'>" + p.preditionsToString() + "</td>");
					fw.write("<td style='padding:8px; text-align:center;'>" + (p.getScore() != null ? p.getScore() : "-") + "</td>");
					fw.write("</tr>\n");
				}
				fw.write("</tbody></table>\n");
				fw.write("</section>\n");

				// === 3️⃣ Ayraç ===
				fw.write("<hr>\n");

				// === 4️⃣ Alt kısım: Detaylı Futbol Tahminleri ===
				fw.write("<section id='detailed'>\n");
				fw.write("<h1>⚽ Detaylı Futbol Tahminleri</h1>\n");

				// HtmlReportGenerator.generateHtml içeriğini çağırmadan, direkt ayrı dosyaya değil bu FileWriter’a gömelim:
				StringBuilder htmlDetailed = new StringBuilder();
				HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "TEMP.html");
				File tempFile = new File("public/TEMP.html");
				if (tempFile.exists()) {
					String tempContent = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
					// <body>...</body> kısmını çıkar
					int bodyStart = tempContent.indexOf("<body>");
					int bodyEnd = tempContent.indexOf("</body>");
					if (bodyStart != -1 && bodyEnd != -1) {
						htmlDetailed.append(tempContent.substring(bodyStart + 6, bodyEnd));
					}
					tempFile.delete(); // geçici dosya silinsin
				}
				fw.write(htmlDetailed.toString());
				fw.write("</section>\n");

				// === 5️⃣ HTML sonu ===
				fw.write("</body></html>");
			}

			System.out.println("✅ Birleşik HTML üretildi: " + file.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

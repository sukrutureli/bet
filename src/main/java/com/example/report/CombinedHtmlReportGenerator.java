package com.example.report;

import com.example.MatchHistoryManager;
import com.example.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CombinedHtmlReportGenerator {

	public static void generateCombinedHtml(List<LastPrediction> sublistPredictions, List<MatchInfo> matches,
			MatchHistoryManager historyManager, List<Match> matchStats, List<PredictionResult> results,
			List<PredictionData> sublistPredictionData, String fileName) {
		try {
			// 1️⃣ Önce iki geçici HTML oluştur
			HtmlReportGenerator.generateHtmlForSublist(sublistPredictions, sublistPredictionData, "TEMP_SUB.html");
			HtmlReportGenerator.generateHtml(matches, historyManager, matchStats, results, "TEMP_DETAIL.html");

			File tempSub = new File("public/TEMP_SUB.html");
			File tempDetail = new File("public/TEMP_DETAIL.html");

			String subContent = Files.exists(tempSub.toPath()) ? new String(Files.readAllBytes(tempSub.toPath())) : "";
			String detailContent = Files.exists(tempDetail.toPath())
					? new String(Files.readAllBytes(tempDetail.toPath()))
					: "";

			// 2️⃣ CSS ve BODY kısımlarını çıkar
			String subBody = extractBody(subContent);
			String detailBody = extractBody(detailContent);

			String subStyle = extractStyle(subContent);
			String detailStyle = extractStyle(detailContent);

			// 3️⃣ Nihai dosya oluştur
			File dir = new File("public/futbol");
			if (!dir.exists())
				dir.mkdirs();

			File output = new File(dir, fileName);
			try (FileWriter fw = new FileWriter(output)) {
				fw.write("<!DOCTYPE html><html lang='tr'><head><meta charset='UTF-8'>");
				fw.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
				fw.write("<title>⚽ Futbol Tahminleri + 💰 Hazır Kupon</title>");
				fw.write("<style>");
				fw.write(subStyle);
				fw.write(detailStyle);
				fw.write(
						"section{margin:30px auto; max-width:1200px;} hr{border:none;border-top:3px solid #0077cc;margin:40px 0;}");
				fw.write("</style></head><body>");
				fw.write("<section id='sublist'>");
				fw.write(subBody);
				fw.write("</section>");
				fw.write("<hr>");
				fw.write("<section id='detailed'>");
				fw.write(detailBody);
				fw.write("</section>");
				fw.write("</body></html>");
			}

			// 4️⃣ Geçici dosyaları sil
			tempSub.delete();
			tempDetail.delete();

			System.out.println("✅ Birleşik HTML üretildi: " + output.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String extractBody(String html) {
		int s = html.indexOf("<body>");
		int e = html.indexOf("</body>");
		return (s != -1 && e != -1) ? html.substring(s + 6, e) : html;
	}

	private static String extractStyle(String html) {
		int s = html.indexOf("<style>");
		int e = html.indexOf("</style>");
		return (s != -1 && e != -1) ? html.substring(s + 7, e) : "";
	}
}

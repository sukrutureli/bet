package com.example.report;

import com.example.MatchHistoryManager;
import com.example.model.LastPrediction;
import com.example.model.Match;
import com.example.model.MatchInfo;
import com.example.model.MatchResult;
import com.example.model.PredictionResult;
import com.example.model.TeamMatchHistory;
import com.example.util.MathUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class HtmlReportGenerator {

	public static void generateHtml(List<MatchInfo> matches, MatchHistoryManager historyManager, List<Match> matchStats,
			List<PredictionResult> results, String fileName) {

		ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

		// HTML oluşturmaya başla
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
		html.append("<title>⚽ Futbol Tahminleri</title>");
		html.append("<style>");
		html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
		html.append(
				".match { background: white; border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 8px; }");
		html.append(".match.insufficient { background-color: #ffe5e5; }"); // açık kırmızı arka plan
		html.append(
				".match-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }");
		html.append(".match-name { font-weight: bold; color: #333; font-size: 1.1em; }");
		html.append(".match-time { color: #666; }");
		html.append(".odds { background: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0; }");
		html.append(".history { margin-top: 15px; }");
		html.append(".history-section { background: #e9ecef; margin: 10px 0; padding: 15px; border-radius: 5px; }");
		html.append(
				".match-result { background: white; padding: 8px; margin: 5px 0; border-left: 4px solid #007bff; border-radius: 3px; font-size: 0.9em; }");
		html.append(".win { border-left-color: #28a745; background-color: #d4edda; }");
		html.append(".draw { border-left-color: #ffc107; background-color: #fff3cd; }");
		html.append(".loss { border-left-color: #dc3545; background-color: #f8d7da; }");
		html.append(
				".team-stats { background: #d1ecf1; color: #0c5460; padding: 10px; border-radius: 5px; margin: 10px 0; }");
		html.append(".no-data { color: #999; font-style: italic; padding: 20px; text-align: center; }");
		html.append(
				".stats { background: #d1ecf1; color: #0c5460; padding: 15px; margin: 20px 0; border-radius: 8px; }");
		html.append(
				".export-section { background: #fff; padding: 20px; margin: 20px 0; border: 1px solid #ddd; border-radius: 8px; }");
		html.append(".quick-summary { margin: 10px 0 14px; }");
		html.append(
				".quick-summary table.qs { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ccc; border-radius: 6px; overflow: hidden; font-size: 0.9em; }");
		html.append(
				".quick-summary th, .quick-summary td { padding: 6px 8px; text-align: center; border: 1px solid #ddd; }");
		html.append(".quick-summary th { background: #f2f2f2; font-weight: 600; color: #333; }");
		html.append(".quick-summary td.qs-odd { color: #222; font-variant-numeric: tabular-nums; }");
		html.append(
				".quick-summary td.qs-pick .pick { display: inline-block; padding: 3px 8px; border-radius: 10px; background: #e7f1ff; color: #0056b3; font-weight: 700; }");
		html.append(".quick-summary td.qs-score { color: #444; font-weight: 600; }");

		html.append("</style>");
		html.append("</head><body>");
		html.append("<h1>⚽ Futbol Tahminleri</h1>");
		html.append("<p>Son güncelleme: " + LocalDateTime.now(istanbulZone) + "</p>");

		// İstatistik bilgileri
		int detailUrlCount = 0;
		int processedTeamCount = 0;

		// URL'li maçları say
		for (MatchInfo match : matches) {
			if (match.hasDetailUrl()) {
				detailUrlCount++;
			}
		}

		html.append("<div class='stats'>");
		html.append("<h3>İstatistikler</h3>");
		html.append("<p>- Toplam maç: ").append(matches.size()).append("</p>");
		html.append("<p>- Detay URL'si olan: ").append(detailUrlCount).append("</p>");
		html.append("<p>- Geçmiş verisi çekilecek: ").append(detailUrlCount).append("</p>");
		html.append("</div>");

		for (int i = 0; i < matches.size(); i++) {
			MatchInfo match = matches.get(i);

			// Detay URL'si varsa geçmiş verilerini çek
			if (match.hasDetailUrl()) {

				TeamMatchHistory teamHistory = historyManager.getTeamHistories().get(i);
				
				boolean insufficient = (teamHistory != null && !teamHistory.isInfoEnough());
			    
			    html.append("<div class='match").append(insufficient ? " insufficient" : "").append("'>");
				//html.append("<div class='match'>");
			    
				html.append("<div class='match-header'>");
				html.append("<div class='match-name'>").append(match.getName()).append("</div>");
				html.append("<div class='match-time'>").append(match.getTime()).append("</div>");
				html.append("<button onclick=\"toggleHistory(this)\">Göster/Gizle</button>");
				html.append("</div>");

				if (teamHistory != null && teamHistory.getTotalMatches() > 0) {
					html.append("<div class='odds' style='margin-top:10px;'>");
					html.append("<strong>Güncel Oranlar ve Yüzdeler:</strong>");

					if (!teamHistory.isInfoEnough()) {
						html.append(" DİKKAT! Yeterli geçmiş veri yok");
					}

					html.append(
							"<table style='width:100%; border-collapse: collapse; margin-top:6px; text-align:center;'>");

					html.append("<tr>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getMs1(), "MS1") + "'>MS1<br><strong>")
							.append(match.getOdds().getMs1()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getMs1())).append("</td>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getMs0(), "MSX") + "'>MSX<br><strong>")
							.append(match.getOdds().getMsX()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getMs0())).append("</td>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getMs2(), "MS2") + "'>MS2<br><strong>")
							.append(match.getOdds().getMs2()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getMs2())).append("</td>");
					html.append("</tr>");

					html.append("<tr>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getAlt(), "Alt") + "'>Alt<br><strong>")
							.append(match.getOdds().getUnder25()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getAlt())).append("</td>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getUst(), "Üst") + "'>Üst<br><strong>")
							.append(match.getOdds().getOver25()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getUst())).append("</td>");
					html.append("<td style='padding:6px; border:1px solid #ccc;'>-</td>");
					html.append("</tr>");

					html.append("<tr>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getVar(), "Var") + "'>Var<br><strong>")
							.append(match.getOdds().getBttsYes()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getVar())).append("</td>");
					html.append("<td style='padding:6px; border:1px solid #ccc; "
							+ teamHistory.getStyle(teamHistory.getYok(), "Yok") + "'>Yok<br><strong>")
							.append(match.getOdds().getBttsNo()).append("</strong><br>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getYok())).append("</td>");
					html.append("<td style='padding:6px; border:1px solid #ccc;'>-</td>");
					html.append("</tr>");

					html.append("</table>");
					html.append("</div>");

					if (!Double.isNaN(results.get(i).getpHome())) { // herhangi biri NaN ise bu tabloyu ekleme
						// stats eklendi
						html.append("<div class='quick-summary'>");
						html.append("<table class='qs'>");
						html.append("<thead>");
						html.append("<tr>");
						html.append("<th>MS1</th>");
						html.append("<th>MSX</th>");
						html.append("<th>MS2</th>");
						html.append("<th>Üst 2.5</th>");
						html.append("<th>KG Var</th>");
						html.append("<th>Seçim</th>");
						html.append("<th>Skor</th>");
						html.append("</tr>");
						html.append("</thead>");
						html.append("<tbody>");
						html.append("<tr>");
						html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpHome()) + "</td>");
						html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpDraw()) + "</td>");
						html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpAway()) + "</td>");
						html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpOver25()) + "</td>");
						html.append("<td class='qs-odd'>" + MathUtils.fmtPct(results.get(i).getpBttsYes()) + "</td>");
						html.append(
								"<td class='qs-pick'><span class='pick'>" + results.get(i).getPick() + "</span></td>");
						html.append("<td class='qs-score'>" + results.get(i).getScoreline() + "</td>");
						html.append("</tr>");
						html.append("</tbody>");
						html.append("</table>");
						html.append("</div>");
					}

					int rekabetMacCount = Math.min(10, teamHistory.getRekabetGecmisi().size());
					int sonMaclarHomeCount = Math.min(10, teamHistory.getSonMaclar(1).size());
					int sonMaclarAwayCount = Math.min(10, teamHistory.getSonMaclar(2).size());

					html.append("<div class='history'>");

					// Takım istatistikleri
					html.append("<div class='team-stats'>");
					html.append("<p style='margin-top:8px; font-size:0.9em;'>");
					html.append("Bakılan maç sayısı: Rekabet - ").append(rekabetMacCount)
							.append(" | Ev sahibi son maçlar  - ").append(sonMaclarHomeCount)
							.append(" | Deplasman son maçlar  - ").append(sonMaclarAwayCount);
					html.append("</p>");
					html.append("</div>");

					html.append("<div class='history-section'>");
					html.append("<strong>").append(teamHistory.getTeamName()).append("</strong>");
					html.append("</div>");

					// Rekabet Geçmişi
					if (!teamHistory.getRekabetGecmisi().isEmpty()) {
						html.append("<div class='history-section'>");
						html.append("<h5>Rekabet Geçmişi (").append(teamHistory.getRekabetGecmisi().size())
								.append(" maç):</h5>");

						int count = 0;
						for (MatchResult matchResult : teamHistory.getRekabetGecmisi()) {
							if (count >= 10)
								break; // İlk 10 maçı göster

							String resultClass = getResultClass(matchResult, teamHistory.getTeamName());
							html.append("<div class='match-result ").append(resultClass).append("'>");
							html.append(matchResult.getMatchDate()).append(" - ");
							html.append(matchResult.getHomeTeam()).append(" ");
							html.append(matchResult.getScoreString()).append(" ");
							html.append(matchResult.getAwayTeam());
							html.append(" [").append(matchResult.getTournament()).append("]");
							html.append("</div>");
							count++;
						}

						if (teamHistory.getRekabetGecmisi().size() > 10) {
							html.append("<p><em>... ve ").append(teamHistory.getRekabetGecmisi().size() - 10)
									.append(" maç daha</em></p>");
						}
						html.append("</div>");
					}

					// Son Maçlar Home
					if (!teamHistory.getSonMaclar(1).isEmpty()) {
						html.append("<div class='history-section'>");
						html.append("<h5>Ev Sahibi Son Maçlar (").append(teamHistory.getSonMaclar(1).size())
								.append(" maç):</h5>");

						int count = 0;
						for (MatchResult matchResult : teamHistory.getSonMaclar(1)) {
							if (count >= 10)
								break; // İlk 10 maçı göster

							String resultClass = getResultClass(matchResult, teamHistory.getTeamName());
							html.append("<div class='match-result ").append(resultClass).append("'>");
							html.append(matchResult.getMatchDate()).append(" - ");
							html.append(matchResult.getHomeTeam()).append(" ");
							html.append(matchResult.getScoreString()).append(" ");
							html.append(matchResult.getAwayTeam());
							html.append("</div>");
							count++;
						}

						if (teamHistory.getSonMaclar(1).size() > 10) {
							html.append("<p><em>... ve ").append(teamHistory.getSonMaclar(2).size() - 10)
									.append(" maç daha</em></p>");
						}
						html.append("</div>");
					}

					// Son Maçlar Away
					if (!teamHistory.getSonMaclar(2).isEmpty()) {
						html.append("<div class='history-section'>");
						html.append("<h5>Deplasman Son Maçlar (").append(teamHistory.getSonMaclar(2).size())
								.append(" maç):</h5>");

						int count = 0;
						for (MatchResult matchResult : teamHistory.getSonMaclar(2)) {
							if (count >= 10)
								break; // İlk 10 maçı göster

							String resultClass = getResultClass(matchResult, teamHistory.getTeamName());
							html.append("<div class='match-result ").append(resultClass).append("'>");
							html.append(matchResult.getMatchDate()).append(" - ");
							html.append(matchResult.getHomeTeam()).append(" ");
							html.append(matchResult.getScoreString()).append(" ");
							html.append(matchResult.getAwayTeam());
							html.append("</div>");
							count++;
						}

						if (teamHistory.getSonMaclar(2).size() > 10) {
							html.append("<p><em>... ve ").append(teamHistory.getSonMaclar(2).size() - 10)
									.append(" maç daha</em></p>");
						}
						html.append("</div>");
					}

					html.append("</div>");
					processedTeamCount++;
				} else {
					html.append("<div class='no-data'>Bu maç için geçmiş veri bulunamadı</div>");
				}

			} else {
				html.append("<div class='no-data'>Detay URL'si bulunamadı</div>");
			}

			html.append("<p><small>Element #").append(match.getIndex()).append("</small></p>");
			html.append("</div>");
		}

		// Final istatistikleri
		html.append("<div class='stats'>");
		html.append("<h3>Final İstatistikleri</h3>");
		html.append("<p>- Toplam maç: ").append(matches.size()).append("</p>");
		html.append("<p>- Detay URL'si olan: ").append(detailUrlCount).append("</p>");
		html.append("<p>- Başarıyla geçmişi çekilen: ").append(processedTeamCount).append("</p>");
		html.append("<p>- Toplam takım: ").append(historyManager.getTotalTeams()).append("</p>");
		html.append("<p>- Başarı oranı: ").append(
				detailUrlCount > 0 ? String.format("%.1f%%", (processedTeamCount * 100.0 / detailUrlCount)) : "0%")
				.append("</p>");
		html.append("</div>");

		html.append("<p style='text-align: center; color: #666; margin-top: 30px;'>");
		html.append("Bu veriler otomatik olarak çekilmiştir - Son güncelleme: ")
				.append(LocalDateTime.now(istanbulZone));
		html.append("</p>");

		html.append("<script>");
		html.append("function toggleHistory(button) {");
		html.append("  const matchDiv = button.closest('.match');");
		html.append("  const historySections = matchDiv.querySelectorAll('.history .history-section');");
		html.append("  let isHidden = historySections[0].style.display === 'none';");
		html.append("  historySections.forEach(section => {");
		html.append("    section.style.display = isHidden ? 'block' : 'none';");
		html.append("  });");
		html.append("  button.textContent = isHidden ? 'Gizle' : 'Göster';");
		html.append("}");
		html.append("document.querySelectorAll('.history .history-section').forEach(s => s.style.display = 'none');");
		html.append("</script>");

		html.append("</body></html>");

		// Dosyaları kaydet
		File dir = new File("public");
		if (!dir.exists())
			dir.mkdirs();

		// HTML dosyasını kaydet
		try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
			fw.write(html.toString());
			html = null; // Reference'i sil
			System.gc(); // HTML string'i temizle
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void generateHtmlForSublist(List<LastPrediction> predictions, String fileName) {
		StringBuilder html = new StringBuilder();

		html.append("<!DOCTYPE html>\n");
		html.append("<html lang='tr'>\n");
		html.append("<head>\n");
		html.append("<meta charset='UTF-8'>\n");
		html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
		html.append("<title>✅ Hazır Kupon</title>\n");
		html.append(
				"<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css'>\n");
		html.append("<style>\n");

		/* --- Genel Stil --- */
		html.append(
				"body { font-family: Arial, sans-serif; background-color: #f7f8fa; margin: 0; padding: 20px; color: #222; }\n");
		html.append("h1 { text-align: center; margin-bottom: 20px; color: #333; font-size: 22px; }\n");

		/* --- Tablo --- */
		html.append("table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; ");
		html.append("box-shadow: 0 2px 8px rgba(0,0,0,0.1); overflow: hidden; }\n");
		html.append("th, td { padding: 10px 12px; text-align: left; }\n");
		html.append("th { background-color: #0077cc; color: white; font-size: 15px; }\n");
		html.append("tr:nth-child(even) { background-color: #f3f6fa; }\n");
		html.append("tr:hover { background-color: #eaf3ff; }\n");
		html.append("td { font-size: 14px; border-bottom: 1px solid #ddd; }\n");

		/* --- İkon hizalama --- */
		html.append(
				"td i, td svg, td img { display:inline-block; vertical-align:middle; margin-right:4px; color:#0077cc; }\n");

		/* --- Sütun oranları --- */
		html.append("th:nth-child(1), td:nth-child(1) { width: 60px; text-align: left; white-space: nowrap; }\n");
		html.append(
				"th:nth-child(2), td:nth-child(2) { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n");
		html.append("th:nth-child(3), td:nth-child(3) { width: auto; }\n");
		html.append(
				"th:nth-child(4), td:nth-child(4) { width: 120px; text-align: left; color: #333; font-weight: bold; }\n");

		html.append(".match { font-weight: bold; color: #1a1a1a; }\n");
		html.append(".prediction { color: #444; white-space: pre-line; }\n");

		/* --- Mobil görünüm (max 600px) --- */
		html.append("@media (max-width: 600px) {\n");
		html.append("  table, thead, tbody, th, td, tr { display: block; width: 100%; }\n");
		html.append("  thead { display: none; }\n");
		html.append(
				"  tr { margin-bottom: 12px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); background: #fff; padding: 8px; }\n");
		html.append("  td { border: none; padding: 6px 8px; }\n");
		html.append("  td i { margin-right: 6px; }\n");
		html.append("  td span.label { display:block; font-weight:bold; color:#0077cc; margin-bottom:3px; }\n");
		html.append("}\n");

		html.append("</style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("<h1>✅ Hazır Kupon</h1>\n");
		html.append("<table>\n");
		html.append(
				"<thead><tr><th>🕒 Saat</th><th>⚽ Maç</th><th>🎯 Tahmin</th><th>📊 Skor Tahmini</th></tr></thead>\n");
		html.append("<tbody>\n");

		for (LastPrediction p : predictions) {
			html.append("<tr>");
			html.append("<td><i class='fa-regular fa-clock'></i>").append(p.getTime()).append("</td>");
			html.append("<td class='match'><i class='fa-solid fa-futbol'></i>").append(p.getName()).append("</td>");
			html.append("<td class='prediction'><i class='fa-solid fa-bullseye'></i>").append(p.preditionsToString())
					.append("</td>");
			html.append("<td class='score'><i class='fa-solid fa-chart-line'></i>")
					.append(p.getScore() != null ? p.getScore() : "-").append("</td>");
			html.append("</tr>\n");
		}

		html.append("</tbody></table>\n");
		html.append("</body>\n</html>");

		File dir = new File("public");
		if (!dir.exists())
			dir.mkdirs();

		try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
			fw.write(html.toString());
			html = null;
			System.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getResultClass(MatchResult match, String teamName) {
		String result = match.getResult();

		// Takımın ev sahibi mi deplasman mı olduğunu kontrol et
		boolean isHome = teamName.contains(match.getHomeTeam());

		if (result.equals("D")) {
			return "draw";
		} else if ((isHome && result.equals("H")) || (!isHome && result.equals("A"))) {
			return "win";
		} else {
			return "loss";
		}
	}
}

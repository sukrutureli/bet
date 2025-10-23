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
		html.append(
				"body { font-family: 'Segoe UI', Roboto, Arial, sans-serif; margin: 0; padding: 20px; background-color: #f3f6fa; color: #222; }");
		html.append("h1 { text-align: center; color: #004d80; margin-bottom: 25px; font-size: 26px; }");
		html.append("p { margin: 6px 0; }");

		/* --- Genel kutu ve kart yapısı --- */
		html.append(
				".match { background: #fff; border: 1px solid #dce3ec; margin: 18px 0; padding: 18px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: transform 0.2s, box-shadow 0.2s; }");
		html.append(".match:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.12); }");
		html.append(".match.insufficient { background-color: #fff1f1; border-left: 4px solid #dc3545; }");

		/* --- Başlık --- */
		/*html.append(
				".match-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }");
		html.append(".match-name { font-weight: 700; color: #003366; font-size: 1.1em; }");
		html.append(".match-time { color: #666; font-size: 0.9em; }");
		html.append(
				".match-header button { background: linear-gradient(180deg,#007bff,#0062cc); border: none; color: #fff; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.9em; }");
		html.append(".match-header button:hover { background: linear-gradient(180deg,#0069d9,#005cbf); }");*/
		
		html.append(".match-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }");
		html.append(".match-info { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }");
		html.append(".match-separator { color: #aaa; margin: 0 5px; }");
		html.append(".match-name { font-weight: 700; color: #003366; font-size: 1.1em; }");
		html.append(".match-time { color: #666; font-size: 0.9em; font-style: italic; }");
		html.append(".match-header button { background: linear-gradient(180deg,#007bff,#0062cc); border: none; color: #fff; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.9em; }");
		html.append(".match-header button:hover { background: linear-gradient(180deg,#0069d9,#005cbf); }");
		html.append(".match-mbs { font-weight: bold; border-radius: 6px; padding: 3px 7px; font-size: 0.85em; min-width: 55px; text-align: center; }");
		html.append(".match-mbs-1 { background: #dc3545; color: #fff; }"); // kırmızı
		html.append(".match-mbs-2 { background: #fd7e14; color: #fff; }"); // turuncu
		html.append(".match-mbs-3 { background: #28a745; color: #fff; }"); // yeşil


		/* --- Oran tablosu --- */
		html.append(
				".odds { background: #f8fafc; border: 1px solid #dbe2ea; padding: 12px; border-radius: 8px; margin: 12px 0; }");
		html.append(".odds strong { color: #004d80; }");
		html.append(".odds table { width: 100%; border-collapse: collapse; margin-top: 8px; }");
		html.append(".odds td { border: 1px solid #ddd; padding: 6px; font-size: 0.9em; text-align: center; }");
		html.append(".odds td strong { color: #111; }");

		/* --- Hızlı özet tablosu --- */
		html.append(
				".quick-summary table.qs { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ccd6e0; border-radius: 8px; overflow: hidden; margin-top: 10px; }");
		html.append(
				".quick-summary th, .quick-summary td { padding: 8px; text-align: center; border: 1px solid #e1e7ef; }");
		html.append(".quick-summary th { background: #f0f5fb; color: #003366; font-weight: 600; }");
		html.append(".quick-summary tr:nth-child(even) { background: #f9fbfd; }");
		html.append(".qs-odd { font-variant-numeric: tabular-nums; color: #333; }");
		html.append(
				".qs-pick .pick { display: inline-block; padding: 3px 10px; border-radius: 12px; background: #e7f1ff; color: #004d80; font-weight: 700; }");
		html.append(".qs-score { color: #111; font-weight: 600; }");

		/* --- Tarih & sonuçlar --- */
		html.append(".history { margin-top: 14px; }");
		html.append(
				".history-section { background: #f4f7fb; border: 1px solid #e1e7ef; padding: 14px; border-radius: 8px; margin: 10px 0; }");
		html.append(".history-section h5 { margin-top: 0; color: #004d80; }");
		html.append(
				".match-result { background: #fff; padding: 6px 10px; margin: 4px 0; border-left: 4px solid #007bff; border-radius: 4px; font-size: 0.9em; transition: background 0.2s; }");
		html.append(".match-result:hover { background: #f3f7ff; }");
		html.append(".match-result.win { border-left-color: #28a745; background-color: #e9f7ef; }");
		html.append(".match-result.draw { border-left-color: #ffc107; background-color: #fff7e6; }");
		html.append(".match-result.loss { border-left-color: #dc3545; background-color: #fdeaea; }");

		/* --- Takım istatistikleri --- */
		html.append(
				".team-stats { background: #e3f2fd; color: #0c5460; padding: 10px 12px; border-radius: 6px; margin: 8px 0; font-size: 0.9em; }");
		html.append(
				".stats { background: #fff; border: 1px solid #dbe2ea; padding: 18px; margin: 20px 0; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }");
		html.append(".stats h3 { color: #004d80; margin-top: 0; }");

		html.append(
				".odds-mini { background:#f9fbfd; border:1px solid #dbe2ea; border-radius:10px; padding:10px 14px; margin:12px 0; font-size:0.9em; box-shadow:0 1px 3px rgba(0,0,0,0.05);}");
		html.append(".odds-mini h4 { margin:0 0 6px 0; color:#004d80; font-size:0.95em; font-weight:600;}");
		html.append(
				".odds-grid { display:grid; grid-template-columns:repeat(auto-fit, minmax(100px,1fr)); gap:6px; text-align:center;}");
		html.append(".odds-cell { border:1px solid #e0e6ec; border-radius:6px; padding:6px 4px; background:#fff;}");
		html.append(".odds-label { font-weight:700; color:#004080; }");
		html.append(
				".odds-line { display:flex; justify-content:center; align-items:center; gap:4px; font-weight:600; color:#222;}");
		html.append(".odds-value { color:#000; font-weight:700;}");
		html.append(".odds-pct { display:block; color:#777; margin-top:2px;}");
		html.append("@media (max-width:600px){.odds-grid{grid-template-columns:repeat(2,1fr);} }");

		/* --- Uyarı, no-data --- */
		html.append(
				".no-data { color: #999; font-style: italic; padding: 20px; text-align: center; background: #fff; border: 1px dashed #ccc; border-radius: 8px; }");

		/* --- Footer --- */
		html.append("footer { text-align: center; color: #777; font-size: 0.85em; margin-top: 40px; }");

		/* --- Mobil uyum --- */
		html.append("@media (max-width: 600px) {");
		html.append("  body { padding: 10px; }");
		html.append("  .match { padding: 12px; }");
		html.append("  .match-header { flex-direction: column; align-items: flex-start; gap: 6px; }");
		html.append("  .match-header button { width: 100%; }");
		html.append("  .quick-summary table.qs, .odds table { font-size: 0.8em; }");
		html.append("}");
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
				// html.append("<div class='match'>");

				String mbsClass = "match-mbs-" + match.getOdds().getMbs();
				
				html.append("<div class='match-header'>");
				html.append("  <div class='match-info'>");
				html.append("    <span class='match-time'>" + match.getTime() + "</span>");
				html.append("    <span class='match-separator'>•</span>");
				html.append("    <span class='match-name'>" + match.getName() + "</span>");
				html.append("    <span class='match-separator'>•</span>");
				html.append("    <span class='match-mbs " + mbsClass + "'>" + match.getOdds().getMbs() + "</span>");
				html.append("  </div>");
				html.append("  <button onclick=\"toggleHistory(this)\">Göster</button>");
				html.append("</div>");

				if (teamHistory != null && teamHistory.getTotalMatches() > 0) {
					html.append("<div class='odds-mini'>");
					html.append("<h4>Güncel Oranlar ve Yüzdeler</h4>");

					html.append("<div class='odds-grid'>");

					// MS1
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getMs1(), "MS1") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>MS1:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getMs1())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getMs1())).append("</span>")
							.append("</div>");

					// MSX
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getMs0(), "MSX") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>MSX:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getMsX())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getMs0())).append("</span>")
							.append("</div>");

					// MS2
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getMs2(), "MS2") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>MS2:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getMs2())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getMs2())).append("</span>")
							.append("</div>");
					html.append("</div>");

					// ALT / ÜST / VAR / YOK
					html.append("<div class='odds-grid' style='margin-top:8px;'>");

					// ALT
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getAlt(), "Alt") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>Alt:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getUnder25())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getAlt())).append("</span>")
							.append("</div>");

					// ÜST
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getUst(), "Üst") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>Üst:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getOver25())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getUst())).append("</span>")
							.append("</div>");

					// VAR
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getVar(), "Var") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>KG Var:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getBttsYes())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getVar())).append("</span>")
							.append("</div>");

					// YOK
					html.append(
							"<div class='odds-cell' style='" + teamHistory.getStyle(teamHistory.getYok(), "Yok") + "'>")
							.append("<div class='odds-line'><span class='odds-label'>KG Yok:</span>")
							.append("<span class='odds-value'>").append(match.getOdds().getBttsNo())
							.append("</span></div>").append("<span class='odds-pct'>")
							.append(teamHistory.toStringAsPercentage(teamHistory.getYok())).append("</span>")
							.append("</div>");

					html.append("</div>");
					html.append("</div>");

					int rekabetMacCount = teamHistory.getRekabetGecmisi().size();
					int sonMaclarHomeCount = teamHistory.getSonMaclar(1).size();
					int sonMaclarAwayCount = teamHistory.getSonMaclar(2).size();

					if (sonMaclarHomeCount > 0 && sonMaclarAwayCount > 0) { // herhangi biri NaN ise bu tabloyu ekleme
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

						for (MatchResult matchResult : teamHistory.getRekabetGecmisi()) {
							String resultClass = getResultClass(matchResult, teamHistory.getTeamName());
							html.append("<div class='match-result ").append(resultClass).append("'>");
							html.append(matchResult.getMatchDate()).append(" - ");
							html.append(matchResult.getHomeTeam()).append(" ");
							html.append(matchResult.getScoreString()).append(" ");
							html.append(matchResult.getAwayTeam());
							html.append(" [").append(matchResult.getTournament()).append("]");
							html.append("</div>");
						}
						html.append("</div>");
					}

					// Son Maçlar Home
					if (!teamHistory.getSonMaclar(1).isEmpty()) {
						html.append("<div class='history-section'>");
						html.append("<h5>Ev Sahibi Son Maçlar (").append(teamHistory.getSonMaclar(1).size())
								.append(" maç):</h5>");

						for (MatchResult matchResult : teamHistory.getSonMaclar(1)) {
							String resultClass = getResultClass(matchResult, teamHistory.getTeamName());
							html.append("<div class='match-result ").append(resultClass).append("'>");
							html.append(matchResult.getMatchDate()).append(" - ");
							html.append(matchResult.getHomeTeam()).append(" ");
							html.append(matchResult.getScoreString()).append(" ");
							html.append(matchResult.getAwayTeam());
							html.append("</div>");
						}
						html.append("</div>");
					}

					// Son Maçlar Away
					if (!teamHistory.getSonMaclar(2).isEmpty()) {
						html.append("<div class='history-section'>");
						html.append("<h5>Deplasman Son Maçlar (").append(teamHistory.getSonMaclar(2).size())
								.append(" maç):</h5>");

						for (MatchResult matchResult : teamHistory.getSonMaclar(2)) {
							String resultClass = getResultClass(matchResult, teamHistory.getTeamName());
							html.append("<div class='match-result ").append(resultClass).append("'>");
							html.append(matchResult.getMatchDate()).append(" - ");
							html.append(matchResult.getHomeTeam()).append(" ");
							html.append(matchResult.getScoreString()).append(" ");
							html.append(matchResult.getAwayTeam());
							html.append("</div>");
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
		html.append("<title>💰 Hazır Kupon</title>\n");
		html.append("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css'>\n");
		html.append("<style>\n");

		/* --- Genel Stil --- */
		html.append("body { font-family: Arial, sans-serif; background-color: #f7f8fa; margin: 0; padding: 20px; color: #222; }\n");
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
		html.append("td i, td svg, td img { display:inline-block; vertical-align:middle; margin-right:4px; color:#0077cc; }\n");

		/* --- Sütun oranları --- */
		html.append("th:nth-child(1), td:nth-child(1) { width: 80px; text-align: left; white-space: nowrap; }\n");  // Saat
		html.append("th:nth-child(2), td:nth-child(2) { width: 60px; text-align: center; white-space: nowrap; }\n"); // MBS
		html.append("th:nth-child(3), td:nth-child(3) { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n"); // Maç
		html.append("th:nth-child(4), td:nth-child(4) { width: auto; }\n");
		html.append("th:nth-child(5), td:nth-child(5) { width: 120px; text-align: left; color: #333; font-weight: bold; }\n");

		html.append(".match { font-weight: bold; color: #1a1a1a; }\n");
		html.append(".prediction { color: #444; white-space: pre-line; }\n");

		/* --- MBS renkleri --- */
		html.append(".match-mbs { font-weight: bold; border-radius: 6px; padding: 3px 8px; font-size: 0.85em; min-width: 40px; display:inline-block; text-align:center; }\n");
		html.append(".match-mbs-1 { background: #dc3545; color: #fff; } /* kırmızı */\n");
		html.append(".match-mbs-2 { background: #fd7e14; color: #fff; } /* turuncu */\n");
		html.append(".match-mbs-3 { background: #28a745; color: #fff; } /* yeşil */\n");

		/* --- Mobil görünüm (max 600px) --- */
		html.append("@media (max-width: 600px) {\n");
		html.append("  table, thead, tbody, th, td, tr { display: block; width: 100%; }\n");
		html.append("  thead { display: none; }\n");
		html.append("  tr { margin-bottom: 12px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); background: #fff; padding: 8px; }\n");
		html.append("  td { border: none; padding: 6px 8px; }\n");
		html.append("  td i { margin-right: 6px; }\n");
		html.append("  td span.label { display:block; font-weight:bold; color:#0077cc; margin-bottom:3px; }\n");
		html.append("  .match-mbs { display:inline-block; margin-left:8px; }\n");
		html.append("}\n");

		html.append("</style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("<h1>💰 Hazır Kupon</h1>\n");
		html.append("<table>\n");
		html.append("<thead><tr><th>🕒 Saat</th><th>MBS</th><th>⚽ Maç</th><th>🎯 Tahmin</th><th>📊 Skor Tahmini</th></tr></thead>\n");
		html.append("<tbody>\n");

		// --- TABLO SATIRLARI ---
		for (LastPrediction p : predictions) {
		    String mbsClass = "match-mbs-" + p.getMbs(); // örn. match-mbs-1 / 2 / 3

		    html.append("<tr>");
		    html.append("<td><i class='fa-regular fa-clock'></i>").append(p.getTime()).append("</td>");
		    html.append("<td><span class='match-mbs ").append(mbsClass).append("'>").append(String.valueOf(p.getMbs())).append("</span></td>");
		    html.append("<td class='match'><i class='fa-solid fa-futbol'></i>").append(p.getName()).append("</td>");
		    html.append("<td class='prediction'><i class='fa-solid fa-bullseye'></i>").append(p.preditionsToString()).append("</td>");
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

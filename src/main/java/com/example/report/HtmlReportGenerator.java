package com.example.report;

import com.example.MatchHistoryManager;
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

    public static void generateHtml(List<MatchInfo> matches, MatchHistoryManager historyManager, 
    		List<Match> matchStats, List<PredictionResult> results, String fileName) {

        ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");
     
        // HTML oluşturmaya başla
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<title>İddaa Bülteni</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
        html.append(".match { background: white; border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 8px; }");
        html.append(".match-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }");
        html.append(".match-name { font-weight: bold; color: #333; font-size: 1.1em; }");
        html.append(".match-time { color: #666; }");
        html.append(".odds { background: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0; }");
        html.append(".history { margin-top: 15px; }");
        html.append(".history-section { background: #e9ecef; margin: 10px 0; padding: 15px; border-radius: 5px; }");
        html.append(".match-result { background: white; padding: 8px; margin: 5px 0; border-left: 4px solid #007bff; border-radius: 3px; font-size: 0.9em; }");
        html.append(".win { border-left-color: #28a745; background-color: #d4edda; }");
        html.append(".draw { border-left-color: #ffc107; background-color: #fff3cd; }");
        html.append(".loss { border-left-color: #dc3545; background-color: #f8d7da; }");
        html.append(".team-stats { background: #d1ecf1; color: #0c5460; padding: 10px; border-radius: 5px; margin: 10px 0; }");
        html.append(".no-data { color: #999; font-style: italic; padding: 20px; text-align: center; }");
        html.append(".stats { background: #d1ecf1; color: #0c5460; padding: 15px; margin: 20px 0; border-radius: 8px; }");
        html.append(".export-section { background: #fff; padding: 20px; margin: 20px 0; border: 1px solid #ddd; border-radius: 8px; }");
        html.append(".quick-summary { margin: 10px 0 14px; }");
        html.append(".quick-summary table.qs { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ccc; border-radius: 6px; overflow: hidden; font-size: 0.9em; }");
        html.append(".quick-summary th, .quick-summary td { padding: 6px 8px; text-align: center; border: 1px solid #ddd; }");
        html.append(".quick-summary th { background: #f2f2f2; font-weight: 600; color: #333; }");
        html.append(".quick-summary td.qs-odd { color: #222; font-variant-numeric: tabular-nums; }");
        html.append(".quick-summary td.qs-pick .pick { display: inline-block; padding: 3px 8px; border-radius: 10px; background: #e7f1ff; color: #0056b3; font-weight: 700; }");
        html.append(".quick-summary td.qs-score { color: #444; font-weight: 600; }");

        html.append("</style>");
        html.append("</head><body>");
        html.append("<h1>İddaa Maç Geçmişi Analizi</h1>");
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
        
        System.out.println("Detay URL'si olan " + detailUrlCount);

        html.append("<div class='stats'>");
        html.append("<h3>İstatistikler</h3>");
        html.append("<p>• Toplam maç: ").append(matches.size()).append("</p>");
        html.append("<p>• Detay URL'si olan: ").append(detailUrlCount).append("</p>");
        html.append("<p>• Geçmiş verisi çekilecek: ").append(detailUrlCount).append("</p>");
        html.append("</div>");
        
        // Her maç için geçmiş bilgilerini işle
        System.out.println("\n2. Takım geçmişleri çekiliyor...");
        
        for (int i = 0; i < matches.size(); i++) {
            MatchInfo match = matches.get(i);
            
            html.append("<div class='match'>");
            html.append("<div class='match-header'>");
            html.append("<div class='match-name'>").append(match.getName()).append("</div>");
            html.append("<div class='match-time'>").append(match.getTime()).append("</div>");
            html.append("<button onclick=\"toggleHistory(this)\">Göster/Gizle</button>");
            html.append("</div>");
             
            // Detay URL'si varsa geçmiş verilerini çek
            if (match.hasDetailUrl()) {
                System.out.println("Geçmiş çekiliyor " + (i+1) + "/" + matches.size() + ": " + match.getName());
                
                
                TeamMatchHistory teamHistory = historyManager.getTeamHistories().get(i);
                
                if (teamHistory != null && teamHistory.getTotalMatches() > 0) {
                    html.append("<div class='odds' style='margin-top:10px;'>");
                    html.append("<strong>Güncel Oranlar:</strong>");
                    html.append("<table style='width:100%; border-collapse: collapse; margin-top:6px; text-align:center;'>");

                    html.append("<tr>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getMs1(), "MS1") + "'>MS1<br><strong>")
                        .append(match.getOdds().getMs1()).append("</strong></td>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getMs0(), "MSX") + "'>MSX<br><strong>")
                        .append(match.getOdds().getMsX()).append("</strong></td>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getMs2(), "MS2") + "'>MS2<br><strong>")
                        .append(match.getOdds().getMs2()).append("</strong></td>");
                    html.append("</tr>");

                    html.append("<tr>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getAlt(), "Alt") + "'>Alt<br><strong>")
                        .append(match.getOdds().getUnder25()).append("</strong></td>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getUst(), "Üst") + "'>Üst<br><strong>")
                        .append(match.getOdds().getOver25()).append("</strong></td>");
                    html.append("<td style='padding:6px; border:1px solid #ccc;'>-</td>");
                    html.append("</tr>");

                    html.append("<tr>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getVar(), "Var") + "'>Var<br><strong>")
                        .append(match.getOdds().getBttsYes()).append("</strong></td>");
                    html.append("<td style='padding:6px; border:1px solid #ccc; " + teamHistory.getStyle(teamHistory.getYok(), "Yok") + "'>Yok<br><strong>")
                        .append(match.getOdds().getBttsNo()).append("</strong></td>");
                    html.append("<td style='padding:6px; border:1px solid #ccc;'>-</td>");
                    html.append("</tr>");

                    html.append("</table>");
                    html.append("</div>");
                    
                    // stats eklendi
                    html.append("<div class='quick-summary'>");
                    html.append("<table class='qs'>");
                    html.append("<thead>");
                    html.append("<tr>");
                    html.append("<th>MS1</th>");
                    html.append("<th>MSX</th>");
                    html.append("<th>MS2</th>");
                    html.append("<th>�st 2.5</th>");
                    html.append("<th>KG Var</th>");
                    html.append("<th>Se�im</th>");
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
                    html.append("<td class='qs-pick'><span class='pick'>" + results.get(i).getPick() + "</span></td>");
                    html.append("<td class='qs-score'>" + results.get(i).getScoreline() + "</td>");
                    html.append("</tr>");
                    html.append("</tbody>");
                    html.append("</table>");
                    html.append("</div>");


                    int rekabetMacCount = Math.min(10, teamHistory.getRekabetGecmisi().size());
                    int sonMaclarCount = Math.min(10, teamHistory.getSonMaclar(1).size()) + 
                                            Math.min(10, teamHistory.getSonMaclar(2).size());
              
                    html.append("<div class='history'>");
                    html.append("<h4>Geçmiş Maç Analizi:</h4>");
                    
                    // Takım istatistikleri
                    html.append("<div class='team-stats'>");
                    html.append("<strong>").append(teamHistory.getTeamName()).append("</strong>");
                    html.append("<table style='width:100%; border-collapse: collapse; margin-top:10px;'>");
                    html.append("<tr>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getMs1(), "MS1")).append("</td>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getMs0(), "MSX")).append("</td>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getMs2(), "MS2")).append("</td>");
                    html.append("</tr>");
                    html.append("<tr>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getAlt(), "Alt")).append("</td>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getUst(), "Üst")).append("</td>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>-</td>");
                    html.append("</tr>");
                    html.append("<tr>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getYok(), "Yok")).append("</td>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>").append(teamHistory.toStringAsPercentage(teamHistory.getVar(), "Var")).append("</td>");
                    html.append("<td style='padding:4px; border:1px solid #ccc;'>-</td>");
                    html.append("</tr>");
                    html.append("</table>");
                    html.append("<p style='margin-top:8px; font-size:0.9em;'>");
                    html.append("Bakılan maç sayısı: Rekabet - ").append(rekabetMacCount).append(" | Son maçlar - ").append(sonMaclarCount);
                    html.append("</p>");
                    html.append("</div>");

                    
                    // Rekabet Geçmişi
                    if (!teamHistory.getRekabetGecmisi().isEmpty()) {
                        html.append("<div class='history-section'>");
                        html.append("<h5>Rekabet Geçmişi (").append(teamHistory.getRekabetGecmisi().size()).append(" maç):</h5>");
                        
                        int count = 0;
                        for (MatchResult matchResult : teamHistory.getRekabetGecmisi()) {
                            if (count >= 10) break; // İlk 10 maçı göster
                            
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
                            html.append("<p><em>... ve ").append(teamHistory.getRekabetGecmisi().size() - 10).append(" maç daha</em></p>");
                        }
                        html.append("</div>");
                    }
                    
                    // Son Maçlar Home
                    if (!teamHistory.getSonMaclar(1).isEmpty()) {
                        html.append("<div class='history-section'>");
                        html.append("<h5>Ev Sahibi Son Maçlar (").append(teamHistory.getSonMaclar(1).size()).append(" maç):</h5>");
                        
                        int count = 0;
                        for (MatchResult matchResult : teamHistory.getSonMaclar(1)) {
                            if (count >= 10) break; // İlk 10 maçı göster
                            
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
                            html.append("<p><em>... ve ").append(teamHistory.getSonMaclar(2).size() - 10).append(" maç daha</em></p>");
                        }
                        html.append("</div>");
                    }

                    // Son Maçlar Away
                    if (!teamHistory.getSonMaclar(2).isEmpty()) {
                        html.append("<div class='history-section'>");
                        html.append("<h5>Deplasman Son Maçlar (").append(teamHistory.getSonMaclar(2).size()).append(" maç):</h5>");
                        
                        int count = 0;
                        for (MatchResult matchResult : teamHistory.getSonMaclar(2)) {
                            if (count >= 10) break; // İlk 10 maçı göster
                            
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
                            html.append("<p><em>... ve ").append(teamHistory.getSonMaclar(2).size() - 10).append(" maç daha</em></p>");
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
            
            // Her 20 maçta bir progress yazdır
            if ((i + 1) % 20 == 0) {
                System.out.println("İşlendi: " + (i + 1) + "/" + matches.size());
            }
        }
        
        // Final istatistikleri
        html.append("<div class='stats'>");
        html.append("<h3>Final İstatistikleri</h3>");
        html.append("<p>• Toplam maç: ").append(matches.size()).append("</p>");
        html.append("<p>• Detay URL'si olan: ").append(detailUrlCount).append("</p>");
        html.append("<p>• Başarıyla geçmişi çekilen: ").append(processedTeamCount).append("</p>");
        html.append("<p>• Toplam takım: ").append(historyManager.getTotalTeams()).append("</p>");
        html.append("<p>• Başarı oranı: ").append(detailUrlCount > 0 ? String.format("%.1f%%", (processedTeamCount * 100.0 / detailUrlCount)) : "0%").append("</p>");
        html.append("</div>");
        
        html.append("<p style='text-align: center; color: #666; margin-top: 30px;'>");
        html.append("Bu veriler otomatik olarak çekilmiştir • Son güncelleme: ").append(LocalDateTime.now(istanbulZone));
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
        if (!dir.exists()) dir.mkdirs();

        // HTML dosyasını kaydet
        try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
            fw.write(html.toString());
            html = null; // Reference'i sil
            System.gc(); // HTML string'i temizle
        } catch (IOException e) {
			e.printStackTrace();
		}
        
        System.out.println("\n=== Scraping Tamamlandı ===");
        System.out.println("✓ public/index.html başarıyla oluşturuldu!");
        System.out.println("✓ Toplam maç sayısı: " + matches.size());
        System.out.println("✓ Detay URL'li maç sayısı: " + detailUrlCount);
        System.out.println("✓ Başarıyla geçmişi çekilen: " + processedTeamCount);
        System.out.println("✓ Toplam takım: " + historyManager.getTotalTeams());
        System.out.println("✓ Toplam geçmiş maç: " + historyManager.getTotalMatches());
        System.out.println("✓ Bitiş zamanı: " + LocalDateTime.now(istanbulZone));
            
            
        
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

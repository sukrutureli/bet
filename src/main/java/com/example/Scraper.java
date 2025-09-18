package com.example;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {
        MatchScraper scraper = null;
        MatchHistoryManager historyManager = new MatchHistoryManager();
        
        try {
            System.out.println("=== İddaa Scraper Başlatılıyor ===");
            System.out.println("Zaman: " + LocalDateTime.now());
            
            // Scraper'ı başlat
            scraper = new MatchScraper();
            
            // Ana sayfa verilerini çek
            System.out.println("\n1. Ana sayfa maçları çekiliyor...");
            List<MatchInfo> matches = scraper.scrapeMainPage();
            
            System.out.println("Ana sayfadan " + matches.size() + " maç çekildi");
            
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
            html.append("</style>");
            html.append("</head><body>");
            html.append("<h1>İddaa Maç Geçmişi Analizi</h1>");
            html.append("<p>Son güncelleme: " + LocalDateTime.now() + "</p>");
            
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
                html.append("</div>");
                
                html.append("<div class='odds'>");
                html.append("<strong>Güncel Oranlar:</strong> ");
                html.append("1: ").append(match.getOdd1()).append(" | ");
                html.append("X: ").append(match.getOddX()).append(" | ");
                html.append("2: ").append(match.getOdd2());
                html.append("</div>");
                
                // Detay URL'si varsa geçmiş verilerini çek
                if (match.hasDetailUrl()) {
                    System.out.println("Geçmiş çekiliyor " + (i+1) + "/" + matches.size() + ": " + match.getName());
                    
                    try {
                        TeamMatchHistory teamHistory = scraper.scrapeTeamHistory(match.getDetailUrl(), match.getName());
                        
                        if (teamHistory != null && teamHistory.getTotalMatches() > 0) {
                            historyManager.addTeamHistory(teamHistory);
                            
                            html.append("<div class='history'>");
                            html.append("<h4>Geçmiş Maç Analizi:</h4>");
                            
                            // Takım istatistikleri
                            html.append("<div class='team-stats'>");
                            html.append("<strong>").append(teamHistory.getTeamName()).append("</strong><br>");
                            html.append("Toplam Maç: ").append(teamHistory.getTotalMatches()).append(" | ");
                            html.append("Galibiyet: ").append(teamHistory.getWinCount()).append(" | ");
                            html.append("Beraberlik: ").append(teamHistory.getDrawCount()).append(" | ");
                            html.append("Mağlubiyet: ").append(teamHistory.getLossCount()).append(" | ");
                            html.append("Başarı Oranı: ").append(String.format("%.1f%%", teamHistory.getWinRate()));
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
                            
                            // Son Maçlar
                            if (!teamHistory.getSonMaclar().isEmpty()) {
                                html.append("<div class='history-section'>");
                                html.append("<h5>Son Maçlar (").append(teamHistory.getSonMaclar().size()).append(" maç):</h5>");
                                
                                int count = 0;
                                for (MatchResult matchResult : teamHistory.getSonMaclar()) {
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
                                
                                if (teamHistory.getSonMaclar().size() > 10) {
                                    html.append("<p><em>... ve ").append(teamHistory.getSonMaclar().size() - 10).append(" maç daha</em></p>");
                                }
                                html.append("</div>");
                            }
                            
                            html.append("</div>");
                            processedTeamCount++;
                        } else {
                            html.append("<div class='no-data'>Bu maç için geçmiş veri bulunamadı</div>");
                        }
                        
                        // Rate limiting - 3 saniye bekle
                        Thread.sleep(3000);
                        
                    } catch (Exception e) {
                        System.out.println("Geçmiş çekme hatası: " + e.getMessage());
                        html.append("<div class='no-data'>Geçmiş veri çekme hatası: ").append(e.getMessage()).append("</div>");
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
            html.append("<p>• Toplam geçmiş maç: ").append(historyManager.getTotalMatches()).append("</p>");
            html.append("<p>• Başarı oranı: ").append(detailUrlCount > 0 ? String.format("%.1f%%", (processedTeamCount * 100.0 / detailUrlCount)) : "0%").append("</p>");
            html.append("</div>");
            
            // Export bölümü
            html.append("<div class='export-section'>");
            html.append("<h3>Veri Export</h3>");
            html.append("<p>Toplanan veriler aşağıdaki formatlarda kaydedildi:</p>");
            html.append("<p>• <strong>CSV:</strong> public/match_results.csv</p>");
            html.append("<p>• <strong>JSON:</strong> public/match_results.json</p>");
            html.append("</div>");
            
            html.append("<p style='text-align: center; color: #666; margin-top: 30px;'>");
            html.append("Bu veriler otomatik olarak çekilmiştir • Son güncelleme: ").append(LocalDateTime.now());
            html.append("</p>");
            html.append("</body></html>");
            
            // Dosyaları kaydet
            File dir = new File("public");
            if (!dir.exists()) dir.mkdirs();
            
            // HTML dosyasını kaydet
            try (FileWriter fw = new FileWriter(new File(dir, "index.html"))) {
                fw.write(html.toString());
            }
            
            // CSV dosyasını kaydet
            try (FileWriter fw = new FileWriter(new File(dir, "match_results.csv"))) {
                fw.write(historyManager.toCsvString());
            }
            
            // JSON dosyasını kaydet
            try (FileWriter fw = new FileWriter(new File(dir, "match_results.json"))) {
                fw.write(historyManager.toJsonString());
            }
            
            System.out.println("\n=== Scraping Tamamlandı ===");
            System.out.println("✓ public/index.html başarıyla oluşturuldu!");
            System.out.println("✓ public/match_results.csv başarıyla oluşturuldu!");
            System.out.println("✓ public/match_results.json başarıyla oluşturuldu!");
            System.out.println("✓ Toplam maç sayısı: " + matches.size());
            System.out.println("✓ Detay URL'li maç sayısı: " + detailUrlCount);
            System.out.println("✓ Başarıyla geçmişi çekilen: " + processedTeamCount);
            System.out.println("✓ Toplam takım: " + historyManager.getTotalTeams());
            System.out.println("✓ Toplam geçmiş maç: " + historyManager.getTotalMatches());
            System.out.println("✓ Bitiş zamanı: " + LocalDateTime.now());
            
            // Console'da özet bilgileri yazdır
            System.out.println("\n=== Takım Geçmişi Özeti ===");
            for (TeamMatchHistory teamHistory : historyManager.getTeamHistories()) {
                System.out.println(teamHistory.toString());
            }
            
        } catch (Exception e) {
            System.out.println("GENEL HATA: " + e.getMessage());
            e.printStackTrace();
            
            // Hata durumunda da HTML oluştur
            try {
                File dir = new File("public");
                if (!dir.exists()) dir.mkdirs();
                
                try (FileWriter fw = new FileWriter(new File(dir, "index.html"))) {
                    fw.write("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Hata</title></head><body>");
                    fw.write("<h1>Scraper Hatası</h1>");
                    fw.write("<p>Hata: " + e.getMessage() + "</p>");
                    fw.write("<p>Zaman: " + LocalDateTime.now() + "</p>");
                    if (historyManager.getTotalTeams() > 0) {
                        fw.write("<p>Kısmi veri mevcut: " + historyManager.getTotalTeams() + " takım, " + historyManager.getTotalMatches() + " maç</p>");
                    }
                    fw.write("</body></html>");
                }
                
                // Kısmi veri varsa onu da kaydet
                if (historyManager.getTotalMatches() > 0) {
                    try (FileWriter fw = new FileWriter(new File(dir, "match_results.csv"))) {
                        fw.write(historyManager.toCsvString());
                    }
                    try (FileWriter fw = new FileWriter(new File(dir, "match_results.json"))) {
                        fw.write(historyManager.toJsonString());
                    }
                }
                
            } catch (Exception writeError) {
                System.out.println("HTML yazma hatası: " + writeError.getMessage());
            }
        } finally {
            if (scraper != null) {
                scraper.close();
            }
        }
    }
    
    private static String getResultClass(MatchResult match, String teamName) {
        String result = match.getResult();
        
        // Takımın ev sahibi mi deplasman mı olduğunu kontrol et
        boolean isHome = match.getHomeTeam().contains(teamName) || teamName.contains(match.getHomeTeam());
        
        if (result.equals("D")) {
            return "draw";
        } else if ((isHome && result.equals("H")) || (!isHome && result.equals("A"))) {
            return "win";
        } else {
            return "loss";
        }
    }
}
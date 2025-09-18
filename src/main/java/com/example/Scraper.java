package com.example;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {
        MatchScraper scraper = null;
        
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
            html.append(".match-name { font-weight: bold; color: #333; }");
            html.append(".match-time { color: #666; }");
            html.append(".odds { background: #f8f9fa; padding: 10px; border-radius: 5px; }");
            html.append(".details { margin-top: 15px; padding: 10px; background: #e9ecef; border-radius: 5px; font-size: 0.9em; }");
            html.append(".no-details { color: #999; font-style: italic; }");
            html.append(".stats { background: #d1ecf1; color: #0c5460; padding: 15px; margin: 20px 0; border-radius: 8px; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<h1>İddaa Güncel Bülteni</h1>");
            html.append("<p>Son güncelleme: " + LocalDateTime.now() + "</p>");
            
            // İstatistik bilgileri
            int detailUrlCount = 0;
            int processedDetailCount = 0;
            
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
            html.append("<p>• Detay verisi çekilecek: ").append(detailUrlCount).append("</p>");
            html.append("</div>");
            
            // Her maç için bilgileri işle
            System.out.println("\n2. Detay sayfalar işleniyor...");
            
            for (int i = 0; i < matches.size(); i++) {
                MatchInfo match = matches.get(i);
                
                html.append("<div class='match'>");
                html.append("<div class='match-header'>");
                html.append("<div class='match-name'>").append(match.getName()).append("</div>");
                html.append("<div class='match-time'>").append(match.getTime()).append("</div>");
                html.append("</div>");
                
                html.append("<div class='odds'>");
                html.append("<strong>Oranlar:</strong> ");
                html.append("1: ").append(match.getOdd1()).append(" | ");
                html.append("X: ").append(match.getOddX()).append(" | ");
                html.append("2: ").append(match.getOdd2());
                html.append("</div>");
                
                // Detay URL'si varsa detay sayfayı çek
                if (match.hasDetailUrl()) {
                    System.out.println("Detay çekiliyor " + (i+1) + "/" + matches.size() + ": " + match.getName());
                    
                    try {
                        MatchDetails details = scraper.scrapeMatchDetails(match.getDetailUrl());
                        
                        if (details != null) {
                            html.append("<div class='details'>");
                            html.append("<h4>Detay Bilgileri:</h4>");
                            html.append("<p><strong>URL:</strong> <a href='").append(details.getUrl()).append("' target='_blank'>").append(details.getUrl()).append("</a></p>");
                            html.append("<p><strong>Sayfa Başlığı:</strong> ").append(details.getTitle()).append("</p>");
                            html.append("<p><strong>İçerik Özeti:</strong><br>").append(details.getContent()).append("...</p>");
                            html.append("</div>");
                            processedDetailCount++;
                        } else {
                            html.append("<div class='details no-details'>Detay bilgisi alınamadı</div>");
                        }
                        
                        // Rate limiting - 2 saniye bekle
                        Thread.sleep(2000);
                        
                    } catch (Exception e) {
                        System.out.println("Detay çekme hatası: " + e.getMessage());
                        html.append("<div class='details no-details'>Detay çekme hatası: ").append(e.getMessage()).append("</div>");
                    }
                } else {
                    html.append("<div class='details no-details'>Detay URL'si bulunamadı</div>");
                }
                
                html.append("<p><small>Element #").append(match.getIndex()).append("</small></p>");
                html.append("</div>");
                
                // Her 20 maçta bir progress yazdır
                if ((i + 1) % 20 == 0) {
                    System.out.println("İşlendi: " + (i + 1) + "/" + matches.size());
                }
            }
            
            // Final istatistikleri güncelle
            html.append("<div class='stats'>");
            html.append("<h3>Final İstatistikleri</h3>");
            html.append("<p>• Toplam maç: ").append(matches.size()).append("</p>");
            html.append("<p>• Detay URL'si olan: ").append(detailUrlCount).append("</p>");
            html.append("<p>• Başarıyla detayı çekilen: ").append(processedDetailCount).append("</p>");
            html.append("<p>• Başarı oranı: ").append(detailUrlCount > 0 ? String.format("%.1f%%", (processedDetailCount * 100.0 / detailUrlCount)) : "0%").append("</p>");
            html.append("</div>");
            
            html.append("<p style='text-align: center; color: #666; margin-top: 30px;'>");
            html.append("Bu veriler otomatik olarak çekilmiştir • Son güncelleme: ").append(LocalDateTime.now());
            html.append("</p>");
            html.append("</body></html>");
            
            // HTML dosyasını kaydet
            File dir = new File("public");
            if (!dir.exists()) dir.mkdirs();
            
            try (FileWriter fw = new FileWriter(new File(dir, "index.html"))) {
                fw.write(html.toString());
            }
            
            System.out.println("\n=== Scraping Tamamlandı ===");
            System.out.println("✓ public/index.html başarıyla oluşturuldu!");
            System.out.println("✓ Toplam maç sayısı: " + matches.size());
            System.out.println("✓ Detay URL'li maç sayısı: " + detailUrlCount);
            System.out.println("✓ Başarıyla detayı çekilen: " + processedDetailCount);
            System.out.println("✓ Bitiş zamanı: " + LocalDateTime.now());
            
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
                    fw.write("</body></html>");
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
}
package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {
        // ChromeDriver yolu ayarlama - Container'da gerekli değil
        // System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver"); // KALDIR!

        ChromeOptions options = new ChromeOptions();
        
        // Container için gerekli ayarlar
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // Selenium container'da Chrome binary path
        options.setBinary("/usr/bin/google-chrome");
        
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);

        try {
            String url = "https://www.nesine.com/iddaa?et=1&le=2&ocg=MS-2%2C5&gt=Pop%C3%BCler";
            driver.get(url);
            
            System.out.println("Sayfa yüklendi, URL: " + driver.getCurrentUrl());

            // Sayfanın tam yüklenmesini bekle - daha uzun süre
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            // Önce sayfanın temel yapısının yüklenmesini bekle
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000); // JavaScript'in çalışması için ek süre
            
            System.out.println("Sayfa başlığı: " + driver.getTitle());
            
            // Scroll işlemi öncesi debug
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Scroll yapmadan önce mevcut elementleri kontrol et
            List<WebElement> initialCheck = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("İlk kontrol - bulunan element sayısı: " + initialCheck.size());
            
            // Scroll stratejisi: Daha yavaş ve daha uzun
            int previousCount = 0;
            int stableCount = 0;
            
            for (int i = 0; i < 40; i++) { // Daha fazla scroll
                // Çok yavaş scroll
                js.executeScript("window.scrollBy(0, 300);");
                Thread.sleep(2500); // Daha uzun bekleme
                
                // Şu anki element sayısını kontrol et
                List<WebElement> currentElements = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                int currentCount = currentElements.size();
                
                System.out.println("Scroll " + (i+1) + " - Element sayısı: " + currentCount);
                
                // Eğer element sayısı değişmiyorsa, daha fazla bekle
                if (currentCount == previousCount) {
                    stableCount++;
                    if (stableCount >= 5) { // Daha sabırlı ol
                        System.out.println("Element sayısı 5 adımdir sabit, ekstra bekleme yapılıyor...");
                        Thread.sleep(5000);
                        
                        // Bir kez daha kontrol et
                        currentElements = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                        if (currentElements.size() == currentCount) {
                            System.out.println("Hala aynı sayı, scroll durduruluyor.");
                            break;
                        } else {
                            System.out.println("Yeni elementler yüklendi, devam ediliyor.");
                            stableCount = 0;
                        }
                    }
                    Thread.sleep(3000); // Ekstra bekleme
                } else {
                    stableCount = 0; // Reset
                }
                
                previousCount = currentCount;
            }
            
            // Son scroll'dan sonra elementlerin tamamen yüklenmesi için uzun bekleme
            System.out.println("Scroll tamamlandı, elementlerin tamamen yüklenmesi için bekleniyor...");
            Thread.sleep(15000); // 15 saniye bekle
            
            // Elementleri tekrar lazy-load için sayfayı baştan sona scroll et (hızlı)
            System.out.println("Lazy loading için hızlı scroll yapılıyor...");
            for (int i = 0; i < 5; i++) {
                js.executeScript("window.scrollTo(0, 0);"); // Başa git
                Thread.sleep(1000);
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);"); // Sona git
                Thread.sleep(2000);
            }

            // Final element kontrolü - farklı selector'lar dene
            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("Final - Toplam maç sayısı (ana selector): " + events.size());
            
            // Alternatif selector'lar dene
            List<WebElement> altEvents1 = driver.findElements(By.cssSelector("div[class*='event-list']"));
            System.out.println("Alternatif 1 - Element sayısı: " + altEvents1.size());
            
            List<WebElement> altEvents2 = driver.findElements(By.cssSelector("div.odd-col"));
            System.out.println("Alternatif 2 - Element sayısı: " + altEvents2.size());
            
            // Sayfa kaynağını debug için yazdır (ilk 1000 karakter)
            String pageSource = driver.getPageSource();
            System.out.println("Sayfa kaynağı (ilk 1000 karakter):");
            System.out.println(pageSource.substring(0, Math.min(1000, pageSource.length())));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            html.append("<title>IDDAA Bülteni</title>");
            html.append("<style>body{font-family:Arial;margin:20px;} .match{border:1px solid #ccc;margin:10px 0;padding:10px;}</style>");
            html.append("</head><body>");
            html.append("<h1>IDDAA Güncel Bülteni</h1>");
            html.append("<p>Debug: Toplam bulunan element sayısı: ").append(events.size()).append("</p>");

            if (events.isEmpty()) {
                html.append("<p style='color:red;'>HATA: Hiç maç bulunamadı!</p>");
                html.append("<p>Sayfa başlığı: ").append(driver.getTitle()).append("</p>");
                html.append("<p>URL: ").append(driver.getCurrentUrl()).append("</p>");
            } else {
                int processedCount = 0;
                int emptyCount = 0;
                
                for (int idx = 0; idx < events.size(); idx++) {
                    WebElement event = events.get(idx);
                    try {
                        // Element görünür mü kontrol et
                        if (!event.isDisplayed()) {
                            System.out.println("Element " + idx + " görünür değil, atlanıyor");
                            continue;
                        }
                        
                        // Elemente yavaşça scroll yap ve bekle
                        js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", event);
                        Thread.sleep(1500); // Daha uzun bekleme
                        
                        // Element DOM'da tam yüklendi mi ekstra kontrol
                        Boolean elementReady = (Boolean) js.executeScript(
                            "return arguments[0].offsetHeight > 0 && arguments[0].offsetWidth > 0;", event
                        );
                        
                        if (!elementReady) {
                            System.out.println("Element " + idx + " henüz hazır değil, ekstra bekleme...");
                            Thread.sleep(2000);
                        }
                        
                        // Maç adı - daha fazla alternatif selector
                        String matchName = "İsim bulunamadı";
                        
                        // 1. Ana selector
                        List<WebElement> nameList = event.findElements(By.cssSelector("div.name > a"));
                        if (!nameList.isEmpty() && !nameList.get(0).getText().trim().isEmpty()) {
                            matchName = nameList.get(0).getText().trim();
                        } else {
                            // 2. Alternatif - herhangi bir link
                            nameList = event.findElements(By.cssSelector("a"));
                            for (WebElement link : nameList) {
                                String linkText = link.getText().trim();
                                if (!linkText.isEmpty() && linkText.contains("-")) { // Takım adları genelde - içerir
                                    matchName = linkText;
                                    break;
                                }
                            }
                            
                            // 3. Alternatif - title attribute'u kontrol et
                            if (matchName.equals("İsim bulunamadı")) {
                                nameList = event.findElements(By.cssSelector("[title]"));
                                for (WebElement titled : nameList) {
                                    String title = titled.getAttribute("title");
                                    if (title != null && !title.trim().isEmpty() && title.contains("-")) {
                                        matchName = title.trim();
                                        break;
                                    }
                                }
                            }
                        }

                        // Maç zamanı - daha fazla alternatif
                        String matchTime = "Zaman bulunamadı";
                        
                        // 1. Ana selector
                        List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
                        if (!timeList.isEmpty() && !timeList.get(0).getText().trim().isEmpty()) {
                            matchTime = timeList.get(0).getText().trim();
                        } else {
                            // 2. Alternatif - herhangi bir time class'ı
                            timeList = event.findElements(By.cssSelector("[class*='time']"));
                            for (WebElement timeEl : timeList) {
                                String timeText = timeEl.getText().trim();
                                if (!timeText.isEmpty() && (timeText.contains(":") || timeText.contains("'"))) {
                                    matchTime = timeText;
                                    break;
                                }
                            }
                            
                            // 3. Alternatif - span içinde saat formatı ara
                            if (matchTime.equals("Zaman bulunamadı")) {
                                timeList = event.findElements(By.cssSelector("span"));
                                for (WebElement span : timeList) {
                                    String spanText = span.getText().trim();
                                    if (spanText.matches("\\d{2}:\\d{2}") || spanText.matches("\\d{2}'")) {
                                        matchTime = spanText;
                                        break;
                                    }
                                }
                            }
                        }

                        // Oranlar - çok daha agresif arama
                        String odd1 = "-", oddX = "-", odd2 = "-";
                        
                        // 1. Ana selector
                        List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));
                        
                        if (oddsList.isEmpty()) {
                            // 2. Alternatif - herhangi bir .odd class'ı
                            oddsList = event.findElements(By.cssSelector(".odd"));
                        }
                        if (oddsList.isEmpty()) {
                            // 3. Alternatif - a tag'i içinde sayı olan
                            oddsList = event.findElements(By.cssSelector("a"));
                            List<WebElement> filteredOdds = new java.util.ArrayList<>();
                            for (WebElement link : oddsList) {
                                String text = link.getText().trim();
                                if (text.matches("\\d+\\.\\d+") || text.matches("\\d+,\\d+")) {
                                    filteredOdds.add(link);
                                }
                            }
                            oddsList = filteredOdds;
                        }
                        if (oddsList.isEmpty()) {
                            // 4. Son alternatif - herhangi bir element içinde oran formatı ara
                            List<WebElement> allElements = event.findElements(By.cssSelector("*"));
                            for (WebElement el : allElements) {
                                String text = el.getText().trim();
                                if (text.matches("\\d+\\.\\d+") && text.length() < 6) { // Kısa oran formatı
                                    oddsList.add(el);
                                    if (oddsList.size() >= 3) break;
                                }
                            }
                        }

                        // Oranları ata
                        if (oddsList.size() > 0) odd1 = oddsList.get(0).getText().trim();
                        if (oddsList.size() > 1) oddX = oddsList.get(1).getText().trim();
                        if (oddsList.size() > 2) odd2 = oddsList.get(2).getText().trim();

                        // Eğer hiçbir veri bulunamadıysa, elementin HTML'ini debug için yazdır
                        if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı") && odd1.equals("-")) {
                            String elementHtml = event.getAttribute("outerHTML");
                            System.out.println("Boş element " + idx + " HTML (ilk 200 karakter): " + 
                                elementHtml.substring(0, Math.min(200, elementHtml.length())));
                            emptyCount++;
                            continue;
                        }

                        html.append("<div class='match'>")
                            .append("<h3>").append(matchTime).append(" - ").append(matchName).append("</h3>")
                            .append("<p>1: ").append(odd1).append(" | X: ").append(oddX).append(" | 2: ").append(odd2).append("</p>")
                            .append("<p><small>Element #").append(idx).append("</small></p>")
                            .append("</div>");
                        
                        processedCount++;
                        
                    } catch (Exception inner) {
                        System.out.println("Element " + idx + " parse edilirken hata: " + inner.getMessage());
                        inner.printStackTrace(); // Stack trace'i de yazdır
                        html.append("<div class='match' style='color:red; border: 2px solid red;'>")
                            .append("<p><strong>Parse hatası - Element #").append(idx).append(":</strong></p>")
                            .append("<p>").append(inner.getMessage()).append("</p>")
                            .append("</div>");
                    }
                }
                html.append("<p><strong>İstatistik:</strong></p>");
                html.append("<p>• Toplam element: ").append(events.size()).append("</p>");
                html.append("<p>• Başarıyla işlenen: ").append(processedCount).append("</p>");
                html.append("<p>• Boş element: ").append(emptyCount).append("</p>");
            }

            html.append("<p>Güncelleme zamanı: ").append(java.time.LocalDateTime.now()).append("</p>");
            html.append("</body></html>");

            File dir = new File("public");
            if (!dir.exists()) dir.mkdirs();

            try (FileWriter fw = new FileWriter(new File(dir, "index.html"))) {
                fw.write(html.toString());
            }

            System.out.println("public/index.html başarıyla oluşturuldu! Toplam maç: " + events.size());

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
                    fw.write("<p>Zaman: " + java.time.LocalDateTime.now() + "</p>");
                    fw.write("</body></html>");
                }
            } catch (Exception writeError) {
                System.out.println("HTML yazma hatası: " + writeError.getMessage());
            }
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
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
                        // Element görünürlük kontrolünü kaldır - çok sıkı kontrol
                        // if (!event.isDisplayed()) {
                        //     System.out.println("Element " + idx + " görünür değil, atlanıyor");
                        //     continue;
                        // }
                        
                        // Daha esnek görünürlük kontrolü
                        try {
                            Boolean elementExists = (Boolean) js.executeScript(
                                "return arguments[0].offsetParent !== null || arguments[0].offsetWidth > 0 || arguments[0].offsetHeight > 0;", event
                            );
                            if (!elementExists) {
                                System.out.println("Element " + idx + " DOM'da görünür değil");
                                // Yine de devam et, parse etmeyi dene
                            }
                        } catch (Exception e) {
                            System.out.println("Element " + idx + " visibility check hatası: " + e.getMessage());
                        }
                        
                        // Elemente focus yap (lazy loading için)
                        try {
                            js.executeScript("arguments[0].scrollIntoView({behavior: 'auto', block: 'center'});", event);
                            Thread.sleep(800); // Kısa bekleme
                            
                            // Force trigger lazy loading
                            js.executeScript("arguments[0].focus(); arguments[0].click();", event);
                            Thread.sleep(300);
                        } catch (Exception scrollEx) {
                            System.out.println("Element " + idx + " scroll hatası: " + scrollEx.getMessage());
                        }
                        
                        // Maç adı - çok agresif arama
                        String matchName = "İsim bulunamadı";
                        
                        try {
                            // 1. Ana selector
                            List<WebElement> nameList = event.findElements(By.cssSelector("div.name > a"));
                            if (!nameList.isEmpty()) {
                                String text = nameList.get(0).getText().trim();
                                if (!text.isEmpty()) matchName = text;
                            }
                            
                            // 2. Alternatif - tüm linkler
                            if (matchName.equals("İsim bulunamadı")) {
                                nameList = event.findElements(By.tagName("a"));
                                for (WebElement link : nameList) {
                                    String text = link.getText().trim();
                                    if (!text.isEmpty() && text.length() > 5 && (text.contains("-") || text.contains(" vs "))) {
                                        matchName = text;
                                        break;
                                    }
                                }
                            }
                            
                            // 3. JavaScript ile text çek
                            if (matchName.equals("İsim bulunamadı")) {
                                String jsText = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText || '';", event);
                                if (jsText != null && !jsText.trim().isEmpty()) {
                                    // Text içinden takım isimlerini bulmaya çalış
                                    String[] lines = jsText.split("\n");
                                    for (String line : lines) {
                                        line = line.trim();
                                        if (line.length() > 5 && (line.contains("-") || line.contains(" vs ")) && !line.matches(".*\\d+.*")) {
                                            matchName = line;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception nameEx) {
                            System.out.println("Element " + idx + " isim arama hatası: " + nameEx.getMessage());
                        }

                        // Maç zamanı - çok agresif arama
                        String matchTime = "Zaman bulunamadı";
                        
                        try {
                            // 1. Ana selector
                            List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
                            if (!timeList.isEmpty()) {
                                String text = timeList.get(0).getText().trim();
                                if (!text.isEmpty()) matchTime = text;
                            }
                            
                            // 2. JavaScript ile tüm text'i çek ve zaman formatı ara
                            if (matchTime.equals("Zaman bulunamadı")) {
                                String jsText = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText || '';", event);
                                if (jsText != null) {
                                    // Regex ile zaman formatlarını bul
                                    java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d{2}:\\d{2})|(\\d{1,2}')");
                                    java.util.regex.Matcher matcher = timePattern.matcher(jsText);
                                    if (matcher.find()) {
                                        matchTime = matcher.group();
                                    }
                                }
                            }
                        } catch (Exception timeEx) {
                            System.out.println("Element " + idx + " zaman arama hatası: " + timeEx.getMessage());
                        }

                        // Oranlar - çok agresif arama
                        String odd1 = "-", oddX = "-", odd2 = "-";
                        
                        try {
                            // 1. Ana selector
                            List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));
                            
                            if (oddsList.isEmpty()) {
                                // 2. Alternatif - herhangi bir .odd
                                oddsList = event.findElements(By.cssSelector(".odd"));
                            }
                            
                            if (oddsList.isEmpty()) {
                                // 3. JavaScript ile tüm text'i al ve oran formatı ara
                                String jsText = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText || '';", event);
                                if (jsText != null) {
                                    // Oran formatını regex ile bul (1.50, 2.30 gibi)
                                    java.util.regex.Pattern oddPattern = java.util.regex.Pattern.compile("\\b\\d{1,2}\\.\\d{2}\\b");
                                    java.util.regex.Matcher matcher = oddPattern.matcher(jsText);
                                    
                                    java.util.List<String> foundOdds = new java.util.ArrayList<>();
                                    while (matcher.find() && foundOdds.size() < 3) {
                                        foundOdds.add(matcher.group());
                                    }
                                    
                                    if (foundOdds.size() > 0) odd1 = foundOdds.get(0);
                                    if (foundOdds.size() > 1) oddX = foundOdds.get(1);
                                    if (foundOdds.size() > 2) odd2 = foundOdds.get(2);
                                }
                            } else {
                                // Normal element'lerden oranları al
                                if (oddsList.size() > 0) {
                                    String text = oddsList.get(0).getText().trim();
                                    if (!text.isEmpty()) odd1 = text;
                                }
                                if (oddsList.size() > 1) {
                                    String text = oddsList.get(1).getText().trim();
                                    if (!text.isEmpty()) oddX = text;
                                }
                                if (oddsList.size() > 2) {
                                    String text = oddsList.get(2).getText().trim();
                                    if (!text.isEmpty()) odd2 = text;
                                }
                            }
                        } catch (Exception oddsEx) {
                            System.out.println("Element " + idx + " oran arama hatası: " + oddsEx.getMessage());
                        }

                        // Debug: Element'in raw text'ini yazdır
                        try {
                            String elementText = (String) js.executeScript("return (arguments[0].textContent || arguments[0].innerText || '').substring(0, 100);", event);
                            System.out.println("Element " + idx + " text (ilk 100 kar): " + elementText);
                        } catch (Exception debugEx) {
                            // Ignore
                        }

                        // Eğer hiçbir yararlı veri yoksa atla
                        if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı") && odd1.equals("-")) {
                            emptyCount++;
                            System.out.println("Element " + idx + " tamamen boş, atlanıyor");
                            continue;
                        }

                        html.append("<div class='match'>")
                            .append("<h3>").append(matchTime).append(" - ").append(matchName).append("</h3>")
                            .append("<p>1: ").append(odd1).append(" | X: ").append(oddX).append(" | 2: ").append(odd2).append("</p>")
                            .append("<p><small>Element #").append(idx).append("</small></p>")
                            .append("</div>");
                        
                        processedCount++;
                        
                    } catch (Exception inner) {
                        System.out.println("Element " + idx + " genel parse hatası: " + inner.getMessage());
                        inner.printStackTrace();
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
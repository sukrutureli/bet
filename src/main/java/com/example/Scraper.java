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
            
            // Sayfayı scroll ile sonuna kadar yüklet
            for (int i = 0; i < 15; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(3000); // Daha fazla bekleme süresi
                
                // Her scroll'da element sayısını kontrol et
                List<WebElement> currentElements = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                System.out.println("Scroll " + (i+1) + " - Element sayısı: " + currentElements.size());
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
                for (WebElement event : events) {
                    try {
                        // Maç adı
                        List<WebElement> nameList = event.findElements(By.cssSelector("div.name > a"));
                        String matchName = !nameList.isEmpty() ? nameList.get(0).getText() : "İsim bulunamadı";

                        // Maç zamanı
                        List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
                        String matchTime = !timeList.isEmpty() ? timeList.get(0).getText() : "Zaman bulunamadı";

                        // Oranlar
                        List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));

                        String odd1 = oddsList.size() > 0 ? oddsList.get(0).getText() : "-";
                        String oddX = oddsList.size() > 1 ? oddsList.get(1).getText() : "-";
                        String odd2 = oddsList.size() > 2 ? oddsList.get(2).getText() : "-";

                        html.append("<div class='match'>")
                            .append("<h3>").append(matchTime).append(" - ").append(matchName).append("</h3>")
                            .append("<p>1: ").append(odd1).append(" | X: ").append(oddX).append(" | 2: ").append(odd2).append("</p>")
                            .append("</div>");
                        
                        processedCount++;
                    } catch (Exception inner) {
                        System.out.println("Maç parse edilirken hata: " + inner.getMessage());
                        html.append("<div class='match' style='color:red;'>Parse hatası: ")
                            .append(inner.getMessage()).append("</div>");
                    }
                }
                html.append("<p>Başarıyla işlenen maç sayısı: ").append(processedCount).append("</p>");
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
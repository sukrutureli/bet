package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

public class MatchScraper {
    private WebDriver driver;
    private JavascriptExecutor js;
    private WebDriverWait wait;
    
    public MatchScraper() {
        setupDriver();
    }
    
    private void setupDriver() {
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

        this.driver = new ChromeDriver(options);
        this.js = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }
    
    public List<MatchInfo> scrapeMainPage() {
        List<MatchInfo> matches = new ArrayList<>();
		
        // Bugünün tarihi
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String todayStr = today.format(formatter);
        
        try {
            String url = "https://www.nesine.com/iddaa?et=1&dt=" + todayStr + "&le=2&ocg=MS-2%2C5>=Pop%C3%BCler";
            driver.get(url);
            
            System.out.println("Ana sayfa yüklendi: " + driver.getCurrentUrl());
            
            // Sayfa yüklenmesini bekle
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000);
            
            // Scroll işlemi
            performScrolling();
            
            // Elementleri çek
            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("Toplam bulunan element: " + events.size());
            
            // Her elementi işle
            for (int idx = 0; idx < events.size(); idx++) {
                WebElement event = events.get(idx);
                try {
                    MatchInfo matchInfo = extractMatchInfo(event, idx);
                    if (matchInfo != null) {
                        matches.add(matchInfo);
                        System.out.println("Maç " + idx + " işlendi: " + matchInfo.getName());
                    }
                } catch (Exception e) {
                    System.out.println("Element " + idx + " işlenirken hata: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.out.println("Ana sayfa scraping hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return matches;
    }
    
    private void performScrolling() {
        try {
            int previousCount = 0;
            int stableCount = 0;
            
            for (int i = 0; i < 40; i++) {
                js.executeScript("window.scrollBy(0, 300);");
                Thread.sleep(2500);
                
                List<WebElement> currentElements = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                int currentCount = currentElements.size();
                
                System.out.println("Scroll " + (i+1) + " - Element sayısı: " + currentCount);
                
                if (currentCount == previousCount) {
                    stableCount++;
                    if (stableCount >= 5) {
                        System.out.println("Element sayısı sabit, scroll durduruluyor.");
                        Thread.sleep(5000);
                        break;
                    }
                    Thread.sleep(3000);
                } else {
                    stableCount = 0;
                }
                
                previousCount = currentCount;
            }
            
            System.out.println("Scroll tamamlandı, final bekleme...");
            Thread.sleep(15000);
            
            // Lazy loading için hızlı scroll
            for (int i = 0; i < 5; i++) {
                js.executeScript("window.scrollTo(0, 0);");
                Thread.sleep(1000);
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            System.out.println("Scroll işlemi hatası: " + e.getMessage());
        }
    }
    
    private MatchInfo extractMatchInfo(WebElement event, int idx) {
        try {
            // Element'e scroll yap
            js.executeScript("arguments[0].scrollIntoView({behavior: 'auto', block: 'center'});", event);
            Thread.sleep(800);
            
            // Maç adı ve URL'i çek
            String matchName = "İsim bulunamadı";
            String detailUrl = null;
            
            // Name ve href'i aynı anda çek
            List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name > a"));
            if (!nameLinks.isEmpty()) {
                WebElement nameLink = nameLinks.get(0);
                matchName = nameLink.getText().trim();
                detailUrl = nameLink.getAttribute("href");
                
                if (matchName.isEmpty()) {
                    matchName = "İsim bulunamadı";
                }
            }
            
            // Alternatif URL arama
            if (detailUrl == null || detailUrl.isEmpty()) {
                List<WebElement> allLinks = event.findElements(By.tagName("a"));
                for (WebElement link : allLinks) {
                    String href = link.getAttribute("href");
                    if (href != null && href.contains("istatistik.nesine.com")) {
                        detailUrl = href;
                        if (matchName.equals("İsim bulunamadı")) {
                            String linkText = link.getText().trim();
                            if (!linkText.isEmpty()) {
                                matchName = linkText;
                            }
                        }
                        break;
                    }
                }
            }
            
            // JavaScript ile text çekme
            if (matchName.equals("İsim bulunamadı")) {
                String jsText = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText || '';", event);
                if (jsText != null && !jsText.trim().isEmpty()) {
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
            
            // Maç zamanı
            String matchTime = extractMatchTime(event);
            
            // Oranlar
            String[] odds = extractOdds(event);
            
            // Eğer minimum veri yoksa null döndür
            if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı")) {
                System.out.println("Element " + idx + " yeterli veri yok, atlanıyor");
                return null;
            }
            
            return new MatchInfo(matchName, matchTime, detailUrl, odds[0], odds[1], odds[2], idx);
            
        } catch (Exception e) {
            System.out.println("Element " + idx + " extract hatası: " + e.getMessage());
            return null;
        }
    }
    
    private String extractMatchTime(WebElement event) {
        try {
            // Ana selector
            List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
            if (!timeList.isEmpty()) {
                String text = timeList.get(0).getText().trim();
                if (!text.isEmpty()) return text;
            }
            
            // JavaScript ile zaman ara
            String jsText = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText || '';", event);
            if (jsText != null) {
                java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d{2}:\\d{2})|(\\d{1,2}')");
                java.util.regex.Matcher matcher = timePattern.matcher(jsText);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
            
            return "Zaman bulunamadı";
        } catch (Exception e) {
            return "Zaman hatası";
        }
    }
    
    private String[] extractOdds(WebElement event) {
        String[] odds = {"-", "-", "-"};
        
        try {
            // Ana selector
            List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));
            
            if (oddsList.isEmpty()) {
                // Alternatif selector
                oddsList = event.findElements(By.cssSelector(".odd"));
            }
            
            if (oddsList.isEmpty()) {
                // JavaScript ile oran formatı ara
                String jsText = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText || '';", event);
                if (jsText != null) {
                    java.util.regex.Pattern oddPattern = java.util.regex.Pattern.compile("\\b\\d{1,2}\\.\\d{2}\\b");
                    java.util.regex.Matcher matcher = oddPattern.matcher(jsText);
                    
                    int i = 0;
                    while (matcher.find() && i < 3) {
                        odds[i] = matcher.group();
                        i++;
                    }
                }
            } else {
                // Normal elementlerden oranları al
                for (int i = 0; i < Math.min(oddsList.size(), 3); i++) {
                    String text = oddsList.get(i).getText().trim();
                    if (!text.isEmpty()) {
                        odds[i] = text;
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("Oran çekme hatası: " + e.getMessage());
        }
        
        return odds;
    }
    
    public MatchDetails scrapeMatchDetails(String detailUrl) {
        if (detailUrl == null || detailUrl.isEmpty()) {
            return null;
        }
        
        try {
            System.out.println("Detay sayfası yükleniyor: " + detailUrl);
            driver.get(detailUrl);
            
            // Sayfa yüklenmesini bekle
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000);
            
            // Buraya detay sayfa scraping kodları gelecek
            // Şimdilik basit bir örnek
            String pageTitle = driver.getTitle();
            String pageText = js.executeScript("return document.body.textContent.substring(0, 500);").toString();
            
            return new MatchDetails(detailUrl, pageTitle, pageText);
            
        } catch (Exception e) {
            System.out.println("Detay sayfa scraping hatası: " + e.getMessage());
            return null;
        }
    }
    
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}

// Maç bilgilerini tutan data class
class MatchInfo {
    private String name;
    private String time;
    private String detailUrl;
    private String odd1;
    private String oddX;
    private String odd2;
    private int index;
    
    public MatchInfo(String name, String time, String detailUrl, String odd1, String oddX, String odd2, int index) {
        this.name = name;
        this.time = time;
        this.detailUrl = detailUrl;
        this.odd1 = odd1;
        this.oddX = oddX;
        this.odd2 = odd2;
        this.index = index;
    }
    
    // Getters
    public String getName() { return name; }
    public String getTime() { return time; }
    public String getDetailUrl() { return detailUrl; }
    public String getOdd1() { return odd1; }
    public String getOddX() { return oddX; }
    public String getOdd2() { return odd2; }
    public int getIndex() { return index; }
    
    public boolean hasDetailUrl() {
        return detailUrl != null && !detailUrl.isEmpty() && detailUrl.contains("istatistik.nesine.com");
    }
}

// Detay sayfa bilgilerini tutan data class
class MatchDetails {
    private String url;
    private String title;
    private String content;
    
    public MatchDetails(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
    }
    
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
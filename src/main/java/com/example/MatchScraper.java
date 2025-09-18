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
            // Daha esnek görünürlük kontrolü
            try {
                Boolean elementExists = (Boolean) js.executeScript(
                    "return arguments[0].offsetParent !== null || arguments[0].offsetWidth > 0 || arguments[0].offsetHeight > 0;", event
                );
                if (!elementExists) {
                    System.out.println("Element " + idx + " DOM'da görünür değil");
                }
            } catch (Exception e) {
                System.out.println("Element " + idx + " visibility check hatası: " + e.getMessage());
            }
            
            // Elemente focus yap (lazy loading için)
            try {
                js.executeScript("arguments[0].scrollIntoView({behavior: 'auto', block: 'center'});", event);
                Thread.sleep(800);
                
                // Force trigger lazy loading
                js.executeScript("arguments[0].focus(); arguments[0].click();", event);
                Thread.sleep(300);
            } catch (Exception scrollEx) {
                System.out.println("Element " + idx + " scroll hatası: " + scrollEx.getMessage());
            }
            
            // Maç adı ve URL'i çek
            String matchName = "İsim bulunamadı";
            String detailUrl = null;
            
            try {
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
            } catch (Exception nameEx) {
                System.out.println("Element " + idx + " isim arama hatası: " + nameEx.getMessage());
            }
            
            // Maç zamanı
            String matchTime = extractMatchTime(event);
            
            // Oranlar
            String[] odds = extractOdds(event);
            
            // Debug: Element'in raw text'ini yazdır
            try {
                String elementText = (String) js.executeScript("return (arguments[0].textContent || arguments[0].innerText || '').substring(0, 100);", event);
                System.out.println("Element " + idx + " text (ilk 100 kar): " + elementText);
            } catch (Exception debugEx) {
                // Ignore
            }
            
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
    
    public TeamMatchHistory scrapeTeamHistory(String detailUrl, String teamName) {
        if (detailUrl == null || detailUrl.isEmpty()) {
            return null;
        }
        
        TeamMatchHistory teamHistory = new TeamMatchHistory(teamName, detailUrl);
        
        try {
            // 1. Rekabet Geçmişi sayfası
            System.out.println("Rekabet geçmişi çekiliyor: " + teamName);
            List<MatchResult> rekabetGecmisi = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
            for (MatchResult match : rekabetGecmisi) {
                teamHistory.addRekabetGecmisiMatch(match);
            }
            
            // 2. Son Maçlar sayfası
            System.out.println("Son maçlar çekiliyor: " + teamName);
            List<MatchResult> sonMaclar = scrapeSonMaclar(detailUrl + "/son-maclar");
            for (MatchResult match : sonMaclar) {
                teamHistory.addSonMacMatch(match);
            }
            
            System.out.println("Tamamlandı - " + teamHistory.toString());
            return teamHistory;
            
        } catch (Exception e) {
            System.out.println("Takım geçmişi çekme hatası: " + e.getMessage());
            e.printStackTrace();
            return teamHistory; // Partial data döndür
        }
    }
    
    private List<MatchResult> scrapeRekabetGecmisi(String url) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000);
            
            System.out.println("Rekabet geçmişi sayfası yüklendi: " + url);
            
            // Turnuva seçimi yap
            selectTournament();
            
            // "Daha eski maçları göster" butonuna bas
            clickShowMoreMatches();
            
            // Maç sonuçlarını çek
            matches = extractMatchResults("rekabet-gecmisi", url);
            
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi çekme hatası: " + e.getMessage());
        }
        
        return matches;
    }
    
    private List<MatchResult> scrapeSonMaclar(String url) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000);
            
            System.out.println("Son maçlar sayfası yüklendi: " + url);
            
            // Turnuva seçimi yap
            selectTournament();
            
            // "Daha eski maçları göster" butonuna bas
            clickShowMoreMatches();
            
            // Maç sonuçlarını çek
            matches = extractMatchResults("son-maclar", url);
            
        } catch (Exception e) {
            System.out.println("Son maçlar çekme hatası: " + e.getMessage());
        }
        
        return matches;
    }
    
    private void selectTournament() {
        try {
            // Turnuva dropdown'ını bul ve aç
            List<WebElement> tournamentDropdowns = driver.findElements(By.cssSelector("select[class*='tournament'], select[class*='turnuva'], .tournament-select"));
            
            if (!tournamentDropdowns.isEmpty()) {
                WebElement dropdown = tournamentDropdowns.get(0);
                System.out.println("Turnuva dropdown bulundu, seçim yapılıyor...");
                
                // JavaScript ile dropdown'ı trigger et
                js.executeScript("arguments[0].click();", dropdown);
                Thread.sleep(1000);
                
                // İlk seçeneği seç (genelde "Tümü" olur)
                List<WebElement> options = dropdown.findElements(By.tagName("option"));
                if (options.size() > 1) {
                    // İkinci seçeneği seç (ilki genelde placeholder)
                    js.executeScript("arguments[0].selected = true; arguments[0].dispatchEvent(new Event('change'));", options.get(1));
                    Thread.sleep(2000);
                    System.out.println("Turnuva seçildi: " + options.get(1).getText());
                }
            } else {
                // Alternatif selector'lar dene
                List<WebElement> altDropdowns = driver.findElements(By.cssSelector("div[class*='dropdown'], div[class*='select']"));
                for (WebElement altDropdown : altDropdowns) {
                    try {
                        String text = altDropdown.getText().toLowerCase();
                        if (text.contains("turnuva") || text.contains("tournament") || text.contains("lig")) {
                            js.executeScript("arguments[0].click();", altDropdown);
                            Thread.sleep(1000);
                            
                            // İlk seçeneği seç
                            List<WebElement> dropdownOptions = altDropdown.findElements(By.cssSelector("div, span, a"));
                            if (!dropdownOptions.isEmpty()) {
                                js.executeScript("arguments[0].click();", dropdownOptions.get(0));
                                Thread.sleep(2000);
                                System.out.println("Alternatif turnuva seçimi yapıldı");
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore and continue
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Turnuva seçimi hatası: " + e.getMessage());
        }
    }
    
    private void clickShowMoreMatches() {
        try {
            // "Daha eski maçları göster" benzeri butonları ara
            String[] buttonTexts = {
                "daha eski", "show more", "load more", "daha fazla", 
                "eski maçlar", "more matches", "devamı", "tümünü göster"
            };
            
            boolean clicked = false;
            
            // Önce text içeriğine göre ara
            for (String buttonText : buttonTexts) {
                List<WebElement> buttons = driver.findElements(By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + buttonText + "')]"));
                
                for (WebElement button : buttons) {
                    try {
                        if (button.isDisplayed() && button.isEnabled()) {
                            // Butona scroll yap
                            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                            Thread.sleep(1000);
                            
                            // Click
                            js.executeScript("arguments[0].click();", button);
                            Thread.sleep(3000);
                            
                            System.out.println("'Daha fazla göster' butonuna basıldı: " + button.getText());
                            clicked = true;
                            
                            // Birkaç defa daha basmayı dene
                            for (int i = 0; i < 3; i++) {
                                try {
                                    if (button.isDisplayed()) {
                                        js.executeScript("arguments[0].click();", button);
                                        Thread.sleep(2000);
                                        System.out.println("Ek tıklama " + (i+1));
                                    } else {
                                        break;
                                    }
                                } catch (Exception e) {
                                    break;
                                }
                            }
                            
                            break;
                        }
                    } catch (Exception e) {
                        // Continue trying other buttons
                    }
                }
                
                if (clicked) break;
            }
            
            // CSS selector'larla da dene
            if (!clicked) {
                String[] selectors = {
                    "button[class*='more']", "button[class*='load']", 
                    "a[class*='more']", "a[class*='load']",
                    ".load-more", ".show-more", ".btn-more"
                };
                
                for (String selector : selectors) {
                    try {
                        List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                        for (WebElement button : buttons) {
                            if (button.isDisplayed()) {
                                js.executeScript("arguments[0].click();", button);
                                Thread.sleep(2000);
                                System.out.println("CSS selector ile buton bulundu ve basıldı");
                                clicked = true;
                                break;
                            }
                        }
                        if (clicked) break;
                    } catch (Exception e) {
                        // Continue
                    }
                }
            }
            
            if (!clicked) {
                System.out.println("'Daha fazla göster' butonu bulunamadı");
            }
            
        } catch (Exception e) {
            System.out.println("'Daha fazla göster' butonu tıklama hatası: " + e.getMessage());
        }
    }
    
    private List<MatchResult> extractMatchResults(String matchType, String originalUrl) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            // Sayfanın tam yüklenmesi için bekle
            Thread.sleep(3000);
            
            // Farklı maç tablosu selector'ları dene
            String[] tableSelectors = {
                "table[class*='match']", "table[class*='result']", "table[class*='score']",
                ".match-table", ".result-table", ".score-table",
                "tbody tr", ".match-row", ".result-row"
            };
            
            List<WebElement> matchRows = new ArrayList<>();
            
            for (String selector : tableSelectors) {
                try {
                    matchRows = driver.findElements(By.cssSelector(selector));
                    if (!matchRows.isEmpty()) {
                        System.out.println("Maç satırları bulundu: " + selector + " (" + matchRows.size() + " satır)");
                        break;
                    }
                } catch (Exception e) {
                    // Continue trying
                }
            }
            
            if (matchRows.isEmpty()) {
                System.out.println("Maç tablosu bulunamadı, alternatif yöntem deneniyor...");
                // Tüm div'lerde skor formatı ara
                String pageContent = driver.getPageSource();
                matches.addAll(extractMatchResultsFromText(pageContent, matchType, originalUrl));
                return matches;
            }
            
            System.out.println("Maç satırları işleniyor: " + matchRows.size());
            
            for (int i = 0; i < matchRows.size(); i++) {
                WebElement row = matchRows.get(i);
                try {
                    MatchResult match = extractSingleMatchResult(row, matchType, originalUrl);
                    if (match != null) {
                        matches.add(match);
                        if (matches.size() % 10 == 0) {
                            System.out.println("İşlenen maç sayısı: " + matches.size());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Satır " + i + " işlenirken hata: " + e.getMessage());
                }
            }
            
            System.out.println("Toplam çekilen maç sayısı: " + matches.size());
            
        } catch (Exception e) {
            System.out.println("Maç sonuçları çekme hatası: " + e.getMessage());
        }
        
        return matches;
    }
    
    private MatchResult extractSingleMatchResult(WebElement row, String matchType, String originalUrl) {
        try {
            // Takım isimleri
            String homeTeam = "", awayTeam = "";
            int homeScore = 0, awayScore = 0;
            java.time.LocalDate matchDate = null;
            String tournament = "";
            
            String rowText = row.getText();
            System.out.println("Row text: " + rowText);
            
            // Skor formatını bul (1-2, 3:1, 0 - 0 gibi)
            java.util.regex.Pattern scorePattern = java.util.regex.Pattern.compile("(\\d+)[-:\\s]+(\\d+)");
            java.util.regex.Matcher scoreMatcher = scorePattern.matcher(rowText);
            
            if (scoreMatcher.find()) {
                homeScore = Integer.parseInt(scoreMatcher.group(1));
                awayScore = Integer.parseInt(scoreMatcher.group(2));
                
                // Takım isimlerini çıkar (skor öncesi ve sonrası)
                String beforeScore = rowText.substring(0, scoreMatcher.start()).trim();
                String afterScore = rowText.substring(scoreMatcher.end()).trim();
                
                // Takım isimlerini parse et
                String[] beforeParts = beforeScore.split("\\s+");
                String[] afterParts = afterScore.split("\\s+");
                
                if (beforeParts.length > 0) {
                    homeTeam = beforeParts[beforeParts.length - 1]; // Son kelime home team
                }
                if (afterParts.length > 0) {
                    awayTeam = afterParts[0]; // İlk kelime away team
                }
            }
            
            // Tarih çek (DD.MM.YYYY, DD/MM/YYYY formatları)
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2})[./](\\d{1,2})[./](\\d{4})");
            java.util.regex.Matcher dateMatcher = datePattern.matcher(rowText);
            
            if (dateMatcher.find()) {
                try {
                    int day = Integer.parseInt(dateMatcher.group(1));
                    int month = Integer.parseInt(dateMatcher.group(2));
                    int year = Integer.parseInt(dateMatcher.group(3));
                    matchDate = java.time.LocalDate.of(year, month, day);
                } catch (Exception e) {
                    matchDate = java.time.LocalDate.now(); // Fallback
                }
            } else {
                matchDate = java.time.LocalDate.now(); // Fallback
            }
            
            // Turnuva ismini çıkarmaya çalış
            tournament = extractTournamentFromRow(row);
            
            // Validation
            if (homeTeam.isEmpty() || awayTeam.isEmpty()) {
                System.out.println("Takım isimleri bulunamadı: " + rowText);
                return null;
            }
            
            MatchResult match = new MatchResult(homeTeam, awayTeam, homeScore, awayScore, 
                                              matchDate, tournament, matchType, originalUrl);
            
            return match;
            
        } catch (Exception e) {
            System.out.println("Tek maç çıkarma hatası: " + e.getMessage());
            return null;
        }
    }
    
    private String extractTournamentFromRow(WebElement row) {
        try {
            // Turnuva ismini farklı yöntemlerle bul
            List<WebElement> elements = row.findElements(By.cssSelector("*"));
            for (WebElement element : elements) {
                String className = element.getAttribute("class");
                if (className != null && (className.contains("tournament") || className.contains("league") || className.contains("turnuva"))) {
                    String text = element.getText().trim();
                    if (!text.isEmpty() && !text.matches("\\d+[-:]\\d+")) { // Skor değilse
                        return text;
                    }
                }
            }
            
            // Default olarak genel bir isim ver
            return "Genel";
        } catch (Exception e) {
            return "Bilinmeyen";
        }
    }
    
    private List<MatchResult> extractMatchResultsFromText(String pageContent, String matchType, String originalUrl) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            // HTML'den text çıkar ve skor formatlarını bul
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\w\\s]+?)\\s+(\\d+)[-:](\\d+)\\s+([\\w\\s]+?)");
            java.util.regex.Matcher matcher = pattern.matcher(pageContent);
            
            while (matcher.find() && matches.size() < 50) { // Max 50 maç
                try {
                    String homeTeam = matcher.group(1).trim();
                    int homeScore = Integer.parseInt(matcher.group(2));
                    int awayScore = Integer.parseInt(matcher.group(3));
                    String awayTeam = matcher.group(4).trim();
                    
                    if (homeTeam.length() > 2 && awayTeam.length() > 2) {
                        MatchResult match = new MatchResult(homeTeam, awayTeam, homeScore, awayScore,
                                                          java.time.LocalDate.now(), "Genel", matchType, originalUrl);
                        matches.add(match);
                    }
                } catch (Exception e) {
                    // Skip invalid matches
                }
            }
            
            System.out.println("Text'ten çıkarılan maç sayısı: " + matches.size());
        } catch (Exception e) {
            System.out.println("Text'ten maç çıkarma hatası: " + e.getMessage());
        }
        
        return matches;
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
package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        options.setBinary("/usr/bin/google-chrome");
        
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        this.driver = new ChromeDriver(options);
        this.js = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }
    
    public List<MatchInfo> scrapeMainPage() {
        List<MatchInfo> matches = new ArrayList<>();
        
        try {
            String url = "https://www.nesine.com/iddaa?et=1&le=2&ocg=MS-2%2C5&gt=Pop%C3%BCler";
            driver.get(url);
            
            System.out.println("Ana sayfa yüklendi: " + driver.getCurrentUrl());
            
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000);
            
            performScrolling();
            
            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("Toplam bulunan element: " + events.size());
            
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
            js.executeScript("arguments[0].scrollIntoView({behavior: 'auto', block: 'center'});", event);
            Thread.sleep(800);
            
            String matchName = "İsim bulunamadı";
            String detailUrl = null;
            
            List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name > a"));
            if (!nameLinks.isEmpty()) {
                WebElement nameLink = nameLinks.get(0);
                matchName = nameLink.getText().trim();
                detailUrl = nameLink.getAttribute("href");
                
                if (matchName.isEmpty()) {
                    matchName = "İsim bulunamadı";
                }
            }
            
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
            
            String matchTime = extractMatchTime(event);
            String[] odds = extractOdds(event);
            
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
            List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
            if (!timeList.isEmpty()) {
                String text = timeList.get(0).getText().trim();
                if (!text.isEmpty()) return text;
            }
            
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
            List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));
            
            if (oddsList.isEmpty()) {
                oddsList = event.findElements(By.cssSelector(".odd"));
            }
            
            if (oddsList.isEmpty()) {
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
            System.out.println("Rekabet geçmişi çekiliyor: " + teamName);
            List<MatchResult> rekabetGecmisi = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
            for (MatchResult match : rekabetGecmisi) {
                teamHistory.addRekabetGecmisiMatch(match);
            }
            
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
            return teamHistory;
        }
    }
    
    private List<MatchResult> scrapeRekabetGecmisi(String url) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000);
            
            System.out.println("Rekabet geçmişi sayfası yüklendi: " + url);
            
            selectTournament();
            clickShowMoreMatches();
            matches = extractMatchResults("rekabet-gecmisi", url);
            
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi çekme hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return matches;
    }
    
    private List<MatchResult> scrapeSonMaclar(String url) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000);
            
            System.out.println("Son maçlar sayfası yüklendi: " + url);
            
            selectTournament();
            clickShowMoreMatches();
            matches = extractMatchResults("son-maclar", url);
            
        } catch (Exception e) {
            System.out.println("Son maçlar çekme hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return matches;
    }
    
    private void selectTournament() {
        try {
            System.out.println("Turnuva seçimi deneniyor...");
            
            String[] dropdownSelectors = {
                "select[name='tournament']",
                "select[id*='tournament']", 
                "select[class*='tournament']",
                "select[id*='turnuva']",
                "select[class*='turnuva']",
                "select[name='lig']",
                ".tournament-dropdown select",
                ".filter-select select",
                "select"
            };
            
            WebElement dropdown = null;
            
            for (String selector : dropdownSelectors) {
                try {
                    List<WebElement> dropdowns = driver.findElements(By.cssSelector(selector));
                    for (WebElement drop : dropdowns) {
                        if (drop.isDisplayed() && drop.isEnabled()) {
                            List<WebElement> options = drop.findElements(By.tagName("option"));
                            if (options.size() > 1) {
                                dropdown = drop;
                                System.out.println("Turnuva dropdown bulundu: " + selector + " (" + options.size() + " seçenek)");
                                break;
                            }
                        }
                    }
                    if (dropdown != null) break;
                } catch (Exception e) {
                    // Continue trying
                }
            }
            
            if (dropdown != null) {
                Select select = new Select(dropdown);
                List<WebElement> options = select.getOptions();
                
                System.out.println("Mevcut turnuva seçenekleri:");
                for (int i = 0; i < options.size(); i++) {
                    System.out.println(i + ": " + options.get(i).getText());
                }
                
                if (options.size() > 2) {
                    select.selectByIndex(1);
                    System.out.println("Turnuva seçildi: " + options.get(1).getText());
                    Thread.sleep(3000);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                    Thread.sleep(2000);
                }
            } else {
                System.out.println("Turnuva dropdown bulunamadı");
            }
            
        } catch (Exception e) {
            System.out.println("Turnuva seçimi hatası: " + e.getMessage());
        }
    }
    
    private void clickShowMoreMatches() {
        try {
            System.out.println("'Daha fazla göster' butonu aranıyor...");
            
            String[] buttonSelectors = {
                "button[class*='load-more']",
                "button[class*='show-more']", 
                "a[class*='load-more']",
                "a[class*='show-more']",
                ".load-more-btn",
                ".show-more-btn",
                "button[id*='loadmore']",
                "button[id*='more']"
            };
            
            String[] buttonTexts = {
                "daha fazla", "load more", "show more", "daha eski", 
                "more", "devamı", "tümünü göster", "eski maçlar"
            };
            
            boolean clicked = false;
            
            for (String selector : buttonSelectors) {
                try {
                    List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                    for (WebElement button : buttons) {
                        if (button.isDisplayed() && button.isEnabled()) {
                            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                            Thread.sleep(1000);
                            
                            js.executeScript("arguments[0].click();", button);
                            System.out.println("CSS selector ile buton tıklandı: " + selector);
                            Thread.sleep(3000);
                            
                            clicked = true;
                            break;
                        }
                    }
                    if (clicked) break;
                } catch (Exception e) {
                    // Continue trying
                }
            }
            
            if (!clicked) {
                for (String buttonText : buttonTexts) {
                    try {
                        String xpath = "//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZĞÜŞIÖÇ', 'abcdefghijklmnopqrstuvwxyzğüşiöç'), '" + buttonText + "')]";
                        List<WebElement> buttons = driver.findElements(By.xpath(xpath));
                        
                        for (WebElement button : buttons) {
                            if (button.isDisplayed() && button.isEnabled()) {
                                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                                Thread.sleep(1000);
                                
                                js.executeScript("arguments[0].click();", button);
                                System.out.println("Text bazlı buton tıklandı: " + button.getText());
                                Thread.sleep(3000);
                                
                                clicked = true;
                                break;
                            }
                        }
                        
                        if (clicked) break;
                    } catch (Exception e) {
                        // Continue trying
                    }
                }
            }
            
            if (!clicked) {
                String jsScript = """
                    var buttons = document.querySelectorAll('button, a, div[role="button"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var btn = buttons[i];
                        var text = (btn.textContent || btn.innerText || '').toLowerCase();
                        if ((text.includes('daha') || text.includes('more') || text.includes('load')) && 
                            btn.offsetParent !== null) {
                            btn.click();
                            return 'JS clicked: ' + text;
                        }
                    }
                    return 'No suitable button found';
                """;
                
                String result = (String) js.executeScript(jsScript);
                System.out.println("JavaScript tıklama sonucu: " + result);
                if (!result.equals("No suitable button found")) {
                    clicked = true;
                    Thread.sleep(3000);
                }
            }
            
            if (clicked) {
                System.out.println("'Daha fazla göster' butonuna başarıyla tıklandı");
            } else {
                System.out.println("'Daha fazla göster' butonu bulunamadı");
            }
            
        } catch (Exception e) {
            System.out.println("'Daha fazla göster' butonu tıklama hatası: " + e.getMessage());
        }
    }
    
    private List<MatchResult> extractMatchResults(String matchType, String originalUrl) {
        List<MatchResult> matches = new ArrayList<>();
        
        try {
            Thread.sleep(5000);
            
            String[] tableSelectors = {
                "table tbody tr",
                ".match-table tbody tr", 
                ".result-table tbody tr",
                ".match-row",
                ".result-row",
                "tr[class*='match']",
                "tr[class*='result']",
                ".statistics-table tbody tr"
            };
            
            List<WebElement> matchRows = new ArrayList<>();
            String usedSelector = "";
            
            for (String selector : tableSelectors) {
                try {
                    matchRows = driver.findElements(By.cssSelector(selector));
                    if (!matchRows.isEmpty()) {
                        usedSelector = selector;
                        System.out.println("Maç satırları bulundu: " + selector + " (" + matchRows.size() + " satır)");
                        break;
                    }
                } catch (Exception e) {
                    // Continue trying
                }
            }
            
            if (matchRows.isEmpty()) {
                System.out.println("Hiçbir maç satırı bulunamadı, alternatif yöntem deneniyor...");
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
            String rowText = row.getText();
            System.out.println("Satır metni: " + rowText);
            
            LocalDate matchDate = parseMatchDate(rowText);
            
            String homeTeam = "", awayTeam = "";
            int homeScore = 0, awayScore = 0;
            String tournament = "";
            
            java.util.regex.Pattern scorePattern = java.util.regex.Pattern.compile("(\\d+)\\s*[-:\\s]+\\s*(\\d+)");
            java.util.regex.Matcher scoreMatcher = scorePattern.matcher(rowText);
            
            if (scoreMatcher.find()) {
                homeScore = Integer.parseInt(scoreMatcher.group(1));
                awayScore = Integer.parseInt(scoreMatcher.group(2));
                
                String beforeScore = rowText.substring(0, scoreMatcher.start()).trim();
                String afterScore = rowText.substring(scoreMatcher.end()).trim();
                
                String[] beforeParts = beforeScore.split("\\s+");
                String[] afterParts = afterScore.split("\\s+");
                
                if (beforeParts.length >= 2) {
                    homeTeam = beforeParts[beforeParts.length - 2] + " " + beforeParts[beforeParts.length - 1];
                } else if (beforeParts.length > 0) {
                    homeTeam = beforeParts[beforeParts.length - 1];
                }
                
                if (afterParts.length >= 2) {
                    awayTeam = afterParts[0] + " " + afterParts[1];
                } else if (afterParts.length > 0) {
                    awayTeam = afterParts[0];
                }
                
                homeTeam = cleanTeamName(homeTeam);
                awayTeam = cleanTeamName(awayTeam);
            }
            
            tournament = extractTournamentFromRow(row);
            
            if (homeTeam.length() < 2 || awayTeam.length() < 2) {
                System.out.println("Takım isimleri çok kısa: '" + homeTeam + "' vs '" + awayTeam + "'");
                return null;
            }
            
            if (matchDate == null) {
                matchDate = LocalDate.now();
                System.out.println("Tarih parse edilemedi, bugünün tarihi kullanılıyor");
            }
            
            MatchResult match = new MatchResult(homeTeam, awayTeam, homeScore, awayScore, 
                                              matchDate, tournament, matchType, originalUrl);
            
            System.out.println("Maç oluşturuldu: " + match.toString());
            return match;
            
        } catch (Exception e) {
            System.out.println("Tek maç çıkarma hatası: " + e.getMessage());
            return null;
        }
    }
    
    private LocalDate parseMatchDate(String text) {
        try {
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("d.MM.yyyy"), 
                DateTimeFormatter.ofPattern("dd.M.yyyy"),
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/MM/yyyy")
            };
            
            String[] datePatterns = {
                "(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})",
                "(\\d{1,2})/(\\d{1,2})/(\\d{4})"
            };
            
            for (String pattern : datePatterns) {
                java.util.regex.Pattern dateRegex = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher matcher = dateRegex.matcher(text);
                
                if (matcher.find()) {
                    String dateStr = matcher.group();
                    System.out.println("Tarih stringi bulundu: " + dateStr);
                    
                    for (DateTimeFormatter formatter : formatters) {
                        try {
                            LocalDate date = LocalDate.parse(dateStr, formatter);
                            System.out.println("Tarih parse edildi: " + date);
                            return date;
                        } catch (DateTimeParseException e) {
                            // Try next formatter
                        }
                    }
                    
                    if (pattern.contains("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})")) {
                        try {
                            String[] parts = dateStr.split("\\.");
                            int day = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int year = Integer.parseInt(parts[2]);
                            LocalDate date = LocalDate.of(year, month, day);
                            System.out.println("Manuel tarih parse: " + date);
                            return date;
                        } catch (Exception e) {
                            // Continue
                        }
                    }
                }
            }
            
            if (text.toLowerCase().contains("bugün") || text.toLowerCase().contains("today")) {
                return LocalDate.now();
            } else if (text.toLowerCase().contains("dün") || text.toLowerCase().contains("yesterday")) {
                return LocalDate.now().minusDays(1);
            }
            
            return null;
            
        } catch (Exception e) {
            System.out.println("Tarih parse hatası: " + e.getMessage());
            return null;
        }
    }
    
    private String cleanTeamName(String teamName) {
        if (teamName == null) return "";
        
        return teamName
            .replaceAll("\\d+", "")
            .replaceAll("[\\(\\)\\[\\]]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    private String extractTournamentFromRow(WebElement row) {
        try {
            List<WebElement> elements = row.findElements(By.cssSelector("*"));
            for (WebElement element : elements) {
                String className = element.getAttribute("class");
                if (className != null) {
                    if (className.contains("tournament") || className.contains("league") || 
                        className.contains("turnuva") || className.contains("lig")) {
                        String text = element.getText().trim();
                        if (!text.isEmpty() && !text.matches(".*\\d+[-:]\\d+.*")) {
                            return text;
                        }
                    }
                }
            }
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


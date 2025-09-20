package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.time.ZonedDateTime;
import java.time.ZoneId;

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
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-background-timer-throttling");
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
        
        ZoneId turkeyZone = ZoneId.of("Europe/Istanbul");
        LocalDate today = LocalDate.now(turkeyZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String todayStr = today.format(formatter);

        ZonedDateTime istanbulTime = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
        int nowHour = istanbulTime.getHour();

        try {
            String url = "https://www.nesine.com/iddaa?et=1&dt=" + todayStr + "&le=2&ocg=MS-2%2C5>=Pop%C3%BCler";
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(3000);
            performScrolling();

            // SCROLL BİTTİKTEN SONRA FRESH ELEMENTLERİ ÇEK
            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("Final element sayısı: " + events.size());
            
            for (int idx = 0; idx < events.size(); idx++) {
                try {
                    // Her iterasyonda element'i tekrar bul (fresh reference)
                    List<WebElement> freshEvents = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                    if (idx >= freshEvents.size()) {
                        System.out.println("Element " + idx + " artık mevcut değil, çıkılıyor");
                        break;
                    }
                    
                    WebElement event = freshEvents.get(idx);
                    MatchInfo matchInfo = extractMatchInfo(event, idx);
                    
                    if (matchInfo != null/* && matchInfo.isClose(nowHour)*/) {
                        System.out.println(matchInfo.getName());
                        matches.add(matchInfo);
                    } /*else if (matchInfo == null) {
                        // Null ise devam et, break yapma
                        continue;
                    } else {
                        // isClose() false ise dur
                        break;
                    }*/
                    
                } catch (Exception e) {
                    System.out.println("Element " + idx + " işlenirken hata: " + e.getMessage());
                    // Hata olsa bile devam et
                    continue;
                }
            }

        } catch (Exception e) {
            System.out.println("Ana sayfa scraping hatası: " + e.getMessage());
        }

        return matches;
    }

    private void performScrolling() {
        try {
            int previousCount = -1;
            int stableRounds = 0;

            while (stableRounds < 2) { // 5'ten 3'e düşür
                List<WebElement> matches = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                int currentCount = matches.size();

                js.executeScript("window.scrollBy(0, 1500);"); // 1000'den 1500'e
                Thread.sleep(1500); // 2000'den 1000'e

                if (currentCount == previousCount) {
                    stableRounds++;
                } else {
                    stableRounds = 0;
                }
                previousCount = currentCount;
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
                Thread.sleep(300);
                
                // Force trigger lazy loading
                js.executeScript("arguments[0].focus(); arguments[0].click();", event);
                Thread.sleep(200);
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
                //System.out.println("Element " + idx + " text (ilk 100 kar): " + elementText);
                //System.out.println(matchName);
            } catch (Exception debugEx) {
                // Ignore
            }
            
            // Eğer minimum veri yoksa null döndür
            if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı")) {
                System.out.println("Element " + idx + " yeterli veri yok, atlanıyor");
                return null;
            }
            
            return new MatchInfo(matchName, matchTime, detailUrl, odds[0], odds[1], odds[2],
                                odds[3], odds[4], odds[5], odds[6], idx);
            
        } catch (Exception e) {
            System.out.println("Element " + idx + " extract hatası: " + e.getMessage());
            return null;
        }
    }

    private String extractMatchTime(WebElement event) {
        try {
            List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span"));
            if (!timeList.isEmpty()) return timeList.get(0).getText().trim();
            return "Zaman bulunamadı";
        } catch (Exception e) {
            return "Zaman hatası";
        }
    }

    private String[] extractOdds(WebElement event) {
        // 1X2 + Alt/Üst + Var/Yok = toplam 7 oran
        String[] odds = {"-", "-", "-", "-", "-", "-", "-"};
        try {
            // 1X2 oranları
            List<WebElement> mainOdds = event.findElements(By.cssSelector("dd.col-03.event-row .cell a.odd"));
            for (int i = 0; i < Math.min(mainOdds.size(), 3); i++) {
                String text = mainOdds.get(i).getText().trim();
                if (!text.isEmpty()) odds[i] = text;
            }

            // Alt/Üst oranları
            List<WebElement> overUnderOdds = event.findElements(By.cssSelector("dd.col-02.event-row .cell a.odd"));
            for (int i = 0; i < Math.min(overUnderOdds.size(), 2); i++) {
                String text = overUnderOdds.get(i).getText().trim();
                if (!text.isEmpty()) odds[3 + i] = text;  // odds[3], odds[4]
            }

            // Var/Yok oranları
            List<WebElement> goalOdds = event.findElements(By.cssSelector("dd.col-04.event-row .cell a.odd"));
            for (int i = 0; i < Math.min(goalOdds.size(), 2); i++) {
                String text = goalOdds.get(i).getText().trim();
                if (!text.isEmpty()) odds[5 + i] = text;  // odds[5], odds[6]
            }

        } catch (Exception e) {
            System.out.println("Oran çekme hatası: " + e.getMessage());
        }
        return odds; // [1, X, 2, Alt, Üst, Var, Yok]
    }


    public TeamMatchHistory scrapeTeamHistory(String detailUrl, String teamName) {
        if (detailUrl == null || detailUrl.isEmpty()) return null;
        List<String> names = scrapeDetailUrl(detailUrl);
        TeamMatchHistory teamHistory = new TeamMatchHistory(names.get(0), 
                                        names.get(1), names.get(2), detailUrl);
        try {
            List<MatchResult> rekabetGecmisi = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
            rekabetGecmisi.forEach(teamHistory::addRekabetGecmisiMatch);

            List<MatchResult> sonMaclarHome = scrapeSonMaclar(detailUrl + "/son-maclari", 1);
            sonMaclarHome.forEach(m -> teamHistory.addSonMacMatch(m, 1));

            List<MatchResult> sonMaclarAway = scrapeSonMaclar(detailUrl + "/son-maclari", 2);
            sonMaclarAway.forEach(m -> teamHistory.addSonMacMatch(m, 2));

        } catch (Exception e) {
            System.out.println("Takım geçmişi çekme hatası: " + e.getMessage());
        }
        return teamHistory;
    }

    private List<String> scrapeDetailUrl(String url) {
        List<String> names = new ArrayList<>();
        try {
            driver.get(url);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
            Thread.sleep(1500);
            // Tüm takım linklerini bul
            List<WebElement> teamLinks = driver.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"));

            // İlk takım
            String homeTeam = teamLinks.size() > 0 ? teamLinks.get(0).getText().trim() : "-";

            // İkinci takım
            String awayTeam = teamLinks.size() > 1 ? teamLinks.get(1).getText().trim() : "-";


            String teamNameShort = homeTeam + " - " + awayTeam;

            names.add(teamNameShort);
            names.add(homeTeam);
            names.add(awayTeam);
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
        }
        return names;
    }

    private List<MatchResult> scrapeRekabetGecmisi(String url) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
            Thread.sleep(1500);

            List<WebElement> container = driver.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTable']"));
            if (!container.isEmpty()) {
                if (hasNoData(container.get(0))) {
                    System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
                    return matches;
                }
            } 

            selectTournament();
            clickShowMoreMatches();
            matches = extractCompetitionHistoryResults("rekabet-gecmisi", url);
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
        }
        return matches;
    }

    private List<MatchResult> scrapeSonMaclar(String url, int homeOrAway) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
            Thread.sleep(1500);

            String selectorString = "";
            if (homeOrAway == 1) {
                selectorString = "div[data-test-id='LastMatchesTableFirst'] table";
            } else if (homeOrAway == 2) {
                selectorString = "div[data-test-id='LastMatchesTableSecond'] table";
            }
            List<WebElement> container = driver.findElements(By.cssSelector(selectorString));
            if (!container.isEmpty()) {
                if (hasNoData(container.get(0))) {
                    System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
                    return matches;
                }
            }

            selectTournament();
            clickShowMoreMatches();
            matches = extractMatchResults("son-maclari", url, homeOrAway);
        } catch (Exception e) {
            System.out.println("Son maçlar hatası: " + e.getMessage());
        }
        return matches;
    }

    private void selectTournament() {
        try {
            // Timeout'u 5 saniyeye düşür
            WebElement dropdown = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[data-test-id='CustomDropdown']")));
            dropdown.click();
            Thread.sleep(300); // 1000'den 300'e
            
            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@role='option']//span[contains(text(), 'Bu Turnuva')]")));
            option.click();
            Thread.sleep(200); // 1500'den 500'e
        } catch (Exception e) {
            // Hızla geç, takılma
            System.out.println("Turnuva seçimi atlandı");
        }
    }

    private void clickShowMoreMatches() {
        try {
            List<WebElement> clickables = driver.findElements(By.cssSelector("button, a"));
            for (WebElement element : clickables) {
                String text = element.getText().toLowerCase();
                if (text.contains("daha") || text.contains("more") || text.contains("load")) {
                    if (element.isDisplayed() && element.isEnabled()) {
                        js.executeScript("arguments[0].click();", element);
                        Thread.sleep(500);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Buton tıklama hatası: " + e.getMessage());
        }
    }

    // Takım isimlerini güvenli şekilde çeken metod
    private String extractTeamName(WebElement teamElement) {
        List<WebElement> spans = teamElement.findElements(By.tagName("span"));
        StringBuilder sb = new StringBuilder();

        for (WebElement span : spans) {
            // Tarafsız saha ikonunu direkt atla
            String cls = span.getAttribute("class");
            if (cls != null && cls.contains("nsn-i-neutral-ground")) {
                continue;
            }

            String txt = span.getText();
            if (txt == null) continue;

            txt = txt.trim();
            if (txt.isEmpty()) continue;

            // Tek başına sayı (örn. "1", "2") → kart sayısı, atla
            if (txt.matches("\\d+")) continue;

            // Sadece harf, rakam, boşluk, nokta, parantez ve tire kalsın
            txt = txt.replaceAll("[^\\p{L}0-9\\s\\.\\-\\(\\)]", "");

            if (txt.isEmpty()) continue;

            if (sb.length() > 0) sb.append(" ");
            sb.append(txt);
        }
    
        return sb.toString().trim();
    }

    // Örnek: extractCompetitionHistoryResults içinde kullanımı
    private List<MatchResult> extractCompetitionHistoryResults(String matchType, String originalUrl) {
        List<MatchResult> matches = new ArrayList<>();

        List<WebElement> container = driver.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTable']"));
        if (!container.isEmpty()) {
            if (hasNoData(container.get(0))) {
                System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
                return matches;
            }
        }      

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div[data-test-id='CompitionHistoryTableItem']")));
            List<WebElement> rows = driver.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));

            for (WebElement row : rows) {
                try {
                    String league = row.findElement(By.cssSelector("[data-test-id='CompitionTableItemLeague']")).getText().trim();
                    String dateText = row.findElement(By.cssSelector("[data-test-id='CompitionTableItemSeason']")).getText().trim();
                    String homeTeam = extractTeamName(row.findElement(By.cssSelector("div[data-test-id='HomeTeam']")));
                    String awayTeam = extractTeamName(row.findElement(By.cssSelector("div[data-test-id='AwayTeam']")));
                    String scoreText = extractScore(row);
                    String[] parts = scoreText.split("-");
                    int homeScore = Integer.parseInt(parts[0].trim());
                    int awayScore = Integer.parseInt(parts[1].trim());

                    MatchResult match = new MatchResult(
                        homeTeam, awayTeam, homeScore, awayScore, dateText, league, matchType, originalUrl
                    );
                    matches.add(match);
                } catch (Exception e) {
                    System.out.println("Satır işlenemedi: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("extractCompetitionHistoryResults hatası: " + e.getMessage());
        }
        return matches;
    }

    // Skor çekme metodu
    private String extractScore(WebElement row) {
        try {
            List<WebElement> spans = row.findElements(By.cssSelector("button[data-test-id='NsnButton'] span"));
            for (WebElement span : spans) {
                String txt = span.getText().trim();
                txt = txt.replaceAll("(\\(.*?\\))", "").trim(); // (H) engeller
                // Skor formatı "X - Y" (ör. "2 - 1")
                if (txt.matches("\\d+\\s*-\\s*\\d+")) {
                    return txt;
                }
            }
        } catch (Exception e) {
            System.out.println("Skor çekme hatası: " + e.getMessage());
        }
        return "-";
    }

    private boolean hasNoData(WebElement container) {
        try {
            List<WebElement> noDataElems = container.findElements(By.cssSelector("div[data-test-id='NoData']"));
            return !noDataElems.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // Maç sonuçlarını listeleyen metot
    private List<MatchResult> extractMatchResults(String matchType, String originalUrl, int homeOrAway) {
        List<MatchResult> matches = new ArrayList<>();
        String selectorString = "";
        if (homeOrAway == 1) {
            selectorString = "div[data-test-id='LastMatchesTableFirst'] table";
        } else if (homeOrAway == 2) {
            selectorString = "div[data-test-id='LastMatchesTableSecond'] table";
        }
        List<WebElement> container = driver.findElements(By.cssSelector(selectorString));
        if (!container.isEmpty()) {
            if (hasNoData(container.get(0))) {
                System.out.println("Bu müsabaka için veri yok, tablo beklenmeyecek.");
                return matches;
            }
        }
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(selectorString)));
            WebElement table = driver.findElement(By.cssSelector(selectorString));
            List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

            for (WebElement row : rows) {
                try {
                    // Takım isimlerini çek
                    String homeTeam = extractTeamName(row.findElement(By.cssSelector("div[data-test-id='HomeTeam']")));
                    String awayTeam = extractTeamName(row.findElement(By.cssSelector("div[data-test-id='AwayTeam']")));

                    // Skoru güvenli şekilde çek
                    String scoreText = extractScore(row);
                    int homeScore = -1;
                    int awayScore = -1;
                    if (!scoreText.equals("-")) {
                        String[] parts = scoreText.split("-");
                        if (parts.length == 2) {
                            homeScore = Integer.parseInt(parts[0].trim());
                            awayScore = Integer.parseInt(parts[1].trim());
                        }
                    }

                    // Lig + tarih bilgisini al
                    String leagueAndDate = row.findElement(By.cssSelector("td[data-test-id='TableBodyLeague']")).getText();

                    MatchResult match = new MatchResult(
                        homeTeam, awayTeam, homeScore, awayScore, leagueAndDate,
                        "", matchType, originalUrl
                    );
                    matches.add(match);

                } catch (Exception e) {
                    System.out.println("Satır işlenemedi: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("extractMatchResults hatası: " + e.getMessage());
        }
        return matches;
    }



    public void close() {
        if (driver != null) driver.quit();
    }
}
// Maç bilgilerini tutan data class (güncellenmiş)
class MatchInfo {
    private String name;
    private String time;
    private String detailUrl;
    private String odd1;   // 1
    private String oddX;   // X
    private String odd2;   // 2
    private String oddAlt; // Alt
    private String oddUst; // Üst
    private String oddVar; // Var
    private String oddYok; // Yok
    private int index;

    public MatchInfo(String name, String time, String detailUrl,
                     String odd1, String oddX, String odd2,
                     String oddAlt, String oddUst, String oddVar, String oddYok,
                     int index) {
        this.name = name;
        this.time = time;
        this.detailUrl = detailUrl;
        this.odd1 = odd1;
        this.oddX = oddX;
        this.odd2 = odd2;
        this.oddAlt = oddAlt;
        this.oddUst = oddUst;
        this.oddVar = oddVar;
        this.oddYok = oddYok;
        this.index = index;
    }

    // Getters
    public String getName() { return name; }
    public String getTime() { return time; }
    public String getDetailUrl() { return detailUrl; }
    public String getOdd1() { return odd1; }
    public String getOddX() { return oddX; }
    public String getOdd2() { return odd2; }
    public String getOddAlt() { return oddAlt; }
    public String getOddUst() { return oddUst; }
    public String getOddVar() { return oddVar; }
    public String getOddYok() { return oddYok; }
    public int getIndex() { return index; }

    public boolean isClose(int nowHour) {
        try {
            // "Zaman hatası" string'ini parse etmeye çalışırsa hata verir
            if (time.equals("Zaman bulunamadı") || time.equals("Zaman hatası")) {
                return true; // Zaman bilinmiyorsa işle
            }

            int timeInHour = Integer.parseInt(time.split(":")[0]);
            return nowHour + 2 >= timeInHour && nowHour <= timeInHour;
            
        } catch (Exception e) {
            System.out.println("isClose() hatası: " + e.getMessage() + " - time: " + time);
            return true; // Hata varsa işle
        }
    }

    public boolean hasDetailUrl() {
        return detailUrl != null && !detailUrl.isEmpty() && detailUrl.contains("istatistik.nesine.com");
    }
}

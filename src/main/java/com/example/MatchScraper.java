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

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String todayStr = today.format(formatter);

        try {
            String url = "https://www.nesine.com/iddaa?et=1&dt=" + todayStr + "&le=2&ocg=MS-2%2C5>=Pop%C3%BCler";
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(5000);
            performScrolling();

            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            for (int idx = 0; idx < Math.min(events.size(), 5); idx++) {
                WebElement event = events.get(idx);
                MatchInfo matchInfo = extractMatchInfo(event, idx);
                if (matchInfo != null) matches.add(matchInfo);
            }

        } catch (Exception e) {
            System.out.println("Ana sayfa scraping hatası: " + e.getMessage());
        }

        return matches;
    }

    private void performScrolling() {
        try {
            for (int i = 0; i < 10; i++) {
                js.executeScript("window.scrollBy(0, 400);");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println("Scroll işlemi hatası: " + e.getMessage());
        }
    }

    private MatchInfo extractMatchInfo(WebElement event, int idx) {
        try {
            String matchName = "İsim bulunamadı";
            String detailUrl = null;

            List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name > a"));
            if (!nameLinks.isEmpty()) {
                WebElement nameLink = nameLinks.get(0);
                matchName = nameLink.getText().trim();
                detailUrl = nameLink.getAttribute("href");
            }

            String matchTime = extractMatchTime(event);
            String[] odds = extractOdds(event);

            if (matchName.equals("İsim bulunamadı") && matchTime.equals("Zaman bulunamadı")) {
                return null;
            }

            return new MatchInfo(matchName, matchTime, detailUrl, odds[0], odds[1], odds[2], idx);

        } catch (Exception e) {
            System.out.println("extractMatchInfo hatası: " + e.getMessage());
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
        String[] odds = {"-", "-", "-"};
        try {
            List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));
            for (int i = 0; i < Math.min(oddsList.size(), 3); i++) {
                String text = oddsList.get(i).getText().trim();
                if (!text.isEmpty()) odds[i] = text;
            }
        } catch (Exception e) {
            System.out.println("Oran çekme hatası: " + e.getMessage());
        }
        return odds;
    }

    public TeamMatchHistory scrapeTeamHistory(String detailUrl, String teamName) {
        if (detailUrl == null || detailUrl.isEmpty()) return null;
        TeamMatchHistory teamHistory = new TeamMatchHistory(teamName, detailUrl);
        try {
            List<MatchResult> rekabetGecmisi = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
            rekabetGecmisi.forEach(teamHistory::addRekabetGecmisiMatch);

            List<MatchResult> sonMaclar = scrapeSonMaclar(detailUrl + "/son-maclari");
            sonMaclar.forEach(teamHistory::addSonMacMatch);

        } catch (Exception e) {
            System.out.println("Takım geçmişi çekme hatası: " + e.getMessage());
        }
        return teamHistory;
    }

    private List<MatchResult> scrapeRekabetGecmisi(String url) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);
            selectTournament();
            clickShowMoreMatches();
            matches = extractCompetitionHistoryResults("rekabet-gecmisi", url);
        } catch (Exception e) {
            System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
        }
        return matches;
    }

    private List<MatchResult> scrapeSonMaclar(String url) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);
            selectTournament();
            clickShowMoreMatches();
            matches = extractMatchResults("son-maclari", url);
        } catch (Exception e) {
            System.out.println("Son maçlar hatası: " + e.getMessage());
        }
        return matches;
    }

    private void selectTournament() {
        try {
            WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("div[data-test-id='CustomDropdown']")));
            dropdown.click();
            Thread.sleep(1000);
            WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='option']//span[contains(text(), 'Bu Turnuva')]")));
            option.click();
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Turnuva seçimi hatası: " + e.getMessage());
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
                        Thread.sleep(2000);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Buton tıklama hatası: " + e.getMessage());
        }
    }

    private List<MatchResult> extractCompetitionHistoryResults(String matchType, String originalUrl) {
        List<MatchResult> matches = new ArrayList<>();
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
                    String scoreText = row.findElement(By.cssSelector("button[data-testid='nsn-button'] span")).getText().trim();
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

    private List<MatchResult> extractMatchResults(String matchType, String originalUrl) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("tr[data-test-id='LastMatchesTable']")));
            List<WebElement> rows = driver.findElements(By.cssSelector("tr[data-test-id='LastMatchesTable']"));

            for (WebElement row : rows) {
                try {
                    String homeTeam = extractTeamName(row.findElement(By.cssSelector("div[data-test-id='HomeTeam']")));
                    String awayTeam = extractTeamName(row.findElement(By.cssSelector("div[data-test-id='AwayTeam']")));
                    String scoreText = row.findElement(By.cssSelector("button[data-test-id='NsnButton'] span")).getText().trim();
                    String[] parts = scoreText.split("-");
                    int homeScore = Integer.parseInt(parts[0].trim());
                    int awayScore = Integer.parseInt(parts[1].trim());
                    String leagueAndDate = row.findElement(By.cssSelector("td[data-test-id='TableBodyLeague']")).getText();

                    MatchResult match = new MatchResult(
                        homeTeam, awayTeam, homeScore, awayScore, leagueAndDate,
                        "İstatistik", matchType, originalUrl
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

    private String extractTeamName(WebElement teamElement) {
        List<WebElement> spans = teamElement.findElements(By.tagName("span"));
        StringBuilder sb = new StringBuilder();
        for (WebElement span : spans) {
            String txt = span.getText().trim();
            if (txt.isEmpty()) continue;
            if (txt.equalsIgnoreCase("N")) continue;
            if (txt.length() <= 2) continue; // kart ikonları
            if (sb.length() > 0) sb.append(" ");
            sb.append(txt);
        }
        return sb.toString().trim();
    }

    public void close() {
        if (driver != null) driver.quit();
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
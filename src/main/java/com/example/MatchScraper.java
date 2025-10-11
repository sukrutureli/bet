package com.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.example.model.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MatchScraper {
    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    public MatchScraper() {
        setupDriver();
    }

    private void setupDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--window-size=1920,1080", "--disable-blink-features=AutomationControlled");
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.js = (JavascriptExecutor) driver;
    }

    // ------------------------------ ANA SAYFA ------------------------------
    public List<MatchInfo> scrapeMainPage() {
        List<MatchInfo> matches = new ArrayList<>();
        try {
            String today = LocalDate.now(ZoneId.of("Europe/Istanbul"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String url = "https://www.nesine.com/iddaa?et=1&dt=" + today + "&le=2&ocg=MS-2%2C5>=Pop%C3%BCler";
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);

            performScrolling();

            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
            System.out.println("Toplam maç bulundu: " + events.size());

            for (int i = 0; i < events.size(); i++) {
                try {
                    WebElement e = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event")).get(i);
                    MatchInfo info = extractMatchInfo(e);
                    if (info != null) {
                        matches.add(info);
                        System.out.println("➕ " + info.getName());
                    }
                } catch (Exception ex) {
                    System.out.println("Maç işlenemedi: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Ana sayfa scraping hatası: " + e.getMessage());
        }
        return matches;
    }

    private void performScrolling() {
        try {
            int lastCount = -1, stableRounds = 0;
            while (stableRounds < 2) {
                List<WebElement> elements = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
                int count = elements.size();
                js.executeScript("window.scrollBy(0, 2000)");
                Thread.sleep(1000);
                if (count == lastCount) stableRounds++;
                else stableRounds = 0;
                lastCount = count;
            }
        } catch (Exception e) {
            System.out.println("Scroll hatası: " + e.getMessage());
        }
    }

    private MatchInfo extractMatchInfo(WebElement event) {
        try {
            String name = "Bilinmiyor", url = "", time = "Zaman yok";
            List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name a"));
            if (!nameLinks.isEmpty()) {
                WebElement link = nameLinks.get(0);
                name = link.getText().trim();
                url = link.getAttribute("href");
            }

            List<WebElement> timeEl = event.findElements(By.cssSelector("div.time span"));
            if (!timeEl.isEmpty()) time = timeEl.get(0).getText().trim();

            Odds odds = extractOdds(event);
            return new MatchInfo(name, time, url, odds, 0);
        } catch (Exception e) {
            System.out.println("MatchInfo hatası: " + e.getMessage());
            return null;
        }
    }

    private Odds extractOdds(WebElement event) {
        String[] values = {"-", "-", "-", "-", "-", "-", "-"}; // [1,X,2,Alt,Üst,Var,Yok]
        try {
            List<WebElement> main = event.findElements(By.cssSelector("dd.col-03.event-row .cell a.odd"));
            for (int i = 0; i < Math.min(main.size(), 3); i++)
                values[i] = main.get(i).getText().trim();

            List<WebElement> extras = event.findElements(By.cssSelector("dd.col-02.event-row .cell a.odd"));
            for (int i = 0; i < Math.min(extras.size(), 4); i++)
                values[3 + i] = extras.get(i).getText().trim();
        } catch (Exception e) {
            System.out.println("Oran çekme hatası: " + e.getMessage());
        }

        return new Odds(toDouble(values[0]), toDouble(values[1]), toDouble(values[2]),
                toDouble(values[4]), toDouble(values[3]), toDouble(values[5]), toDouble(values[6]));
    }

    private double toDouble(String s) {
        try {
            return s == null || s.equals("-") ? 0.0 : Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ------------------------------ DETAY SAYFALARI ------------------------------
    public TeamMatchHistory scrapeTeamHistory(String url, String name) {
        if (url == null || url.isEmpty()) return null;
        TeamMatchHistory hist = new TeamMatchHistory(name, "", "", url);
        try {
            List<MatchResult> rekabet = scrapeTable(url + "/rekabet-gecmisi",
                    "div[data-test-id='CompitionHistoryTableItem']");
            List<MatchResult> sonHome = scrapeTable(url + "/son-maclari",
                    "div[data-test-id='LastMatchesTableFirst'] table tr");
            List<MatchResult> sonAway = scrapeTable(url + "/son-maclari",
                    "div[data-test-id='LastMatchesTableSecond'] table tr");

            rekabet.forEach(hist::addRekabetGecmisiMatch);
            sonHome.forEach(m -> hist.addSonMacMatch(m, 1));
            sonAway.forEach(m -> hist.addSonMacMatch(m, 2));
        } catch (Exception e) {
            System.out.println("Takım geçmişi hatası: " + e.getMessage());
        }
        return hist;
    }

    private List<MatchResult> scrapeTable(String url, String selector) {
        List<MatchResult> matches = new ArrayList<>();
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(1000);

            selectTournament();
            clickShowMoreMatches();

            List<WebElement> rows = driver.findElements(By.cssSelector(selector));
            for (WebElement row : rows) {
                try {
                    String text = row.getText();
                    if (!text.matches(".*\\d+-\\d+.*")) continue;
                    String[] score = text.replaceAll("[^0-9\\-]", "").split("-");
                    int hs = Integer.parseInt(score[0]);
                    int as = Integer.parseInt(score[1]);
                    matches.add(new MatchResult("-", "-", hs, as, "", "", "", url));
                } catch (Exception ex) {
                    // Ignore
                }
            }
        } catch (Exception e) {
            System.out.println("Scrape table hatası: " + e.getMessage());
        }
        return matches;
    }

    // ------------------------------ YARDIMCI METOTLAR ------------------------------
    private void selectTournament() {
        try {
            WebElement dropdown = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-test-id='CustomDropdown']")));
            dropdown.click();
            Thread.sleep(300);

            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")));
            option.click();
            Thread.sleep(200);
        } catch (Exception e) {
            System.out.println("Turnuva seçimi atlandı: " + e.getMessage());
        }
    }

    private void clickShowMoreMatches() {
        try {
            List<WebElement> clickables = driver.findElements(By.cssSelector("button, a"));
            for (WebElement el : clickables) {
                String t = el.getText().toLowerCase();
                if ((t.contains("daha") || t.contains("more") || t.contains("load"))
                        && el.isDisplayed() && el.isEnabled()) {
                    js.executeScript("arguments[0].click();", el);
                    Thread.sleep(500);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Daha fazla maç yükle butonu bulunamadı: " + e.getMessage());
        }
    }

    public void close() {
        if (driver != null) driver.quit();
    }
}

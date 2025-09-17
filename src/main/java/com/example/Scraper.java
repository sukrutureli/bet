package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            String url = "https://www.nesine.com/iddaa?et=1&le=2&ocg=MS-2%2C5&gt=Pop%C3%BCler";
            driver.get(url);

            // 1️⃣ Sayfanın tüm maçları yüklemesini bekle
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.odd-col.event-list.pre-event")));

            // 2️⃣ Lazy load varsa scroll yap
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(1500); // sayfanın yüklenmesi için bekle
                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) break;
                lastHeight = newHeight;
            }

            // 3️⃣ Maçları seç
            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>IDDAA Bülteni</title></head><body>");
            html.append("<h1>IDDAA Güncel Bülteni</h1>");

            for (WebElement event : events) {
                try {
                    WebElement nameEl = event.findElement(By.cssSelector("div.code-time-name > div.name > a"));
                    String matchName = nameEl.getText();

                    WebElement timeEl = event.findElement(By.cssSelector("div.code-time-name > div.time"));
                    String matchTime = timeEl.getText();

                    List<WebElement> odds = event.findElements(By.cssSelector("div.odds-content > dl > dd:nth-child(2)"));
                    String odd1 = odds.size() > 0 ? odds.get(0).getText() : "-";
                    String oddX = odds.size() > 1 ? odds.get(1).getText() : "-";
                    String odd2 = odds.size() > 2 ? odds.get(2).getText() : "-";

                    html.append("<div class='match'>")
                        .append("<h3>").append(matchTime).append(" - ").append(matchName).append("</h3>")
                        .append("<p>1: ").append(odd1).append(" | X: ").append(oddX).append(" | 2: ").append(odd2).append("</p>")
                        .append("</div>");
                } catch (Exception inner) {
                    System.out.println("Bir maç parse edilirken hata: " + inner.getMessage());
                }
            }

            html.append("<p>Güncelleme zamanı: ").append(java.time.LocalDateTime.now()).append("</p>");
            html.append("</body></html>");

            File dir = new File("public");
            if (!dir.exists()) dir.mkdirs();

            try (FileWriter fw = new FileWriter(new File(dir, "index.html"))) {
                fw.write(html.toString());
            }

            System.out.println("public/index.html başarıyla oluşturuldu! Toplam maç sayısı: " + events.size());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}

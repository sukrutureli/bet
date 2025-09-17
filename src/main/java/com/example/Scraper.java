package com.example;

import org.openqa.selenium.By;
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
        // ChromeDriver yolu
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            String url = "https://www.nesine.com/iddaa?et=1&le=2&ocg=MS-2%2C5&gt=Pop%C3%BCler";
            driver.get(url);

            // Sayfanın tam yüklenmesini bekle
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.odd-col.event-list.pre-event")));

            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>IDDAA Bülteni</title></head><body>");
            html.append("<h1>IDDAA Güncel Bülteni</h1>");

            for (WebElement event : events) {
                try {
                    // Maç adı
                    List<WebElement> nameList = event.findElements(By.cssSelector("div.name > a"));
                    String matchName = !nameList.isEmpty() ? nameList.get(0).getText() : "-";

                    // Maç zamanı
                    List<WebElement> timeList = event.findElements(By.cssSelector("div.time > span.passive-time"));
                    String matchTime = !timeList.isEmpty() ? timeList.get(0).getText() : "-";

                    // Oranlar (tüm dd.event-row içindeki .cell a.odd)
                    List<WebElement> oddsList = event.findElements(By.cssSelector("dd.event-row .cell a.odd"));

                    // İlk 3 oranı al, yoksa "-"
                    String odd1 = oddsList.size() > 0 ? oddsList.get(0).getText() : "-";
                    String oddX = oddsList.size() > 1 ? oddsList.get(1).getText() : "-";
                    String odd2 = oddsList.size() > 2 ? oddsList.get(2).getText() : "-";

                    html.append("<div class='match'>")
                        .append("<h3>").append(matchTime).append(" - ").append(matchName).append("</h3>")
                        .append("<p>1: ").append(odd1).append(" | X: ").append(oddX).append(" | 2: ").append(odd2).append("</p>")
                        .append("</div>");
                } catch (Exception inner) {
                    System.out.println("Maç parse edilirken hata: " + inner.getMessage());
                }
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
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}

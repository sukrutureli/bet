package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {

        // Docker container içinde chromedriver yolu
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            String url = "https://www.nesine.com/iddaa?et=1&le=2&ocg=MS-2%2C5&gt=Pop%C3%BCler";
            driver.get(url);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

            List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>IDDAA Bülteni</title></head><body>");
            html.append("<h1>IDDAA Güncel Bülteni</h1>");

            for (WebElement event : events) {
                try {
                    WebElement nameEl = event.findElement(By.cssSelector("div.name > a"));
                    String matchName = nameEl.getText();

                    /*WebElement timeEl = event.findElement(By.cssSelector("div.time > span.passive-time"));
                    String matchTime = timeEl.getText();

                    List<WebElement> odds = event.findElements(By.cssSelector("dd.col-03.event-row .cell .odd"));
                    String odd1 = odds.size() > 0 ? odds.get(0).getText() : "-";
                    String oddX = odds.size() > 1 ? odds.get(1).getText() : "-";
                    String odd2 = odds.size() > 2 ? odds.get(2).getText() : "-";*/

                    html.append("<div class='match'>")
                        .append("<h3>").append(matchName).append("</h3>")
                        .append("</div>");

                    /*html.append("<div class='match'>")
                        .append("<h3>").append(matchTime).append(" - ").append(matchName).append("</h3>")
                        .append("<p>1: ").append(odd1).append(" | X: ").append(oddX).append(" | 2: ").append(odd2).append("</p>")
                        .append("</div>");*/
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

            System.out.println("public/index.html başarıyla oluşturuldu!");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}

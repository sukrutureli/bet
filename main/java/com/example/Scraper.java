import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {
        // ChromeDriver otomatik yönetimi
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");   // Headless mod
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            // Örnek statik site (IDDAA bülteni)
            String url = "https://www.example.com/iddaabulleti"; // Statik URL
            driver.get(url);

            // Örnek: tüm maçları bir listeye al
            List<WebElement> matches = driver.findElements(By.cssSelector(".match-row"));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>IDDAA Bülteni</title></head><body>");
            html.append("<h1>IDDAA Güncel Bülteni</h1><ul>");

            for (WebElement match : matches) {
                String home = match.findElement(By.cssSelector(".home-team")).getText();
                String away = match.findElement(By.cssSelector(".away-team")).getText();
                String time = match.findElement(By.cssSelector(".match-time")).getText();

                html.append("<li>").append(time).append(": ").append(home).append(" - ").append(away).append("</li>");
            }

            html.append("</ul>");
            html.append("<p>Güncelleme zamanı: ").append(java.time.LocalDateTime.now()).append("</p>");
            html.append("</body></html>");

            // public/index.html oluştur
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

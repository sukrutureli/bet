package com.example.scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.example.model.RealScores;

import java.time.*;
import java.util.*;

/**
 * Nesine canlı skor sayfalarından (futbol ve basketbol) bitmiş maç skorlarını
 * çeker. - 00:00–06:00 arası "Dün" sekmesine otomatik geçer - Bitmiş maçları
 * .board varlığına göre tespit eder - Headless, incognito, cache disable
 * modunda çalışır
 */
public class ControlScraper {

	private WebDriver driver;
	private WebDriverWait wait;
	private List<RealScores> results;

	public ControlScraper() {
		setupDriver();
		results = new ArrayList<RealScores>();
	}

	private void setupDriver() {
		System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
				"--window-size=1920,1080", "--disable-blink-features=AutomationControlled", "--disable-cache",
				"--incognito");

		driver = new ChromeDriver(options);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	// =============================================================
	// ⚽ FUTBOL: Bitmiş maç skorlarını çek
	// =============================================================
	public Map<String, String> fetchFinishedScores(List<RealScores> rsList) {
		Map<String, String> scores = new HashMap<>();
		if (rsList != null && !rsList.isEmpty()) {
			results.addAll(rsList);
		}
		try {
			String url = "https://www.nesine.com/iddaa/canli-skor/futbol";
			driver.get(url);

			waitForPageLoad(driver, 15);
			Thread.sleep(1500);
			clickYesterdayTabIfNeeded(driver);

			// lazy load: scroll aşağıya
			JavascriptExecutor js = (JavascriptExecutor) driver;
			for (int i = 0; i < 4; i++) {
				js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
				Thread.sleep(1200);
			}

			// 🔹 hem match hem extra-time class’larını kapsa
			String selector = "li[class*='match'], li[class*='extra-time']";
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(selector)));

			List<WebElement> matches = driver.findElements(By.cssSelector(selector));
			System.out.println("Toplam maç bulundu: " + matches.size());

			for (WebElement match : matches) {
				try {
					String cls = match.getAttribute("class");
					if (cls == null)
						continue;

					// bitmiş olanlar
					if (!(cls.contains("finished") || cls.contains("unlive") || cls.contains("not-play")
							|| cls.contains("extra-time")))
						continue;

					// sadece normal süre board’u (penaltı board’u değil)
					WebElement board = match.findElement(By.cssSelector(".teams-score-content .board"));
					String home = safeText(match.findElement(By.cssSelector(".home-team span[aria-hidden='true']")),
							driver);
					String away = safeText(match.findElement(By.cssSelector(".away-team span[aria-hidden='true']")),
							driver);
					String homeScore = safeText(board.findElement(By.cssSelector(".home-score")), driver);
					String awayScore = safeText(board.findElement(By.cssSelector(".away-score")), driver);
					String score = homeScore + "-" + awayScore;
					
					RealScores tempRealScores = new RealScores();
					tempRealScores.setHomeTeam(home);
					tempRealScores.setAwayTeam(away);
					tempRealScores.setScore(score);
					int count = 0;
					for (RealScores rs : results) {
						if (rs.getHomeTeam().equals(tempRealScores.getHomeTeam())
								&& rs.getAwayTeam().equals(tempRealScores.getAwayTeam())) {
							if (rs.getScore().equals("-")) {
								rs.setScore(score);
							}
							count++;
							break;
						}
					}
					if (count == 0) {
						results.add(tempRealScores);
					}

					scores.put(home + " - " + away, score);
					System.out.println("✅ " + home + " - " + away + " → " + score);

				} catch (Exception e) {
					System.out.println("⚠️ Tekil maç hatası: " + e.getMessage());
				}
			}

			System.out.println("⚽ Bitmiş maç sayısı: " + scores.size());

		} catch (Exception e) {
			System.out.println("fetchFinishedScores hata: " + e.getMessage());
		}
		return scores;
	}

	// =============================================================
	// ⏪ Gece 00:00–06:00 arası "Dün" sekmesine geç
	// =============================================================
	private void clickYesterdayTabIfNeeded(WebDriver driver) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			JavascriptExecutor js = (JavascriptExecutor) driver;

			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".live-result-menu")));
			Thread.sleep(1000);

			LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
			if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.of(6, 0))) {

				List<WebElement> tabs = driver
						.findElements(By.xpath("//span[contains(@class,'menu-item') and contains(@class,'tab')]"));
				WebElement yesterdayTab = null;

				for (int i = 0; i < tabs.size(); i++) {
					if (tabs.get(i).getText().contains("Bugün") && i > 0) {
						yesterdayTab = tabs.get(i - 1);
						break;
					}
				}

				if (yesterdayTab != null) {
					js.executeScript("arguments[0].classList.remove('disabled');", yesterdayTab);
					js.executeScript("arguments[0].scrollIntoView({block:'center'});", yesterdayTab);
					Thread.sleep(1000);
					js.executeScript("arguments[0].click();", yesterdayTab);
					Thread.sleep(1500);
					System.out.println("⏪ Dün sekmesine geçildi.");
				} else {
					System.out.println("⚠️ Dün sekmesi bulunamadı.");
				}

			} else {
				System.out.println("📅 Şu an bugün sekmesi aktif, geçiş yapılmadı.");
			}

		} catch (Exception e) {
			System.out.println("⚠️ Dün sekmesine geçilemedi: " + e.getMessage());
		}
	}

	// =============================================================
	// 🧹 Yardımcı metotlar
	// =============================================================

	public void close() {
		try {
			driver.quit();
		} catch (Exception ignore) {
		}
	}

	private String safeText(WebElement el, WebDriver driver) {
		try {
			String text = el.getAttribute("textContent");
			if (text == null || text.trim().isEmpty())
				text = el.getText();
			return text == null ? "-" : text.trim();
		} catch (Exception e) {
			try {
				return ((JavascriptExecutor) driver)
						.executeScript("return arguments[0].innerText || arguments[0].textContent;", el).toString()
						.trim();
			} catch (Exception inner) {
				return "-";
			}
		}
	}

	public void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
		new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
				.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
						.equals("complete"));
	}

	public List<RealScores> getResults() {
		return results;
	}
}

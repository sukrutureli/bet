package com.example;

import com.example.model.MatchInfo;
import com.example.model.MatchResult;
import com.example.model.Odds;
import com.example.model.TeamMatchHistory;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchScraper {

	private WebDriver driver;
	private JavascriptExecutor js;
	private WebDriverWait wait;

	public MatchScraper() {
		setupDriver();
	}

	private void setupDriver() {
		System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
				"--window-size=1920,1080", "--disable-blink-features=AutomationControlled");
		driver = new ChromeDriver(options);
		js = (JavascriptExecutor) driver;
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	// =============================================================
	// ANA SAYFA MAÇLARINI ÇEK
	// =============================================================
	public List<MatchInfo> fetchMatches() {
		List<MatchInfo> list = new ArrayList<>();
		try {
			String date = LocalDate.now(ZoneId.of("Europe/Istanbul")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
			String url = "https://www.nesine.com/iddaa?et=1&le=2&dt=" + date;
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 20);
			scrollToEnd();

			List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
			System.out.println("Final element sayısı: " + events.size());

			for (int i = 0; i < events.size(); i++) {
				WebElement e = events.get(i);
				MatchInfo info = extractMatchInfo(e, i);
				if (info != null)
					list.add(info);
			}

		} catch (Exception e) {
			System.out.println("fetchMatches hata: " + e.getMessage());
		}
		return list;
	}

	private void scrollToEnd() throws InterruptedException {
		int stable = 0, prev = -1;
		while (stable < 3) {
			List<WebElement> events = driver.findElements(By.cssSelector("div.odd-col.event-list.pre-event"));
			int size = events.size();
			js.executeScript("window.scrollBy(0,1500)");
			Thread.sleep(800);
			stable = (size == prev) ? stable + 1 : 0;
			prev = size;
		}
	}

	private MatchInfo extractMatchInfo(WebElement event, int index) {
		try {
			js.executeScript("arguments[0].scrollIntoView({block:'center'});", event);
			Thread.sleep(100);

			String name = "?";
			String url = null;

			List<WebElement> nameLinks = event.findElements(By.cssSelector("div.name a"));
			if (!nameLinks.isEmpty()) {
				name = nameLinks.get(0).getText().trim();
				url = nameLinks.get(0).getAttribute("href");
			}

			if (url == null) {
				for (WebElement a : event.findElements(By.tagName("a"))) {
					String href = a.getAttribute("href");
					if (href != null && href.contains("istatistik.nesine.com")) {
						url = href;
						break;
					}
				}
			}

			String time = extractMatchTime(event);
			Odds odds = extractOdds(event);
			return new MatchInfo(name, time, url, odds, index);
		} catch (Exception e) {
			System.out.println("extractMatchInfo hata: " + e.getMessage());
			return null;
		}
	}

	private String extractMatchTime(WebElement e) {
		try {
			List<WebElement> spans = e.findElements(By.cssSelector("div.time span"));
			return spans.isEmpty() ? "?" : spans.get(0).getText().trim();
		} catch (Exception ex) {
			return "?";
		}
	}

	private String extractOddFromCell(WebElement cell) {
		try {
			// Önce <a class="odd"> var mı bak
			List<WebElement> link = cell.findElements(By.cssSelector("a.odd"));
			if (!link.isEmpty()) {
				return safeText(link.get(0));
			}

			// Eğer <a> yoksa <span class="odd"> kontrol et
			List<WebElement> span = cell.findElements(By.cssSelector("span.odd"));
			if (!span.isEmpty()) {
				return safeText(span.get(0));
			}

			// Hiçbiri yoksa "-"
			return "-";
		} catch (Exception e) {
			return "-";
		}
	}

	private Odds extractOdds(WebElement event) {
		String[] o = { "-", "-", "-", "-", "-", "-", "-" };
		int mbs = -1;

		try {
			List<WebElement> mbsElements = event.findElements(By.cssSelector(".mbs-box-desktop"));
			if (!mbsElements.isEmpty()) {
				String classAttr = mbsElements.get(0).getAttribute("class");
				if (classAttr.contains("mbs")) {
					String num = classAttr.replaceAll(".*mbs(\\d+)-desktop.*", "$1");
					try {
						mbs = Integer.parseInt(num);
					} catch (NumberFormatException ignore) {
					}
				}
			}

			// 1X2 oranları
			List<WebElement> main = event.findElements(By.cssSelector("dd.col-03.event-row .cell"));
			for (int i = 0; i < main.size() && i < 3; i++) {
				o[i] = extractOddFromCell(main.get(i));
			}

			// Alt/Üst ve Var/Yok
			List<WebElement> extra = event.findElements(By.cssSelector("dd.col-02.event-row .cell"));
			for (int i = 0; i < extra.size() && i < 4; i++) {
				o[3 + i] = extractOddFromCell(extra.get(i));
			}

		} catch (Exception e) {
			System.out.println("Oran ana hatası: " + e.getMessage());
		}

		return new Odds(toDouble(o[0]), // 1
				toDouble(o[1]), // X
				toDouble(o[2]), // 2
				toDouble(o[4]), // Üst
				toDouble(o[3]), // Alt
				toDouble(o[5]), // Var
				toDouble(o[6]), // Yok
				mbs);
	}

	private double toDouble(String s) {
		try {
			if (s == null || s.equals("-") || s.isEmpty())
				return 0.0;
			return Double.parseDouble(s.replace(",", "."));
		} catch (Exception e) {
			return 0.0;
		}
	}

	private String safeText(WebElement el) {
		try {
			return el.getText().trim();
		} catch (Exception e) {
			return "-";
		}
	}

	// =============================================================
	// TAKIM GEÇMİŞİ
	// =============================================================
	public TeamMatchHistory scrapeTeamHistory(String detailUrl, String name) {
		if (detailUrl == null || !detailUrl.startsWith("http"))
			return null;

		String[] teams = extractTeamsFromHeader(detailUrl);
		String home = teams[0];
		String away = teams[1];
		String title = teams[2];

		TeamMatchHistory th = new TeamMatchHistory(title, home, away, detailUrl);
		try {
			List<MatchResult> rekabet = scrapeRekabetGecmisi(detailUrl + "/rekabet-gecmisi");
			rekabet.forEach(th::addRekabetGecmisiMatch);

			List<MatchResult> sonHome = scrapeSonMaclar(detailUrl + "/son-maclari", 1);
			sonHome.forEach(m -> th.addSonMacMatch(m, 1));

			List<MatchResult> sonAway = scrapeSonMaclar(detailUrl + "/son-maclari", 2);
			sonAway.forEach(m -> th.addSonMacMatch(m, 2));
		} catch (Exception e) {
			System.out.println("scrapeTeamHistory hata: " + e.getMessage());
		}
		return th;
	}

	private List<MatchResult> scrapeRekabetGecmisi(String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 10);
			selectTournament();

			try {
				wait.until(ExpectedConditions
						.presenceOfElementLocated(By.cssSelector("div[data-test-id='CompitionHistoryTable']")));
			} catch (Exception e) {
				System.out.println("Rekabet geçmişi tablosu yok");
				return list;
			}

			// clickMoreMatches();
			list = extractCompetitionHistoryResults(url);
		} catch (Exception e) {
			System.out.println("Rekabet geçmişi hatası: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> scrapeSonMaclar(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 10);
			selectTournament();
			String sel = (side == 1) ? "div[data-test-id='LastMatchesTableFirst'] tbody tr"
					: "div[data-test-id='LastMatchesTableSecond'] tbody tr";

			try {
				wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sel)));
			} catch (Exception e) {
				System.out.println("Son maçlar tablosu yok: " + ((side == 1) ? "EvSahibi" : "Deplasman"));
				return list;
			}

			// clickMoreMatches();
			list = extractMatchResults(url, side);
		} catch (Exception e) {
			System.out.println("Son maç hatası: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> extractCompetitionHistoryResults(String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			List<WebElement> rows = driver
					.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));
			for (WebElement r : rows) {
				try {
					String league = safeText(
							r.findElement(By.cssSelector("[data-test-id='CompitionTableItemLeague']")));
					String date = safeText(r.findElement(By.cssSelector("[data-test-id='CompitionTableItemSeason']")));
					String homeTeam = extractTeamName(r.findElement(By.cssSelector("div[data-test-id='HomeTeam']")));
					String awayTeam = extractTeamName(r.findElement(By.cssSelector("div[data-test-id='AwayTeam']")));
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(homeTeam, awayTeam, sc[0], sc[1], date, league, "rekabet-gecmisi", url));
				} catch (Exception ex) {
					System.out.println("Rekabet satırı hatası: " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractCompetitionHistoryResults hata: " + e.getMessage());
		}
		return list;
	}

	private List<MatchResult> extractMatchResults(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		String sel = (side == 1) ? "div[data-test-id='LastMatchesTableFirst'] tbody tr"
				: "div[data-test-id='LastMatchesTableSecond'] tbody tr";
		try {
			List<WebElement> rows = driver.findElements(By.cssSelector(sel));
			for (WebElement r : rows) {
				try {
					String league = safeText(r.findElement(By.cssSelector("td[data-test-id='TableBodyLeague']")));
					String homeTeam = extractTeamName(r.findElement(By.cssSelector("div[data-test-id='HomeTeam']")));
					String awayTeam = extractTeamName(r.findElement(By.cssSelector("div[data-test-id='AwayTeam']")));
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(homeTeam, awayTeam, sc[0], sc[1], league, "", "son-maclari", url));
				} catch (Exception ex) {
					System.out.println("Son maç satırı hatası: " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("extractMatchResults hata: " + e.getMessage());
		}
		return list;
	}

	private String extractScore(WebElement r) {
		try {
			for (WebElement s : r.findElements(By.cssSelector("button[data-test-id='NsnButton'] span"))) {
				String t = s.getText().replaceAll("\\(.*?\\)", "").trim();
				if (t.matches("\\d+\\s*-\\s*\\d+"))
					return t;
			}
		} catch (Exception e) {
			return "-";
		}
		return "-";
	}

	private int[] parseScore(String s) {
		try {
			String[] p = s.split("-");
			return new int[] { Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()) };
		} catch (Exception e) {
			return new int[] { -1, -1 };
		}
	}

	private void selectTournament() {
		try {
			PageWaitUtils.safeClick(driver, By.cssSelector("div[data-test-id='CustomDropdown']"), 5);
			wait.until(ExpectedConditions.visibilityOfElementLocated(
					By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")));
			driver.findElement(By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")).click();
			Thread.sleep(300);
		} catch (Exception e) {
			System.out.println("Turnuva seçimi atlandı");
		}
	}

	/*
	 * private void clickMoreMatches() { try { // sayfadaki tüm buton ve linkleri al
	 * List<WebElement> candidates =
	 * driver.findElements(By.cssSelector("button, a")); WebElement found = null;
	 * 
	 * // olası "daha", "more", "load", "show" kelimelerini içerenleri tara for
	 * (WebElement el : candidates) { String text = ""; try { text =
	 * el.getText().toLowerCase().trim(); } catch (Exception ignore) { } if
	 * (text.isEmpty()) continue;
	 * 
	 * if (text.contains("daha") || text.contains("more") || text.contains("load")
	 * || text.contains("show")) { if (el.isDisplayed() && el.isEnabled()) { found =
	 * el; break; } } }
	 * 
	 * // buton bulundu → kaydır & tıkla try {
	 * js.executeScript("arguments[0].scrollIntoView({block:'center'});", found); }
	 * catch (Exception ignore) { } boolean clicked =
	 * PageWaitUtils.safeClick(driver, found, 5); if (clicked) { Thread.sleep(1000);
	 * // yükleme için kısa bekleme } } catch (Exception e) {
	 * System.out.println("clickMoreMatches hata: " + e.getMessage()); } }
	 */

	private String[] extractTeamsFromHeader(String url) {
		String home = "-", away = "-", name = "";
		try {
			driver.get(url);
			PageWaitUtils.waitForPageLoad(driver, 12);

			try {
				wait.until(ExpectedConditions
						.visibilityOfElementLocated(By.cssSelector("div[data-test-id='HeaderTeams']")));
			} catch (Exception e) {
				System.out.println("Takım adları çekilemedi.");
			}

			WebElement header = driver.findElement(By.cssSelector("div[data-test-id='HeaderTeams']"));
			List<WebElement> teams = header
					.findElements(By.cssSelector("a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams']"));

			if (teams.size() >= 2) {
				home = teams.get(0).getText().trim();
				away = teams.get(1).getText().trim();
			}

		} catch (Exception e) {
			System.out.println("Takım adları çekilemedi: " + e.getMessage());
		}
		name = home + " - " + away;
		return new String[] { home, away, name };
	}

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
			if (txt == null)
				continue;

			txt = txt.trim();
			if (txt.isEmpty())
				continue;

			// Tek başına sayı (Örn. "1", "2") â†’ kart sayısı, atla
			if (txt.matches("\\d+"))
				continue;

			// Sadece harf, rakam, boşluk, nokta, parantez ve tire kalsın
			txt = txt.replaceAll("[^\\p{L}0-9\\s\\.\\-\\(\\)'&´]", "");

			if (txt.isEmpty())
				continue;

			if (sb.length() > 0)
				sb.append(" ");
			sb.append(txt);
		}

		return sb.toString().trim();
	}

	// =============================================================
	// CANLI SKOR (BİTMİŞ MAÇLAR) ÇEK
	// =============================================================
	public Map<String, String> fetchFinishedScores() {
		Map<String, String> scores = new HashMap<>();
		try {
			String url = "https://www.nesine.com/iddaa/canli-skor/futbol";
			driver.get(url);
			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.match-not-play")));

			Thread.sleep(2000); // Dinamik skor tablosunun yüklenmesi için kısa bekleme

			List<WebElement> allMatches = driver.findElements(By.cssSelector("li.match-not-play"));
			List<WebElement> finishedMatches = new ArrayList<>();

			for (WebElement match : allMatches) {
				try {
					WebElement statusEl = match.findElement(By.cssSelector(".statusLive.status"));
					String status = statusEl.getAttribute("class");
					if (status.contains("finished")) {
						finishedMatches.add(match);
					}
				} catch (Exception ignore) {
				}
			}

			System.out.println("Bitmiş maç sayısı: " + finishedMatches.size());

			for (WebElement match : finishedMatches) {
				try {
					String home = safeText(match.findElement(By.cssSelector(".home-team span")));
					String away = safeText(match.findElement(By.cssSelector(".away-team span")));

					// Skor
					WebElement scoreBoard = match.findElement(By.cssSelector(".board"));
					String homeScore = safeText(scoreBoard.findElement(By.cssSelector(".home-score")));
					String awayScore = safeText(scoreBoard.findElement(By.cssSelector(".away-score")));
					String score = homeScore + "-" + awayScore;

					String key = home + " - " + away;
					scores.put(key, score);
					System.out.println("✅ " + key + " → " + score);

				} catch (Exception e) {
					System.out.println("⚠️ Tekil maç çekilemedi: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("fetchFinishedScores hata: " + e.getMessage());
		}
		return scores;
	}

	public void close() {
		try {
			driver.quit();
		} catch (Exception ignore) {
		}
	}
}

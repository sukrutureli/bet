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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			String url = "https://www.nesine.com/iddaa?et=1&dt=" + date + "&le=2&ocg=MS-2%2C5&gt=Pop%C3%BCler";

			driver.manage().deleteAllCookies();
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 25);

			List<Map<String, String>> rawData = scrollAndCollectMatchData();
			System.out.println("✅ Toplam benzersiz maç: " + rawData.size());

			int index = 0;
			for (Map<String, String> data : rawData) {
				try {
					String name = data.getOrDefault("name", "-");
					String href = data.getOrDefault("url", "-");
					String time = data.getOrDefault("time", "-");

					Odds odds = new Odds(toDouble(data.get("ms1")), toDouble(data.get("ms0")),
							toDouble(data.get("ms2")), toDouble(data.get("ust")), toDouble(data.get("alt")),
							toDouble(data.get("var")), toDouble(data.get("yok")),
							Integer.parseInt(data.getOrDefault("mbs", "-1")));

					list.add(new MatchInfo(name, time, href, odds, index++));
				} catch (Exception e) {
					System.out.println("⚠️ MatchInfo oluşturulamadı: " + e.getMessage());
				}
			}

		} catch (Exception e) {
			System.out.println("fetchMatches hata: " + e.getMessage());
		}
		return list;
	}

	private List<Map<String, String>> scrollAndCollectMatchData() throws InterruptedException {
	    By eventSelector = By.cssSelector("div[id^='r_'].event-list[data-sport-id='1']");
	    Set<String> seen = new HashSet<>();
	    List<Map<String, String>> collected = new ArrayList<>();

	    int stable = 0, prevCount = 0;
	    int maxScroll = 150; // Maksimum scroll iterasyonu
	    int scrollAmount = 600;
	    
	    // Sayfanın yüklenmesini bekle
	    int waitTry = 0;
	    while (driver.findElements(eventSelector).isEmpty() && waitTry < 15) {
	        Thread.sleep(500);
	        waitTry++;
	    }
	    System.out.println("⏳ İlk maçlar göründü (" + waitTry + "sn sonra) - scroll başlıyor...");

	    long startTime = System.currentTimeMillis();
	    long maxWaitTime = 240000; // 2 dakika max

	    for (int i = 0; i < maxScroll; i++) {
	        // Timeout kontrolü
	        if (System.currentTimeMillis() - startTime > maxWaitTime) {
	            System.out.println("⏱️ Maksimum süre aşıldı, scroll sonlandırılıyor");
	            break;
	        }

	        Thread.sleep(300);
	        
	        List<WebElement> matches = driver.findElements(eventSelector);
	        
	        // Tüm maçları işle
	        for (WebElement el : matches) {
	            try {
	                String name = el.findElement(By.cssSelector("div.name a")).getText().trim();
	                if (!seen.contains(name) && !name.isEmpty()) {
	                    seen.add(name);

	                    Map<String, String> map = new HashMap<>();
	                    map.put("name", name);

	                    String href = el.findElement(By.cssSelector("div.name a")).getAttribute("href");
	                    if (href == null || href.contains("javascript:void") || href.isEmpty()) {
	                        continue;
	                    }
	                    map.put("url", href);

	                    map.put("time", el.findElement(By.cssSelector("div.time span")).getText().trim());

	                    // MS1, MS0, MS2
	                    List<WebElement> odds1x2 = el.findElements(By.cssSelector("dd.col-03.event-row .cell"));
	                    if (odds1x2.size() >= 3) {
	                        map.put("ms1", odds1x2.get(0).getText());
	                        map.put("ms0", odds1x2.get(1).getText());
	                        map.put("ms2", odds1x2.get(2).getText());
	                    } else {
	                        map.put("ms1", "-");
	                        map.put("ms0", "-");
	                        map.put("ms2", "-");
	                    }

	                    // ALT, ÜST
	                    List<WebElement> altust = el.findElements(By.cssSelector("dd.col-02.event-row .cell"));
	                    if (altust.size() >= 4) {
	                        map.put("alt", altust.get(0).getText());
	                        map.put("ust", altust.get(1).getText());
	                        map.put("var", altust.get(2).getText());
	                        map.put("yok", altust.get(3).getText());
	                    } else {
	                        map.put("alt", "-");
	                        map.put("ust", "-");
	                        map.put("var", "-");
	                        map.put("yok", "-");
	                    }

	                    // MBS
	                    try {
	                        WebElement mbsEl = el.findElement(By.cssSelector(".mbs-box-desktop"));
	                        String cls = mbsEl.getAttribute("class");
	                        String mbsNum = cls.replaceAll(".*mbs(\\d+)-desktop.*", "$1");
	                        map.put("mbs", mbsNum);
	                    } catch (Exception ex) {
	                        map.put("mbs", "-1");
	                    }

	                    collected.add(map);
	                }
	            } catch (Exception ignore) {
	            }
	        }

	        // Stabilite kontrolü
	        if (seen.size() == prevCount) {
	            stable++;
	            System.out.println("  ⚠️ Stabilite sayacı: " + stable + "/8 (toplam: " + seen.size() + ")");
	        } else {
	            stable = 0;
	            System.out.println("  ✓ Maç sayısı: " + seen.size() + " (+yeni " + (seen.size() - prevCount) + ")");
	        }
	        prevCount = seen.size();

	        // 8 kez ardarda sabitlik → dur
	        if (stable >= 8) {
	            System.out.println("✅ Scroll tamamlandı (sabitliğe ulaşıldı)");
	            break;
	        }

	        // Scroll yap
	        js.executeScript("window.scrollBy(0, " + scrollAmount + ");");
	        Thread.sleep(800); // Stabil waiting time
	    }

	    System.out.println("🧩 TOPLAM BENZERSIZ MAÇ: " + seen.size());
	    return collected;
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

	// =============================================================
	// REKABET GEÇMİŞİ
	// =============================================================
	private List<MatchResult> scrapeRekabetGecmisi(String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 12);
			Thread.sleep(1000);

			selectTournament();

			// tabloyu bekle
			try {
				wait.until(ExpectedConditions.or(
						ExpectedConditions
								.presenceOfElementLocated(By.cssSelector("div[data-test-id='CompitionHistoryTable']")),
						ExpectedConditions.presenceOfElementLocated(
								By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"))));
			} catch (Exception e) {
				System.out.println("⚠️ Rekabet geçmişi tablosu yok");
				return list;
			}

			Thread.sleep(800);
			list = extractCompetitionHistoryResults(url);

		} catch (Exception e) {
			System.out.println("⚠️ Rekabet geçmişi hatası: " + e.getMessage());
		}
		return list;
	}

	// =============================================================
	// SON MAÇLAR
	// =============================================================
	private List<MatchResult> scrapeSonMaclar(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		try {
			driver.get(url);
			PageWaitUtils.safeWaitForLoad(driver, 12);
			Thread.sleep(1000);

			selectTournament();

			String sel = (side == 1)
					? "div[data-test-id^='LastMatchesTable'][data-test-id*='First'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Home'] tbody tr"
					: "div[data-test-id^='LastMatchesTable'][data-test-id*='Second'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Away'] tbody tr";

			try {
				wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sel)));
			} catch (Exception e) {
				System.out.println("⚠️ Son maçlar tablosu yok: " + ((side == 1) ? "Ev Sahibi" : "Deplasman"));
				return list;
			}

			Thread.sleep(800);
			list = extractMatchResults(url, side);

		} catch (Exception e) {
			System.out.println("⚠️ Son maç hatası: " + e.getMessage());
		}
		return list;
	}

	// =============================================================
	// REKABET SONUÇLARINI AYIKLA
	// =============================================================
	private List<MatchResult> extractCompetitionHistoryResults(String url) {
		List<MatchResult> list = new ArrayList<>();
		try {
			List<WebElement> rows = driver
					.findElements(By.cssSelector("div[data-test-id='CompitionHistoryTableItem']"));
			System.out.println("🔹 Rekabet geçmişi satır sayısı: " + rows.size());

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
					System.out.println("⚠️ Rekabet satırı hatası: " + ex.getMessage());
				}
			}

		} catch (Exception e) {
			System.out.println("extractCompetitionHistoryResults hata: " + e.getMessage());
		}
		return list;
	}

	// =============================================================
	// SON MAÇ SONUÇLARINI AYIKLA
	// =============================================================
	private List<MatchResult> extractMatchResults(String url, int side) {
		List<MatchResult> list = new ArrayList<>();
		String sel = (side == 1)
				? "div[data-test-id^='LastMatchesTable'][data-test-id*='First'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Home'] tbody tr"
				: "div[data-test-id^='LastMatchesTable'][data-test-id*='Second'] tbody tr, div[data-test-id^='LastMatchesTable'][data-test-id*='Away'] tbody tr";

		try {
			List<WebElement> rows = driver.findElements(By.cssSelector(sel));
			System.out.println("🔹 Son maç (" + (side == 1 ? "Ev" : "Dep") + ") satır sayısı: " + rows.size());

			for (WebElement r : rows) {
				try {
					String league = safeText(r.findElement(By.cssSelector("td[data-test-id='TableBodyLeague']")));
					String homeTeam = extractTeamName(r.findElement(By.cssSelector("div[data-test-id='HomeTeam']")));
					String awayTeam = extractTeamName(r.findElement(By.cssSelector("div[data-test-id='AwayTeam']")));
					String score = extractScore(r);
					int[] sc = parseScore(score);
					list.add(new MatchResult(homeTeam, awayTeam, sc[0], sc[1], league, "", "son-maclari", url));
				} catch (Exception ex) {
					System.out.println("⚠️ Son maç satırı hatası: " + ex.getMessage());
				}
			}

		} catch (Exception e) {
			System.out.println("extractMatchResults hata: " + e.getMessage());
		}
		return list;
	}

	private String extractScore(WebElement r) {
		try {
			List<WebElement> scoreEls = r
					.findElements(By.cssSelector("div[data-test-id='Score'], button[data-test-id='NsnButton'] span"));
			for (WebElement s : scoreEls) {
				String t = s.getText().replaceAll("\\(.*?\\)", "").trim();
				if (t.matches("\\d+\\s*-\\s*\\d+"))
					return t;
			}
		} catch (Exception e) {
			// ignore
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
			WebElement dropdown = wait.until(
					ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-test-id='CustomDropdown']")));
			dropdown.click();
			wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//div[@role='option']//span[contains(text(),'Bu Turnuva')]")))
					.click();
			Thread.sleep(500);
		} catch (Exception e) {
			System.out.println("Turnuva seçimi atlandı veya zaten seçili.");
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
			List<WebElement> teams = header.findElements(By.cssSelector(
					"a[data-test-id='TeamLink'] span[data-test-id='HeaderTeams'], a[data-test-id='TeamLink'] div[data-test-id='HeaderTeams']"));

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

package com.example.model;

public class MatchInfo {
	private String name;
	private String time;
	private String detailUrl;
	private Odds odds;
	private int index;
	private int mbs;

	public MatchInfo(String name, String time, String detailUrl, Odds odds, int index) {
		this.name = name;
		this.time = time;
		this.detailUrl = detailUrl;
		this.odds = odds;
		this.index = index;
	}

	// Getters
	public String getName() {
		return name;
	}

	public String getTime() {
		return time;
	}

	public String getDetailUrl() {
		return detailUrl;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Odds getOdds() {
		return odds;
	}

	public int getMbs() {
		return mbs;
	}

	public void setMbs(int mbs) {
		this.mbs = mbs;
	}

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

package com.example.model;

public class Odds {
	// 1-X-2, Over2.5/Under2.5, BTTS Yes/No
	private final Double ms1;
	private final Double msX;
	private final Double ms2;
	private final Double over25;
	private final Double under25;
	private final Double bttsYes;
	private final Double bttsNo;
	private final int mbs;

	public Odds(Double ms1, Double msX, Double ms2, Double over25, Double under25, Double bttsYes, Double bttsNo,
			int mbs) {
		this.ms1 = ms1;
		this.msX = msX;
		this.ms2 = ms2;
		this.over25 = over25;
		this.under25 = under25;
		this.bttsYes = bttsYes;
		this.bttsNo = bttsNo;
		this.mbs = mbs;
	}

	public Double getMs1() {
		return ms1;
	}

	public Double getMsX() {
		return msX;
	}

	public Double getMs2() {
		return ms2;
	}

	public Double getOver25() {
		return over25;
	}

	public Double getUnder25() {
		return under25;
	}

	public Double getBttsYes() {
		return bttsYes;
	}

	public Double getBttsNo() {
		return bttsNo;
	}

	public int getMbs() {
		return mbs;
	}

	public static double impliedProb(Double odd) {
		if (odd == null || odd <= 1.0)
			return Double.NaN;
		return 1.0 / odd;
	}
}

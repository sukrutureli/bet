package com.example.model;

public class TeamStats {

    private String name;

    // temel ortalamalar (Poisson vb. modellerde kullanılıyor)
    private double avgGF;   // maç başı atılan
    private double avgGA;   // maç başı yenilen
    private double ppg;

    // --- ek alanlar ---
    private int last5Points = 0;    // Son 5 maçtan puan toplamı (0-15)
    private int rating100   = 50;   // 0-100 arası basit güç puanı

    // --- getters/setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getAvgGF() { return avgGF; }
    public void setAvgGF(double avgGF) { this.avgGF = avgGF; }
    public double getAvgGA() { return avgGA; }
    public void setAvgGA(double avgGA) { this.avgGA = avgGA; }
    

    public int getLast5Points() { return last5Points; }
    public void setLast5Points(int v) { this.last5Points = v; }

    public int getRating100() { return rating100; }
    public void setRating100(int v) { this.rating100 = v; }
	public double getPpg() {
		return ppg;
	}
	public void setPpg(double ppg) {
		this.ppg = ppg;
	}
	
	@Override
    public String toString() {
		return "avgGF -> " + avgGF + ", " + "avgGA -> " + avgGA + ", " + "ppg -> " + ppg + ", " +
				"last5Points -> " + last5Points + ", " +"rating100 -> " + rating100; 
	}
}


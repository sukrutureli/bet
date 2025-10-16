package com.example.prediction;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class PredictionUpdater {

	private final ObjectMapper mapper;

	public PredictionUpdater() {
		this.mapper = new ObjectMapper();
	}

	public void updateYesterday(String outputDir) throws Exception {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		String fileName = "predictions_" + yesterday + ".json";
		Path jsonPath = Paths.get(outputDir, fileName);
		File jsonFile = jsonPath.toFile();

		if (!jsonFile.exists()) {
			System.out.println("⚠️ Dünkü tahmin dosyası bulunamadı: " + jsonFile);
			return;
		}

		JsonNode root = mapper.readTree(jsonFile);
		for (JsonNode match : root.get("matches")) {
			ObjectNode matchObj = (ObjectNode) match;
			String home = match.get("homeTeam").asText();
			String away = match.get("awayTeam").asText();

			// 🔸 Burada gerçek skoru kendi scraper'ından çek
			String actualScore = fetchScoreFromNesine(home, away);
			matchObj.put("actualScore", actualScore);

			// 🔸 Basit tutma kontrolü
			String pick = match.get("predictions").get("pick").asText();
			String result = evaluate(pick, actualScore);
			matchObj.put("result", result);
		}

		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(jsonFile, root);
		System.out.println("✅ Güncellendi: " + jsonFile.getName());
	}

	private String fetchScoreFromNesine(String home, String away) {
		// TODO: senin scraper’dan gerçek sonucu döndür
		return "-"; // geçici placeholder
	}

	private String evaluate(String pick, String actualScore) {
		if (actualScore == null || !actualScore.contains("-"))
			return "BELİRSİZ";
		String[] s = actualScore.split("-");
		int homeGoals = Integer.parseInt(s[0].trim());
		int awayGoals = Integer.parseInt(s[1].trim());

		if (pick.startsWith("MS1") && homeGoals > awayGoals)
			return "TUTTU";
		if (pick.startsWith("MS2") && homeGoals < awayGoals)
			return "TUTTU";
		if (pick.startsWith("MSX") && homeGoals == awayGoals)
			return "TUTTU";
		return "YATTI";
	}
}

package com.example.prediction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.example.model.PredictionData;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PredictionSaver {

	private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	public static void saveTodayPredictions(List<PredictionData> predictions) throws IOException {
		// Bugünün tarihini al (İstanbul saatine göre)
		String today = LocalDate.now(ZoneId.of("Europe/Istanbul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		// data klasörü yoksa oluştur
		File folder = new File("public/data");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		// Dosya yolu (örnek: data/2025-10-16.json)
		File file = new File(folder, today + ".json");

		// JSON olarak kaydet
		mapper.writeValue(file, predictions);

		System.out.println("✅ Tahminler kaydedildi: " + file.getAbsolutePath());
	}

	// JSON'dan geri okumak istersen:
	public static List<PredictionData> readPredictions(String date) throws IOException {
		File file = new File("data", date + ".json");
		if (!file.exists())
			return new ArrayList<>();
		return mapper.readerForListOf(PredictionData.class).readValue(file);
	}
}

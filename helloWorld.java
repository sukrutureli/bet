import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HelloWorldHtml {
    public static void main(String[] args) {
        // Çıktı dosyası: public/index.html
        File outputDir = new File("public");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, "index.html");

        String html = """
                <!DOCTYPE html>
                <html lang="tr">
                <head>
                    <meta charset="UTF-8">
                    <title>Hello World</title>
                </head>
                <body>
                    <h1>Merhaba Dünya!</h1>
                    <p>Bu sayfa Java tarafından üretildi 🚀</p>
                </body>
                </html>
                """;

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html);
            System.out.println("index.html başarıyla oluşturuldu: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

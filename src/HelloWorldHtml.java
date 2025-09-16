import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HelloWorldHtml {
    public static void main(String[] args) {
        try {
            // public klasörünü oluştur
            File dir = new File("public");
            if (!dir.exists()) dir.mkdirs();

            // index.html dosyasını oluştur / overwrite et
            File file = new File(dir, "index.html");

            String html = """
                <!DOCTYPE html>
                <html lang="tr">
                <head>
                    <meta charset="UTF-8">
                    <title>Merhaba Dünya</title>
                </head>
                <body>
                    <h1>Merhaba Dünya!</h1>
                    <p>Bu sayfa Java tarafından üretildi 🚀</p>
                    <p>Güncelleme zamanı: %s</p>
                </body>
                </html>
                """.formatted(java.time.LocalDateTime.now());

            try (FileWriter fw = new FileWriter(file)) {
                fw.write(html);
            }

            System.out.println("public/index.html başarıyla oluşturuldu!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.scraper.htmltotext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Scraper {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            List<String> urls = new ArrayList<>();

            while (true) {
                System.out.print("Enter a link (URL): ");
                String url = scanner.nextLine().trim();

                if (url.isEmpty()) {
                    System.err.println("Error: URL cannot be empty.");
                    System.exit(2);
                }
                urls.add(url);

                String answer;
                while (true) {
                    System.out.print("Add another link? (y/n): ");
                    answer = scanner.nextLine().trim();

                    if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("n")) {
                        break;
                    }
                    System.out.println("Invalid input. Allowed characters are only 'y' or 'n'.");
                }

                if (answer.equalsIgnoreCase("n")) {
                    break;
                }
            }

            System.out.print("Enter output filename (e.g., notes.md): ");
            String filename = scanner.nextLine().trim();

            if (filename.isEmpty()) {
                System.err.println("Error: filename cannot be empty.");
                System.exit(2);
            }

            if (!filename.toLowerCase().endsWith(".md")) {
                filename = filename + ".md";
            }

            Path out = Path.of(filename);

            try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                for (String url : urls) {
                    String html = fetchHtml(url);
                    String text = extractReadableText(html);

                    writer.write("---");
                    writer.write("\n");
                    writer.write("Source: " + url);
                    writer.write("\n");
                    writer.write("Text:");
                    writer.write("\n\n");

                    if (text != null && !text.isBlank()) {
                        writer.write(text);
                        writer.write("\n");
                    }

                    writer.write("\n");
                    writer.flush();
                }
            }

            System.out.println("Saved: " + out.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Fetches HTML from a URL using Java HttpClient.
    private static String fetchHtml(String url) throws IOException, InterruptedException {
        URI uri = URI.create(url);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "HtmlToTextApp/1.0")
                .GET()
                .build();

        HttpResponse<byte[]> resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " fetching " + url);
        }

        return new String(resp.body(), StandardCharsets.UTF_8);
    }

    // Extracts readable text from HTML by removing noise and keeping key content elements.
    private static String extractReadableText(String html) {
        Document doc = Jsoup.parse(html);

        doc.select("script, style, noscript, svg, canvas, iframe").remove();
        doc.select("nav, header, footer").remove();

        Element body = doc.body();
        if (body == null) {
            return "";
        }

        Elements keep = body.select("h1, h2, h3, h4, h5, p, pre, table");
        StringBuilder sb = new StringBuilder();

        for (Element el : keep) {
            String tag = el.tagName();

            if (tag.equals("table")) {
                appendTableText(sb, el);
            } else if (tag.equals("pre")) {
                String preText = el.text().trim();
                if (!preText.isEmpty()) {
                    sb.append(preText).append("\n\n");
                }
            } else {
                String t = el.text().trim();
                if (!t.isEmpty()) {
                    sb.append(t).append("\n\n");
                }
            }
        }

        return sb.toString()
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    // Converts an HTML table into pipe-delimited plain text rows.
    private static void appendTableText(StringBuilder sb, Element table) {
        Elements rows = table.select("tr");
        for (Element tr : rows) {
            Elements cells = tr.select("th, td");
            if (cells.isEmpty()) {
                continue;
            }

            for (int i = 0; i < cells.size(); i++) {
                String cellText = cells.get(i).text().trim();
                sb.append(cellText);
                if (i < cells.size() - 1) {
                    sb.append(" | ");
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
    }
}

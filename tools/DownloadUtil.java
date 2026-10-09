import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Simple HTTPS downloader that uses Java's bundled cacerts (bypasses Windows Schannel issues).
 * Usage: java DownloadUtil.java <url> <dest>
 */
public class DownloadUtil {
    public static void main(String[] args) throws Exception {
        String url = args[0];
        Path dest = Paths.get(args[1]);
        Files.createDirectories(dest.getParent());

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 dsh-downloader");
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(120000);
        int code = conn.getResponseCode();
        if (code >= 400) {
            throw new IOException("HTTP " + code + " for " + url);
        }
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
            }
            System.out.println("OK " + dest + " (" + total + " bytes)");
        }
    }
}

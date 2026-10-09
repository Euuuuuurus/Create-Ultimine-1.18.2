import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;

/**
 * Rewrites a single text entry inside a jar, preserving every other entry byte-for-byte.
 * Used as a DEV-ENVIRONMENT workaround for FTB Library / FTB Ranks 1.18.2 maven artifacts,
 * whose mixin configs declare "compatibilityLevel": "JAVA_8". Mixin then refuses to parse
 * Minecraft 1.18.2 target classes (class file version 61 / Java 17), which aborts startup with
 * "Class version 61 required is higher than the class version supported by the current version
 * of Mixin (JAVA_8 supports class version 52)".
 *
 * Usage: java PatchJarEntry <in.jar> <out.jar> <entryName> <oldText> <newText>
 */
public class PatchJarEntry {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        String entryName = args[2];
        String oldText = args[3];
        String newText = args[4];

        int patched = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                byte[] data = zis.readAllBytes();
                if (e.getName().equals(entryName)) {
                    String text = new String(data, StandardCharsets.UTF_8);
                    if (text.contains(oldText)) {
                        text = text.replace(oldText, newText);
                        data = text.getBytes(StandardCharsets.UTF_8);
                        patched++;
                    }
                }
                ZipEntry copy = new ZipEntry(e.getName());
                copy.setTime(e.getTime());
                zos.putNextEntry(copy);
                zos.write(data);
                zos.closeEntry();
            }
        }
        if (patched != 1) {
            System.out.println("ABORT: expected exactly 1 patch site in " + entryName + ", got " + patched);
            Files.deleteIfExists(out);
            System.exit(1);
        }
        System.out.println("patched " + entryName + " in " + out.getFileName());
    }
}

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;

/**
 * Removes the jar-in-jar nested Registrate from the Create jar used by the local maven mirror.
 *
 * Rationale: Create ships Registrate as a nested jar, and Forge's JarJar turns it into a module named
 * "Registrate". This project also needs a *remapped* Registrate module (the nested one is SRG, which
 * makes Create fail at runtime with NoSuchFieldError: f_76278_ in an official-mapped dev runtime).
 * Having both produces:
 *   java.lang.module.ResolutionException: Modules Registrate.MC1._18._2 and Registrate export package
 *   com.tterrag.registrate.providers.loot to module ftbultimine
 * So the nested copy is dropped here and the remapped module dependency is used instead.
 *
 * Usage: java StripJarJarEntry <in.jar> <out.jar> <nestedJarPath>
 */
public class StripJarJarEntry {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        String drop = args[2];

        int dropped = 0, metaPatched = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(drop)) {
                    dropped++;
                    continue;
                }
                byte[] data = zis.readAllBytes();
                if (e.getName().equals("META-INF/jarjar/metadata.json")) {
                    String text = new String(data, StandardCharsets.UTF_8);
                    String patched = removeJarStanza(text, drop);
                    if (!patched.equals(text)) {
                        data = patched.getBytes(StandardCharsets.UTF_8);
                        metaPatched++;
                    }
                }
                ZipEntry copy = new ZipEntry(e.getName());
                copy.setTime(e.getTime());
                zos.putNextEntry(copy);
                zos.write(data);
                zos.closeEntry();
            }
        }
        System.out.println("dropped entries: " + dropped + ", metadata stanzas removed: " + metaPatched);
        if (dropped != 1 || metaPatched != 1) {
            System.out.println("ABORT: unexpected patch counts");
            Files.deleteIfExists(out);
            System.exit(1);
        }
    }

    /** Removes the JSON object in the "jars" array whose "path" equals nestedPath. */
    private static String removeJarStanza(String json, String nestedPath) {
        int pathIdx = json.indexOf("\"path\": \"" + nestedPath + "\"");
        if (pathIdx < 0) return json;
        // Walk backwards from the "path" key to find the '{' that opens this jar object: the first
        // brace encountered going backwards at nesting depth 0 (inner objects such as "version" are
        // balanced and skipped).
        int objStart = -1;
        int depth = 0;
        for (int i = pathIdx; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) { objStart = i; break; }
                depth--;
            }
        }
        if (objStart < 0) return json;
        // Forward walk to the matching close brace.
        int d = 0, objEnd = -1;
        for (int i = objStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') d++;
            else if (c == '}') { d--; if (d == 0) { objEnd = i; break; } }
        }
        if (objEnd < 0) return json;
        int from = objStart, to = objEnd + 1;
        // Swallow a trailing comma, or a preceding comma if this was the last element.
        int after = to;
        while (after < json.length() && Character.isWhitespace(json.charAt(after))) after++;
        if (after < json.length() && json.charAt(after) == ',') {
            to = after + 1;
        } else {
            int before = from - 1;
            while (before >= 0 && Character.isWhitespace(json.charAt(before))) before--;
            if (before >= 0 && json.charAt(before) == ',') from = before;
        }
        return json.substring(0, from) + json.substring(to);
    }
}

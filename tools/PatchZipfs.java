import java.nio.file.*;

public class PatchZipfs {
    // Goal: force ZipFileSystem.readOnly = false regardless of Files.isWritable().
    // The JDK computes `readOnly = !local5` where local5 = isWritable(...), i.e.
    //   ifne L1; iconst_1; goto L2; L1: iconst_0; L2: putfield readOnly
    // Replacing `iload 5` (15 05) with `iconst_1` (04) + nop (00) makes local5 a constant 1,
    // so readOnly = !1 = false, with IDENTICAL stack shapes (int in/out everywhere), so the
    // StackMapTable stays valid. Branch offsets and all other bytes are untouched.
    static final byte[] CONTEXT = { 0x2A, 0x15, 0x05, (byte) 0x9A }; // aload_0, iload 5, ifne

    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]);
        Path out = Paths.get(a[1]);
        byte[] data = Files.readAllBytes(in);
        int count = 0, first = -1;
        for (int i = 0; i + CONTEXT.length <= data.length; i++) {
            boolean m = true;
            for (int j = 0; j < CONTEXT.length; j++) if (data[i + j] != CONTEXT[j]) { m = false; break; }
            if (m) { count++; if (first < 0) first = i; }
        }
        System.out.println("context occurrences: " + count + " at offset " + first);
        if (count != 1) { System.out.println("ABORT: expected exactly 1 occurrence"); System.exit(1); }
        int iload = first + 1; // bytes: [0]=2A [1]=15 [2]=05 [3]=9A
        if (data[iload] != 0x15 || data[iload + 1] != 0x05) {
            System.out.println("ABORT: unexpected bytes at iload position");
            System.exit(1);
        }
        data[iload] = 0x04;     // iconst_1
        data[iload + 1] = 0x00; // nop
        Files.createDirectories(out.getParent());
        Files.write(out, data);
        System.out.println("patched -> " + out);
    }
}

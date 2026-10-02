package com.crabmods.algocraft.logic.catalog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/** Keep code submissions below the serverbound payload limit, with bounded decompression. */
public final class SubmissionWire {
    public static final int MAX_COMPRESSED_BYTES = 24 * 1024;
    public static byte[] encode(String code) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) { zip.write(code.getBytes(StandardCharsets.UTF_8)); }
        if (bytes.size() > MAX_COMPRESSED_BYTES) throw new IOException("Compressed submission exceeds 24 KiB");
        return bytes.toByteArray();
    }
    public static String decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_COMPRESSED_BYTES) throw new IOException("Submission too large");
        try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] plain = zip.readNBytes(200001);
            if (plain.length > 200000) throw new IOException("Expanded submission too large");
            String code = new String(plain, StandardCharsets.UTF_8);
            if (code.length() > 50000) throw new IOException("Submission too long");
            return code;
        }
    }
}

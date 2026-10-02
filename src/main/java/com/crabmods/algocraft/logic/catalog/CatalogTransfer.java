package com.crabmods.algocraft.logic.catalog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/** Bounded, ordered assembly. A new manifest supersedes every partial old publication. */
public final class CatalogTransfer {
    private String revision;
    private String server;
    private int bytes, chunks, next;
    private ByteArrayOutputStream data;
    public void begin(String server, String revision, int bytes, int chunks) throws IOException {
        clear();
        try { UUID.fromString(server); } catch (RuntimeException e) { throw new IOException("Invalid server identity", e); }
        if (!revision.matches("[a-f0-9]{64}") || bytes <= 0 || bytes > PublishedCatalog.MAX_ARCHIVE_BYTES
                || chunks != (bytes + PublishedCatalog.CHUNK_BYTES - 1) / PublishedCatalog.CHUNK_BYTES)
            throw new IOException("Invalid catalog manifest");
        this.server = server; this.revision = revision; this.bytes = bytes; this.chunks = chunks;
        data = new ByteArrayOutputStream(bytes);
    }
    public byte[] accept(String revision, int index, byte[] chunk) throws IOException {
        if (data == null || !this.revision.equals(revision)) return null; // superseded publication
        int expected = Math.min(PublishedCatalog.CHUNK_BYTES, bytes - data.size());
        if (index != next || chunk.length != expected || next >= chunks) {
            clear(); throw new IOException("Invalid or out-of-order catalog chunk");
        }
        data.write(chunk);
        next++;
        if (next != chunks) return null;
        byte[] completed = data.toByteArray();
        if (completed.length != bytes || !PublishedCatalog.hash(completed).equals(revision)) {
            clear(); throw new IOException("Catalog checksum mismatch");
        }
        data = null;
        return completed;
    }
    public String server() { return server; }
    public String revision() { return revision; }
    public void clear() { data = null; next = 0; server = null; revision = null; }
}

package com.crabmods.algocraft.gametest;
import com.crabmods.algocraft.network.*;

import com.crabmods.algocraft.network.compat.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Map;
import java.util.HashMap;


public final class ForgeWireCompatibilityChecks {
    public static void verify() {
        unicodeSubmissionAndEveryPacketFieldSurviveTheForgeTransport();
        oversizedAndMalformedPeerDataIsRejectedBeforeAllocation();
    }
    private static void assertEquals(Object expected, Object actual) { assertEquals(expected, actual, "Wire value differs"); }
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) throw new IllegalStateException(message);
    }
    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        if (!java.util.Arrays.equals(expected, actual)) throw new IllegalStateException("Packet bytes differ");
    }
    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new IllegalStateException("Unexpected wire rejection", error);
        }
        throw new IllegalStateException("Invalid peer input was accepted");
    }
    private static <T> T roundTrip(StreamCodec<ByteBuf,T> codec, T value) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, value);
            T decoded = codec.decode(buffer);
            assertEquals(0, buffer.readableBytes(), "a packet must consume its complete wire value");
            return decoded;
        } finally { buffer.release(); }
    }

    private static void unicodeSubmissionAndEveryPacketFieldSurviveTheForgeTransport() {
        var submission = new PacketSubmitSolution("official:7", "class Solution { // 中文 😀\n}",
                Long.MAX_VALUE, "official", "abc123", "12345678-1234-1234-1234-123456789abc");
        assertEquals(submission, roundTrip(PacketSubmitSolution.STREAM_CODEC, submission));
        var feedback = new PacketSubmissionResult(submission.requestId(), false, "compile error 中文", 4, 9, 30000);
        assertEquals(feedback, roundTrip(PacketSubmissionResult.STREAM_CODEC, feedback));
        var manifest = new PacketCatalogManifest(submission.requestId(), "revision", 100000, 4);
        assertEquals(manifest, roundTrip(PacketCatalogManifest.STREAM_CODEC, manifest));
        assertEquals(new PacketRequestCatalog("revision"), roundTrip(PacketRequestCatalog.STREAM_CODEC, new PacketRequestCatalog("revision")));
        assertEquals(new PacketSetSolvingState(true, "official:7"), roundTrip(PacketSetSolvingState.STREAM_CODEC, new PacketSetSolvingState(true, "official:7")));
        assertEquals(new PacketSyncProgress(Map.of("7", Long.MAX_VALUE)),
                roundTrip(PacketSyncProgress.STREAM_CODEC, new PacketSyncProgress(Map.of("7", Long.MAX_VALUE))));
        var chunk = new PacketCatalogChunk("revision", 3, new byte[]{0, -1, 2});
        var decoded = roundTrip(PacketCatalogChunk.STREAM_CODEC, chunk);
        assertEquals(chunk.revision(), decoded.revision());
        assertEquals(chunk.index(), decoded.index());
        assertArrayEquals(chunk.data(), decoded.data());
    }

    private static void oversizedAndMalformedPeerDataIsRejectedBeforeAllocation() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(RuntimeException.class, () -> ByteBufCodecs.stringUtf8(36).encode(buffer, "x".repeat(37)));
            buffer.clear();
            ByteBufCodecs.VAR_INT.encode(buffer, Integer.MAX_VALUE);
            assertThrows(RuntimeException.class, () -> ByteBufCodecs.byteArray(1024).decode(buffer));
            buffer.clear();
            ByteBufCodecs.VAR_INT.encode(buffer, 10001);
            var map = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG);
            assertThrows(RuntimeException.class, () -> map.decode(buffer));
            buffer.clear();
            ByteBufCodecs.VAR_INT.encode(buffer, -1);
            assertThrows(RuntimeException.class, () -> map.decode(buffer));
            buffer.clear();
            assertThrows(RuntimeException.class, () -> ByteBufCodecs.byteArray(2).encode(buffer, new byte[3]));
        } finally { buffer.release(); }
    }
}

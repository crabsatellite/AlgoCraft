package com.crabmods.algocraft.network.compat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import java.util.Map;
import java.util.function.IntFunction;
/** Wire bounds are enforced before allocating or decoding packet data. */
public final class ByteBufCodecs {
    private ByteBufCodecs() {}
    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (b, v) -> new FriendlyByteBuf(b).writeVarInt(v), b -> new FriendlyByteBuf(b).readVarInt());
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (b, v) -> new FriendlyByteBuf(b).writeVarLong(v), b -> new FriendlyByteBuf(b).readVarLong());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    public static StreamCodec<ByteBuf, String> stringUtf8(int maxChars) {
        return StreamCodec.of((b, v) -> new FriendlyByteBuf(b).writeUtf(v, maxChars), b -> new FriendlyByteBuf(b).readUtf(maxChars));
    }
    public static StreamCodec<ByteBuf, byte[]> byteArray(int maximum) {
        return StreamCodec.of((b, value) -> {
            if (value.length > maximum) throw new IllegalArgumentException("Packet array too large");
            new FriendlyByteBuf(b).writeByteArray(value);
        }, b -> new FriendlyByteBuf(b).readByteArray(maximum));
    }
    public static <K, V, M extends Map<K, V>> StreamCodec<ByteBuf, M> map(
            IntFunction<M> factory, StreamCodec<ByteBuf, K> keys, StreamCodec<ByteBuf, V> values) {
        return StreamCodec.of((b, value) -> {
            if (value.size() > 10000) throw new IllegalArgumentException("Progress map too large");
            VAR_INT.encode(b, value.size());
            value.forEach((k, v) -> { keys.encode(b, k); values.encode(b, v); });
        }, b -> {
            int size = VAR_INT.decode(b);
            if (size < 0 || size > 10000) throw new IllegalArgumentException("Invalid progress map size");
            M result = factory.apply(size);
            for (int i = 0; i < size; i++) result.put(keys.decode(b), values.decode(b));
            return result;
        });
    }
}

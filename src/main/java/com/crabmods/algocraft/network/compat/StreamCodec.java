package com.crabmods.algocraft.network.compat;
import java.util.function.*;
/** Small typed codec adapter; the packet definitions remain identical across loaders. */
public interface StreamCodec<B, T> {
    void encode(B buffer, T value);
    T decode(B buffer);
    static <B,T> StreamCodec<B,T> of(BiConsumer<B,T> encoder, Function<B,T> decoder) {
        return new StreamCodec<>() {
            public void encode(B buffer,T value) { encoder.accept(buffer,value); }
            public T decode(B buffer) { return decoder.apply(buffer); }
        };
    }
    default <R> StreamCodec<B,R> map(Function<T,R> decoder, Function<R,T> encoder) {
        return of((b,r) -> encode(b,encoder.apply(r)), b -> decoder.apply(decode(b)));
    }
    interface Fn1<T1,R> { R apply(T1 v1); }
    static <B,R,T1> StreamCodec<B,R> composite(StreamCodec<B,T1> c1,Function<R,T1> g1,Fn1<T1,R> constructor) {
        return of((b,r) -> { c1.encode(b,g1.apply(r)); },
                b -> constructor.apply(c1.decode(b)));
    }
    interface Fn2<T1,T2,R> { R apply(T1 v1,T2 v2); }
    static <B,R,T1,T2> StreamCodec<B,R> composite(StreamCodec<B,T1> c1,Function<R,T1> g1,StreamCodec<B,T2> c2,Function<R,T2> g2,Fn2<T1,T2,R> constructor) {
        return of((b,r) -> { c1.encode(b,g1.apply(r)); c2.encode(b,g2.apply(r)); },
                b -> constructor.apply(c1.decode(b),c2.decode(b)));
    }
    interface Fn3<T1,T2,T3,R> { R apply(T1 v1,T2 v2,T3 v3); }
    static <B,R,T1,T2,T3> StreamCodec<B,R> composite(StreamCodec<B,T1> c1,Function<R,T1> g1,StreamCodec<B,T2> c2,Function<R,T2> g2,StreamCodec<B,T3> c3,Function<R,T3> g3,Fn3<T1,T2,T3,R> constructor) {
        return of((b,r) -> { c1.encode(b,g1.apply(r)); c2.encode(b,g2.apply(r)); c3.encode(b,g3.apply(r)); },
                b -> constructor.apply(c1.decode(b),c2.decode(b),c3.decode(b)));
    }
    interface Fn4<T1,T2,T3,T4,R> { R apply(T1 v1,T2 v2,T3 v3,T4 v4); }
    static <B,R,T1,T2,T3,T4> StreamCodec<B,R> composite(StreamCodec<B,T1> c1,Function<R,T1> g1,StreamCodec<B,T2> c2,Function<R,T2> g2,StreamCodec<B,T3> c3,Function<R,T3> g3,StreamCodec<B,T4> c4,Function<R,T4> g4,Fn4<T1,T2,T3,T4,R> constructor) {
        return of((b,r) -> { c1.encode(b,g1.apply(r)); c2.encode(b,g2.apply(r)); c3.encode(b,g3.apply(r)); c4.encode(b,g4.apply(r)); },
                b -> constructor.apply(c1.decode(b),c2.decode(b),c3.decode(b),c4.decode(b)));
    }
    interface Fn6<T1,T2,T3,T4,T5,T6,R> { R apply(T1 v1,T2 v2,T3 v3,T4 v4,T5 v5,T6 v6); }
    static <B,R,T1,T2,T3,T4,T5,T6> StreamCodec<B,R> composite(StreamCodec<B,T1> c1,Function<R,T1> g1,StreamCodec<B,T2> c2,Function<R,T2> g2,StreamCodec<B,T3> c3,Function<R,T3> g3,StreamCodec<B,T4> c4,Function<R,T4> g4,StreamCodec<B,T5> c5,Function<R,T5> g5,StreamCodec<B,T6> c6,Function<R,T6> g6,Fn6<T1,T2,T3,T4,T5,T6,R> constructor) {
        return of((b,r) -> { c1.encode(b,g1.apply(r)); c2.encode(b,g2.apply(r)); c3.encode(b,g3.apply(r)); c4.encode(b,g4.apply(r)); c5.encode(b,g5.apply(r)); c6.encode(b,g6.apply(r)); },
                b -> constructor.apply(c1.decode(b),c2.decode(b),c3.decode(b),c4.decode(b),c5.decode(b),c6.decode(b)));
    }
}

import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, ease, lin, pop, rnd, clamp, beat} from './theme';

// Shows a 1920x1080 capture inside a viewport, zoomed so that capture point (x, y) is centred.
export const CapView: React.FC<{src: string; width: number; x?: number; y?: number; s?: number; height?: number}> = ({src, width, x = 960, y = 540, s = 1, height}) => {
  const vh = height ?? width * 9 / 16;
  const b = width / 1920;
  let tx = width / 2 - x * b * s, ty = vh / 2 - y * b * s;
  tx = Math.min(0, Math.max(width - 1920 * b * s, tx));
  ty = Math.min(0, Math.max(vh - 1080 * b * s, ty));
  return <div style={{width, height: vh, overflow: 'hidden', position: 'relative', background: '#0d1117'}}>
    <Img src={staticFile(src)} style={{position: 'absolute', left: 0, top: 0, width: 1920, height: 1080, transformOrigin: '0 0', transform: `translate(${tx}px, ${ty}px) scale(${b * s})`}} />
  </div>;
};

export const mix = (f: number, a: number, b: number, from: number, to: number) => interpolate(f, [a, b], [from, to], {...clamp, easing: (t: number) => 1 - Math.pow(1 - t, 3)});

// Typewriter-style glowing headline used across scenes.
export const Headline: React.FC<{at: number; out?: number; children: React.ReactNode; size?: number; x?: number; y?: number; align?: 'left' | 'center'; width?: number}> =
  ({at, out = 1e9, children, size = 86, x = 120, y = 120, align = 'left', width = 1300}) => {
  const f = useCurrentFrame();
  const p = ease(f, at, at + 18);
  const q = ease(f, out, out + 12);
  if (f < at || f > out + 12) return null;
  return <div style={{position: 'absolute', left: align === 'center' ? (W - width) / 2 : x, top: y, width, textAlign: align,
    fontFamily: F.display, fontWeight: 700, fontSize: size, lineHeight: 1.02, letterSpacing: -size * 0.03, color: C.ink,
    opacity: p * (1 - q), filter: `blur(${(1 - p) * 10 + q * 8}px)`, transform: `translateY(${(1 - p) * 30 - q * 20}px)`, textShadow: '0 6px 40px rgba(0,0,0,0.6)'}}>{children}</div>;
};
export const Em: React.FC<{children: React.ReactNode; c?: string}> = ({children, c = C.emerald}) => <span style={{color: c, textShadow: `0 0 30px ${c}66`}}>{children}</span>;

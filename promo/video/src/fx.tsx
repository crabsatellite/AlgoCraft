import React from 'react';
import {AbsoluteFill, useCurrentFrame, Img, staticFile, interpolate} from 'remotion';
import {C, F, W, H, ease, lin, pop, rnd, clamp} from './theme';

// Deep navy stage with a slow perspective voxel floor and soft aurora glows.
export const Stage: React.FC<{hue?: string; floor?: boolean; intensity?: number}> = ({hue = C.emerald, floor = true, intensity = 1}) => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{background: `radial-gradient(120% 90% at 50% 20%, ${C.bg2} 0%, ${C.bg} 62%)`, overflow: 'hidden'}}>
      <div style={{position: 'absolute', left: -300, top: -300, width: 1300, height: 1000, borderRadius: '50%',
        background: `radial-gradient(closest-side, ${hue}33, transparent)`, filter: 'blur(40px)', opacity: 0.55 * intensity,
        transform: `translate(${Math.sin(f / 90) * 80}px, ${Math.cos(f / 110) * 50}px)`}} />
      <div style={{position: 'absolute', right: -360, bottom: -260, width: 1400, height: 1000, borderRadius: '50%',
        background: `radial-gradient(closest-side, #3d6bff2a, transparent)`, filter: 'blur(50px)', opacity: 0.6 * intensity,
        transform: `translate(${Math.cos(f / 100) * 70}px, ${Math.sin(f / 80) * 40}px)`}} />
      {floor && (
        <div style={{position: 'absolute', left: -W, right: -W, top: H * 0.56, height: H * 1.2, perspective: 700, perspectiveOrigin: '50% 0%'}}>
          <div style={{position: 'absolute', inset: 0, transformOrigin: '50% 0%', transform: 'rotateX(72deg)',
            backgroundImage: `linear-gradient(${C.line} 1.5px, transparent 1.5px), linear-gradient(90deg, ${C.line} 1.5px, transparent 1.5px)`,
            backgroundSize: '80px 80px', backgroundPosition: `0px ${(f * 2.2) % 80}px`,
            maskImage: 'linear-gradient(to bottom, transparent 0%, black 30%, black 60%, transparent 100%)',
            WebkitMaskImage: 'linear-gradient(to bottom, transparent 0%, black 25%, black 55%, transparent 100%)'}} />
        </div>
      )}
    </AbsoluteFill>
  );
};

export const Grain: React.FC<{opacity?: number}> = ({opacity = 0.07}) => {
  const f = useCurrentFrame();
  return <AbsoluteFill style={{pointerEvents: 'none', backgroundImage: `url(${staticFile('fx/noise.png')})`, backgroundSize: '256px 256px',
    backgroundPosition: `${Math.floor(rnd(f, 3) * 256)}px ${Math.floor(rnd(f, 9) * 256)}px`, opacity, mixBlendMode: 'overlay'}} />;
};
export const Vignette: React.FC<{strength?: number}> = ({strength = 0.75}) => (
  <AbsoluteFill style={{pointerEvents: 'none', background: `radial-gradient(ellipse at center, transparent 55%, rgba(0,0,0,${strength}) 100%)`}} />
);

export const Flash: React.FC<{at: number; dur?: number; color?: string; peak?: number}> = ({at, dur = 10, color = '#ffffff', peak = 0.85}) => {
  const f = useCurrentFrame();
  const o = interpolate(f, [at - 1, at, at + dur], [0, peak, 0], clamp);
  return o > 0 ? <AbsoluteFill style={{background: color, opacity: o, mixBlendMode: 'screen', pointerEvents: 'none'}} /> : null;
};

export const Shockwave: React.FC<{at: number; x?: number; y?: number; color?: string; size?: number; dur?: number; width?: number}> =
  ({at, x = W / 2, y = H / 2, color = C.emerald, size = 1400, dur = 26, width = 10}) => {
  const f = useCurrentFrame();
  if (f < at || f > at + dur) return null;
  const p = ease(f, at, at + dur);
  const r = size * p;
  return <div style={{position: 'absolute', left: x - r / 2, top: y - r / 2, width: r, height: r, borderRadius: '50%',
    border: `${width * (1 - p) + 1}px solid ${color}`, opacity: 1 - p, boxShadow: `0 0 ${40 * (1 - p)}px ${color}`}} />;
};

// Square voxel particles bursting from a point.
export const Burst: React.FC<{at: number; x?: number; y?: number; n?: number; colors?: string[]; speed?: number; dur?: number; seed?: number; size?: number}> =
  ({at, x = W / 2, y = H / 2, n = 70, colors = [C.emerald, '#b6ffe0', C.gold], speed = 26, dur = 55, seed = 1, size = 14}) => {
  const f = useCurrentFrame();
  const t = f - at;
  if (t < 0 || t > dur) return null;
  return <>{Array.from({length: n}).map((_, i) => {
    const a = rnd(i, seed) * Math.PI * 2;
    const v = speed * (0.35 + rnd(i, seed + 1));
    const k = 1 - Math.exp(-t / 9);
    const px = x + Math.cos(a) * v * 9 * k;
    const py = y + Math.sin(a) * v * 9 * k + t * t * 0.05;
    const s = size * (0.4 + rnd(i, seed + 2)) * (1 - t / dur);
    return <div key={i} style={{position: 'absolute', left: px, top: py, width: s, height: s, background: colors[i % colors.length],
      boxShadow: `0 0 ${s}px ${colors[i % colors.length]}`, transform: `rotate(${t * 6 * (rnd(i, 4) - 0.5)}deg)`, opacity: 1 - t / dur}} />;
  })}</>;
};

// Ambient drifting voxel dust.
export const Dust: React.FC<{n?: number; color?: string; seed?: number}> = ({n = 40, color = C.emerald, seed = 5}) => {
  const f = useCurrentFrame();
  return <AbsoluteFill style={{pointerEvents: 'none'}}>{Array.from({length: n}).map((_, i) => {
    const x = rnd(i, seed) * W;
    const y = (rnd(i, seed + 1) * H - f * (0.3 + rnd(i, seed + 2) * 0.9)) % H;
    const s = 3 + rnd(i, seed + 3) * 6;
    return <div key={i} style={{position: 'absolute', left: x + Math.sin(f / 40 + i) * 20, top: (y + H) % H, width: s, height: s, background: color,
      opacity: 0.15 + 0.35 * rnd(i, seed + 4), boxShadow: `0 0 ${s * 2}px ${color}`}} />;
  })}</AbsoluteFill>;
};

// Text that resolves from blur, letter by letter.
export const Reveal: React.FC<{text: string; at: number; style?: React.CSSProperties; stagger?: number; color?: string}> = ({text, at, style, stagger = 1.2, color}) => {
  const f = useCurrentFrame();
  return <span style={{display: 'inline-block', whiteSpace: 'pre', ...style}}>{Array.from(text).map((ch, i) => {
    const p = ease(f, at + i * stagger, at + i * stagger + 14);
    return <span key={i} style={{display: 'inline-block', opacity: p, filter: `blur(${(1 - p) * 12}px)`, transform: `translateY(${(1 - p) * 24}px)`, color}}>{ch}</span>;
  })}</span>;
};

// RGB split title used on impact hits.
export const Glitch: React.FC<{children: React.ReactNode; at: number; style?: React.CSSProperties; amount?: number}> = ({children, at, style, amount = 18}) => {
  const f = useCurrentFrame();
  const g = Math.max(0, 1 - (f - at) / 14) * (f >= at ? 1 : 0);
  const j = g * amount;
  const base: React.CSSProperties = {position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center'};
  return <div style={{position: 'relative', ...style}}>
    {g > 0 && <div style={{...base, color: '#ff2d55', transform: `translate(${-j}px, ${rnd(f, 2) * j * 0.3}px)`, mixBlendMode: 'screen', opacity: 0.8}}>{children}</div>}
    {g > 0 && <div style={{...base, color: '#2de2ff', transform: `translate(${j}px, ${-rnd(f, 7) * j * 0.3}px)`, mixBlendMode: 'screen', opacity: 0.8}}>{children}</div>}
    <div style={{position: 'relative'}}>{children}</div>
  </div>;
};

export const Tag: React.FC<{children: React.ReactNode; color?: string; style?: React.CSSProperties}> = ({children, color = C.emerald, style}) => (
  <div style={{display: 'inline-flex', alignItems: 'center', gap: 12, fontFamily: F.pixel, fontSize: 15, letterSpacing: 2, color,
    padding: '10px 14px', border: `2px solid ${color}`, background: `${color}14`, boxShadow: `0 0 24px ${color}33`, ...style}}>
    <span style={{width: 8, height: 8, background: color, boxShadow: `0 0 10px ${color}`}} />{children}
  </div>
);

// Lower-third caption with a sliding accent bar.
export const Caption: React.FC<{at: number; out: number; title: string; sub?: string; color?: string; x?: number; y?: number; align?: 'left' | 'right'; panel?: boolean}> =
  ({at, out, title, sub, color = C.emerald, x = 120, y = 860, align = 'left', panel = false}) => {
  const f = useCurrentFrame();
  if (f < at - 2 || f > out + 12) return null;
  const pin = ease(f, at, at + 16);
  const pout = ease(f, out, out + 12);
  const o = pin * (1 - pout);
  return <div style={{position: 'absolute', [align === 'left' ? 'left' : 'right']: x, top: y, opacity: o,
    transform: `translateX(${(1 - pin) * (align === 'left' ? -40 : 40)}px)`, textAlign: align, maxWidth: 1100,
    ...(panel ? {background: 'rgba(5,8,14,0.94)', padding: '22px 30px 24px 22px', borderRadius: 14, border: '1px solid rgba(255,255,255,0.08)', boxShadow: '0 30px 80px rgba(0,0,0,0.6)'} : {})}}>
    <div style={{display: 'flex', alignItems: 'center', gap: 18, flexDirection: align === 'left' ? 'row' : 'row-reverse'}}>
      <div style={{width: 10, height: 64 * pin, background: color, boxShadow: `0 0 18px ${color}`}} />
      <div>
        <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 54, color: C.ink, letterSpacing: -1, lineHeight: 1.05, textShadow: '0 4px 30px rgba(0,0,0,0.7)'}}>{title}</div>
        {sub && <div style={{fontFamily: F.body, fontSize: 27, color: '#c9d3e3', marginTop: 10, lineHeight: 1.35, textShadow: '0 2px 16px rgba(0,0,0,0.8)'}}>{sub}</div>}
      </div>
    </div>
  </div>;
};

// "</>" brand mark, echoing the Web IDE logo.
export const Mark: React.FC<{size?: number; glow?: number}> = ({size = 120, glow = 1}) => (
  <div style={{width: size, height: size, borderRadius: size * 0.24, position: 'relative',
    background: `linear-gradient(145deg, #5ff5bb 0%, ${C.emeraldDeep} 100%)`,
    boxShadow: `0 0 ${60 * glow}px ${C.emerald}88, inset 0 -${size * 0.06}px 0 #0a7a52, inset 0 ${size * 0.04}px 0 #b2ffe0`}}>
    <svg viewBox="0 0 100 100" style={{position: 'absolute', inset: 0}}>
      <g fill="none" stroke="#04281b" strokeWidth={9} strokeLinecap="square" strokeLinejoin="miter">
        <polyline points="32,32 16,50 32,68" />
        <polyline points="68,32 84,50 68,68" />
        <line x1="56" y1="26" x2="44" y2="74" />
      </g>
    </svg>
  </div>
);
export const Wordmark: React.FC<{size?: number; color?: string}> = ({size = 150, color = C.ink}) => (
  <span style={{fontFamily: F.display, fontWeight: 700, fontSize: size, letterSpacing: -size * 0.045, color, lineHeight: 1}}>
    Algo<span style={{color: C.emerald, textShadow: `0 0 ${size * 0.3}px ${C.emerald}88`}}>Craft</span>
  </span>
);

// Dark browser window used to frame real Web IDE captures.
export const Browser: React.FC<{children: React.ReactNode; width: number; url?: string; style?: React.CSSProperties}> = ({children, width, url = '127.0.0.1 · AlgoCraft Web IDE', style}) => {
  const bar = Math.round(width * 0.028);
  return <div style={{width, borderRadius: 18, overflow: 'hidden', background: '#0d1117', border: '1px solid rgba(255,255,255,0.12)',
    boxShadow: '0 60px 140px rgba(0,0,0,0.65), 0 0 0 1px rgba(52,227,160,0.08), 0 0 90px rgba(52,227,160,0.12)', ...style}}>
    <div style={{height: bar, background: '#161b24', display: 'flex', alignItems: 'center', gap: bar * 0.3, padding: `0 ${bar * 0.5}px`, borderBottom: '1px solid rgba(255,255,255,0.06)'}}>
      {['#ff5f57', '#febc2e', '#28c840'].map(c => <div key={c} style={{width: bar * 0.32, height: bar * 0.32, borderRadius: '50%', background: c}} />)}
      <div style={{marginLeft: bar * 0.6, flex: 1, maxWidth: width * 0.42, height: bar * 0.58, borderRadius: bar, background: '#0b0f15',
        color: '#8b97ab', fontFamily: F.body, fontSize: bar * 0.34, display: 'flex', alignItems: 'center', paddingLeft: bar * 0.5}}>{url}</div>
    </div>
    <div style={{position: 'relative'}}>{children}</div>
  </div>;
};

// Framed in-game capture with an honest label.
export const GameShot: React.FC<{src: string; width: number; label?: string; style?: React.CSSProperties; pixel?: boolean}> = ({src, width, label = 'IN-GAME', style, pixel = true}) => (
  <div style={{width, position: 'relative', borderRadius: 14, overflow: 'hidden', border: '3px solid #1d2636',
    boxShadow: '0 50px 120px rgba(0,0,0,0.7), 0 0 70px rgba(52,227,160,0.12)', ...style}}>
    <Img src={staticFile(src)} style={{width: '100%', display: 'block', imageRendering: pixel ? 'pixelated' : 'auto'}} />
    {label && <div style={{position: 'absolute', left: 18, top: 18}}><Tag color={C.emerald} style={{fontSize: 12, background: 'rgba(5,7,13,0.75)'}}>{label}</Tag></div>}
  </div>
);

export const ItemIcon: React.FC<{name: string; size: number; style?: React.CSSProperties}> = ({name, size, style}) => (
  <Img src={staticFile(`items/${name}.png`)} style={{width: size, height: size, imageRendering: 'pixelated', filter: 'drop-shadow(0 6px 10px rgba(0,0,0,0.6))', ...style}} />
);

// Pixel dissolve transition: grid of squares that cover then reveal.
export const PixelWipe: React.FC<{at: number; dur?: number; color?: string; cols?: number}> = ({at, dur = 16, color = C.bg, cols = 24}) => {
  const f = useCurrentFrame();
  const t = f - at;
  if (t < -dur || t > dur) return null;
  const rows = Math.ceil(cols * H / W);
  const s = W / cols;
  return <AbsoluteFill style={{pointerEvents: 'none'}}>{Array.from({length: cols * rows}).map((_, i) => {
    const x = i % cols, y = Math.floor(i / cols);
    const d = (x / cols) * 0.6 + rnd(i, 11) * 0.4;
    const cover = lin(t, -dur + d * dur * 0.9, -dur + d * dur * 0.9 + dur * 0.35);
    const reveal = lin(t, d * dur * 0.9, d * dur * 0.9 + dur * 0.35);
    const k = cover * (1 - reveal);
    return k > 0.01 ? <div key={i} style={{position: 'absolute', left: x * s, top: y * s, width: s + 1, height: s + 1, background: color,
      transform: `scale(${k})`, boxShadow: k > 0.5 ? `0 0 0 1px ${C.emerald}22` : undefined}} /> : null;
  })}</AbsoluteFill>;
};

export const useShake = (at: number, amount = 14, dur = 14) => {
  const f = useCurrentFrame();
  const t = f - at;
  if (t < 0 || t > dur) return 'translate(0px,0px)';
  const k = (1 - t / dur) * amount;
  return `translate(${(rnd(f, 21) - 0.5) * 2 * k}px, ${(rnd(f, 22) - 0.5) * 2 * k}px)`;
};

export {pop, ease, lin};




import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, DIFF, ease, lin, pop, rnd, clamp, beat} from '../theme';
import {Stage, Dust, Reveal, Tag, Caption, Glitch, Shockwave, Burst, Flash, Browser, useShake, Vignette, PixelWipe} from '../fx';
import {CapView, Headline, Em} from '../common';
import bank from '../../public/data/bank.json';

const titles = (bank as any).titles as {id: string; t: string; d: string}[];
const counts = (bank as any).counts as Record<'EASY' | 'MEDIUM' | 'HARD', number>;
const browse = ((bank as any).browse as {id: string; title: string}[]);

const Wall: React.FC<{f: number; speed?: number; opacity?: number}> = ({f, speed = 6, opacity = 1}) => {
  const cols = 5;
  const rowH = 58;
  const rows = Math.ceil(titles.length / cols);
  const scroll = (f * speed) % (rows * rowH);
  return <div style={{position: 'absolute', left: -500, right: -500, top: -300, bottom: -300, perspective: 1200, opacity}}>
    <div style={{position: 'absolute', inset: 0, transform: 'rotateX(52deg) rotateZ(-14deg)', transformOrigin: '50% 50%'}}>
      {[0, 1].map(rep => <div key={rep} style={{position: 'absolute', left: 0, right: 0, top: rep * rows * rowH - scroll, display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)`, columnGap: 28}}>
        {titles.map((p, i) => <div key={i} style={{height: rowH, display: 'flex', alignItems: 'center', gap: 14, fontFamily: F.body, fontSize: 26, color: '#c8d2e2', whiteSpace: 'nowrap', overflow: 'hidden'}}>
          <span style={{fontFamily: F.mono, fontSize: 18, color: C.dim, width: 52, textAlign: 'right'}}>{p.id}</span>
          <span style={{width: 10, height: 10, background: DIFF[p.d.toUpperCase() as 'EASY'], boxShadow: `0 0 10px ${DIFF[p.d.toUpperCase() as 'EASY']}`}} />
          <span style={{opacity: 0.5 + 0.5 * rnd(i, 3)}}>{p.t}</span>
        </div>)}
      </div>)}
    </div>
  </div>;
};

export const Bank: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.bank;
  const n = Math.round(interpolate(f, [t0 + 2, t0 + 44], [0, 500], {...clamp, easing: (t: number) => 1 - Math.pow(1 - t, 4)}));
  const shake = useShake(t0, 16, 14);
  const big = pop(f, t0, 30, 10, 0.7);
  const toSplit = ease(f, T.split - 8, T.split + 10);
  return <AbsoluteFill style={{transform: shake}}>
    <Stage hue={C.emerald} floor={false} intensity={1.2} />
    <Wall f={f - t0} speed={7} opacity={0.55} />
    <AbsoluteFill style={{background: 'radial-gradient(ellipse at 50% 50%, rgba(5,7,13,0.92) 0%, rgba(5,7,13,0.6) 38%, rgba(5,7,13,0.1) 70%)'}} />
    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', transform: `translateY(${-toSplit * 190}px) scale(${1 - toSplit * 0.42})`}}>
      <div style={{fontFamily: F.pixel, fontSize: 22, color: C.emerald, letterSpacing: 6, opacity: ease(f, t0, t0 + 10)}}>OFFICIAL PROBLEM BANK</div>
      <Glitch at={t0} amount={24}><div style={{fontFamily: F.display, fontWeight: 700, fontSize: 380, lineHeight: 0.95, letterSpacing: -18, color: C.ink,
        transform: `scale(${0.7 + big * 0.3})`, textShadow: `0 0 90px ${C.emerald}aa`}}>{n}</div></Glitch>
      <div style={{fontFamily: F.display, fontWeight: 500, fontSize: 48, color: '#d6deeb', opacity: ease(f, t0 + 20, t0 + 34) * (1 - toSplit)}}>problems. Built in. Works offline.</div>
    </AbsoluteFill>
    <Shockwave at={t0} size={2600} width={14} />
    <Burst at={t0} n={60} speed={30} colors={[DIFF.EASY, DIFF.MEDIUM, DIFF.HARD]} />
    {f >= T.split - 4 && <Split f={f} />}
    <Flash at={t0} dur={12} />
    <Vignette />
  </AbsoluteFill>;
};

const Split: React.FC<{f: number}> = ({f}) => {
  const t0 = T.split;
  const max = 205;
  return <div style={{position: 'absolute', left: 360, right: 360, top: 560, display: 'flex', flexDirection: 'column', gap: 26}}>
    {(['EASY', 'MEDIUM', 'HARD'] as const).map((d, i) => {
      const at = t0 + i * 8;
      const p = ease(f, at, at + 26);
      const v = Math.round(counts[d] * p);
      return <div key={d} style={{display: 'flex', alignItems: 'center', gap: 28, opacity: ease(f, at - 4, at + 6)}}>
        <div style={{width: 170, fontFamily: F.pixel, fontSize: 22, color: DIFF[d]}}>{d}</div>
        <div style={{flex: 1, height: 44, background: '#121a28', borderRadius: 4, overflow: 'hidden'}}>
          <div style={{width: `${(counts[d] / max) * 100 * p}%`, height: '100%', background: `linear-gradient(90deg, ${DIFF[d]}88, ${DIFF[d]})`, boxShadow: `0 0 30px ${DIFF[d]}`}} />
        </div>
        <div style={{width: 120, fontFamily: F.display, fontWeight: 700, fontSize: 54, color: C.ink, textAlign: 'right'}}>{v}</div>
      </div>;
    })}
  </div>;
};

// Statement + diagram crops from real Web IDE captures.
export const Diagrams: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.diagrams;
  const step = beat(1) - beat(0);
  const items = browse.slice(0, 8);
  const cur = Math.max(0, Math.min(items.length - 1, Math.floor((f - t0) / step)));
  return <AbsoluteFill>
    <Stage hue={C.cyan} />
    <Dust n={24} color={C.cyan} />
    {items.map((it, i) => {
      if (i > cur || i < cur - 3) return null;
      const at = t0 + i * step;
      const p = pop(f, at, 30, 11, 0.6);
      const depth = cur - i;
      const rot = (rnd(i, 4) - 0.5) * 10;
      return <div key={it.id} style={{position: 'absolute', left: 820 + depth * -50 + (rnd(i, 2) - 0.5) * 60, top: 120 + depth * 16,
        transform: `rotate(${rot * (depth ? 1 : 1 - p * 0.6)}deg) scale(${(0.5 + 0.5 * p) * (1 - depth * 0.07)}) translateY(${(1 - p) * 200}px)`,
        opacity: depth > 2 ? 0.35 : 1, filter: `brightness(${1 - depth * 0.25})`, zIndex: 10 - depth}}>
        <div style={{width: 960, borderRadius: 16, overflow: 'hidden', border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 40px 100px rgba(0,0,0,0.7)'}}>
          <CapView src={`cap/browse/p${it.id}.png`} width={960} height={720} x={600} y={330} s={2.75} />
        </div>
      </div>;
    })}
    <Headline at={t0 + 2} x={110} y={300} size={80} width={640}>Clear statements. <Em c={C.cyan}>Real diagrams.</Em></Headline>
    <div style={{position: 'absolute', left: 114, top: 560, width: 560, fontFamily: F.body, fontSize: 27, lineHeight: 1.45, color: '#c3cddd', opacity: ease(f, t0 + 14, t0 + 28)}}>
      Grids, trees, graphs and linked lists drawn where words are not enough.
    </div>
    <div style={{position: 'absolute', left: 114, top: 720, fontFamily: F.mono, fontSize: 22, color: C.cyan}}>
      #{items[cur].id} · {items[cur].title}
    </div>
    <Vignette />
  </AbsoluteFill>;
};

export const Bilingual: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.bilingual;
  const wipe = interpolate(f, [t0 + 4, t0 + 34], [0, 100], {...clamp, easing: (t: number) => 1 - Math.pow(1 - t, 3)});
  const enter = ease(f, t0, t0 + 14);
  return <AbsoluteFill>
    <Stage />
    <div style={{position: 'absolute', left: 230, top: 40, opacity: enter, transform: `scale(${0.96 + enter * 0.04})`}}>
      <Browser width={1460}>
        <CapView src="cap/browse/p419.png" width={1460} x={600} y={300} s={1.25} />
        <div style={{position: 'absolute', inset: 0, clipPath: `polygon(0 0, ${wipe}% 0, ${wipe - 12}% 100%, 0 100%)`}}>
          <CapView src="cap/zh.png" width={1460} x={600} y={300} s={1.25} />
        </div>
      </Browser>
    </div>
    <div style={{position: 'absolute', left: 0, right: 0, bottom: 26, textAlign: 'center', fontFamily: F.display, fontWeight: 700, fontSize: 60, color: C.ink, opacity: ease(f, t0 + 8, t0 + 20)}}>
      English <span style={{color: C.emerald}}>⇄</span> 简体中文
    </div>
    <PixelWipe at={t0} dur={12} />
  </AbsoluteFill>;
};




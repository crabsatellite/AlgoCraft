import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, ease, lin, pop, rnd, clamp, beat} from '../theme';
import {Stage, Dust, Reveal, Tag, Caption, Shockwave, Burst, Flash, ItemIcon, Vignette, PixelWipe} from '../fx';
import {Headline, Em} from '../common';

const Flame: React.FC<{size: number; t: number}> = ({size, t}) => {
  // 8x8 pixel flame with a gentle flicker.
  const rows = ['...#....', '..##..#.', '..###.#.', '.#####..', '.######.', '######o#', '#oooo##', '.#oooo#.'];
  const c = size / 8;
  return <div style={{position: 'relative', width: size, height: size, filter: `drop-shadow(0 0 ${size / 4}px #ff7a1a)`}}>
    {rows.map((r, y) => Array.from(r).map((ch, x) => ch === '.' ? null : <div key={`${x}${y}`} style={{position: 'absolute', left: x * c,
      top: y * c + (y < 3 ? Math.sin(t / 4 + x) * c * 0.25 : 0), width: c + 0.5, height: c + 0.5, background: ch === 'o' ? '#ffe066' : (y < 3 ? '#ff5a1f' : '#ff8c1a')}} />))}
  </div>;
};

const WEEKLY = ['diamond', 'netherite_ingot'];
export const Daily: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.daily;
  const step = (beat(1) - beat(0)) / 2;
  const start = t0 + 34;
  const lit = Math.max(0, Math.min(14, Math.floor((f - start) / step) + 1));
  const out = ease(f, T.review - 12, T.review);
  return <AbsoluteFill>
    <Stage hue="#ff8c1a" intensity={0.8} />
    <Dust n={26} color="#ff9b3d" />
    <div style={{opacity: 1 - out, filter: `blur(${out * 10}px)`}}>
      <Headline at={t0 + 4} x={120} y={110} size={84} width={1500}>Practice a little <Em c={C.amber}>every day.</Em></Headline>
      <div style={{position: 'absolute', left: 124, top: 230, fontFamily: F.body, fontSize: 28, color: '#c3cddd', opacity: ease(f, t0 + 16, t0 + 30)}}>
        Solve on consecutive days to build a streak. Miss a day and it starts over.
      </div>
      <div style={{position: 'absolute', left: 120, top: 340, display: 'grid', gridTemplateColumns: 'repeat(7, 150px)', gap: 18}}>
        {Array.from({length: 14}).map((_, i) => {
          const on = i < lit;
          const at = start + i * step;
          const p = pop(f, at, 30, 9);
          const day = i + 1;
          const weekly = day % 7 === 0;
          const bonus = day >= 3;
          const c = weekly ? C.cyan : bonus ? C.amber : '#ffd9a8';
          return <div key={i} style={{height: 150, borderRadius: 14, position: 'relative', background: on ? `linear-gradient(160deg, ${c}33, #121a28)` : '#0e1520',
            border: `2px solid ${on ? c : '#1f2a3a'}`, boxShadow: on ? `0 0 ${30 * Math.max(0.4, 1 - (f - at) / 20)}px ${c}66` : 'none', transform: `scale(${on ? 0.9 + 0.1 * p : 0.92})`}}>
            <div style={{position: 'absolute', left: 14, top: 12, fontFamily: F.mono, fontSize: 20, color: on ? C.ink : C.dim}}>DAY {day}</div>
            {on && !weekly && <div style={{position: 'absolute', left: 50, top: 52}}><Flame size={56} t={f + i * 7} /></div>}
            {on && weekly && <div style={{position: 'absolute', left: 39, top: 46}}><ItemIcon name={WEEKLY[day / 7 - 1]} size={72} /></div>}
            {on && day === 3 && <div style={{position: 'absolute', left: -6, right: -6, bottom: -14, textAlign: 'center', fontFamily: F.pixel, fontSize: 11, color: '#1b1204', background: C.amber, padding: '6px 0'}}>STREAK BONUS</div>}
            {on && weekly && <div style={{position: 'absolute', left: -6, right: -6, bottom: -14, textAlign: 'center', fontFamily: F.pixel, fontSize: 11, color: '#04181b', background: C.cyan, padding: '6px 0'}}>WEEKLY REWARD</div>}
          </div>;
        })}
      </div>
      <div style={{position: 'absolute', right: 120, top: 360, width: 420, textAlign: 'center'}}>
        <div style={{display: 'flex', justifyContent: 'center'}}><Flame size={150} t={f} /></div>
        <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 170, color: C.ink, lineHeight: 1, textShadow: `0 0 60px ${C.amber}88`}}>{lit}</div>
        <div style={{fontFamily: F.pixel, fontSize: 18, color: C.amber, letterSpacing: 3, marginTop: 10}}>DAY STREAK</div>
      </div>
      <div style={{position: 'absolute', left: 124, top: 770, display: 'flex', gap: 46, fontFamily: F.body, fontSize: 25, color: '#c3cddd', opacity: ease(f, start + step * 3, start + step * 4)}}>
        <span><b style={{color: C.amber}}>Day 3+</b> XP + daily supplies</span>
        <span style={{opacity: ease(f, start + step * 7, start + step * 8)}}><b style={{color: C.cyan}}>Day 14</b> 2 netherite ingots · <b style={{color: C.cyan}}>Day 28+</b> 4 every week</span>
      </div>
    </div>
    <Vignette />
  </AbsoluteFill>;
};

const MILESTONES: [number, string, string][] = [[10, 'diamond', '×10'], [25, 'emerald_block', '×5'], [50, 'netherite_scrap', '×4'], [100, 'netherite_ingot', '×2'], [200, 'nether_star', '×1'], [500, 'dragon_egg_icon', '×1']];
export const Milestones: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.review;
  const step = (beat(2) - beat(0));
  const prog = interpolate(f, [t0 + 30, t0 + 30 + step * 5], [0, 1], clamp);
  const egg = t0 + 30 + step * 5;
  const out = ease(f, T.build + 40, T.trophies - 6);
  return <AbsoluteFill>
    <Stage hue={C.violet} intensity={0.8 + ease(f, egg, egg + 20) * 0.6} />
    <Dust n={30} color={C.violet} />
    <div style={{opacity: 1 - out}}>
      <Headline at={t0 + 4} x={120} y={130} size={84} width={1600}>Milestones you can <Em c={C.violet}>hold.</Em></Headline>
      <div style={{position: 'absolute', left: 124, top: 250, fontFamily: F.body, fontSize: 28, color: '#c3cddd', opacity: ease(f, t0 + 16, t0 + 30)}}>
        Unique solves unlock one-time rewards, all the way to the bank's last problem.
      </div>
      <div style={{position: 'absolute', left: 180, right: 180, top: 560, height: 8, background: '#1b2333', borderRadius: 4}}>
        <div style={{width: `${prog * 100}%`, height: '100%', background: `linear-gradient(90deg, ${C.emerald}, ${C.violet})`, boxShadow: `0 0 24px ${C.violet}`, borderRadius: 4}} />
      </div>
      {MILESTONES.map(([n, it, q], i) => {
        const x = 180 + (i / 5) * (W - 360);
        const at = t0 + 30 + step * i;
        const on = f >= at;
        const p = pop(f, at, 30, 9);
        const last = i === 5;
        const sz = last ? 150 : 96;
        return <div key={n} style={{position: 'absolute', left: x - 110, top: 360, width: 220, textAlign: 'center'}}>
          <div style={{height: 170, display: 'flex', alignItems: 'flex-end', justifyContent: 'center'}}>
            <div style={{transform: `translateY(${on ? (1 - p) * 40 + Math.sin((f - at) / 10) * 5 : 30}px) scale(${on ? 0.6 + p * 0.4 : 0.6})`, opacity: on ? 1 : 0.18,
              filter: on ? `drop-shadow(0 0 ${last ? 40 : 18}px ${last ? C.violet : C.emerald})` : 'grayscale(1)'}}>
              <ItemIcon name={it} size={sz} />
            </div>
          </div>
          <div style={{margin: '18px auto 0', width: 34, height: 34, transform: 'rotate(45deg)', background: on ? (last ? C.violet : C.emerald) : '#1b2333', boxShadow: on ? `0 0 24px ${last ? C.violet : C.emerald}` : 'none'}} />
          <div style={{fontFamily: F.display, fontWeight: 700, fontSize: last ? 64 : 50, color: on ? C.ink : C.dim, marginTop: 24}}>{n}</div>
          <div style={{fontFamily: F.mono, fontSize: 20, color: on ? C.mute : C.dim}}>solves · {q}</div>
        </div>;
      })}
      <Shockwave at={egg} x={W - 180} y={460} color={C.violet} size={900} />
      <Burst at={egg} x={W - 180} y={460} n={50} colors={[C.violet, '#e0ccff', '#1a0f2e']} speed={20} />
      <div style={{position: 'absolute', right: 120, top: 860, fontFamily: F.pixel, fontSize: 20, color: C.violet, opacity: ease(f, egg + 6, egg + 18), letterSpacing: 2}}>500 SOLVES = A DRAGON EGG</div>
    </div>
    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: ease(f, T.build, T.build + 20) * (1 - ease(f, T.trophies - 8, T.trophies))}}>
      <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 96, color: C.ink, letterSpacing: -3, textAlign: 'center', lineHeight: 1.05}}>
        <Reveal text="And every achievement" at={T.build + 4} stagger={0.9} /><br /><Reveal text="becomes a trophy." at={T.build + 26} stagger={1} color={C.gold} />
      </div>
    </AbsoluteFill>
    <Vignette />
  </AbsoluteFill>;
};



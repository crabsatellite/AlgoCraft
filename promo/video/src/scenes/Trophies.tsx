import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, ease, lin, pop, rnd, clamp, beat} from '../theme';
import {Stage, Dust, Reveal, Tag, Caption, Shockwave, Burst, Flash, ItemIcon, Vignette, Glitch, useShake} from '../fx';
import {Scene3D, VoxelModel, Lights} from '../Model3D';
import {Headline, Em} from '../common';
import trophies from '../../public/data/trophies.json';

export const TIER_C: Record<string, string> = {bronze: '#e08a4f', silver: '#dfe6ef', gold: '#ffcc4d', diamond: '#55e6f0', netherite: '#b28cff'};
const TIERS = ['bronze', 'silver', 'gold', 'diamond', 'netherite'];

const Beam: React.FC<{x: number; color: string; o: number}> = ({x, color, o}) => (
  <div style={{position: 'absolute', left: x - 170, top: -80, width: 340, height: 1000, opacity: o,
    background: `linear-gradient(180deg, ${color}44 0%, ${color}11 60%, transparent 100%)`, clipPath: 'polygon(40% 0, 60% 0, 100% 100%, 0 100%)', filter: 'blur(6px)'}} />
);

export const Trophies: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.trophies;
  const shake = useShake(t0, 16, 14);
  const step = beat(1) - beat(0);
  const camX = interpolate(f, [t0, T.engraving], [0.5, -0.5]);
  const out = ease(f, T.engraving - 10, T.engraving);
  return <AbsoluteFill style={{transform: shake}}>
    <Stage hue={C.gold} intensity={1.1} />
    <div style={{opacity: 1 - out}}>
      {TIERS.map((t, i) => <Beam key={t} x={960 + (i - 2) * 300} color={TIER_C[t]} o={ease(f, t0 + i * step, t0 + i * step + 10) * (0.7 + 0.3 * Math.sin(f / 12 + i))} />)}
      <div style={{position: 'absolute', left: 0, right: 0, top: 690, height: 60, background: `radial-gradient(ellipse at center, ${C.gold}33, transparent 70%)`, filter: 'blur(10px)'}} />
      <Scene3D width={W} height={H} cam={[camX, 1.9, 10.4]} target={[0, 0.45, 0]} fov={26}>
        <Lights rim="#ffd27a" />
        {TIERS.map((t, i) => {
          const at = t0 + i * step;
          const p = pop(f, at, 30, 10);
          return <VoxelModel key={t} id={t} position={[(i - 2) * 1.32, (1 - Math.min(1, p)) * -1.2, 0]} scale={1.05 * Math.max(0.001, p)} rotationY={Math.PI + 0.5 + (f - t0) / 40 + i * 0.7} />;
        })}
      </Scene3D>
      {TIERS.map((t, i) => {
        const at = t0 + i * step + 6;
        return <div key={t} style={{position: 'absolute', left: 960 + (i - 2) * 300 - 150, width: 300, top: 780, textAlign: 'center', opacity: ease(f, at, at + 10), transform: `translateY(${(1 - ease(f, at, at + 14)) * 20}px)`}}>
          <div style={{fontFamily: F.pixel, fontSize: 20, color: TIER_C[t], letterSpacing: 2, textShadow: `0 0 18px ${TIER_C[t]}`}}>{t.toUpperCase()}</div>
        </div>;
      })}
      <Headline at={t0 + 6} align="center" y={90} size={84} width={1600}>Five tiers. <Em c={C.gold}>Real trophies.</Em></Headline>
      <div style={{position: 'absolute', left: 0, right: 0, top: 200, textAlign: 'center', fontFamily: F.body, fontSize: 27, color: '#c3cddd', opacity: ease(f, t0 + 20, t0 + 34)}}>
        Every trophy is a 3D item you can hold, collect and hang in an item frame.
      </div>
    </div>
    <Flash at={t0} dur={12} color="#fff3d1" />
    <Vignette />
  </AbsoluteFill>;
};

export const Engraving: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.engraving;
  const enter = ease(f, t0, t0 + 24);
  const out = ease(f, T.variants - 10, T.variants);
  return <AbsoluteFill style={{opacity: 1 - out}}>
    <Stage hue={TIER_C.bronze} />
    <div style={{position: 'absolute', left: 260, top: 560, width: 700, height: 160, borderRadius: '50%', background: `radial-gradient(closest-side, ${TIER_C.bronze}55, transparent)`, filter: 'blur(16px)'}} />
    <Scene3D width={1100} height={H} cam={[0, 1.5, 4.9]} target={[0, 0.5, 0]} fov={28} style={{left: 0}}>
      <Lights rim="#ffb27a" />
      <VoxelModel id="v_streak_7" rotationY={Math.PI + (f - t0) / 28} scale={1.2 * enter} />
    </Scene3D>
    <div style={{position: 'absolute', left: 1080, top: 250, width: 700, padding: '40px 44px', borderRadius: 18, background: 'linear-gradient(160deg, #1a1410ee, #0d1018ee)',
      border: `1px solid ${TIER_C.bronze}66`, boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 60px ${TIER_C.bronze}22`, opacity: enter, transform: `translateX(${(1 - enter) * 80}px)`}}>
      <div style={{fontFamily: F.pixel, fontSize: 14, color: TIER_C.bronze, letterSpacing: 2}}>BRONZE TROPHY · STREAK</div>
      <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 64, color: C.ink, marginTop: 18}}><Reveal text="Weekly Warrior" at={t0 + 10} /></div>
      <div style={{fontFamily: F.body, fontSize: 26, color: '#c3cddd', marginTop: 8, opacity: ease(f, t0 + 20, t0 + 32)}}>Maintain a 7-day solving streak</div>
      <div style={{height: 1, background: `${TIER_C.bronze}44`, margin: '28px 0'}} />
      {[['Awarded to', 'Alex'], ['Awarded on', '2026-10-01']].map(([k, v], i) => <div key={k} style={{fontFamily: F.mono, fontSize: 26, color: C.mute, marginTop: 10, opacity: ease(f, t0 + 34 + i * 8, t0 + 46 + i * 8)}}>
        {k}: <span style={{color: C.ink}}>{v}</span></div>)}
    </div>
    <div style={{position: 'absolute', left: 1084, top: 690, display: 'flex', alignItems: 'center', gap: 18, opacity: ease(f, t0 + 60, t0 + 72)}}>
      <ItemIcon name="item_frame" size={56} />
      <div style={{fontFamily: F.body, fontSize: 26, color: '#c3cddd'}}>Engraved with your name, the date and the feat.</div>
    </div>
    <Vignette />
  </AbsoluteFill>;
};

export const Variants: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.variants;
  const list = trophies as {id: string; tier: string; name: string}[];
  const cols = 8;
  const step = 2;
  const out = ease(f, T.server - 10, T.server);
  const hero = list.length - 1;
  return <AbsoluteFill style={{opacity: 1 - out}}>
    <Stage hue={C.violet} intensity={1.1} />
    <Scene3D width={W} height={H} cam={[0, 1.2, 11.8]} target={[0, 0.8, 0]} fov={30}>
      <Lights rim="#c9a7ff" />
      {list.map((v, i) => {
        const r = Math.floor(i / cols), c = i % cols;
        const n = r === 2 ? list.length - 2 * cols : cols;
        const x = (c - (n - 1) / 2) * 1.25;
        const y = 1.55 - r * 1.45;
        const p = pop(f, t0 + i * step, 30, 11);
        return <VoxelModel key={v.id} id={'v_' + v.id} position={[x, y, 0]} scale={0.9 * Math.max(0.001, p)} rotationY={Math.PI + 0.6 + Math.sin((f - t0) / 30 + i) * 0.5} />;
      })}
    </Scene3D>
    <Headline at={t0 + 4} align="center" y={60} size={78} width={1600}><Em c={C.violet}>23</Em> achievements to collect</Headline>
    <div style={{position: 'absolute', left: 0, right: 0, bottom: 70, textAlign: 'center', fontFamily: F.body, fontSize: 28, color: '#c3cddd', opacity: ease(f, t0 + 30, t0 + 44)}}>
      Milestones · Streaks · Difficulty · Night Owl, Speed Demon… and <b style={{color: TIER_C.netherite}}>Completionist</b> for clearing a whole bank.
    </div>
    <Vignette />
  </AbsoluteFill>;
};



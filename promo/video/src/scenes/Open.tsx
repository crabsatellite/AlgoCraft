import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, ease, lin, pop, rnd, clamp, beat, bar} from '../theme';
import {Stage, Dust, Reveal, Tag, Caption, Mark, Wordmark, Glitch, Shockwave, Burst, Flash, GameShot, ItemIcon, PixelWipe, useShake, Vignette} from '../fx';
import {Scene3D, VoxelModel, Lights} from '../Model3D';
import {Headline, Em, mix} from '../common';

export const Intro: React.FC = () => {
  const f = useCurrentFrame();
  const out = ease(f, T.computer - 22, T.computer);
  const blink = Math.floor(f / 9) % 2 === 0;
  return <AbsoluteFill>
    <Stage intensity={lin(f, 0, 60, 0, 1)} />
    <Dust n={46} />
    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: 1 - out, filter: `blur(${out * 14}px)`, transform: `scale(${1 + out * 0.08})`}}>
      <div style={{fontFamily: F.mono, fontSize: 26, color: C.mute, letterSpacing: 4, marginBottom: 34, opacity: ease(f, 20, 40)}}>
        {'// '}<Reveal text="a Minecraft mod for curious coders" at={22} stagger={0.6} />
      </div>
      <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 132, letterSpacing: -4, color: C.ink, lineHeight: 1.05}}>
        <Reveal text="Learn algorithms." at={beat(1)} stagger={1.1} />
      </div>
      <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 132, letterSpacing: -4, lineHeight: 1.05, display: 'flex', alignItems: 'center'}}>
        <Reveal text="Inside " at={beat(5)} stagger={1.1} color={C.ink} />
        <span style={{textShadow: `0 0 50px ${C.emerald}aa`}}><Reveal text="Minecraft." at={beat(5) + 8} stagger={1.3} color={C.emerald} /></span>
        <span style={{display: 'inline-block', width: 22, height: 110, marginLeft: 14, background: C.emerald, opacity: f > beat(5) && blink ? 1 : 0, boxShadow: `0 0 20px ${C.emerald}`}} />
      </div>
    </AbsoluteFill>
    <Vignette />
  </AbsoluteFill>;
};

const RECIPE = ['iron_ingot', 'iron_ingot', 'iron_ingot', 'redstone', 'glass', 'redstone', 'iron_ingot', 'iron_ingot', 'iron_ingot'];
const Slot: React.FC<{children?: React.ReactNode; size?: number; glow?: number}> = ({children, size = 92, glow = 0}) => (
  <div style={{width: size, height: size, background: '#131a26', border: '2px solid #2a3547', boxShadow: `inset 0 4px 0 #0a0f17, 0 0 ${30 * glow}px ${C.emerald}`,
    display: 'flex', alignItems: 'center', justifyContent: 'center'}}>{children}</div>
);

export const Computer: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.computer;
  const enter = ease(f, t0, t0 + 30);
  const rot = interpolate(f, [t0, T.ingameWorld], [-1.25, 0.35]);
  const camZ = interpolate(f, [t0, T.ingameWorld], [4.6, 3.6]);
  return <AbsoluteFill>
    <Stage hue={C.emerald} />
    <div style={{position: 'absolute', left: 140, top: 640, width: 820, height: 180, borderRadius: '50%', background: `radial-gradient(closest-side, ${C.emerald}55, transparent)`, filter: 'blur(20px)', opacity: enter}} />
    <div style={{opacity: enter, transform: `translateY(${(1 - enter) * 60}px)`}}>
      <Scene3D width={1100} height={1080} cam={[0, 1.15, camZ]} target={[0, 0.42, 0]} fov={30}>
        <Lights />
        <VoxelModel id="computer" rotationY={Math.PI + rot} scale={1.25} />
      </Scene3D>
    </div>
    <div style={{position: 'absolute', left: 1060, top: 170, width: 760}}>
      <div style={{opacity: ease(f, t0 + 10, t0 + 24)}}><Tag>NEW BLOCK</Tag></div>
      <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 92, color: C.ink, letterSpacing: -3, marginTop: 26, lineHeight: 1}}>
        <Reveal text="Algorithm" at={t0 + 14} /><br /><Reveal text="Computer" at={t0 + 22} color={C.emerald} />
      </div>
      <div style={{fontFamily: F.body, fontSize: 30, color: '#c3cddd', marginTop: 26, opacity: ease(f, t0 + 36, t0 + 54), lineHeight: 1.4}}>
        Your coding station. Crafted from vanilla materials.
      </div>
      <div style={{display: 'flex', alignItems: 'center', gap: 34, marginTop: 50}}>
        <div style={{display: 'grid', gridTemplateColumns: 'repeat(3, 92px)', gap: 6, padding: 14, background: '#0b111b', border: '2px solid #222c3c', borderRadius: 6}}>
          {RECIPE.map((it, i) => {
            const at = t0 + 48 + i * (beat(1) - beat(0)) / 2;
            const p = pop(f, at, 30, 10);
            return <Slot key={i} glow={Math.max(0, 1 - (f - at) / 10) * (f >= at ? 1 : 0)}>
              {f >= at && <div style={{transform: `scale(${p})`}}><ItemIcon name={it} size={64} /></div>}
            </Slot>;
          })}
        </div>
        <div style={{fontFamily: F.pixel, fontSize: 34, color: C.emerald, opacity: ease(f, t0 + 120, t0 + 130)}}>{'>'}</div>
        <div style={{opacity: ease(f, t0 + 124, t0 + 136), transform: `scale(${pop(f, t0 + 124)})`}}>
          <Slot size={150} glow={0.6}><Img src={staticFile('fx/computer-icon.png')} style={{width: 140}} /></Slot>
        </div>
      </div>
      <div style={{fontFamily: F.mono, fontSize: 21, color: C.mute, marginTop: 24, opacity: ease(f, t0 + 132, t0 + 150), lineHeight: 1.6}}>
        6 iron ingots · 2 redstone dust · 1 glass<br />Recipe unlocks when you pick up redstone.
      </div>
    </div>
    <Vignette />
  </AbsoluteFill>;
};

export const InGameWorld: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.ingameWorld;
  const z = interpolate(f, [t0, T.drop], [1.0, 1.18]);
  const dark = ease(f, T.drop - 30, T.drop);
  return <AbsoluteFill style={{background: '#000'}}>
    <AbsoluteFill style={{transform: `scale(${z})`, transformOrigin: '50% 58%'}}>
      <Img src={staticFile('game/model-computer-world.png')} style={{width: W, height: H, imageRendering: 'pixelated'}} />
    </AbsoluteFill>
    <AbsoluteFill style={{background: 'linear-gradient(90deg, rgba(5,7,13,0.85) 0%, rgba(5,7,13,0.2) 45%, transparent 70%)'}} />
    <div style={{position: 'absolute', left: 40, top: 40}}><Tag style={{background: 'rgba(5,7,13,0.7)'}}>IN-GAME · MINECRAFT 1.21.1</Tag></div>
    <Caption at={t0 + 6} out={T.drop - 16} title="Place it anywhere." sub="Right-click to open your IDE." y={800} />
    <AbsoluteFill style={{background: C.bg, opacity: dark * 0.9}} />
    <PixelWipe at={t0} dur={14} />
  </AbsoluteFill>;
};

export const Drop: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.drop;
  const s = 1 + (1 - pop(f, t0, 30, 9, 0.8)) * 0.6;
  const shake = useShake(t0, 22, 16);
  const rays = (f - t0) * 0.4;
  const out = ease(f, T.ingame - 14, T.ingame);
  return <AbsoluteFill style={{transform: shake}}>
    <Stage intensity={1.4} />
    <AbsoluteFill style={{opacity: 0.35 * (1 - out), background: `repeating-conic-gradient(from ${rays}deg at 50% 46%, ${C.emerald}22 0deg 4deg, transparent 4deg 18deg)`,
      maskImage: 'radial-gradient(circle at 50% 46%, black 0%, transparent 60%)', WebkitMaskImage: 'radial-gradient(circle at 50% 46%, black 0%, transparent 60%)'}} />
    <Shockwave at={t0} y={490} size={2200} width={14} />
    <Shockwave at={t0 + 6} y={490} size={1500} color="#9dffd6" width={6} />
    <Burst at={t0} y={490} n={90} speed={30} />
    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: 1 - out, filter: `blur(${out * 12}px)`}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 46, transform: `scale(${s})`, marginTop: -60}}>
        <Mark size={190} glow={1.4} />
        <Glitch at={t0} amount={26}><Wordmark size={190} /></Glitch>
      </div>
      <div style={{fontFamily: F.display, fontWeight: 500, fontSize: 46, color: '#d6deeb', marginTop: 38, letterSpacing: 1}}>
        <Reveal text="Learn algorithms in Minecraft." at={t0 + 12} stagger={0.7} />
      </div>
      <div style={{display: 'flex', gap: 22, marginTop: 56}}>
        {['500 official problems', 'In-game + Web IDE', 'Daily streaks & trophies'].map((s, i) => {
          const p = pop(f, t0 + 40 + i * 6);
          return <div key={s} style={{fontFamily: F.mono, fontSize: 24, color: C.ink, padding: '14px 22px', border: `1px solid ${C.emerald}55`, background: '#0c1b1866',
            borderRadius: 999, transform: `translateY(${(1 - p) * 30}px)`, opacity: p}}>{s}</div>;
        })}
      </div>
    </AbsoluteFill>
    <Flash at={t0} dur={12} />
    <Vignette />
  </AbsoluteFill>;
};

export const InGameIDE: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.ingame;
  const p = ease(f, t0, t0 + 34);
  const ry = interpolate(f, [t0, T.web], [-24, -10]);
  const swap = ease(f, t0 + 62, t0 + 76);
  const out = ease(f, T.web - 12, T.web);
  return <AbsoluteFill>
    <Stage />
    <div style={{position: 'absolute', left: 640, top: 140, perspective: 1800, opacity: 1 - out}}>
      <div style={{transform: `translateX(${(1 - p) * 300}px) rotateY(${ry}deg) rotateX(4deg)`, transformOrigin: '0% 50%', position: 'relative'}}>
        <GameShot src="game/ide-wide.png" width={1180} label="IN-GAME IDE" />
        <div style={{position: 'absolute', inset: 0, opacity: swap}}><GameShot src="game/sample-p95.png" width={1180} label="IN-GAME IDE" /></div>
      </div>
    </div>
    <Headline at={t0 + 4} out={T.web - 12} x={110} y={300} size={76} width={540}>Code without leaving <Em>the game.</Em></Headline>
    <div style={{position: 'absolute', left: 114, top: 590, width: 470, fontFamily: F.body, fontSize: 27, lineHeight: 1.45, color: '#c3cddd',
      opacity: ease(f, t0 + 20, t0 + 36) * (1 - out)}}>Problem list, statement, editor and terminal on one screen.</div>
    <Vignette />
  </AbsoluteFill>;
};


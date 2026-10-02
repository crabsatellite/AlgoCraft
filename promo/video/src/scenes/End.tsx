import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, ease, lin, pop, rnd, clamp, beat} from '../theme';
import {Stage, Dust, Reveal, Tag, Caption, Shockwave, Burst, Flash, ItemIcon, Vignette, Mark, Wordmark, Glitch, GameShot, Browser, useShake, PixelWipe} from '../fx';
import {Scene3D, VoxelModel, Lights} from '../Model3D';
import {Headline, Em, CapView} from '../common';
import {TIER_C} from './Trophies';

const PLAYERS = [{n: 'Alex', c: '#5ec8ff', a: -150}, {n: 'Kai', c: '#ffb02e', a: -60}, {n: 'Mia', c: '#ff6b9a', a: 30}, {n: 'Sam', c: '#a77bff', a: 120}];
const CX = 1330, CY = 540, R = 340;

export const Server: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.server;
  const step = beat(4) - beat(0);
  const points = [
    {at: t0 + 20, h: 'The server owns the bank.', s: 'Ops or the console install public banks. Players cannot.'},
    {at: t0 + 20 + step * 2, h: 'Everyone shares one problem set.', s: 'Public submits are judged on the server, hidden tests included.'},
    {at: t0 + 20 + step * 4, h: 'Personal imports stay private.', s: 'Practice your own problems. They never pay server rewards.'},
  ];
  const out = ease(f, T.recap - 10, T.recap);
  return <AbsoluteFill style={{opacity: 1 - out}}>
    <Stage hue="#3d6bff" />
    <Dust n={24} color="#7aa2ff" />
    <Headline at={t0 + 2} x={110} y={90} size={74} width={900}>Built for <Em c="#7aa2ff">servers.</Em></Headline>
    {points.map((p, i) => <div key={i} style={{position: 'absolute', left: 114, top: 250 + i * 215, width: 800, opacity: ease(f, p.at, p.at + 14), transform: `translateX(${(1 - ease(f, p.at, p.at + 18)) * -40}px)`}}>
      <div style={{display: 'flex', gap: 18, alignItems: 'flex-start'}}>
        <div style={{fontFamily: F.pixel, fontSize: 20, color: '#7aa2ff', marginTop: 10}}>0{i + 1}</div>
        <div>
          <div style={{fontFamily: F.display, fontWeight: 700, fontSize: 40, color: C.ink, whiteSpace: 'nowrap'}}>{p.h}</div>
          <div style={{fontFamily: F.body, fontSize: 25, color: '#aab6c8', marginTop: 8, lineHeight: 1.4}}>{p.s}</div>
        </div>
      </div>
    </div>)}
    <svg width={W} height={H} style={{position: 'absolute', inset: 0}}>
      {PLAYERS.map((p, i) => {
        const a = (p.a * Math.PI) / 180;
        const x = CX + Math.cos(a) * R, y = CY + Math.sin(a) * R;
        const d = ease(f, t0 + 30 + i * 6, t0 + 50 + i * 6);
        return <line key={p.n} x1={CX} y1={CY} x2={CX + (x - CX) * d} y2={CY + (y - CY) * d} stroke={p.c} strokeOpacity={0.5} strokeWidth={3} strokeDasharray="10 10" strokeDashoffset={-f * 1.5} />;
      })}
    </svg>
    {PLAYERS.map((p, i) => {
      const a = (p.a * Math.PI) / 180;
      const x = CX + Math.cos(a) * R, y = CY + Math.sin(a) * R;
      const pp = pop(f, t0 + 40 + i * 6);
      // Packets: submit goes up, verdict comes back.
      const cyc = (f - t0 - 60 - i * 17) % 70;
      const up = cyc >= 0 && cyc < 30 ? cyc / 30 : -1;
      const down = cyc >= 35 && cyc < 65 ? (cyc - 35) / 30 : -1;
      return <React.Fragment key={p.n}>
        {f > t0 + 60 && up >= 0 && <div style={{position: 'absolute', left: x + (CX - x) * up - 9, top: y + (CY - y) * up - 9, width: 18, height: 18, background: p.c, boxShadow: `0 0 16px ${p.c}`}} />}
        {f > t0 + 60 && down >= 0 && <div style={{position: 'absolute', left: CX + (x - CX) * down - 9, top: CY + (y - CY) * down - 9, width: 18, height: 18, background: C.emerald, boxShadow: `0 0 16px ${C.emerald}`, transform: 'rotate(45deg)'}} />}
        <div style={{position: 'absolute', left: x - 70, top: y - 70, width: 140, textAlign: 'center', transform: `scale(${pp})`}}>
          <div style={{width: 92, height: 92, margin: '0 auto', borderRadius: 10, background: `linear-gradient(160deg, ${p.c}, ${p.c}88)`, boxShadow: `0 0 30px ${p.c}66, inset 0 -8px 0 #0004`, position: 'relative'}}>
            <div style={{position: 'absolute', left: 22, top: 34, width: 14, height: 14, background: '#0b0f18'}} />
            <div style={{position: 'absolute', right: 22, top: 34, width: 14, height: 14, background: '#0b0f18'}} />
          </div>
          <div style={{fontFamily: F.mono, fontSize: 22, color: C.ink, marginTop: 10}}>{p.n}</div>
        </div>
      </React.Fragment>;
    })}
    <div style={{position: 'absolute', left: CX - 150, top: CY - 120, width: 300, height: 240, borderRadius: 22, background: 'linear-gradient(160deg, #15233b, #0b1220)',
      border: '2px solid #7aa2ff88', boxShadow: '0 0 80px #3d6bff55', transform: `scale(${pop(f, t0 + 16)})`, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12}}>
      <div style={{display: 'flex', flexDirection: 'column', gap: 6}}>{[0, 1, 2].map(k => <div key={k} style={{width: 150, height: 22, background: '#0b1220', border: '2px solid #2b3d5e', display: 'flex', alignItems: 'center', gap: 8, padding: '0 8px'}}>
        <div style={{width: 8, height: 8, background: (Math.floor(f / 6) + k) % 3 === 0 ? C.emerald : '#2b3d5e'}} /><div style={{flex: 1, height: 4, background: '#2b3d5e'}} /></div>)}</div>
      <div style={{fontFamily: F.pixel, fontSize: 16, color: '#9cb8ff'}}>SERVER</div>
      <div style={{fontFamily: F.mono, fontSize: 18, color: C.mute}}>official bank · 500</div>
    </div>
    <div style={{position: 'absolute', left: 1030, top: 950, display: 'flex', gap: 34, fontFamily: F.mono, fontSize: 20, color: C.mute, opacity: ease(f, t0 + 70, t0 + 84)}}>
      <span><span style={{display: 'inline-block', width: 14, height: 14, background: '#5ec8ff', marginRight: 10}} />submit</span>
      <span><span style={{display: 'inline-block', width: 14, height: 14, background: C.emerald, transform: 'rotate(45deg)', marginRight: 10}} />verdict + rewards</span>
    </div>
    <Vignette />
  </AbsoluteFill>;
};

const TILES = [
  {t: 'In-game IDE', img: 'game/ide-wide.png', pix: true},
  {t: 'Web IDE', img: 'cap/code.png'},
  {t: '500 problems', img: 'cap/browse/p7.png'},
  {t: 'Run & Submit', img: 'cap/accepted.png'},
  {t: 'Diagrams', img: 'cap/browse/p148.png'},
  {t: 'Trophies', img: 'game/model-items-gui.png', pix: true},
  {t: 'English & 中文', img: 'cap/zh.png'},
  {t: 'Craftable block', img: 'game/model-computer-world.png', pix: true},
];
export const Recap: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.recap;
  const step = beat(1) - beat(0);
  const zoom = interpolate(f, [t0, T.outro], [1.08, 0.94]);
  const out = ease(f, T.outro - 12, T.outro);
  return <AbsoluteFill style={{opacity: 1 - out}}>
    <Stage />
    <div style={{position: 'absolute', left: 90, top: 120, right: 90, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 26, transform: `scale(${zoom})`}}>
      {TILES.map((tl, i) => {
        const at = t0 + i * step;
        const p = pop(f, at, 30, 12);
        const hot = Math.max(0, 1 - (f - at) / 12) * (f >= at ? 1 : 0);
        return <div key={tl.t} style={{position: 'relative', height: 380, borderRadius: 18, overflow: 'hidden', border: `2px solid ${hot > 0 ? C.emerald : '#1f2a3a'}`,
          boxShadow: `0 0 ${50 * hot}px ${C.emerald}`, opacity: Math.min(1, p), transform: `scale(${0.85 + 0.15 * p})`}}>
          <Img src={staticFile(tl.img)} style={{position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', imageRendering: tl.pix ? 'pixelated' : 'auto', filter: `brightness(${0.75 + hot * 0.4})`}} />
          <div style={{position: 'absolute', left: 0, right: 0, bottom: 0, padding: '60px 22px 20px', background: 'linear-gradient(transparent, rgba(5,7,13,0.95))',
            fontFamily: F.display, fontWeight: 700, fontSize: 36, color: C.ink}}>{tl.t}</div>
        </div>;
      })}
    </div>
    <Flash at={t0} dur={8} peak={0.4} />
    <Vignette />
  </AbsoluteFill>;
};

export const Outro: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.outro;
  const enter = pop(f, t0, 30, 12, 0.8);
  const fade = ease(f, T.end - 70, T.end - 6);
  return <AbsoluteFill style={{opacity: 1 - fade}}>
    <Stage intensity={1.2} />
    <Dust n={50} />
    <div style={{position: 'absolute', left: 0, right: 0, top: 560, opacity: ease(f, t0 + 20, t0 + 50) * 0.9}}>
      <Scene3D width={W} height={520} cam={[0, 1.4, 9.5]} target={[0, 0.5, 0]} fov={24}>
        <Lights />
        {['bronze', 'silver', 'gold', 'diamond', 'netherite'].map((t, i) => <VoxelModel key={t} id={t} position={[(i - 2) * 1.5 + (i >= 2 ? 1.6 : -1.6) * 0, 0, 0]} scale={0.95} rotationY={Math.PI + 0.4 + (f - t0) / 50 + i} />)}
      </Scene3D>
    </div>
    <Shockwave at={t0} y={360} size={1800} />
    <AbsoluteFill style={{alignItems: 'center', paddingTop: 170}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 40, transform: `scale(${0.8 + 0.2 * enter})`, opacity: Math.min(1, enter)}}>
        <Mark size={150} glow={1.2} />
        <Glitch at={t0} amount={16}><Wordmark size={160} /></Glitch>
      </div>
      <div style={{fontFamily: F.display, fontWeight: 500, fontSize: 46, color: '#d6deeb', marginTop: 34}}><Reveal text="Learn algorithms in Minecraft." at={t0 + 12} stagger={0.7} /></div>
      <div style={{fontFamily: F.mono, fontSize: 25, color: C.mute, marginTop: 26, opacity: ease(f, t0 + 40, t0 + 60), letterSpacing: 1}}>
        0.1.0 Beta · Forge 1.20.1 + NeoForge 1.21.1
        <div style={{color: C.emerald, marginTop: 16, textAlign: "center"}}>github.com/crabsatellite/AlgoCraft</div>
      </div>
    </AbsoluteFill>
    <div style={{position: 'absolute', left: 0, right: 0, bottom: 34, textAlign: 'center', fontFamily: F.body, fontSize: 18, color: '#6d7a8f', opacity: ease(f, t0 + 70, t0 + 90)}}>
      Music: “Voxel Revolution” by Kevin MacLeod (incompetech.com), CC BY 4.0 · Sound effects: Kenney.nl (CC0)
    </div>
    <Flash at={t0} dur={10} peak={0.5} />
    <Vignette />
  </AbsoluteFill>;
};


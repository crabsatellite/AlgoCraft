import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {C, F, W, H, T, ease, lin, pop, rnd, clamp, beat} from '../theme';
import {Stage, Dust, Reveal, Tag, Caption, Glitch, Shockwave, Burst, Flash, Browser, ItemIcon, useShake, Vignette, Grain} from '../fx';
import {CapView, Headline, Em, mix} from '../common';

const TYPE_N = 201;
const BW = 1640;
const camAt = (f: number, keys: [number, number, number, number][]) => {
  // keys: [frame, x, y, s]; eased interpolation between consecutive keys.
  const fr = keys.map(k => k[0]);
  const e = {...clamp, easing: (t: number) => t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2};
  return {x: interpolate(f, fr, keys.map(k => k[1]), e), y: interpolate(f, fr, keys.map(k => k[2]), e), s: interpolate(f, fr, keys.map(k => k[3]), e)};
};

export const WebIDE: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.web;
  const typeStart = t0 + 40, typeEnd = T.run - 64;
  const idx = Math.max(0, Math.min(TYPE_N - 1, Math.floor((f - typeStart) / (typeEnd - typeStart) * (TYPE_N - 1))));
  const line = idx / (TYPE_N - 1);
  let src = 'cap/starter.png';
  if (f >= typeStart) src = `cap/type/${String(idx).padStart(4, '0')}.jpg`;
  if (f >= typeEnd) src = 'cap/code.png';
  const cam = camAt(f, [[t0, 960, 540, 1], [typeStart, 960, 540, 1], [typeStart + 34, 1360, 250, 1.62], [typeEnd - 20, 1360, 430, 1.62], [typeEnd + 18, 1300, 760, 1.12], [T.run, 1500, 900, 1.25]]);
  const cy = interpolate(line, [0, 1], [250, 430]);
  const camY = f > typeStart + 34 && f < typeEnd - 20 ? cy : cam.y;
  const enter = ease(f, t0, t0 + 26);
  const pulse = f > T.run - 26 ? (Math.sin((f - T.run) / 3) + 1) / 2 : 0;
  return <AbsoluteFill>
    <Stage hue="#3d6bff" floor={false} />
    <div style={{position: 'absolute', left: (W - BW) / 2, top: 44, transform: `translateY(${(1 - enter) * 120}px) scale(${0.94 + enter * 0.06})`, opacity: enter}}>
      <Browser width={BW}><CapView src={src} width={BW} x={cam.x} y={camY} s={cam.s} /></Browser>
    </div>
    <div style={{position: 'absolute', left: 0, right: 0, bottom: 0, height: 360, background: 'linear-gradient(transparent, rgba(5,7,13,0.92))'}} />
    <Caption at={t0 + 8} out={typeStart + 70} title="Prefer a big screen?" sub="Open the Web IDE: a full Monaco editor that submits through your player." />
    <Caption at={typeStart + 84} out={typeEnd - 6} title="Write real Java." sub="Two Sum, solved with a HashMap. Every problem ships with starter code." />
    <Caption at={typeEnd + 6} out={T.run + 4} title="Run. Then Submit." sub="Two buttons, two levels of confidence." color={C.gold} />
    {pulse > 0 && <div style={{position: 'absolute', right: 150, bottom: 136, width: 300, height: 90, borderRadius: 18, border: `3px solid ${C.gold}`, opacity: pulse, boxShadow: `0 0 40px ${C.gold}`}} />}
    <Vignette strength={0.55} />
  </AbsoluteFill>;
};

export const RunScene: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.run;
  const k = Math.min(12, Math.floor((f - t0) / 1.6));
  const src = f < t0 + 22 ? `cap/run/${String(Math.max(0, k)).padStart(4, '0')}.jpg` : 'cap/run-pass.png';
  const cam = camAt(f, [[t0, 1500, 900, 1.25], [t0 + 22, 520, 905, 1.85], [T.submit, 520, 905, 1.85]]);
  const pass = pop(f, t0 + 24);
  return <AbsoluteFill>
    <Stage hue="#3d6bff" floor={false} />
    <div style={{position: 'absolute', left: (W - BW) / 2, top: 44}}><Browser width={BW}><CapView src={src} width={BW} x={cam.x} y={cam.y} s={cam.s} /></Browser></div>
    <div style={{position: 'absolute', right: 150, top: 300, transform: `scale(${pass}) rotate(${(1 - pass) * -8}deg)`, opacity: pass}}>
      <div style={{fontFamily: F.pixel, fontSize: 44, color: '#06140e', background: C.emerald, padding: '22px 28px', boxShadow: `0 0 60px ${C.emerald}, 8px 8px 0 #0a5e3f`}}>PASS x2</div>
    </div>
    <Flash at={t0 + 24} dur={8} color={C.emerald} peak={0.25} />
    <Caption at={t0 + 4} out={T.submit - 6} title="Run checks the examples." sub="Fast feedback on the visible test cases." y={110} x={150} panel />
    <Vignette strength={0.55} />
  </AbsoluteFill>;
};

export const SubmitScene: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.submit;
  const n = 22;
  const k = Math.max(0, Math.min(n - 1, Math.floor((f - t0 - 6) / ((T.accepted - 12 - t0 - 6) / n))));
  const src = f < T.accepted - 12 ? `cap/submit/${String(k).padStart(4, '0')}.jpg` : 'cap/accepted.png';
  const cam = camAt(f, [[t0, 1600, 950, 1.5], [t0 + 30, 1400, 700, 1.1], [T.accepted - 12, 960, 540, 1.0], [T.accepted + 30, 960, 520, 1.25]]);
  const a = T.accepted;
  const shake = useShake(a, 18, 16);
  const big = pop(f, a, 30, 9, 0.7);
  const back = ease(f, a - 2, a + 6) * (1 - ease(f, T.rewards - 14, T.rewards));
  const count = Math.round(interpolate(f, [a + 8, a + 30], [0, 7], clamp));
  return <AbsoluteFill style={{transform: shake}}>
    <Stage hue="#3d6bff" floor={false} />
    <div style={{position: 'absolute', left: (W - BW) / 2, top: 44}}><Browser width={BW}><CapView src={src} width={BW} x={cam.x} y={cam.y} s={cam.s} /></Browser></div>
    <Caption at={t0 + 2} out={a - 4} title="Submit is judged by the server." sub="Hidden tests included. Progress and rewards are recorded there too." color={C.gold} panel />
    <AbsoluteFill style={{background: 'radial-gradient(circle at 50% 50%, rgba(3,30,20,0.75), rgba(2,6,10,0.92))', opacity: back}} />
    {f >= a && <>
      <Shockwave at={a} size={2400} width={16} />
      <Shockwave at={a + 5} size={1600} color="#c8ffe9" width={8} />
      <Shockwave at={a + 10} size={1100} color={C.gold} width={5} />
      <Burst at={a} n={120} speed={34} dur={60} size={16} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: back}}>
        <div style={{transform: `scale(${0.6 + big * 0.4})`}}>
          <Glitch at={a} amount={30}><div style={{fontFamily: F.display, fontWeight: 700, fontSize: 250, letterSpacing: -10, color: C.emerald,
            textShadow: `0 0 80px ${C.emerald}, 0 0 160px ${C.emerald}88`, lineHeight: 1}}>ACCEPTED</div></Glitch>
        </div>
        <div style={{fontFamily: F.mono, fontSize: 44, color: C.ink, marginTop: 30, opacity: ease(f, a + 6, a + 16)}}>
          <span style={{color: C.emerald}}>{count} / 7</span> test cases passed
        </div>
      </AbsoluteFill>
    </>}
    <Flash at={a} dur={14} color="#d9fff0" />
    <Vignette />
  </AbsoluteFill>;
};

// Hard first-clear rewards per player tier, from RewardSystem.getHardFirstTimeRewards.
const TIERS = [
  {name: 'EARLY GAME', gear: 'starting out', c: '#c9d3e3', loot: [['diamond', 5], ['golden_apple', 3], ['iron_block', 4]]},
  {name: 'MID GAME', gear: 'diamond gear', c: C.cyan, loot: [['diamond', 10], ['netherite_scrap', 2], ['totem_of_undying', 1]]},
  {name: 'LATE GAME', gear: 'netherite gear', c: C.violet, loot: [['netherite_ingot', 1], ['enchanted_golden_apple', 2], ['nether_star', 1]]},
] as const;
export const Rewards: React.FC = () => {
  const f = useCurrentFrame();
  const t0 = T.rewards;
  const enter = ease(f, t0, t0 + 24);
  const out = ease(f, T.bank - 10, T.bank);
  return <AbsoluteFill style={{opacity: 1 - out}}>
    <Stage hue={C.gold} />
    <Dust n={30} color={C.gold} />
    <div style={{position: 'absolute', right: 70, top: 120, perspective: 1600}}>
      <div style={{transform: `rotateY(${-16 + enter * 6}deg) translateX(${(1 - enter) * 200}px)`, opacity: enter * 0.95}}>
        <Browser width={860}><CapView src="cap/history.png" width={860} x={960} y={500} s={1.35} /></Browser>
      </div>
      <div style={{position: 'absolute', left: 30, top: -26, opacity: ease(f, t0 + 30, t0 + 40)}}><Tag color={C.gold}>EVERY ATTEMPT IN HISTORY</Tag></div>
    </div>
    <Headline at={t0 + 2} x={110} y={110} size={84} width={900}>First clears <Em c={C.gold}>pay off.</Em></Headline>
    <div style={{position: 'absolute', left: 114, top: 225, width: 860, fontFamily: F.body, fontSize: 28, lineHeight: 1.45, color: '#c3cddd', opacity: ease(f, t0 + 12, t0 + 26)}}>
      Loot scales with difficulty and with your gear. Here is a Hard first clear:
    </div>
    <div style={{position: 'absolute', left: 110, top: 340, display: 'flex', gap: 22}}>
      {TIERS.map((t, i) => {
        const at = t0 + 22 + i * (beat(2) - beat(0));
        const p = pop(f, at, 30, 10);
        return <div key={t.name} style={{width: 300, padding: '22px 22px 26px', background: '#0f1724e6', border: `1px solid ${t.c}55`, borderRadius: 16,
          boxShadow: `0 0 40px ${t.c}22`, transform: `translateY(${(1 - p) * 50}px)`, opacity: Math.min(1, p)}}>
          <div style={{fontFamily: F.pixel, fontSize: 14, color: t.c, letterSpacing: 1}}>{t.name}</div>
          <div style={{fontFamily: F.body, fontSize: 20, color: C.mute, marginTop: 8}}>{t.gear}</div>
          <div style={{display: 'flex', gap: 14, marginTop: 22}}>
            {t.loot.map(([it, n], j) => <div key={it} style={{position: 'relative'}}>
              <ItemIcon name={it as string} size={72} style={{transform: `translateY(${Math.sin((f - at) / 9 + j) * 4}px)`}} />
              <div style={{position: 'absolute', right: -4, bottom: -6, fontFamily: F.pixel, fontSize: 18, color: '#fff', textShadow: '3px 3px 0 #3f3f3f'}}>{n}</div>
            </div>)}
          </div>
        </div>;
      })}
    </div>
    <div style={{position: 'absolute', left: 114, top: 640, width: 940, fontFamily: F.body, fontSize: 25, lineHeight: 1.5, color: '#aab6c8', opacity: ease(f, t0 + 64, t0 + 80)}}>
      Solved it before? Solve it again for a smaller review reward, once per server day.
    </div>
    <div style={{position: 'absolute', left: 114, top: 740, fontFamily: F.mono, fontSize: 20, color: C.mute, opacity: ease(f, t0 + 80, t0 + 92)}}>
      Server owners can switch rewards off in the config.
    </div>
    <Vignette />
  </AbsoluteFill>;
};



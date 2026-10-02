import React from 'react';
import {AbsoluteFill, Audio, Sequence, staticFile, useCurrentFrame, interpolate} from 'remotion';
import {T, TOTAL, beat, bar, clamp} from './theme';
import {Grain} from './fx';
import {Intro, Computer, InGameWorld, Drop, InGameIDE} from './scenes/Open';
import {WebIDE, RunScene, SubmitScene, Rewards} from './scenes/Code';
import {Bank, Diagrams, Bilingual} from './scenes/Bank';
import {Daily, Milestones} from './scenes/Daily';
import {Trophies, Engraving, Variants} from './scenes/Trophies';
import {Server, Recap, Outro} from './scenes/End';

const SCENES: [number, number, React.FC][] = [
  [T.intro, T.computer, Intro], [T.computer, T.ingameWorld, Computer], [T.ingameWorld, T.drop, InGameWorld], [T.drop, T.ingame, Drop],
  [T.ingame, T.web, InGameIDE], [T.web, T.run, WebIDE], [T.run, T.submit, RunScene], [T.submit, T.rewards, SubmitScene], [T.rewards, T.bank, Rewards],
  [T.bank, T.diagrams, Bank], [T.diagrams, T.bilingual, Diagrams], [T.bilingual, T.daily, Bilingual], [T.daily, T.review, Daily],
  [T.review, T.trophies, Milestones], [T.trophies, T.engraving, Trophies], [T.engraving, T.variants, Engraving], [T.variants, T.server, Variants],
  [T.server, T.recap, Server], [T.recap, T.outro, Recap], [T.outro, T.end, Outro],
];

type Cue = [number, string, number];
const cues = (): Cue[] => {
  const c: Cue[] = [];
  const step = beat(1) - beat(0);
  c.push([beat(1) - 2, 'whoosh', 0.18], [beat(5) + 6, 'glass', 0.25]);
  c.push([T.computer, 'whoosh', 0.35], [T.computer + 14, 'select', 0.3]);
  for (let i = 0; i < 9; i++) c.push([Math.round(T.computer + 48 + i * step / 2), 'pluck', 0.22]);
  c.push([T.computer + 124, 'power', 0.3]);
  c.push([T.ingameWorld, 'maximize', 0.3], [T.drop - 117, 'riser', 0.55]);
  c.push([T.drop, 'sub', 0.9], [T.drop, 'impact', 0.45], [T.drop + 2, 'boom', 0.35]);
  c.push([T.ingame, 'whoosh', 0.35], [T.web, 'whoosh', 0.35]);
  const typeStart = T.web + 40, typeEnd = T.run - 64;
  for (let f = typeStart; f < typeEnd; f += 4) c.push([f, f % 8 === 0 ? 'key' : 'click1', 0.07]);
  c.push([T.run - 2, 'click2', 0.45], [T.run + 24, 'three', 0.4]);
  c.push([T.submit, 'click2', 0.45], [T.submit + 8, 'computer', 0.2], [T.accepted, 'sub', 0.85], [T.accepted, 'confirm', 0.5], [T.accepted + 2, 'power2', 0.4]);
  c.push([T.rewards, 'whoosh', 0.3]);
  for (let i = 0; i < 3; i++) c.push([T.rewards + 22 + i * (beat(2) - beat(0)), 'power', 0.25]);
  c.push([T.bank, 'sub', 0.85], [T.bank, 'impact', 0.4]);
  for (let i = 0; i < 3; i++) c.push([T.split + i * 8, 'select', 0.3]);
  for (let i = 0; i < 8; i++) c.push([T.diagrams + Math.round(i * step), i % 2 ? 'glass' : 'drop', 0.3]);
  c.push([T.bilingual, 'switch', 0.4]);
  c.push([T.daily, 'whoosh', 0.25]);
  for (let i = 0; i < 14; i++) c.push([Math.round(T.daily + 34 + i * step / 2), (i + 1) % 7 === 0 ? 'power' : (i + 1) === 3 ? 'power2' : 'tick', (i + 1) % 7 === 0 ? 0.3 : 0.25]);
  c.push([T.review, 'whoosh', 0.25]);
  for (let i = 0; i < 6; i++) c.push([T.review + 30 + i * (beat(2) - beat(0)), i === 5 ? 'shimmer' : 'select', i === 5 ? 0.5 : 0.28]);
  c.push([T.review + 30 + 5 * (beat(2) - beat(0)), 'sub', 0.45]);
  c.push([T.trophies - 117, 'riser', 0.5]);
  c.push([T.trophies, 'sub', 0.9], [T.trophies, 'impact', 0.4], [T.trophies + 2, 'shimmer', 0.45]);
  for (let i = 0; i < 5; i++) c.push([T.trophies + Math.round(i * step), 'pluck', 0.3]);
  c.push([T.engraving, 'whoosh', 0.3], [T.engraving + 34, 'tick', 0.25], [T.engraving + 42, 'tick', 0.25]);
  c.push([T.variants, 'shimmer', 0.4], [T.variants, 'whoosh', 0.3]);
  c.push([T.server, 'field', 0.3]);
  for (let i = 0; i < 3; i++) c.push([T.server + 20 + i * 2 * (beat(4) - beat(0)), 'select', 0.3]);
  for (let f = T.server + 60; f < T.recap - 20; f += 35) c.push([f, 'laser', 0.06]);
  for (let i = 0; i < 8; i++) c.push([T.recap + Math.round(i * step), 'click1', 0.35]);
  c.push([T.outro, 'sub', 0.8], [T.outro + 2, 'shimmer', 0.4]);
  return c;
};

export const Sfx: React.FC = () => <>
  <Audio src={staticFile('music/voxel.mp3')} volume={(f) => interpolate(f, [0, 8, TOTAL - 80, TOTAL - 4], [0, 0.9, 0.9, 0], clamp)} />
  {cues().map(([at, name, v], i) => <Sequence key={i} from={Math.max(0, at)} durationInFrames={150} layout="none">
    <Audio src={staticFile(`sfx/${name}.wav`)} volume={v} />
  </Sequence>)}
</>;

export const Trailer: React.FC<{silent?: boolean}> = ({silent}) => {
  const f = useCurrentFrame();
  return <AbsoluteFill style={{background: '#05070d'}}>
    {SCENES.map(([a, b, S], i) => f >= a && f < b ? <S key={i} /> : null)}
    <Grain opacity={0.06} />
    {!silent && <Sfx />}
  </AbsoluteFill>;
};



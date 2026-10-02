import {interpolate, Easing, spring} from 'remotion';
export const FPS = 30;
export const W = 1920;
export const H = 1080;
// Voxel Revolution: 123.05 BPM, first downbeat at 0.1294 s (measured from the decoded track).
const BEAT = 0.4876190476;
const PHASE = 0.1294;
export const beat = (n: number) => Math.round((PHASE + n * BEAT) * FPS);
export const bar = (n: number) => beat(n * 4);
export const BEAT_FRAMES = BEAT * FPS;
export const TOTAL = 3810;

export const C = {
  bg: '#05070d',
  bg2: '#0b1322',
  ink: '#eef3fb',
  mute: '#8b97ab',
  dim: '#4b5568',
  emerald: '#34e3a0',
  emeraldDeep: '#0fa86e',
  red: '#ff4d5e',
  gold: '#ffcc4d',
  amber: '#ffae3b',
  cyan: '#55e6f0',
  violet: '#a77bff',
  line: 'rgba(160,190,230,0.14)',
};
export const DIFF = {EASY: '#3ddc84', MEDIUM: '#ffb02e', HARD: '#ff5a6a'} as const;
export const F = {
  display: '"Space Grotesk", "Inter", sans-serif',
  body: '"Inter", "Segoe UI", sans-serif',
  mono: '"JetBrains Mono", Consolas, monospace',
  pixel: '"Press Start 2P", monospace',
};

export const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;
export const ease = (f: number, a: number, b: number, from = 0, to = 1, e = Easing.bezier(0.16, 1, 0.3, 1)) =>
  interpolate(f, [a, b], [from, to], {...clamp, easing: e});
export const lin = (f: number, a: number, b: number, from = 0, to = 1) => interpolate(f, [a, b], [from, to], clamp);
export const pop = (f: number, start: number, fps = FPS, damping = 12, mass = 0.6) =>
  spring({frame: f - start, fps, config: {damping, mass, stiffness: 170}});
// Deterministic hash noise.
export const rnd = (i: number, s = 1) => {
  const x = Math.sin(i * 127.1 + s * 311.7) * 43758.5453;
  return x - Math.floor(x);
};

// Scene timeline (frames), locked to the music bars.
export const T = {
  intro: 0, computer: bar(2), ingameWorld: bar(5) + 12, drop: bar(7), ingame: bar(9), web: bar(11), run: bar(19), submit: bar(20),
  accepted: bar(21), rewards: bar(22), bank: bar(24), split: bar(26), diagrams: bar(27), bilingual: bar(29) + 8, daily: bar(30),
  review: bar(34), milestones: bar(36), build: bar(38), trophies: bar(40), engraving: bar(44), variants: bar(46), server: bar(48),
  recap: bar(54), outro: bar(58), end: TOTAL,
};


import React from 'react';
import {AbsoluteFill, Composition, useCurrentFrame} from 'remotion';
import './fonts';
import {C, F, TOTAL} from './theme';
import {Stage, Mark, Wordmark, Dust, Vignette, Grain} from './fx';
import {Scene3D, VoxelModel, Lights} from './Model3D';
import {Trailer} from './Trailer';

const ComputerIcon: React.FC = () => <AbsoluteFill>
  <Scene3D width={512} height={512} cam={[1.6, 1.5, 2.4]} target={[0, 0.42, 0]} fov={30}>
    <Lights /><VoxelModel id="computer" rotationY={Math.PI - 0.15} />
  </Scene3D>
</AbsoluteFill>;

const Hero: React.FC<{tall?: boolean}> = ({tall}) => {
  const h = tall ? 1080 : 820;
  return <AbsoluteFill>
    <Stage intensity={1.2} />
    <Dust n={40} seed={9} />
    <div style={{position: 'absolute', left: 900, top: tall ? 330 : 240, width: 1000, height: 560, borderRadius: '50%', background: `radial-gradient(closest-side, ${C.emerald}40, transparent)`, filter: 'blur(30px)'}} />
    <Scene3D width={1040} height={h} cam={[0.25, 1.25, tall ? 7.4 : 5.6]} target={[0.15, 0.62, 0]} fov={28} style={{left: 880}}>
      <Lights />
      <VoxelModel id="computer" position={[0, 0, 0]} rotationY={Math.PI - 0.5} scale={1.45} />
      <VoxelModel id="gold" position={[1.12, 0, 0.55]} rotationY={Math.PI - 0.75} scale={0.85} />
      <VoxelModel id="diamond" position={[-1.1, 0, 0.75]} rotationY={Math.PI + 0.55} scale={0.7} />
      <VoxelModel id="netherite" position={[1.7, 0, -1.0]} rotationY={Math.PI - 0.95} scale={0.78} />
    </Scene3D>
    <div style={{position: 'absolute', left: 110, top: tall ? 300 : 200}}>
      <div style={{display: 'flex', alignItems: 'center', gap: 30}}><Mark size={118} /><Wordmark size={128} /></div>
      <div style={{fontFamily: F.display, fontWeight: 500, fontSize: 48, color: '#dbe3ef', marginTop: 30, letterSpacing: -0.5}}>Learn algorithms in Minecraft.</div>
      <div style={{display: 'flex', gap: 14, marginTop: 40, flexWrap: 'wrap', width: 820}}>
        {['500 official problems', 'In-game + Web IDE', 'Real Java judge', 'Daily streaks', 'Trophies'].map(s =>
          <div key={s} style={{fontFamily: F.mono, fontSize: 23, color: C.ink, padding: '12px 20px', border: `1px solid ${C.emerald}55`, background: '#0c1b18aa', borderRadius: 999}}>{s}</div>)}
      </div>
      <div style={{fontFamily: F.mono, fontSize: 22, color: C.mute, marginTop: 34}}>0.1.0 Beta · Forge 1.20.1 + NeoForge 1.21.1</div>
    </div>
    <Vignette strength={0.6} />
    <Grain opacity={0.05} />
  </AbsoluteFill>;
};

export const RemotionRoot: React.FC = () => <>
  <Composition id="Trailer" component={Trailer} width={1920} height={1080} fps={30} durationInFrames={TOTAL} />
  <Composition id="TrailerSilent" component={Trailer} defaultProps={{silent: true}} width={1920} height={1080} fps={30} durationInFrames={TOTAL} />
  <Composition id="ComputerIcon" component={ComputerIcon} width={512} height={512} fps={30} durationInFrames={1} />
  <Composition id="Hero" component={Hero} width={1920} height={820} fps={30} durationInFrames={1} />
  <Composition id="Poster" component={Hero} defaultProps={{tall: true}} width={1920} height={1080} fps={30} durationInFrames={1} />
</>;


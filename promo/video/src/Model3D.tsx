import React, {useEffect, useMemo, useState} from 'react';
import {ThreeCanvas} from '@remotion/three';
import {continueRender, delayRender, staticFile, useCurrentFrame} from 'remotion';
import * as THREE from 'three';
import {useThree} from '@react-three/fiber';

type Face = {t: string; uv?: number[] | null; r?: number};
type El = {from: number[]; to: number[]; rot?: {origin: number[]; axis: 'x' | 'y' | 'z'; angle: number} | null; faces: Record<string, Face>};
type Models = Record<string, El[]>;

let modelsPromise: Promise<Models> | null = null;
const texCache = new Map<string, THREE.Texture>();
const loadModels = () => (modelsPromise ??= fetch(staticFile('models/models.json')).then(r => r.json()));
const loadTex = (name: string) => new Promise<THREE.Texture>((res, rej) => {
  if (texCache.has(name)) return res(texCache.get(name)!);
  new THREE.TextureLoader().load(staticFile(`tex/${name}.png`), t => {
    t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter; t.generateMipmaps = false;
    t.colorSpace = THREE.SRGBColorSpace; texCache.set(name, t); res(t);
  }, undefined, rej);
});

const faceQuad = (e: El, k: string): {p: number[][]; uv: number[]; n: number[]} => {
  const [x0, y0, z0] = e.from, [x1, y1, z1] = e.to;
  const q: Record<string, [number[][], number[], number[]]> = {
    north: [[[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]], [16 - x1, 16 - y1, 16 - x0, 16 - y0], [0, 0, -1]],
    south: [[[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]], [x0, 16 - y1, x1, 16 - y0], [0, 0, 1]],
    east: [[[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]], [16 - z1, 16 - y1, 16 - z0, 16 - y0], [1, 0, 0]],
    west: [[[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]], [z0, 16 - y1, z1, 16 - y0], [-1, 0, 0]],
    up: [[[x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1]], [x0, z0, x1, z1], [0, 1, 0]],
    down: [[[x0, y0, z1], [x1, y0, z1], [x1, y0, z0], [x0, y0, z0]], [x0, 16 - z1, x1, 16 - z0], [0, -1, 0]],
  };
  const [p, uv, n] = q[k];
  return {p, uv, n};
};

const buildGroup = (els: El[]) => {
  const byTex = new Map<string, {pos: number[]; uv: number[]; nor: number[]; idx: number[]}>();
  for (const e of els) {
    let m: THREE.Matrix4 | null = null;
    if (e.rot && e.rot.angle) {
      const o = new THREE.Vector3(...e.rot.origin);
      const axis = {x: new THREE.Vector3(1, 0, 0), y: new THREE.Vector3(0, 1, 0), z: new THREE.Vector3(0, 0, 1)}[e.rot.axis];
      m = new THREE.Matrix4().makeTranslation(o.x, o.y, o.z).multiply(new THREE.Matrix4().makeRotationAxis(axis, THREE.MathUtils.degToRad(e.rot.angle)))
        .multiply(new THREE.Matrix4().makeTranslation(-o.x, -o.y, -o.z));
    }
    for (const [k, f] of Object.entries(e.faces)) {
      const {p, uv: duv, n} = faceQuad(e, k);
      const [u0, v0, u1, v1] = f.uv ?? duv;
      let uvs = [[u0, v0], [u1, v0], [u1, v1], [u0, v1]];
      const shift = ((f.r ?? 0) / 90) % 4;
      for (let s = 0; s < shift; s++) uvs = [uvs[3], uvs[0], uvs[1], uvs[2]];
      const g = byTex.get(f.t) ?? {pos: [], uv: [], nor: [], idx: []};
      byTex.set(f.t, g);
      const base = g.pos.length / 3;
      const nv = new THREE.Vector3(...n);
      if (m) nv.transformDirection(m);
      p.forEach((pt, i) => {
        const v = new THREE.Vector3(...pt);
        if (m) v.applyMatrix4(m);
        g.pos.push((v.x - 8) / 16, v.y / 16, (v.z - 8) / 16);
        g.uv.push(uvs[i][0] / 16, 1 - uvs[i][1] / 16);
        g.nor.push(nv.x, nv.y, nv.z);
      });
      g.idx.push(base, base + 2, base + 1, base, base + 3, base + 2);
    }
  }
  return byTex;
};

export const VoxelModel: React.FC<{id: string; position?: [number, number, number]; rotationY?: number; scale?: number; emissive?: number}> =
  ({id, position = [0, 0, 0], rotationY = 0, scale = 1, emissive = 0}) => {
  const [data, setData] = useState<{groups: Map<string, any>; tex: Map<string, THREE.Texture>} | null>(null);
  const [handle] = useState(() => delayRender('model ' + id));
  useEffect(() => {
    loadModels().then(async ms => {
      const groups = buildGroup(ms[id]);
      const tex = new Map<string, THREE.Texture>();
      await Promise.all(Array.from(groups.keys()).map(async t => tex.set(t, await loadTex(t))));
      setData({groups, tex});
    });
  }, [id, handle]);
  const meshes = useMemo(() => {
    if (!data) return [];
    return Array.from(data.groups.entries()).map(([t, g]) => {
      const geo = new THREE.BufferGeometry();
      geo.setAttribute('position', new THREE.Float32BufferAttribute(g.pos, 3));
      geo.setAttribute('uv', new THREE.Float32BufferAttribute(g.uv, 2));
      geo.setAttribute('normal', new THREE.Float32BufferAttribute(g.nor, 3));
      geo.setIndex(g.idx);
      const metal = /gold|iron|netherite|diamond|copper|quartz/.test(t);
      const mat = new THREE.MeshStandardMaterial({map: data.tex.get(t)!, roughness: metal ? 0.35 : 0.8, metalness: metal ? 0.55 : 0.05,
        side: THREE.DoubleSide, transparent: false, alphaTest: 0.1,
        emissive: new THREE.Color(emissive > 0 && /concrete|lime|green|light_blue|orange|yellow/.test(t) ? '#ffffff' : '#000000'),
        emissiveMap: data.tex.get(t)!, emissiveIntensity: emissive});
      return {t, geo, mat};
    });
  }, [data, emissive]);
  const {invalidate, advance} = useThree();
  useEffect(() => {
    if (!meshes.length) return;
    invalidate();
    requestAnimationFrame(() => { advance(performance.now()); requestAnimationFrame(() => { advance(performance.now()); continueRender(handle); }); });
  }, [meshes, invalidate, advance, handle]);
  return <group position={position} rotation={[0, rotationY, 0]} scale={scale}>
    {meshes.map(m => <mesh key={m.t} geometry={m.geo} material={m.mat} />)}
  </group>;
};

export const Lights: React.FC<{rim?: string; key2?: string; intensity?: number}> = ({rim = '#34e3a0', key2 = '#7aa2ff', intensity = 1}) => (
  <>
    <ambientLight intensity={0.55 * intensity} />
    <hemisphereLight args={['#cfe3ff', '#0b0f18', 0.6 * intensity]} />
    <directionalLight position={[3, 5, 4]} intensity={2.3 * intensity} color="#fff3e2" />
    <directionalLight position={[-4, 2.5, -3]} intensity={2.4 * intensity} color={rim} />
    <directionalLight position={[-3, 1, 4]} intensity={0.8 * intensity} color={key2} />
  </>
);

export const Scene3D: React.FC<{width: number; height: number; fov?: number; cam: [number, number, number]; target?: [number, number, number]; children: React.ReactNode; style?: React.CSSProperties}> =
  ({width, height, fov = 28, cam, target = [0, 0.4, 0], children, style}) => {
  return <div style={{position: 'absolute', left: 0, top: 0, width, height, ...style}}><ThreeCanvas width={width} height={height} gl={{antialias: true, alpha: true, preserveDrawingBuffer: true}}
    camera={{fov, position: cam, near: 0.05, far: 100}}
    onCreated={({camera, gl}) => { camera.lookAt(...target); gl.toneMapping = THREE.ACESFilmicToneMapping; gl.outputColorSpace = THREE.SRGBColorSpace; }}>
    <CamLook cam={cam} target={target} />
    {children}
  </ThreeCanvas></div>;
};

// Keep the camera aimed every frame (the camera prop only applies at creation).
const CamLook: React.FC<{cam: [number, number, number]; target: [number, number, number]}> = ({cam, target}) => {
  const {camera} = useThree();
  useCurrentFrame();
  camera.position.set(...cam);
  camera.lookAt(...target);
  camera.updateProjectionMatrix();
  return null;
};





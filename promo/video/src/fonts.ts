import '@fontsource/space-grotesk/500.css';
import '@fontsource/space-grotesk/700.css';
import '@fontsource/inter/400.css';
import '@fontsource/inter/600.css';
import '@fontsource/jetbrains-mono/400.css';
import '@fontsource/jetbrains-mono/700.css';
import '@fontsource/press-start-2p/400.css';
import {continueRender, delayRender} from 'remotion';
if (typeof document !== 'undefined') {
  const h = delayRender('fonts');
  const fams = ['700 40px "Space Grotesk"', '500 40px "Space Grotesk"', '400 20px Inter', '600 20px Inter', '400 20px "JetBrains Mono"', '700 20px "JetBrains Mono"', '400 20px "Press Start 2P"'];
  Promise.all(fams.map(f => document.fonts.load(f))).then(() => continueRender(h)).catch(() => continueRender(h));
}

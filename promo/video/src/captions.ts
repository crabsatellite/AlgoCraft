import {T, beat} from './theme';
// English subtitle track for the trailer (on-screen text, no narration).
export const CAPTIONS: [number, number, string][] = [
  [beat(1), T.computer, 'Learn algorithms. Inside Minecraft.'],
  [T.computer, T.ingameWorld, 'Algorithm Computer: 6 iron ingots, 2 redstone dust, 1 glass.'],
  [T.ingameWorld, T.drop, 'Place it anywhere. Right-click to open your IDE.'],
  [T.drop, T.ingame, 'AlgoCraft: learn algorithms in Minecraft.'],
  [T.ingame, T.web, 'Code without leaving the game.'],
  [T.web, T.web + 120, 'Prefer a big screen? Open the Web IDE.'],
  [T.web + 120, T.run - 60, 'Write real Java.'],
  [T.run - 60, T.run, 'Run. Then Submit.'],
  [T.run, T.submit, 'Run checks the visible examples.'],
  [T.submit, T.accepted, 'Submit is judged by the server, hidden tests included.'],
  [T.accepted, T.rewards, 'Accepted: 7 / 7 test cases passed.'],
  [T.rewards, T.bank, 'First clears pay off. Loot scales with difficulty and your gear.'],
  [T.bank, T.diagrams, '500 official problems: 172 Easy, 205 Medium, 123 Hard.'],
  [T.diagrams, T.bilingual, 'Clear statements. Real diagrams.'],
  [T.bilingual, T.daily, 'English and Simplified Chinese.'],
  [T.daily, T.review, 'Daily supplies from day 3. Two netherite ingots on day 14, four every week from day 28.'],
  [T.review, T.build, 'Milestone rewards at 10, 25, 50, 100, 200 and 500 solves.'],
  [T.build, T.trophies, 'And every achievement becomes a trophy.'],
  [T.trophies, T.engraving, 'Five trophy tiers: bronze, silver, gold, diamond, netherite.'],
  [T.engraving, T.variants, 'Engraved with your name, the date and the feat.'],
  [T.variants, T.server, '23 achievements to collect.'],
  [T.server, T.recap, 'Built for servers: one shared bank, judged on the server.'],
  [T.recap, T.outro, 'In-game IDE, Web IDE, 500 problems, trophies.'],
  [T.outro, T.end, 'AlgoCraft for Forge 1.20.1 and NeoForge 1.21.1.'],
];


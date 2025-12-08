# AlgoCraft

A Minecraft Mod that adds a LeetCode-style algorithm problem solving platform.

## Features

- **Algorithm Computer Block**: Place it and right-click to open the interface.
- **Code Editor**: Write Java code to solve problems.
- **Problem Set**: Built-in problems and support for custom JSON problems in `.minecraft/algorithm_challenges/`.
- **Rewards**: Earn items for solving problems (Iron, Diamond, Netherite).

## How to add custom problems

Create a JSON file in `.minecraft/algorithm_challenges/` with the following format:

```json
{
  "id": "custom_1",
  "title": "My Custom Problem",
  "description": "Description here...",
  "difficulty": "medium",
  "initialCode": "class Solution { ... }",
  "examples": [{ "input": "...", "output": "..." }],
  "tests": [{ "input": "...", "output": "..." }]
}
```

## Technical Details

- Uses **Janino** for runtime Java compilation.
- Client-side execution for feedback, Server-side verification for rewards (simulated via packet for this prototype).

# Additional Resources:

Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

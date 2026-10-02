# AlgoCraft Textures

## Current Block Asset Policy

AlgoCraft ships no custom PNG textures for the Algorithm Computer or the trophies. Both use vanilla Minecraft block textures and build their detail from cuboids, which keeps them in Minecraft's material language and avoids the old AI-generated texture style.

The models are generated, so edit the scripts and regenerate rather than editing the JSON by hand:

- `scripts/generate_computer_model.py` writes `models/block/algorithm_computer.json`. The computer is a modern workstation: a widescreen monitor showing an IDE (file tree, coloured code lines, cursor and a green "accepted" status bar), a thin stand, a keyboard and mouse on a desk mat, and a tower with a green light strip. Its `COLLISION` boxes must match `AlgorithmComputerBlock.MODEL_BOXES`.
- `scripts/generate_trophy_models.py` writes the five `trophy_*_shape.json` tier models and the 23 achievement variants in `models/item/trophy_variants/`. Every tier is a cup trophy on a blackstone plinth; higher tiers add plinth studs, a lid, gems and crown points. Achievement marks live in `scripts/trophy_variant_marks.json` and sit on the plinth plaque, the cup front or the lid.

Computer palette: `gray_concrete` shell, `black_concrete` bezel and keyboard deck, `light_gray_concrete` stand, keys and mouse, `black_wool` desk mat, `iron_block` stand neck, `deepslate_tiles` vents and file tree, `black_stained_glass` screen, and `lime_concrete`, `light_blue_concrete` and `yellow_concrete` for code, status and accent lights. Green is the shared brand colour of the in-game IDE (`IdeTheme`) and the web IDE.

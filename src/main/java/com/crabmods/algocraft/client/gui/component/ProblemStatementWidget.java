package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.client.gui.modern.IdeTheme;
import com.crabmods.algocraft.logic.Problem;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProblemStatementWidget extends AbstractWidget {
    private static final int BACKGROUND_COLOR = IdeTheme.PANEL;
    private static final int BORDER_COLOR = IdeTheme.BORDER;
    private static final int TITLE_COLOR = IdeTheme.TEXT;
    private static final int META_COLOR = IdeTheme.TEXT_MUTED;
    private static final int TEXT_COLOR = 0xFFD5DDE5;
    private static final int HEADING_COLOR = IdeTheme.INFO;
    private static final int IMAGE_BORDER_COLOR = IdeTheme.BORDER;
    private static final int IMAGE_BACKGROUND_COLOR = IdeTheme.BACKGROUND;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 40;
    private static final int BLOCK_GAP = 6;
    private static final int IMAGE_MAX_HEIGHT = 160;
    private static final int IMAGE_FRAME_PADDING = 2;
    private static final int SCROLLBAR_RESERVE = 8;

    private final Font font;
    private final ProblemImageTextureCache imageTextures = new ProblemImageTextureCache();
    private final List<StatementBlock> blocks = new ArrayList<>();
    private Problem problem;
    private int scrollY;
    private int contentHeight;

    public ProblemStatementWidget(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
    }

    public void setProblem(Problem problem) {
        this.imageTextures.clear();
        this.problem = problem;
        this.scrollY = 0;
        this.rebuildBlocks();
    }

    public void releaseImages() {
        this.imageTextures.clear();
    }

    public void setBounds(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        this.rebuildBlocks();
    }

    public int getLoadedImageBlockCountForSmokeTest() {
        return countBlocks(BlockType.IMAGE);
    }

    public int getMissingImageBlockCountForSmokeTest() {
        return countBlocks(BlockType.MISSING);
    }

    public int getVisibleLoadedImageBlockCountForSmokeTest() {
        int viewportHeight = ProblemStatementLayout.viewportHeight(this.height, PADDING, HEADER_HEIGHT);
        int blockY = 0;
        int visibleImages = 0;
        for (StatementBlock block : this.blocks) {
            if (block.type() == BlockType.IMAGE
                    && ProblemStatementLayout.blockVisibleAfterScroll(blockY, block.height(), this.scrollY, viewportHeight)) {
                visibleImages++;
            }
            blockY += block.height();
        }
        return visibleImages;
    }

    private int countBlocks(BlockType type) {
        int count = 0;
        for (StatementBlock block : this.blocks) {
            if (block.type() == type) {
                count++;
            }
        }
        return count;
    }

    private void rebuildBlocks() {
        this.blocks.clear();
        this.contentHeight = 0;
        if (this.problem == null || this.width <= PADDING * 2) {
            return;
        }

        int contentWidth = contentWidth();
        appendVisuals(contentWidth);
        appendDescription(contentWidth);
        this.contentHeight = this.blocks.stream().mapToInt(StatementBlock::height).sum();
        clampScroll();
    }

    private void appendVisuals(int contentWidth) {
        int imageMaxWidth = ProblemStatementLayout.framedImageMaxWidth(contentWidth, IMAGE_FRAME_PADDING);
        List<Problem.Visual> visuals = ProblemDisplayText.visuals(problem);
        for (ProblemStatementDocument.VisualBlock visualBlock : ProblemStatementDocument.buildVisualBlocks(problem, visuals, imageMaxWidth, IMAGE_MAX_HEIGHT, imageTextures)) {
            Problem.Visual visual = visualBlock.visual();
            if (visualBlock.type() == ProblemStatementDocument.VisualBlockType.IMAGE) {
                Optional<LoadedImage> loadedImage = imageTextures.get(visualBlock.path());
                if (loadedImage.isPresent()) {
                    this.blocks.add(StatementBlock.image(visualBlock.path(), loadedImage.get(), visualBlock.drawWidth(), visualBlock.drawHeight()));
                } else {
                    this.blocks.add(StatementBlock.missing(Component.translatable("algocraft.gui.image_unavailable", visual.file()).getString(), contentWidth));
                }
            } else if (visualBlock.missingReason() == ProblemStatementDocument.MissingReason.NOT_FOUND) {
                this.blocks.add(StatementBlock.missing(Component.translatable("algocraft.gui.image_missing", visual.file()).getString(), contentWidth));
            } else {
                this.blocks.add(StatementBlock.missing(Component.translatable("algocraft.gui.image_unavailable", visual.file()).getString(), contentWidth));
            }

            if (!visual.caption().isBlank()) {
                for (FormattedCharSequence line : font.split(Component.literal(visual.caption()), contentWidth)) {
                    this.blocks.add(StatementBlock.caption(line));
                }
            }
            this.blocks.add(StatementBlock.spacer(BLOCK_GAP));
        }
    }

    private void appendDescription(int contentWidth) {
        String normalized = normalizeDescription(ProblemDisplayText.description(this.problem));
        String[] paragraphs = normalized.split("\n", -1);
        String title = ProblemDisplayText.title(this.problem).trim();
        boolean sawContent = false;
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                if (sawContent) {
                    this.blocks.add(StatementBlock.spacer(LINE_HEIGHT));
                }
                continue;
            }
            String heading = markdownHeading(paragraph);
            if (heading != null) {
                // The panel header already shows the title, so skip a leading "# Title" line.
                if (!sawContent && heading.equalsIgnoreCase(title)) {
                    continue;
                }
                if (sawContent) {
                    this.blocks.add(StatementBlock.spacer(3));
                }
                for (FormattedCharSequence line : this.font.split(Component.literal(heading), contentWidth)) {
                    this.blocks.add(StatementBlock.heading(line));
                }
                this.blocks.add(StatementBlock.spacer(2));
                sawContent = true;
                continue;
            }
            sawContent = true;
            for (FormattedCharSequence line : this.font.split(Component.literal(bulletText(paragraph)), contentWidth)) {
                this.blocks.add(StatementBlock.text(line));
            }
        }
    }

    private static String markdownHeading(String paragraph) {
        String trimmed = paragraph.trim();
        int level = 0;
        while (level < trimmed.length() && level < 6 && trimmed.charAt(level) == '#') {
            level++;
        }
        if (level == 0 || level >= trimmed.length() || trimmed.charAt(level) != ' ') {
            return null;
        }
        String text = trimmed.substring(level).trim();
        return text.isEmpty() ? null : text;
    }

    private static String bulletText(String paragraph) {
        int indent = 0;
        while (indent < paragraph.length() && paragraph.charAt(indent) == ' ') {
            indent++;
        }
        String rest = paragraph.substring(indent);
        if (rest.startsWith("- ") || rest.startsWith("* ")) {
            return paragraph.substring(0, indent) + "\u2022 " + rest.substring(2);
        }
        return paragraph;
    }

    private int contentWidth() {
        return ProblemStatementLayout.contentWidth(this.width, PADDING, SCROLLBAR_RESERVE);
    }

    private static String normalizeDescription(String description) {
        return description
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("```", "")
            .replace("**", "")
            .replace("`", "");
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IdeTheme.frame(guiGraphics, getX(), getY(), width, height, BACKGROUND_COLOR, isFocused() ? IdeTheme.INFO : BORDER_COLOR);

        if (problem == null) {
            guiGraphics.drawString(font, Component.translatable("algocraft.gui.no_problem_selected"), getX() + PADDING, getY() + PADDING, META_COLOR, false);
            return;
        }

        int x = getX() + PADDING;
        int y = getY() + PADDING;
        int right = getX() + width - PADDING;
        String title = font.plainSubstrByWidth(ProblemDisplayText.title(problem), Math.max(20, width - PADDING * 2));
        guiGraphics.drawString(font, title, x, y, TITLE_COLOR, false);

        String difficulty = problem.getDifficulty() == null ? "" : problem.getDifficulty();
        int badgeWidth = difficulty.isEmpty() ? 0 : IdeTheme.pill(guiGraphics, font, difficulty, x, y + 12, IdeTheme.difficultyColor(difficulty)) + 6;

        String meta = problem.getTags().isEmpty()
            ? Component.translatable("algocraft.gui.problem_tests", problem.getTotalTestCount()).getString()
            : String.join("  \u00b7  ", problem.getTags());
        int metaX = x + badgeWidth;
        if (right - metaX < 40) {
            metaX = x;
            guiGraphics.drawString(font, IdeTheme.ellipsize(font, meta, Math.max(20, right - metaX)), metaX, y + 25, META_COLOR, false);
        } else {
            guiGraphics.drawString(font, IdeTheme.ellipsize(font, meta, Math.max(20, right - metaX)), metaX, y + 14, META_COLOR, false);
        }
        guiGraphics.fill(getX() + 1, y + HEADER_HEIGHT - 5, getX() + width - 1, y + HEADER_HEIGHT - 4, BORDER_COLOR);

        int contentTop = y + HEADER_HEIGHT;
        int contentBottom = getY() + height - PADDING;
        int viewportHeight = Math.max(1, contentBottom - contentTop);
        guiGraphics.enableScissor(getX() + 1, contentTop - 2, getX() + width - 1, contentBottom);

        int blockY = contentTop - scrollY;
        int textX = getX() + PADDING;
        int blockWidth = contentWidth();
        for (StatementBlock block : blocks) {
            int blockBottom = blockY + block.height();
            if (blockBottom >= contentTop && blockY <= contentBottom) {
                renderBlock(guiGraphics, block, textX, blockY, blockWidth);
            }
            blockY = blockBottom;
        }

        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics, contentTop, contentBottom, viewportHeight);
    }

    private void renderBlock(GuiGraphics guiGraphics, StatementBlock block, int x, int y, int blockWidth) {
        switch (block.type()) {
            case TEXT -> guiGraphics.drawString(font, block.line(), x, y, TEXT_COLOR, false);
            case HEADING -> guiGraphics.drawString(font, block.line(), x, y, HEADING_COLOR, false);
            case CAPTION -> guiGraphics.drawString(font, block.line(), x, y, META_COLOR, false);
            case IMAGE -> renderImage(guiGraphics, block, x, y, blockWidth);
            case MISSING -> {
                guiGraphics.fill(x, y, x + blockWidth, y + block.height(), IMAGE_BACKGROUND_COLOR);
                guiGraphics.renderOutline(x, y, blockWidth, block.height(), IMAGE_BORDER_COLOR);
                String text = font.plainSubstrByWidth(block.message(), Math.max(20, blockWidth - 8));
                guiGraphics.drawString(font, text, x + 4, y + 8, META_COLOR, false);
            }
            case SPACER -> {
            }
        }
    }

    private void renderImage(GuiGraphics guiGraphics, StatementBlock block, int x, int y, int blockWidth) {
        int imageX = x + Math.max(0, (blockWidth - block.drawWidth()) / 2);
        guiGraphics.fill(imageX - 2, y - 2, imageX + block.drawWidth() + 2, y + block.drawHeight() + 2, IMAGE_BACKGROUND_COLOR);
        guiGraphics.renderOutline(imageX - 2, y - 2, block.drawWidth() + 4, block.drawHeight() + 4, IMAGE_BORDER_COLOR);
        LoadedImage image = block.image();
        guiGraphics.blit(
                image.location(),
                imageX,
                y,
                block.drawWidth(),
                block.drawHeight(),
                0.0F,
                0.0F,
                image.width(),
                image.height(),
                image.width(),
                image.height()
        );
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int contentTop, int contentBottom, int viewportHeight) {
        if (contentHeight <= viewportHeight) {
            return;
        }
        int trackX = getX() + width - 5;
        int trackHeight = contentBottom - contentTop;
        int thumbHeight = Math.max(10, trackHeight * viewportHeight / contentHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int thumbY = contentTop + (trackHeight - thumbHeight) * scrollY / maxScroll;
        IdeTheme.scrollbar(guiGraphics, trackX, contentTop, contentBottom, thumbY, thumbY + thumbHeight, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollY -= (int) Math.signum(scrollY) * 30;
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int viewportHeight = ProblemStatementLayout.viewportHeight(height, PADDING, HEADER_HEIGHT);
        this.scrollY = ProblemStatementLayout.clampScroll(this.scrollY, contentHeight, viewportHeight);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private enum BlockType {
        TEXT,
        HEADING,
        CAPTION,
        IMAGE,
        MISSING,
        SPACER
    }

    private record StatementBlock(
            BlockType type,
            FormattedCharSequence line,
            String message,
            Path path,
            LoadedImage image,
            int drawWidth,
            int drawHeight,
            int height
    ) {
        static StatementBlock text(FormattedCharSequence line) {
            return new StatementBlock(BlockType.TEXT, line, "", null, null, 0, 0, LINE_HEIGHT);
        }

        static StatementBlock heading(FormattedCharSequence line) {
            return new StatementBlock(BlockType.HEADING, line, "", null, null, 0, 0, LINE_HEIGHT + 1);
        }

        static StatementBlock caption(FormattedCharSequence line) {
            return new StatementBlock(BlockType.CAPTION, line, "", null, null, 0, 0, LINE_HEIGHT);
        }

        static StatementBlock image(Path path, LoadedImage image, int width, int height) {
            return new StatementBlock(BlockType.IMAGE, FormattedCharSequence.EMPTY, "", path, image, width, height, height + BLOCK_GAP);
        }

        static StatementBlock missing(String message, int width) {
            return new StatementBlock(BlockType.MISSING, FormattedCharSequence.EMPTY, message, null, null, width, 26, 30);
        }

        static StatementBlock spacer(int height) {
            return new StatementBlock(BlockType.SPACER, FormattedCharSequence.EMPTY, "", null, null, 0, 0, height);
        }
    }

    private record LoadedImage(ResourceLocation location, int width, int height) {
    }

    private static final class ProblemImageTextureCache implements ProblemStatementDocument.ImageProbe {
        private final Map<Path, Optional<LoadedImage>> cache = new HashMap<>();

        void clear() {
            for (Optional<LoadedImage> image : cache.values()) {
                image.ifPresent(loaded -> Minecraft.getInstance().getTextureManager().release(loaded.location()));
            }
            cache.clear();
        }

        @Override
        public Optional<ProblemStatementDocument.ImageInfo> read(Path path) {
            return get(path).map(image -> new ProblemStatementDocument.ImageInfo(image.width(), image.height()));
        }

        Optional<LoadedImage> get(Path path) {
            Path normalized = path.toAbsolutePath().normalize();
            return cache.computeIfAbsent(normalized, this::load);
        }

        private Optional<LoadedImage> load(Path path) {
            NativeImage nativeImage = null;
            try (InputStream input = Files.newInputStream(path)) {
                nativeImage = NativeImage.read(input);
                int width = nativeImage.getWidth();
                int height = nativeImage.getHeight();
                DynamicTexture texture = new DynamicTexture(nativeImage);
                nativeImage = null;
                ResourceLocation location = Minecraft.getInstance()
                        .getTextureManager()
                        .register("algocraft_problem_image", texture);
                return Optional.of(new LoadedImage(location, width, height));
            } catch (IOException | RuntimeException e) {
                return Optional.empty();
            } finally {
                if (nativeImage != null) {
                    nativeImage.close();
                }
            }
        }
    }
}

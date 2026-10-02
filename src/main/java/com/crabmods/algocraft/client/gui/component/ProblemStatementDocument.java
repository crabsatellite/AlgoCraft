package com.crabmods.algocraft.client.gui.component;

import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemAssetResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProblemStatementDocument {
    private static final int IMAGE_BLOCK_GAP = 6;
    private static final int MISSING_IMAGE_HEIGHT = 30;

    private ProblemStatementDocument() {
    }

    public static List<VisualBlock> buildVisualBlocks(Problem problem, int contentWidth, int imageMaxHeight, ImageProbe imageProbe) {
        return buildVisualBlocks(problem, problem == null ? List.of() : problem.getVisuals(), contentWidth, imageMaxHeight, imageProbe);
    }

    public static List<VisualBlock> buildVisualBlocks(Problem problem, List<Problem.Visual> visuals,
                                                      int contentWidth, int imageMaxHeight, ImageProbe imageProbe) {
        if (problem == null || contentWidth <= 0 || imageMaxHeight <= 0) {
            return List.of();
        }

        List<VisualBlock> blocks = new ArrayList<>();
        for (Problem.Visual visual : visuals) {
            Optional<Path> path = ProblemAssetResolver.resolveImage(problem, visual);
            if (path.isEmpty()) {
                blocks.add(VisualBlock.missing(visual, MissingReason.NOT_FOUND));
                continue;
            }

            Optional<ImageInfo> imageInfo = imageProbe.read(path.get());
            if (imageInfo.isEmpty()) {
                blocks.add(VisualBlock.missing(visual, MissingReason.UNREADABLE));
                continue;
            }

            ProblemStatementLayout.ScaledImage scaled = ProblemStatementLayout.scaleImage(
                    imageInfo.get().width(),
                    imageInfo.get().height(),
                    contentWidth,
                    imageMaxHeight
            );
            blocks.add(VisualBlock.image(visual, path.get(), scaled.width(), scaled.height()));
        }

        return List.copyOf(blocks);
    }

    public interface ImageProbe {
        Optional<ImageInfo> read(Path path);
    }

    public record ImageInfo(int width, int height) {
        public ImageInfo {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Image dimensions must be positive");
            }
        }
    }

    public enum VisualBlockType {
        IMAGE,
        MISSING
    }

    public enum MissingReason {
        NOT_FOUND,
        UNREADABLE
    }

    public record VisualBlock(
            VisualBlockType type,
            Problem.Visual visual,
            Path path,
            int drawWidth,
            int drawHeight,
            int height,
            MissingReason missingReason
    ) {
        static VisualBlock image(Problem.Visual visual, Path path, int drawWidth, int drawHeight) {
            return new VisualBlock(VisualBlockType.IMAGE, visual, path, drawWidth, drawHeight, drawHeight + IMAGE_BLOCK_GAP, null);
        }

        static VisualBlock missing(Problem.Visual visual, MissingReason reason) {
            return new VisualBlock(VisualBlockType.MISSING, visual, null, 0, 0, MISSING_IMAGE_HEIGHT, reason);
        }
    }
}

package com.crabmods.algocraft.questionbank;

import com.crabmods.algocraft.client.gui.component.ProblemStatementDocument;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemAssetResolver;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialProblemStatementRenderTest {
    private static final Gson GSON = new Gson();
    private static final Path QUESTION_BANK = Paths.get(System.getProperty("user.dir"), "question_bank", "official");
    private static final Pattern PROBLEM_FILE = Pattern.compile("p(\\d+)\\.json");
    private static final int IMAGE_MAX_HEIGHT = 160;
    private static final int[] CONTENT_WIDTHS = {180, 260, 320};
    private static final int MIN_IMAGE_WIDTH = 256;
    private static final int MIN_IMAGE_HEIGHT = 144;
    private static final int MIN_RENDERED_IMAGE_WIDTH = 160;
    private static final int MIN_RENDERED_IMAGE_HEIGHT = 90;
    private static final int MIN_SAMPLED_UNIQUE_COLORS = 24;
    private static final int MIN_LUMINANCE_RANGE = 80;
    private static final double MIN_OPAQUE_PIXEL_RATIO = 0.95;

    @Test
    void allOfficialProblemDiagramsResolveDecodeAndProduceRenderableBlocks() throws IOException {
        CachedImageProbe imageProbe = new CachedImageProbe();
        int visualCount = 0;

        for (Path problemFile : listProblemFiles()) {
            Problem problem = GSON.fromJson(Files.readString(problemFile, StandardCharsets.UTF_8), Problem.class);
            problem.setAssetBaseDir(QUESTION_BANK);
            visualCount += problem.getVisuals().size();

            for (Problem.Visual visual : problem.getVisuals()) {
                Optional<Path> imagePath = ProblemAssetResolver.resolveImage(problem, visual);
                assertTrue(imagePath.isPresent(), problemFile.getFileName() + " should resolve image " + visual.file());
                Optional<ProblemStatementDocument.ImageInfo> imageInfo = imageProbe.read(imagePath.get());
                assertTrue(imageInfo.isPresent(), problemFile.getFileName() + " should decode image " + visual.file());
                assertImageQuality(problemFile, visual, imageInfo.get(), imageProbe.stats(imagePath.get()).orElseThrow());
            }

            for (int contentWidth : CONTENT_WIDTHS) {
                List<ProblemStatementDocument.VisualBlock> blocks = ProblemStatementDocument.buildVisualBlocks(
                        problem,
                        contentWidth,
                        IMAGE_MAX_HEIGHT,
                        imageProbe
                );

                long imageBlocks = blocks.stream()
                        .filter(block -> block.type() == ProblemStatementDocument.VisualBlockType.IMAGE)
                        .count();
                assertEquals(problem.getVisuals().size(), imageBlocks,
                        problemFile.getFileName() + " should produce an IMAGE block for every diagram at width " + contentWidth);
                assertFalse(blocks.stream().anyMatch(block -> block.type() == ProblemStatementDocument.VisualBlockType.MISSING),
                        problemFile.getFileName() + " should not produce missing-image blocks at width " + contentWidth);

                for (ProblemStatementDocument.VisualBlock block : blocks) {
                    assertTrue(block.drawWidth() > 0, problemFile.getFileName() + " image block width should be positive");
                    assertTrue(block.drawHeight() > 0, problemFile.getFileName() + " image block height should be positive");
                    assertTrue(block.drawWidth() <= contentWidth,
                            problemFile.getFileName() + " image block should fit content width " + contentWidth);
                    assertTrue(block.drawHeight() <= IMAGE_MAX_HEIGHT,
                            problemFile.getFileName() + " image block should fit max image height");
                    assertTrue(block.drawWidth() >= MIN_RENDERED_IMAGE_WIDTH,
                            problemFile.getFileName() + " image block should remain readable at width " + contentWidth);
                    assertTrue(block.drawHeight() >= MIN_RENDERED_IMAGE_HEIGHT,
                            problemFile.getFileName() + " image block should remain tall enough to read at width " + contentWidth);
                    assertTrue(block.height() >= block.drawHeight(),
                            problemFile.getFileName() + " image block layout height should include the rendered image");
                }
            }
        }

        assertTrue(visualCount > 0, "official problem bank should contain renderable problem diagrams");
    }

    private static void assertImageQuality(Path problemFile, Problem.Visual visual,
                                           ProblemStatementDocument.ImageInfo imageInfo,
                                           PixelStats stats) {
        String context = problemFile.getFileName() + " image " + visual.file();
        assertTrue(imageInfo.width() >= MIN_IMAGE_WIDTH, context + " should be wide enough to read in the IDE");
        assertTrue(imageInfo.height() >= MIN_IMAGE_HEIGHT, context + " should be tall enough to read in the IDE");
        assertTrue(stats.opaquePixelRatio() >= MIN_OPAQUE_PIXEL_RATIO,
                context + " should not be mostly transparent or empty");
        assertTrue(stats.uniqueColors() >= MIN_SAMPLED_UNIQUE_COLORS,
                context + " should have enough sampled color detail to reject blank/flat prompt art");
        assertTrue(stats.luminanceRange() >= MIN_LUMINANCE_RANGE,
                context + " should have enough contrast for Minecraft GUI readability");
    }

    private static List<Path> listProblemFiles() throws IOException {
        try (Stream<Path> stream = Files.list(QUESTION_BANK)) {
            return stream
                    .filter(path -> PROBLEM_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(OfficialProblemStatementRenderTest::problemNumber))
                    .toList();
        }
    }

    private static int problemNumber(Path path) {
        Matcher matcher = PROBLEM_FILE.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static final class CachedImageProbe implements ProblemStatementDocument.ImageProbe {
        private final Map<Path, Optional<DecodedImage>> cache = new HashMap<>();

        @Override
        public Optional<ProblemStatementDocument.ImageInfo> read(Path path) {
            return decodeCached(path).map(DecodedImage::info);
        }

        Optional<PixelStats> stats(Path path) {
            return decodeCached(path).map(DecodedImage::stats);
        }

        private Optional<DecodedImage> decodeCached(Path path) {
            return cache.computeIfAbsent(path.toAbsolutePath().normalize(), this::decode);
        }

        private Optional<DecodedImage> decode(Path path) {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                if (image == null) {
                    return Optional.empty();
                }
                ProblemStatementDocument.ImageInfo info =
                        new ProblemStatementDocument.ImageInfo(image.getWidth(), image.getHeight());
                return Optional.of(new DecodedImage(info, PixelStats.analyze(image)));
            } catch (IOException | RuntimeException e) {
                return Optional.empty();
            }
        }
    }

    private record DecodedImage(ProblemStatementDocument.ImageInfo info, PixelStats stats) {}

    private record PixelStats(int uniqueColors, int luminanceRange, double opaquePixelRatio) {
        static PixelStats analyze(BufferedImage image) {
            Set<Integer> colors = new HashSet<>();
            int minLuminance = 255;
            int maxLuminance = 0;
            int opaquePixels = 0;
            int sampledPixels = 0;
            int stepX = Math.max(1, image.getWidth() / 80);
            int stepY = Math.max(1, image.getHeight() / 80);

            for (int y = 0; y < image.getHeight(); y += stepY) {
                for (int x = 0; x < image.getWidth(); x += stepX) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    int red = (argb >>> 16) & 0xFF;
                    int green = (argb >>> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    int luminance = (int) Math.round(0.2126 * red + 0.7152 * green + 0.0722 * blue);

                    sampledPixels++;
                    if (alpha > 0) {
                        opaquePixels++;
                    }
                    minLuminance = Math.min(minLuminance, luminance);
                    maxLuminance = Math.max(maxLuminance, luminance);
                    colors.add(argb);
                }
            }

            double opaqueRatio = sampledPixels == 0 ? 0.0 : opaquePixels / (double) sampledPixels;
            return new PixelStats(colors.size(), maxLuminance - minLuminance, opaqueRatio);
        }
    }
}

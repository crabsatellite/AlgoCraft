package com.crabmods.algocraft.logic.repo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryManagerTest {
    @TempDir
    Path gameDir;

    @BeforeEach
    void setUp() {
        RepositoryManager.resetForTest();
        RepositoryManager.init(gameDir);
    }

    @AfterEach
    void tearDown() {
        RepositoryManager.resetForTest();
    }

    @Test
    void failedInstallDoesNotPersistNewRepositoryMetadata() {
        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "Broken Course",
                        "https://example.test/broken",
                        uri -> {
                            throw new IOException("offline");
                        },
                        123L
                ).join());

        assertTrue(error.getCause().getMessage().contains("offline"));
        assertTrue(RepositoryManager.getRepositories().isEmpty(),
                "failed repository downloads must not leave a selectable metadata entry");
        assertFalse(Files.exists(configFile()),
                "repositories.json should not be created by a failed first install");
    }

    @Test
    void installRejectsFileRepositoryUrlBeforeRemoteClientIsCalled() {
        assertRejectedBeforeRemoteFetch("file:///tmp/algocraft-course");
    }

    @Test
    void installRejectsRelativeRepositoryUrlBeforeRemoteClientIsCalled() {
        assertRejectedBeforeRemoteFetch("question_bank/official");
    }

    @Test
    void installRejectsNamesWithoutStableSlugBeforeRemoteClientIsCalled() {
        for (String name : List.of("", "   ", "!!!")) {
            AtomicInteger requests = new AtomicInteger();

            CompletionException error = assertThrows(CompletionException.class,
                    () -> RepositoryManager.installOrUpdateRepository(
                            name,
                            "https://example.test/unstable-name",
                            uri -> {
                                requests.incrementAndGet();
                                throw new IOException("remote client should not be called");
                            },
                            777L
                    ).join());

            assertTrue(error.getCause().getMessage().contains("letter or digit"),
                    "repository names without a stable slug should fail with an explicit name error: "
                            + error.getCause().getMessage());
            assertEquals(0, requests.get(),
                    "repository names without a stable slug must fail before remote fetch: " + name);
        }

        assertTrue(RepositoryManager.getRepositories().isEmpty(),
                "invalid repository names must not leave selectable metadata");
        assertFalse(Files.exists(configFile()),
                "invalid repository names must not create repositories.json");
        assertThrows(IllegalArgumentException.class,
                () -> RepositoryManager.getRepositoryDir(new RepositoryMetadata("!!!", "https://example.test")));
    }

    @Test
    void installRejectsSystemRepositoryNamesBeforeRemoteClientIsCalled() throws IOException {
        Path officialCacheMarker = gameDir.resolve("algorithm_challenges")
                .resolve("repos")
                .resolve("official")
                .resolve("cached-official-problem.txt");
        Files.writeString(officialCacheMarker, "must survive", StandardCharsets.UTF_8);

        for (String name : List.of("Official", "official!", "User", "Built-in")) {
            AtomicInteger requests = new AtomicInteger();

            CompletionException error = assertThrows(CompletionException.class,
                    () -> RepositoryManager.installOrUpdateRepository(
                            name,
                            "https://example.test/" + name.toLowerCase().replaceAll("[^a-z0-9]+", "-"),
                            uri -> {
                                requests.incrementAndGet();
                                throw new IOException("remote client should not be called");
                            },
                            777L
                    ).join());

            assertTrue(error.getCause().getMessage().contains("reserved"),
                    "reserved repository names should fail with an explicit namespace error: "
                            + error.getCause().getMessage());
            assertEquals(0, requests.get(), "reserved repository names must fail before remote fetch: " + name);
        }

        assertTrue(Files.exists(officialCacheMarker),
                "a custom repository named like Official must not touch the official offline cache directory");
        assertTrue(RepositoryManager.getRepositories().isEmpty(),
                "reserved repository names must not leave selectable metadata");
        assertFalse(Files.exists(configFile()),
                "reserved repository names must not create repositories.json");
        assertThrows(IllegalArgumentException.class,
                () -> RepositoryManager.getRepositoryDir(new RepositoryMetadata("Official", "https://example.test")));
    }

    @Test
    void initDropsReservedRepositoryMetadataFromExistingConfig() throws IOException {
        RepositoryManager.resetForTest();
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(), """
                [
                  {"name": "Official", "url": "https://example.test/not-official", "lastUpdated": 1},
                  {"name": "User", "url": "https://example.test/not-user", "lastUpdated": 2},
                  {"name": "Built-in", "url": "https://example.test/not-builtin", "lastUpdated": 3},
                  {"name": "!!!", "url": "https://example.test/not-stable", "lastUpdated": 4},
                  {"name": "Course", "url": "https://example.test/course", "lastUpdated": 4}
                ]
                """, StandardCharsets.UTF_8);

        RepositoryManager.init(gameDir);

        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        assertEquals(1, repositories.size());
        assertEquals("Course", repositories.get(0).name);
        assertEquals("https://example.test/course", repositories.get(0).url);
        assertEquals(4L, repositories.get(0).lastUpdated);

        String cleanedConfig = Files.readString(configFile(), StandardCharsets.UTF_8);
        assertFalse(cleanedConfig.contains("\"Official\""),
                "startup should clean stale metadata that would alias the official repository namespace");
        assertFalse(cleanedConfig.contains("\"User\""),
                "startup should clean stale metadata that would alias the local user repository namespace");
        assertFalse(cleanedConfig.contains("\"Built-in\""),
                "startup should clean stale metadata that would alias the built-in repository namespace");
        assertFalse(cleanedConfig.contains("\"!!!\""),
                "startup should clean stale metadata that cannot map to a stable repository namespace");
        assertTrue(cleanedConfig.contains("\"Course\""),
                "startup should preserve valid custom repository metadata while dropping reserved entries");
    }

    @Test
    void successfulInstallPersistsMetadataAfterRepositoryValidation() throws IOException {
        FakeRemote remote = new FakeRemote("https://example.test/course");
        remote.publishProblem(1, "First Course Problem");

        RepositoryMetadata installed = RepositoryManager.installOrUpdateRepository(
                "Course",
                remote.baseUrl,
                remote,
                111L
        ).join();

        assertEquals("Course", installed.name);
        assertEquals(remote.baseUrl, installed.url);
        assertEquals(111L, installed.lastUpdated);

        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        assertEquals(1, repositories.size());
        assertEquals("Course", repositories.get(0).name);
        assertEquals(remote.baseUrl, repositories.get(0).url);
        assertEquals(111L, repositories.get(0).lastUpdated);

        String config = Files.readString(configFile(), StandardCharsets.UTF_8);
        assertTrue(config.contains("\"name\": \"Course\""));
        assertTrue(config.contains("\"lastUpdated\": 111"));
    }

    @Test
    void successfulInstallAcceptsUnicodeRepositoryNamesWithStableCacheDirectory() throws IOException {
        String repositoryName = "\u6570\u636e\u7ed3\u6784";
        FakeRemote remote = new FakeRemote("https://example.test/unicode-course");
        remote.publishProblem(1, "Unicode Course Problem");

        RepositoryMetadata installed = RepositoryManager.installOrUpdateRepository(
                repositoryName,
                remote.baseUrl,
                remote,
                112L
        ).join();

        assertEquals(repositoryName, installed.name);
        assertTrue(Files.exists(RepositoryManager.getRepositoryDir(installed).toPath().resolve("p1.json")),
                "readable Unicode repository names should receive their own stable local cache directory");
    }

    @Test
    void failedInstallAfterDownloadedContentValidationDoesNotPersistMetadataOrCache() throws IOException {
        FakeRemote remote = new FakeRemote("https://example.test/bad-course");
        remote.publishProblem(1, "Bad Course Problem");
        remote.replaceProblemBody(1, "{not valid json");
        Path repositoryDir = RepositoryManager.getRepositoryDir(
                new RepositoryMetadata("Bad Course", remote.baseUrl)).toPath();

        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "Bad Course",
                        remote.baseUrl,
                        remote,
                        113L
                ).join());

        assertTrue(error.getCause().getMessage().contains("Invalid official problem JSON")
                        && error.getCause().getMessage().contains("p1.json"),
                "content validation failures should name the invalid downloaded problem");
        assertTrue(RepositoryManager.getRepositories().isEmpty(),
                "failed content validation must not leave selectable metadata");
        assertFalse(Files.exists(configFile()),
                "repositories.json should not be created after content validation failure");
        assertFalse(Files.exists(repositoryDir),
                "failed content validation must not leave a repository cache directory");
    }

    @Test
    void githubTreeRepositoryUrlDownloadsFromRawContentButPersistsUserUrl() throws IOException {
        String githubUrl = "https://github.com/example/course/tree/main/question_bank/official";
        FakeRemote remote = new FakeRemote("https://raw.githubusercontent.com/example/course/main/question_bank/official");
        remote.publishProblem(1, "GitHub Course Problem");

        RepositoryMetadata installed = RepositoryManager.installOrUpdateRepository(
                "GitHub Course",
                githubUrl,
                remote,
                222L
        ).join();

        assertEquals(githubUrl, installed.url);
        assertEquals(1, remote.requestsFor("manifest.json"));
        assertEquals(1, remote.requestsFor("p1.json"));

        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        assertEquals(1, repositories.size());
        assertEquals(githubUrl, repositories.get(0).url);
        assertTrue(Files.readString(RepositoryManager.getRepositoryDir(repositories.get(0)).toPath().resolve("p1.json"),
                        StandardCharsets.UTF_8)
                .contains("GitHub Course Problem"));
    }

    @Test
    void githubTreeRepositoryUrlDropsBrowserQueryAndFragmentBeforeDownloadingRawContent() throws IOException {
        String githubUrl = "https://github.com/example/course/tree/main/question_bank/official?tab=readme-ov-file#files";
        FakeRemote remote = new FakeRemote("https://raw.githubusercontent.com/example/course/main/question_bank/official");
        remote.publishProblem(1, "GitHub Query Fragment Course Problem");

        RepositoryMetadata installed = RepositoryManager.installOrUpdateRepository(
                "GitHub Query Course",
                githubUrl,
                remote,
                333L
        ).join();

        assertEquals(githubUrl, installed.url);
        assertEquals(1, remote.requestsFor("manifest.json"));
        assertEquals(1, remote.requestsFor("p1.json"));
        assertTrue(Files.readString(RepositoryManager.getRepositoryDir(installed).toPath().resolve("p1.json"),
                        StandardCharsets.UTF_8)
                .contains("GitHub Query Fragment Course Problem"));
    }

    @Test
    void githubBlobManifestRepositoryUrlDownloadsContainingRawDirectoryButPersistsUserUrl() throws IOException {
        String githubUrl = "https://github.com/example/course/blob/release-v1/custom/path/manifest.json";
        FakeRemote remote = new FakeRemote("https://raw.githubusercontent.com/example/course/release-v1/custom/path");
        remote.publishProblem(1, "GitHub Blob Manifest Course Problem");

        RepositoryMetadata installed = RepositoryManager.installOrUpdateRepository(
                "GitHub Blob Course",
                githubUrl,
                remote,
                444L
        ).join();

        assertEquals(githubUrl, installed.url);
        assertEquals(1, remote.requestsFor("manifest.json"));
        assertEquals(1, remote.requestsFor("p1.json"));
        assertTrue(Files.readString(RepositoryManager.getRepositoryDir(installed).toPath().resolve("p1.json"),
                        StandardCharsets.UTF_8)
                .contains("GitHub Blob Manifest Course Problem"));
    }

    @Test
    void githubBlobRepositoryUrlMustPointToManifestBeforeRemoteFetch() {
        AtomicInteger requests = new AtomicInteger();

        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "GitHub Single File",
                        "https://github.com/example/course/blob/main/question_bank/official/p1.json",
                        uri -> {
                            requests.incrementAndGet();
                            throw new IOException("remote client should not be called");
                        },
                        555L
                ).join());

        assertTrue(error.getCause().getMessage().contains("manifest.json"),
                "failure should explain that a GitHub blob repository URL must point to manifest.json");
        assertEquals(0, requests.get(), "invalid GitHub blob URLs should fail before remote fetch");
        assertTrue(RepositoryManager.getRepositories().isEmpty(),
                "invalid GitHub blob URLs must not leave selectable metadata");
        assertFalse(Files.exists(configFile()),
                "repositories.json should not be created by an invalid GitHub blob URL");
    }

    @Test
    void failedExistingUpdateKeepsPreviousMetadata() throws IOException {
        FakeRemote remote = new FakeRemote("https://example.test/course-v1");
        remote.publishProblem(1, "First Course Problem");
        RepositoryManager.installOrUpdateRepository("Course", remote.baseUrl, remote, 111L).join();

        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "Course",
                        "https://example.test/course-v2",
                        uri -> {
                            throw new IOException("remote v2 unavailable");
                        },
                        222L
                ).join());

        assertTrue(error.getCause().getMessage().contains("remote v2 unavailable"));
        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        assertEquals(1, repositories.size());
        assertEquals(remote.baseUrl, repositories.get(0).url);
        assertEquals(111L, repositories.get(0).lastUpdated);

        String config = Files.readString(configFile(), StandardCharsets.UTF_8);
        assertTrue(config.contains("course-v1"));
        assertFalse(config.contains("course-v2"),
                "failed updates must not overwrite the previous usable repository URL");
    }

    @Test
    void failedExistingUpdateAfterContentValidationKeepsPreviousMetadataAndCache() throws IOException {
        FakeRemote firstRemote = new FakeRemote("https://example.test/course-v1");
        firstRemote.publishProblem(1, "First Course Problem");
        RepositoryManager.installOrUpdateRepository("Course", firstRemote.baseUrl, firstRemote, 111L).join();
        Path repositoryDir = RepositoryManager.getRepositoryDir(
                new RepositoryMetadata("Course", firstRemote.baseUrl)).toPath();

        FakeRemote badUpdate = new FakeRemote("https://example.test/course-v2");
        badUpdate.publishProblem(1, "Second Course Problem");
        badUpdate.replaceProblemBody(1, "{not valid json");

        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "Course",
                        badUpdate.baseUrl,
                        badUpdate,
                        222L
                ).join());

        assertTrue(error.getCause().getMessage().contains("Invalid official problem JSON")
                        && error.getCause().getMessage().contains("p1.json"),
                "failed existing updates should surface the downloaded content validation error");

        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        assertEquals(1, repositories.size());
        assertEquals(firstRemote.baseUrl, repositories.get(0).url);
        assertEquals(111L, repositories.get(0).lastUpdated);

        String config = Files.readString(configFile(), StandardCharsets.UTF_8);
        assertTrue(config.contains("course-v1"));
        assertFalse(config.contains("course-v2"),
                "failed content validation must not overwrite the previous usable repository URL");
        String cachedProblem = Files.readString(repositoryDir.resolve("p1.json"), StandardCharsets.UTF_8);
        assertTrue(cachedProblem.contains("First Course Problem"),
                "failed content validation must preserve the previous usable repository cache");
        assertFalse(cachedProblem.contains("Second Course Problem"),
                "failed content validation must not leak the rejected update into the cache");
    }

    @Test
    void getRepositoriesReturnsDefensiveCopies() {
        FakeRemote remote = new FakeRemote("https://example.test/course");
        remote.publishProblem(1, "First Course Problem");
        RepositoryManager.installOrUpdateRepository("Course", remote.baseUrl, remote, 111L).join();

        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        repositories.get(0).url = "https://example.test/mutated";
        repositories.get(0).lastUpdated = 999L;

        RepositoryMetadata actual = RepositoryManager.getRepositories().get(0);
        assertEquals(remote.baseUrl, actual.url);
        assertEquals(111L, actual.lastUpdated);
    }

    @Test
    void installRejectsNamesThatCollideAfterRepositorySlugNormalization() throws IOException {
        FakeRemote firstRemote = new FakeRemote("https://example.test/course-one");
        firstRemote.publishProblem(1, "First Course Problem");
        RepositoryManager.installOrUpdateRepository("Course!", firstRemote.baseUrl, firstRemote, 111L).join();

        FakeRemote secondRemote = new FakeRemote("https://example.test/course-two");
        secondRemote.publishProblem(1, "Second Course Problem");

        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "Course?",
                        secondRemote.baseUrl,
                        secondRemote,
                        222L
                ).join());

        assertTrue(error.getCause().getMessage().contains("conflicts"),
                "repository names that normalize to the same slug must not share one local cache directory");

        List<RepositoryMetadata> repositories = RepositoryManager.getRepositories();
        assertEquals(1, repositories.size());
        assertEquals("Course!", repositories.get(0).name);
        assertEquals(firstRemote.baseUrl, repositories.get(0).url);
        assertEquals(111L, repositories.get(0).lastUpdated);

        String config = Files.readString(configFile(), StandardCharsets.UTF_8);
        assertTrue(config.contains("Course!"));
        assertTrue(config.contains("course-one"));
        assertFalse(config.contains("Course?"));
        assertFalse(config.contains("course-two"));

        Path repositoryDir = RepositoryManager.getRepositoryDir(repositories.get(0)).toPath();
        assertTrue(Files.readString(repositoryDir.resolve("p1.json"), StandardCharsets.UTF_8)
                .contains("First Course Problem"));
    }

    @Test
    void problemManagerDoesNotPersistCustomRepositoryBeforeDownloadSucceeds() throws IOException {
        Path source = Path.of(System.getProperty("user.dir"),
                "src", "main", "java", "com", "crabmods", "algocraft", "logic", "ProblemManager.java");
        String problemManager = Files.readString(source, StandardCharsets.UTF_8);

        assertTrue(problemManager.contains("RepositoryManager.installOrUpdateRepository(name, url)"),
                "ProblemManager should install and validate the remote repository before publishing metadata");
        assertFalse(problemManager.contains("RepositoryManager.addRepository(name, url)"),
                "ProblemManager must not persist repository metadata before a download succeeds");
    }

    private Path configFile() {
        return gameDir.resolve("config").resolve("algocraft").resolve("repositories.json");
    }

    private void assertRejectedBeforeRemoteFetch(String url) {
        AtomicInteger requests = new AtomicInteger();

        CompletionException error = assertThrows(CompletionException.class,
                () -> RepositoryManager.installOrUpdateRepository(
                        "Unsafe Course",
                        url,
                        uri -> {
                            requests.incrementAndGet();
                            throw new IOException("remote client should not be called");
                        },
                        123L
                ).join());

        assertTrue(error.getCause().getMessage().contains("http or https")
                        || error.getCause().getMessage().contains("include a host"),
                "failure should identify an unsupported remote repository URL: " + error.getCause().getMessage());
        assertEquals(0, requests.get(), "unsupported repository URLs should fail before remote fetch");
        assertTrue(RepositoryManager.getRepositories().isEmpty(),
                "unsupported repository URLs must not leave selectable metadata");
        assertFalse(Files.exists(configFile()),
                "repositories.json should not be created by an unsupported repository URL");
    }

    private static final class FakeRemote implements RemoteFileClient {
        private final String baseUrl;
        private final Map<URI, byte[]> responses = new LinkedHashMap<>();
        private final Map<String, Integer> requestsByName = new LinkedHashMap<>();

        private FakeRemote(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        void publishProblem(int problemNumber, String title) {
            put("p" + problemNumber + ".json", """
                    {
                      "id": "%d",
                      "title": "%s",
                      "description": "%s",
                      "difficulty": "EASY",
                      "initialCode": "class Solution { public int solve() { return 1; } }",
                      "tags": ["Array"],
                      "examples": [{"input": "", "output": "1"}],
                      "tests": [{"input": "", "output": "1"}, {"input": "1", "output": "1"}],
                      "solutions": [
%s
                      ]
                    }
                    """.formatted(problemNumber, title, problemDescription(title), solutionEntriesJson()));
            put("lang/zh_cn/p" + problemNumber + ".json", """
                    {
                      "title": "\\u8bfe\\u7a0b\\u9898\\u76ee %d",
                      "description": "%s",
                      "solutions": [
%s
                      ]
                    }
                    """.formatted(problemNumber,
                            chineseProblemDescription("\\u8bfe\\u7a0b\\u9898\\u76ee " + problemNumber),
                            chineseSolutionTranslationEntriesJson()));
            put("catalog.json", """
                    {
                      "version": "test",
                      "tracks": [
                        {"id": "array", "name": "Array", "tags": ["Array"]}
                      ]
                    }
                    """);
            publishManifest();
        }

        void replaceProblemBody(int problemNumber, String body) {
            put("p" + problemNumber + ".json", body);
            publishManifest();
        }

        private static String problemDescription(String title) {
            return "# " + title + "\\n\\n"
                    + "Solve the imported teaching exercise and return the requested value.\\n\\n"
                    + "## Example\\n\\n"
                    + "Input: empty input\\n\\nOutput: 1\\n\\n"
                    + "## Constraints\\n\\n"
                    + "The generated fixture keeps inputs small for repository validation.";
        }

        private static String chineseProblemDescription(String title) {
            return "# " + title + "\\n\\n"
                    + "\\u8fd9\\u662f\\u7528\\u4e8e\\u9a8c\\u8bc1\\u8fdc\\u7a0b\\u8bfe\\u7a0b\\u7684\\u4e2d\\u6587\\u9898\\u9762\\u3002\\n\\n"
                    + "## \\u793a\\u4f8b\\n\\n"
                    + "\\u8f93\\u5165\\uff1a\\u7a7a\\u8f93\\u5165\\n\\n\\u8f93\\u51fa\\uff1a1\\n\\n"
                    + "## \\u7ea6\\u675f\\n\\n"
                    + "\\u6d4b\\u8bd5 fixture \\u4f7f\\u7528\\u5f88\\u5c0f\\u7684\\u8f93\\u5165\\u3002";
        }

        private static String solutionEntriesJson() {
            StringBuilder solutions = new StringBuilder();
            for (int i = 1; i <= 3; i++) {
                if (!solutions.isEmpty()) {
                    solutions.append(",\n");
                }
                solutions.append("""
                                {
                                  "name": "Teaching Path %d",
                                  "timeComplexity": "O(1)",
                                  "spaceComplexity": "O(1)",
                                  "description": "## Approach\\n\\nUse teaching path %d with explicit complexity metadata and enough explanation to be useful in a course.",
                                  "code": "%s",
                                  "language": "java"
                                }
                        """.formatted(i, i, solutionCodeForIndex(i - 1)));
            }
            return solutions.toString();
        }

        private static String solutionCodeForIndex(int index) {
            return switch (index % 3) {
                case 0 -> "class Solution { public int solve() { return 1; } }";
                case 1 -> "class Solution { public int solve() { int[] values = new int[]{1}; return values[0]; } }";
                default -> "class Solution { public int solve() { java.util.List<Integer> values = java.util.List.of(1); return values.get(0); } }";
            };
        }

        private static String chineseSolutionTranslationEntriesJson() {
            StringBuilder solutions = new StringBuilder();
            for (int i = 1; i <= 3; i++) {
                if (!solutions.isEmpty()) {
                    solutions.append(",\n");
                }
                solutions.append("""
                                {
                                  "name": "\\u4e2d\\u6587\\u89e3\\u6cd5 %d",
                                  "description": "## \\u601d\\u8def\\n\\n\\u8fd9\\u662f\\u7b2c %d \\u79cd\\u6559\\u5b66\\u89e3\\u6cd5\\u8bf4\\u660e\\u3002"
                                }
                        """.formatted(i, i));
            }
            return solutions.toString();
        }

        private void put(String fileName, String body) {
            responses.put(URI.create(baseUrl + "/" + fileName), body.getBytes(StandardCharsets.UTF_8));
        }

        private void publishManifest() {
            StringBuilder files = new StringBuilder();
            StringBuilder combinedHashes = new StringBuilder();
            int totalProblems = 0;
            List<Map.Entry<URI, byte[]>> manifestEntries = responses.entrySet().stream()
                    .filter(entry -> !manifestName(entry.getKey()).equals("manifest.json"))
                    .sorted(Comparator.comparing(
                            entry -> manifestName(entry.getKey()),
                            FakeRemote::compareManifestNames
                    ))
                    .toList();
            for (Map.Entry<URI, byte[]> entry : manifestEntries) {
                String fileName = entry.getKey().toString().substring((baseUrl + "/").length());
                if (fileName.matches("p\\d+\\.json")) {
                    totalProblems++;
                }
                if (!files.isEmpty()) {
                    files.append(",\n");
                }
                String hash = hash(entry.getValue());
                combinedHashes.append(hash);
                files.append("                        {\"name\": \"")
                        .append(fileName)
                        .append("\", \"hash\": \"")
                        .append(hash)
                        .append("\", \"size\": ")
                        .append(entry.getValue().length)
                        .append("}");
            }
            String manifest = """
                    {
                      "version": "test",
                      "totalProblems": %d,
                      "signature": "%s",
                      "files": [
%s
                      ]
                    }
                    """.formatted(totalProblems, hash(combinedHashes.toString().getBytes(StandardCharsets.UTF_8)), files);
            responses.put(URI.create(baseUrl + "/manifest.json"), manifest.getBytes(StandardCharsets.UTF_8));
        }

        private String manifestName(URI uri) {
            return uri.toString().substring((baseUrl + "/").length());
        }

        private int requestsFor(String fileName) {
            return requestsByName.getOrDefault(fileName, 0);
        }

        private static int compareManifestNames(String left, String right) {
            int leftGroup = manifestOrderGroup(left);
            int rightGroup = manifestOrderGroup(right);
            if (leftGroup != rightGroup) {
                return Integer.compare(leftGroup, rightGroup);
            }
            if (leftGroup == 1) {
                return Integer.compare(problemNumber(left), problemNumber(right));
            }
            return left.compareTo(right);
        }

        private static int manifestOrderGroup(String fileName) {
            if ("catalog.json".equals(fileName)) {
                return 0;
            }
            if (fileName.matches("p\\d+\\.json")) {
                return 1;
            }
            if (fileName.startsWith("images/")) {
                return 2;
            }
            if (fileName.startsWith("lang/")) {
                return 3;
            }
            return 4;
        }

        private static int problemNumber(String fileName) {
            return Integer.parseInt(fileName.substring(1, fileName.length() - ".json".length()));
        }

        @Override
        public byte[] get(URI uri) throws IOException {
            requestsByName.merge(manifestName(uri), 1, Integer::sum);
            byte[] body = responses.get(uri);
            if (body == null) {
                throw new IOException("missing " + uri);
            }
            return body;
        }

        private static String hash(byte[] body) {
            try {
                return RemoteRepositoryDownloader.sha256(body);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}

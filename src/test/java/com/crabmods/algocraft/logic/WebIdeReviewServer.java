package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.ProblemRepository;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/** A local browser QA fixture using production HTTP handlers and the complete bank. */
public final class WebIdeReviewServer {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("question_bank/official").toAbsolutePath();
        System.setProperty(ProgressManager.CONFIG_DIR_PROPERTY, Path.of("build/review/web-state").toAbsolutePath().toString());
        List<Problem> problems = new ArrayList<>();
        Gson gson = new Gson();
        for (int id = 1; id <= 500; id++) {
            Problem problem = gson.fromJson(Files.readString(root.resolve("p" + id + ".json")), Problem.class);
            problem.setAssetBaseDir(root);
            problems.add(problem);
        }
        ProblemTranslationManager.loadAllTranslations(root).join();
        ProblemManager.replaceRepositoriesForTest(List.of(new ProblemRepository() {
            public String getName() { return "Official"; }
            public int getPriority() { return 100; }
            public List<Problem> getProblems() { return List.copyOf(problems); }
            public CompletableFuture<Void> refresh() { return CompletableFuture.completedFuture(null); }
        }));
        if (!AlgoCraftWebServer.start(0)) throw new IllegalStateException("Could not start web review server");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AlgoCraftWebServer.stop();
            CodeExecutor.shutdown();
        }));
        System.out.println("WEB_REVIEW_URL=http://127.0.0.1:" + AlgoCraftWebServer.getActivePort());
        new CountDownLatch(1).await();
    }
}

package com.crabmods.algocraft.logic;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic selection for the algorithm entry method.
 */
public final class SolutionMethodSelector {
    private static final Pattern NAMED_ARGUMENT_PATTERN =
            Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*");
    private static final Set<String> OBJECT_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait",
            "setBadVersion", "isBadVersion"
    );
    private static final Set<String> HELPER_METHOD_NAMES = Set.of(
            "helper", "dfsHelper", "bfsHelper"
    );

    private SolutionMethodSelector() {
    }

    public static Method select(Class<?> clazz, String input, Predicate<Method> inputCompatible) {
        return select(clazz, input, Set.of(), inputCompatible);
    }

    public static Method select(Class<?> clazz, String input, Set<String> preferredMethodNames,
                                Predicate<Method> inputCompatible) {
        List<Method> candidates = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (isCandidate(method)) {
                candidates.add(method);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        Set<String> namedArguments = namedArguments(input);
        Set<String> preferredNames = preferredMethodNames != null ? preferredMethodNames : Set.of();
        candidates.sort(Comparator
                .comparingInt((Method method) -> score(method, namedArguments, preferredNames, inputCompatible)).reversed()
                .thenComparing(Method::getName)
                .thenComparing(SolutionMethodSelector::parameterSignature));
        return candidates.getFirst();
    }

    private static boolean isCandidate(Method method) {
        return !method.isSynthetic()
                && !method.isBridge()
                && !OBJECT_METHODS.contains(method.getName());
    }

    private static int score(Method method, Set<String> namedArguments, Set<String> preferredMethodNames,
                             Predicate<Method> inputCompatible) {
        int score = 0;
        if (inputCompatible == null || inputCompatible.test(method)) {
            score += 10_000;
        } else {
            score -= 10_000;
        }

        if (argumentShapeMatches(method, namedArguments)) {
            score += 5_000;
        } else if (!namedArguments.isEmpty() && method.getParameterCount() == 0) {
            score -= 4_000;
        } else if (method.getParameterCount() > namedArguments.size() && !namedArguments.isEmpty()) {
            score -= 2_000;
        }

        int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            score += 700;
        } else if (!Modifier.isPrivate(modifiers)) {
            score += 400;
        }

        if ("solve".equals(method.getName())) {
            score += 200;
        }
        if (preferredMethodNames.contains(method.getName())) {
            score += 2_000;
        }
        if (HELPER_METHOD_NAMES.contains(method.getName())) {
            score -= 500;
        }
        return score;
    }

    private static boolean argumentShapeMatches(Method method, Set<String> namedArguments) {
        int parameterCount = method.getParameterCount();
        if (namedArguments.isEmpty()) {
            return parameterCount == 0;
        }
        if (parameterCount == namedArguments.size()) {
            return true;
        }
        Class<?>[] types = method.getParameterTypes();
        if (parameterCount == 1 && types[0].getSimpleName().equals("ListNode")
                && namedArguments.contains("head")) {
            return true;
        }
        if (parameterCount == 2
                && types[0].getSimpleName().equals("ListNode")
                && types[1].getSimpleName().equals("ListNode")
                && namedArguments.contains("listA")
                && namedArguments.contains("listB")) {
            return true;
        }
        if (parameterCount == 1 && types[0].isArray()
                && types[0].getComponentType().getSimpleName().equals("ListNode")
                && namedArguments.contains("lists")) {
            return true;
        }
        return parameterCount == 1
                && types[0].getSimpleName().equals("Node")
                && (namedArguments.contains("head")
                || namedArguments.contains("adjList")
                || namedArguments.contains("root"));
    }

    private static Set<String> namedArguments(String input) {
        Set<String> names = new HashSet<>();
        if (input == null || input.isBlank()) {
            return names;
        }
        Matcher matcher = NAMED_ARGUMENT_PATTERN.matcher(input);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static String parameterSignature(Method method) {
        StringBuilder signature = new StringBuilder();
        for (Class<?> type : method.getParameterTypes()) {
            if (!signature.isEmpty()) {
                signature.append(',');
            }
            signature.append(type.getName());
        }
        return signature.toString();
    }
}

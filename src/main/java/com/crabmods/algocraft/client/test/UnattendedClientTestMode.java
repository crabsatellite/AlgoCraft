package com.crabmods.algocraft.client.test;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;

/** Desktop isolation for automated acceptance; normal play is unaffected. */
public final class UnattendedClientTestMode {
    public static volatile int gainWrites;
    public static volatile float deviceGain = Float.NaN;
    public static int verifiedTicks, suppressedMouseGrabs, clipboardReads, clipboardWrites, violationCount;
    public static String clipboard = "";
    private UnattendedClientTestMode() {}
    public static boolean enabled() {
        return Boolean.getBoolean("algocraft.tests.unattended");
    }
    public static void verify() {
        if (!enabled()) throw new IllegalStateException("Automated clients require unattended mode");
        var mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE
                || GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != GLFW.GLFW_FALSE
                || GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL
                || GLFW.glfwGetWindowMonitor(window) != 0
                || (gainWrites > 0 && deviceGain != 0.0f)) {
            violationCount++;
            mc.stop();
            throw new IllegalStateException("Unattended desktop isolation failed");
        }
        mc.options.pauseOnLostFocus = false;
        mc.options.framerateLimit().set(60);
        verifiedTicks++;
    }
    public static Map<String, Object> receipt() {
        verify();
        long heapMiB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int processors = Runtime.getRuntime().availableProcessors();
        if (gainWrites < 1 || deviceGain != 0.0f || clipboardReads < 1 || clipboardWrites < 1
                || heapMiB > 4096 || processors > 2 || violationCount != 0)
            throw new IllegalStateException("Missing silent-device/input/resource evidence");
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : new String[]{"enabled", "hidden", "unfocused", "cursorFree", "windowed", "silentDevice", "clipboardIsolated"})
            result.put(key, true);
        result.put("verifiedTicks", verifiedTicks);
        result.put("violationCount", violationCount);
        result.put("gainWrites", gainWrites);
        result.put("deviceGain", deviceGain);
        result.put("clipboardReads", clipboardReads);
        result.put("clipboardWrites", clipboardWrites);
        result.put("suppressedMouseGrabs", suppressedMouseGrabs);
        result.put("heapMaxMiB", heapMiB);
        result.put("processors", processors);
        result.put("fpsLimit", 60);
        return result;
    }
}

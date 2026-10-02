package com.crabmods.algocraft.client;

import com.crabmods.algocraft.client.gui.AlgorithmSelectionScreen;
import com.crabmods.algocraft.client.gui.modern.ModernAlgorithmScreen;
import com.crabmods.algocraft.network.PacketSubmitSolution;
import com.crabmods.algocraft.web.AlgoCraftWebServer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.crabmods.algocraft.network.compat.PacketDistributor;

public class ClientHooks {
    private static Runnable webIdeOpenObserverForSmokeTest;

    public static void registerWebServerClientBridge() {
        AlgoCraftWebServer.setSubmissionAction(ServerSubmissionBridge::submit);

    }

    public static void openAlgorithmScreen() {
        Minecraft.getInstance().setScreen(new ModernAlgorithmScreen());
    }

    public static void openModeSelectionScreen() {
        Minecraft.getInstance().setScreen(new AlgorithmSelectionScreen());
    }

    public static void openWebIde() {
        if (webIdeOpenObserverForSmokeTest != null) {
            webIdeOpenObserverForSmokeTest.run();
            return;
        }
        if (!AlgoCraftWebServer.isRunning()) {
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("algocraft.msg.web_unavailable"), false);
            return;
        }
        Util.getPlatform().openUri(AlgoCraftWebServer.getLocalUrl());
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("algocraft.msg.opened_web_ide"), true);
        }
    }

    public static void setWebIdeOpenObserverForSmokeTest(Runnable observer) {
        if (!Boolean.getBoolean("algocraft.ideSmokeTest")) {
            throw new IllegalStateException("Web IDE observer is only available during the IDE smoke test");
        }
        webIdeOpenObserverForSmokeTest = observer;
    }

    public static void clearWebIdeOpenObserverForSmokeTest() {
        webIdeOpenObserverForSmokeTest = null;
    }
}

package com.crabmods.algocraft.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientHooks {
    public static void openAlgorithmScreen() {
        // Option 1: Open in external browser (Best experience)
        Util.getPlatform().openUri("http://localhost:3000");
        Minecraft.getInstance().player.displayClientMessage(Component.literal("Opened AlgoCraft IDE in your browser!"), true);
        
        // Option 2: Open in-game GUI (Fallback)
        // Minecraft.getInstance().setScreen(new ModernAlgorithmScreen());
    }
}

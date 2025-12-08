package com.crabmods.algocraft.client;

import com.crabmods.algocraft.client.gui.AlgorithmSelectionScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientHooks {
    public static void openAlgorithmScreen() {
        Minecraft.getInstance().setScreen(new AlgorithmSelectionScreen());
    }
}

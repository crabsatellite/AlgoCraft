package com.crabmods.algocraft.client;

import com.crabmods.algocraft.client.gui.AlgorithmComputerScreen;
import net.minecraft.client.Minecraft;

public class ClientHooks {
    public static void openAlgorithmScreen() {
        Minecraft.getInstance().setScreen(new AlgorithmComputerScreen());
    }
}

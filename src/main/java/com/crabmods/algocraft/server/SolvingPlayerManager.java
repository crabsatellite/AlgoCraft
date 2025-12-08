package com.crabmods.algocraft.server;

import com.crabmods.algocraft.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// @EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class SolvingPlayerManager {
    private static final Set<UUID> solvingPlayers = new HashSet<>();

    public static void setSolving(ServerPlayer player, boolean isSolving) {
        if (isSolving) {
            solvingPlayers.add(player.getUUID());
        } else {
            solvingPlayers.remove(player.getUUID());
        }
    }

    public static boolean isSolving(Entity entity) {
        return entity instanceof ServerPlayer && solvingPlayers.contains(entity.getUUID());
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (Config.PROTECT_WHILE_SOLVING.get() && isSolving(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (Config.PROTECT_WHILE_SOLVING.get() && isSolving(event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        solvingPlayers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        solvingPlayers.remove(event.getEntity().getUUID());
    }
}

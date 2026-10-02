package com.crabmods.algocraft.server;

import com.crabmods.algocraft.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// @EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class SolvingPlayerManager {
    public static final long MAX_SOLVING_SESSION_MS = 30L * 60L * 1000L;
    private static final Map<UUID, SolvingSession> solvingSessions = new ConcurrentHashMap<>();

    public static void setSolving(ServerPlayer player, boolean isSolving, String problemId) {
        setSolving(player, isSolving, problemId, System.currentTimeMillis());
    }

    public static void setSolving(ServerPlayer player, boolean isSolving, String problemId, long now) {
        if (isSolving) {
            if (problemId != null && !problemId.isBlank()) {
                UUID playerId = player.getUUID();
                SolvingSession existingSession = currentSession(playerId, now);
                if (existingSession != null && existingSession.problemId().equals(problemId)) {
                    return;
                }
                solvingSessions.put(playerId, new SolvingSession(problemId, now));
            }
        } else {
            clearSolving(player.getUUID());
        }
    }

    public static boolean isSolving(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        return currentSession(player.getUUID(), System.currentTimeMillis()) != null;
    }

    public static OptionalLong getSolvingElapsedMs(ServerPlayer player, String problemId, long now) {
        SolvingSession session = currentSession(player.getUUID(), now);
        if (session == null || !session.problemId().equals(problemId)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0L, now - session.startedAtMs()));
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
        clearSolving(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearSolving(event.getEntity().getUUID());
    }

    private static void clearSolving(UUID playerId) {
        solvingSessions.remove(playerId);
    }

    private static SolvingSession currentSession(UUID playerId, long now) {
        SolvingSession session = solvingSessions.get(playerId);
        if (session == null) {
            return null;
        }
        if (now - session.startedAtMs() > MAX_SOLVING_SESSION_MS) {
            clearSolving(playerId);
            return null;
        }
        return session;
    }

    private record SolvingSession(String problemId, long startedAtMs) {
    }
}

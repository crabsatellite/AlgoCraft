package com.crabmods.algocraft;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("AlgoCraft Config");
        
        MAX_EXECUTION_TIME = BUILDER.comment("Maximum execution time for user code in milliseconds")
                .defineInRange("maxExecutionTime", 1000, 100, 10000);

        ENABLE_REWARDS = BUILDER.comment("Enable item rewards for solving problems")
                .define("enableRewards", true);

        PROTECT_WHILE_SOLVING = BUILDER.comment("Make player invulnerable and ignored by mobs while solving problems")
                .define("protectWhileSolving", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static final ModConfigSpec.IntValue MAX_EXECUTION_TIME;
    public static final ModConfigSpec.BooleanValue ENABLE_REWARDS;
    public static final ModConfigSpec.BooleanValue PROTECT_WHILE_SOLVING;
}

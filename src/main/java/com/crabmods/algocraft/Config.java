package com.crabmods.algocraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration options for the AlgoCraft mod.
 * All settings can be changed in config/algocraft-common.toml
 */
public class Config
{
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Execution settings
    public static final ModConfigSpec.IntValue MAX_EXECUTION_TIME;
    public static final ModConfigSpec.IntValue MAX_CODE_LENGTH;
    
    // Web server settings
    public static final ModConfigSpec.IntValue WEB_SERVER_PORT;
    public static final ModConfigSpec.IntValue MAX_REQUEST_SIZE;
    
    // Gameplay settings
    public static final ModConfigSpec.BooleanValue ENABLE_REWARDS;
    public static final ModConfigSpec.BooleanValue PROTECT_WHILE_SOLVING;
    
    // Rate limiting
    public static final ModConfigSpec.IntValue RATE_LIMIT_PER_MINUTE;

    // Security settings
    public static final ModConfigSpec.BooleanValue STRICT_SECURITY_MODE;

    static {
        BUILDER.push("Execution Settings");
        
        MAX_EXECUTION_TIME = BUILDER
                .comment("Maximum execution time for user code in milliseconds.",
                         "Higher values allow more complex solutions but increase server load.",
                         "Recommended: 1000-5000ms")
                .defineInRange("maxExecutionTime", 2000, 500, 30000);
        
        MAX_CODE_LENGTH = BUILDER
                .comment("Maximum allowed code length in characters.",
                         "Prevents DoS attacks via extremely long code submissions.")
                .defineInRange("maxCodeLength", 50000, 1000, 200000);
        
        BUILDER.pop();
        
        BUILDER.push("Web Server Settings");
        
        WEB_SERVER_PORT = BUILDER
                .comment("Port for the web-based IDE interface.",
                         "Change this if port 3000 conflicts with other applications.")
                .defineInRange("webServerPort", 3000, 1024, 65535);
        
        MAX_REQUEST_SIZE = BUILDER
                .comment("Maximum HTTP request body size in bytes.",
                         "Prevents DoS attacks via large request bodies.")
                .defineInRange("maxRequestSize", 100000, 10000, 1000000);

        RATE_LIMIT_PER_MINUTE = BUILDER
                .comment("Maximum HTTP requests per IP address per minute.",
                         "Prevents abuse of the web IDE API.")
                .defineInRange("rateLimitPerMinute", 60, 10, 600);

        BUILDER.pop();

        BUILDER.push("Gameplay Settings");

        ENABLE_REWARDS = BUILDER
                .comment("Enable item rewards for solving problems.",
                         "Players receive items based on problem difficulty.")
                .define("enableRewards", true);

        PROTECT_WHILE_SOLVING = BUILDER
                .comment("Make player invulnerable and ignored by mobs while solving problems.",
                         "Prevents interruption during coding sessions.")
                .define("protectWhileSolving", true);

        BUILDER.pop();

        BUILDER.push("Security Settings");
        
        STRICT_SECURITY_MODE = BUILDER
                .comment("Enable strict security mode.",
                         "When enabled, additional code patterns are blocked and imports are validated.",
                         "Recommended to keep enabled for public servers.")
                .define("strictSecurityMode", true);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}

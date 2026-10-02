package com.crabmods.algocraft.gametest;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.Config;
import com.crabmods.algocraft.TrophyBlock;
import com.crabmods.algocraft.TrophyBlockEntity;
import com.crabmods.algocraft.item.TrophyItem;
import com.crabmods.algocraft.logic.AchievementRegistry;
import com.crabmods.algocraft.logic.Problem;
import com.crabmods.algocraft.logic.ProblemManager;
import com.crabmods.algocraft.logic.RewardSystem;
import com.crabmods.algocraft.network.SolvedProblemRewardService;
import com.crabmods.algocraft.world.AlgoCraftSavedData;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(AlgoCraft.MODID)
@PrefixGameTestTemplate(false)
public final class GameplayClosureGameTests {
    private static final String TEMPLATE = "empty";
    private GameplayClosureGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void survival_computer_can_be_crafted_placed_mined_and_recovered(GameTestHelper helper) {
        withPlayer(helper, player -> {
            var holder = helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(AlgoCraft.MODID, "algorithm_computer")).orElseThrow();
            check(holder.value() instanceof ShapedRecipe, "computer has a real workbench recipe");
            ShapedRecipe recipe = (ShapedRecipe) holder.value();
            List<ItemStack> ingredients = List.of(
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.REDSTONE), new ItemStack(Items.GLASS), new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT));
            CraftingInput input = CraftingInput.of(3, 3, ingredients);
            check(recipe.matches(input, helper.getLevel()), "six iron, two redstone and glass match the recipe");
            List<ItemStack> wrong = new ArrayList<>(ingredients);
            wrong.set(4, new ItemStack(Items.COBBLESTONE));
            check(!recipe.matches(CraftingInput.of(3, 3, wrong), helper.getLevel()), "wrong center material is rejected");
            ItemStack computer = recipe.assemble(input, helper.getLevel().registryAccess());
            check(computer.is(AlgoCraft.ALGORITHM_COMPUTER_BLOCK_ITEM.get()) && computer.getCount() == 1,
                    "crafting produces exactly one algorithm computer");

            // Receiving redstone and broadcasting inventory changes should discover the recipe.
            player.getInventory().add(new ItemStack(Items.REDSTONE));
            player.containerMenu.broadcastChanges();
            check(player.getRecipeBook().contains(holder), "the recipe is discoverable from redstone acquisition");
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, computer);
            BlockPos floor = helper.absolutePos(new BlockPos(2, 0, 2));
            BlockPos placed = floor.above();
            BlockState oldFloor = helper.getLevel().getBlockState(floor);
            BlockState oldPlaced = helper.getLevel().getBlockState(placed);
            List<ItemEntity> drops = new ArrayList<>();
            try {
                helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(placed, Blocks.AIR.defaultBlockState());
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
                check(computer.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                        "the crafted item places through the survival item interaction");
                check(helper.getLevel().getBlockState(placed).is(AlgoCraft.ALGORITHM_COMPUTER_BLOCK.get()),
                        "the crafted computer is placed in the world");
                check(computer.isEmpty(), "survival placement consumes the crafted item");
                check(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(helper.getLevel().getBlockState(placed)),
                        "a wooden pickaxe cannot recover the computer");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
                check(player.getMainHandItem().isCorrectToolForDrops(helper.getLevel().getBlockState(placed)),
                        "a stone pickaxe is a valid recovery tool");
                check(player.gameMode.destroyBlock(placed), "survival mining destroys the placed computer");
                drops.addAll(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(placed).inflate(1),
                        entity -> entity.getItem().is(AlgoCraft.ALGORITHM_COMPUTER_BLOCK_ITEM.get())));
                check(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                        "mining yields exactly one recoverable computer");
                drops.getFirst().setNoPickUpDelay();
                drops.getFirst().playerTouch(player);
                check(count(player, AlgoCraft.ALGORITHM_COMPUTER_BLOCK_ITEM.get()) == 1,
                        "the mined computer returns to the player's inventory");
            } finally {
                drops.forEach(ItemEntity::discard);
                helper.getLevel().setBlockAndUpdate(placed, oldPlaced);
                helper.getLevel().setBlockAndUpdate(floor, oldFloor);
            }
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void milestone_cannot_be_claimed_again_by_repeating_old_problems(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AlgoCraftSavedData data = AlgoCraftSavedData.get(player.serverLevel());
            for (int i = 0; i < 9; i++) {
                data.setProblemSolved(player.getUUID(), "milestone-seed-" + i, day(1));
                data.incrementTotalSolved(player.getUUID());
                data.incrementDifficultyCount(player.getUUID(), "easy");
            }
            SolvedProblemRewardService.applySolvedProblem(player, "milestone-tenth", "easy", 90_000, day(1));
            check(count(player, Items.DIAMOND) == 10, "the tenth unique problem gives ten milestone diamonds");
            SolvedProblemRewardService.applySolvedProblem(player, "milestone-tenth", "easy", 90_000, day(2));
            check(count(player, Items.DIAMOND) == 10, "next-day repeats never grant the milestone again");
            check(data.getTotalSolved(player.getUUID()) == 10, "the unique problem counter stays at ten");
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void weekly_bonus_is_awarded_once_on_the_seventh_calendar_day(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AlgoCraftSavedData data = AlgoCraftSavedData.get(player.serverLevel());
            for (int i = 1; i <= 7; i++) {
                player.getInventory().clearContent();
                SolvedProblemRewardService.applySolvedProblem(player, "weekly-first", "easy", 90_000, day(i));
            }
            check(data.getCurrentStreak(player.getUUID()) == 7, "the streak reaches seven real calendar days");
            check(count(player, Items.DIAMOND) == 8 && count(player, Items.GOLD_INGOT) == 16,
                    "the first seventh-day solve gives eight diamonds and sixteen gold ingots");
            check(count(player, Items.EXPERIENCE_BOTTLE) >= 19, "the weekly bundle includes sixteen bottles plus daily supplies");
            player.getInventory().clearContent();
            SolvedProblemRewardService.applySolvedProblem(player, "weekly-second", "easy", 90_000, day(7) + 60_000);
            check(count(player, Items.DIAMOND) == 0, "another unique solve that day cannot claim the weekly bonus again");
            check(data.getCurrentStreak(player.getUUID()) == 7, "same-day solves cannot increase the streak");
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sustained_practice_delivers_growing_bundles_without_duplicate_claims(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AlgoCraftSavedData data = AlgoCraftSavedData.get(player.serverLevel());
            for (int i = 1; i <= 35; i++) {
                player.getInventory().clearContent();
                SolvedProblemRewardService.applySolvedProblem(player, "bundle-review", "easy", 90_000, day(i));
                check(data.getCurrentStreak(player.getUUID()) == i, "streak follows server calendar day " + i);
                if (i >= 3) {
                    check(count(player, Items.EMERALD) >= 2 && count(player, Items.EXPERIENCE_BOTTLE) >= 3,
                            "day " + i + " delivers daily streak supplies");
                }
                if (i >= 14 && i % 7 == 0) {
                    int expected = Math.min(i / 7, 4);
                    check(count(player, Items.NETHERITE_INGOT) == expected,
                            "day " + i + " delivers " + expected + " whole netherite ingots");
                    if (i == 21) check(count(player, Items.TOTEM_OF_UNDYING) == 1, "day 21 includes a totem");
                    if (i >= 28) check(count(player, Items.ENCHANTED_GOLDEN_APPLE) == 1,
                            "later weekly bundles include an enchanted golden apple");
                    player.getInventory().clearContent();
                    SolvedProblemRewardService.applySolvedProblem(player, "bundle-second-" + i, "easy", 90_000, day(i) + 60_000);
                    check(count(player, Items.NETHERITE_INGOT) == 0 && count(player, Items.TOTEM_OF_UNDYING) == 0
                                    && count(player, Items.ENCHANTED_GOLDEN_APPLE) == 0,
                            "another unique solve cannot duplicate day " + i + " bundle");
                    int xp = player.totalExperience;
                    SolvedProblemRewardService.applySolvedProblem(player, "bundle-second-" + i, "easy", 90_000, day(i) + 120_000);
                    check(xp == player.totalExperience, "same-problem repeat cannot farm extra XP or items");
                }
            }
            player.getInventory().clearContent();
            SolvedProblemRewardService.applySolvedProblem(player, "bundle-review", "easy", 90_000, day(37));
            check(data.getCurrentStreak(player.getUUID()) == 1 && count(player, Items.NETHERITE_INGOT) == 0,
                    "missing a server day resets the streak without another weekly bundle");
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void official_bank_completion_awards_the_netherite_trophy_once(GameTestHelper helper) {
        withPlayer(helper, player -> {
            List<Problem> bank = ProblemManager.getOfficialRepository() == null
                    ? initializeOfficialBank() : ProblemManager.getOfficialRepository().getProblems();
            check(bank.size() == 500, "completion is tested against the complete official bank");
            AlgoCraftSavedData data = AlgoCraftSavedData.get(player.serverLevel());
            for (int i = 0; i < bank.size() - 1; i++) {
                Problem problem = bank.get(i);
                data.setProblemSolved(player.getUUID(), problem.getId(), day(1));
                data.incrementTotalSolved(player.getUUID());
                data.incrementDifficultyCount(player.getUUID(), problem.getDifficulty());
            }
            Problem previous = bank.get(bank.size() - 2);
            SolvedProblemRewardService.applySolvedProblem(player, previous.getId(), previous.getDifficulty(), 90_000, day(2));
            check(!data.getAchievementManager().hasAchievement(player.getUUID(), AchievementRegistry.COMPLETIONIST),
                    "499 official solves cannot award full-bank completion");
            player.getInventory().clearContent();
            Problem last = bank.getLast();
            SolvedProblemRewardService.applySolvedProblem(player, last.getId(), last.getDifficulty(), 90_000, day(2));
            check(data.getAchievementManager().hasAchievement(player.getUUID(), AchievementRegistry.COMPLETIONIST),
                    "the final official solve triggers completion from the actual reward service");
            check(trophyCount(player, AchievementRegistry.COMPLETIONIST) == 1, "completion delivers its own named trophy");
            check(count(player, Items.DRAGON_EGG) == 1, "the 500-problem milestone delivers one dragon egg");
            SolvedProblemRewardService.applySolvedProblem(player, last.getId(), last.getDifficulty(), 90_000, day(3));
            check(trophyCount(player, AchievementRegistry.COMPLETIONIST) == 1 && count(player, Items.DRAGON_EGG) == 1,
                    "replaying the final problem cannot duplicate the trophy or dragon egg");
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void progress_and_achievements_are_shared_across_dimensions_and_survive_serialization(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AlgoCraftSavedData data = AlgoCraftSavedData.get(player.serverLevel());
            check(data == AlgoCraftSavedData.get(player.getServer().getLevel(Level.NETHER)), "nether shares overworld progress");
            check(data == AlgoCraftSavedData.get(player.getServer().getLevel(Level.END)), "end shares overworld progress");
            SolvedProblemRewardService.applySolvedProblem(player, "persisted-problem", "easy", 90_000, day(1));
            CompoundTag saved = data.save(new CompoundTag(), player.registryAccess());
            AlgoCraftSavedData restored = AlgoCraftSavedData.load(saved, player.registryAccess());
            check(restored.isProblemSolved(player.getUUID(), "persisted-problem"), "saved progress survives loading");
            check(restored.getTotalSolved(player.getUUID()) == 1, "unique solve count survives loading");
            check(restored.getAchievementManager().hasAchievement(player.getUUID(), AchievementRegistry.FIRST_SOLVE),
                    "earned achievements survive loading");
            check(new AlgoCraftSavedData().getAchievementManager().getPlayerAchievements(player.getUUID()).isEmpty(),
                    "a different world cannot inherit this world's achievement cache");
            AlgoCraftSavedData legacy = new AlgoCraftSavedData();
            legacy.setProblemSolved(player.getUUID(), "persisted-problem", day(2));
            legacy.setProblemSolved(player.getUUID(), "legacy-dimension-problem", day(2));
            legacy.updateStreak(player.getUUID(), day(2));
            legacy.getAchievementManager().loadPlayerAchievementsFromSet(player.getUUID(), Set.of(AchievementRegistry.APPRENTICE));
            restored.mergeLegacyProgress(legacy);
            restored.mergeLegacyProgress(legacy);
            check(restored.getPlayerProgress(player.getUUID()).size() == 2 && restored.getTotalSolved(player.getUUID()) == 2,
                    "legacy dimensions merge unique ids idempotently");
            check(restored.getLastSolvedTime(player.getUUID(), "persisted-problem") == day(2), "migration keeps the latest timestamp");
            check(restored.getAchievementManager().hasAchievement(player.getUUID(), AchievementRegistry.FIRST_SOLVE)
                            && restored.getAchievementManager().hasAchievement(player.getUUID(), AchievementRegistry.APPRENTICE),
                    "migration unions earned achievements without losing either record");
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "gameplay-reward-config")
    public static void rewards_disabled_preserves_progress_without_granting_items_or_xp(GameTestHelper helper) {
        boolean previous = Config.ENABLE_REWARDS.get();
        try {
            Config.ENABLE_REWARDS.set(false);
            withPlayer(helper, player -> {
                SolvedProblemRewardService.applySolvedProblem(player, "unrewarded-progress", "easy", 90_000, day(1));
                AlgoCraftSavedData data = AlgoCraftSavedData.get(player.serverLevel());
                check(data.isProblemSolved(player.getUUID(), "unrewarded-progress") && data.getTotalSolved(player.getUUID()) == 1,
                        "practice-only servers still save unique solved progress");
                check(player.getInventory().isEmpty() && player.totalExperience == 0, "disabled rewards give no items or experience");
            });
        } finally {
            Config.ENABLE_REWARDS.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rewards_drop_safely_when_inventory_is_full_and_bonus_books_have_enchantments(GameTestHelper helper) {
        withPlayer(helper, player -> {
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                player.getInventory().items.set(i, new ItemStack(Items.COBBLESTONE, 64));
            }
            List<ItemEntity> drops = new ArrayList<>();
            try {
                RewardSystem.RewardResult result = RewardSystem.giveRewards(player, "easy", true, true, 1, 1);
                drops.addAll(player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2)));
                check(drops.stream().filter(entity -> entity.getItem().is(Items.IRON_INGOT))
                        .mapToInt(entity -> entity.getItem().getCount()).sum() == 8, "full inventory drops all eight earned iron ingots");
                check(result.getItems().stream().filter(stack -> stack.is(Items.IRON_INGOT))
                        .mapToInt(ItemStack::getCount).sum() == 8, "reward summary preserves the original quantities");
                ItemStack book = RewardSystem.createBonusEnchantedBook(player, new Random(42));
                check(book.is(Items.ENCHANTED_BOOK) && !book.get(DataComponents.STORED_ENCHANTMENTS).isEmpty(),
                        "random enchanted books contain a real usable stored enchantment");
            } finally {
                drops.forEach(ItemEntity::discard);
            }
        });
        helper.succeed();
    }

    private static List<Problem> initializeOfficialBank() {
        ProblemManager.init();
        return ProblemManager.getOfficialRepository().getProblems();
    }

    @GameTest(template = TEMPLATE)
    public static void earned_trophies_place_save_sync_and_drop_with_their_metadata(GameTestHelper helper) {
        withPlayer(helper, player -> {
            BlockPos support = helper.absolutePos(new BlockPos(3, 1, 1));
            BlockPos placed = support.above();
            List<ItemEntity> ownedDrops = new ArrayList<>();
            try {
                for (TrophyItem.TrophyTier tier : TrophyItem.TrophyTier.values()) {
                    Item item = com.crabmods.algocraft.logic.ModItems.getTrophyForItemId(tier.getItemId());
                    AchievementRegistry.Achievement achievement = AchievementRegistry.getAll().stream()
                            .filter(a -> a.getTrophyItemId().equals(tier.getItemId())).findFirst().orElseThrow();
                    CompoundTag extra = new CompoundTag();
                    extra.putString("bank", "official");
                    ItemStack earned = TrophyItem.createTrophy(item, achievement, "Display Owner", player.getUUID().toString(), day(1), extra);
                    ItemStack expected = earned.copy();
                    helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
                    helper.getLevel().setBlock(placed, Blocks.AIR.defaultBlockState(), 3);
                    player.setItemInHand(InteractionHand.MAIN_HAND, earned);
                    player.setYRot(90);
                    var result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false)));
                    check(result.consumesAction(), tier + " trophy uses normal block placement");
                    check(player.getMainHandItem().isEmpty(), tier + " placement consumes exactly one earned trophy");
                    check(helper.getLevel().getBlockState(placed).getBlock() instanceof TrophyBlock, tier + " is a placed trophy block");
                    TrophyBlockEntity entity = (TrophyBlockEntity) helper.getLevel().getBlockEntity(placed);
                    check(entity != null && ItemStack.isSameItemSameComponents(expected, entity.getTrophy()), tier + " placement preserves engraving and model variant");
                    var registries = helper.getLevel().registryAccess();
                    CompoundTag saved = entity.saveWithoutMetadata(registries);
                    TrophyBlockEntity reloaded = new TrophyBlockEntity(placed, entity.getBlockState());
                    reloaded.loadWithComponents(saved, registries);
                    check(ItemStack.isSameItemSameComponents(expected, reloaded.getTrophy()), tier + " world save reload preserves trophy data");
                    TrophyBlockEntity clientCopy = new TrophyBlockEntity(placed, entity.getBlockState());
                    clientCopy.loadWithComponents(entity.getUpdateTag(registries), registries);
                    check(ItemStack.isSameItemSameComponents(expected, clientCopy.getTrophy()), tier + " update tag supplies the client renderer's variant");
                    check(helper.getLevel().destroyBlock(placed, true, player), tier + " trophy can be broken");
                    var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(placed).inflate(1), e -> e.getItem().is(item));
                    ownedDrops.addAll(drops);
                    check(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, tier + " breaking drops exactly one trophy");
                    check(ItemStack.isSameItemSameComponents(expected, drops.get(0).getItem()), tier + " breaking returns the original earned trophy");
                    drops.forEach(ItemEntity::discard);
                }
            } finally {
                ownedDrops.forEach(ItemEntity::discard);
                helper.getLevel().setBlock(placed, Blocks.AIR.defaultBlockState(), 3);
                helper.getLevel().setBlock(support, Blocks.AIR.defaultBlockState(), 3);
            }
        });
        helper.succeed();
    }

    private static long day(int day) {
        return LocalDate.of(2026, 8, 1).plusDays(day - 1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static long trophyCount(ServerPlayer player, String id) {
        return player.getInventory().items.stream().filter(stack -> id.equals(TrophyItem.getAchievementId(stack))).count();
    }

    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> scenario) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "closure-test"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.getInventory().clearContent();
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        try {
            scenario.accept(player);
        } finally {
            player.getInventory().clearContent();
            player.getServer().getPlayerList().remove(player);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }
}

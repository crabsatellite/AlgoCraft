package com.crabmods.algocraft;

import com.crabmods.algocraft.client.ClientHooks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AlgorithmComputerBlock extends Block {
    public static final MapCodec<AlgorithmComputerBlock> CODEC = simpleCodec(AlgorithmComputerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Matches scripts/generate_computer_model.py COLLISION: monitor, stand neck, stand foot, tower, desk mat with keyboard and mouse.
    private static final double[][] MODEL_BOXES = {
            {0.6, 6.0, 7.25, 11.4, 13.1, 9.75},
            {5.4, 0.6, 8.6, 6.6, 6.0, 9.6},
            {3.2, 0, 7.0, 8.8, 0.6, 10.2},
            {12.25, 0, 6.2, 15.6, 10.15, 15.4},
            {0.5, 0, 0.6, 12.8, 1.15, 6.2}
    };
    private static final VoxelShape SHAPE_NORTH = createShape(Direction.NORTH);
    private static final VoxelShape SHAPE_EAST = createShape(Direction.EAST);
    private static final VoxelShape SHAPE_SOUTH = createShape(Direction.SOUTH);
    private static final VoxelShape SHAPE_WEST = createShape(Direction.WEST);

    public AlgorithmComputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<AlgorithmComputerBlock> codec() {
        return CODEC;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            ClientHooks.openAlgorithmScreen();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape createShape(Direction facing) {
        VoxelShape[] boxes = new VoxelShape[MODEL_BOXES.length];
        for (int i = 0; i < MODEL_BOXES.length; i++) {
            double[] box = rotateBox(MODEL_BOXES[i], facing);
            boxes[i] = Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
        }
        return Shapes.or(boxes[0], java.util.Arrays.copyOfRange(boxes, 1, boxes.length));
    }

    private static double[] rotateBox(double[] box, Direction facing) {
        double x1 = box[0];
        double y1 = box[1];
        double z1 = box[2];
        double x2 = box[3];
        double y2 = box[4];
        double z2 = box[5];

        return switch (facing) {
            case EAST -> new double[]{16 - z2, y1, x1, 16 - z1, y2, x2};
            case SOUTH -> new double[]{16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1};
            case WEST -> new double[]{z1, y1, 16 - x2, z2, y2, 16 - x1};
            default -> new double[]{x1, y1, z1, x2, y2, z2};
        };
    }
}

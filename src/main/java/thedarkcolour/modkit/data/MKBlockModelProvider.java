/*
 * MIT License
 *
 * Copyright (c) 2026 thedarkcolour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package thedarkcolour.modkit.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Quadrant;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Block model and block state provider that wraps vanilla's BlockModelGenerators.
 * Provides the same API as the old NeoForge BlockStateProvider for easy migration.
 */
@SuppressWarnings("unused")
public class MKBlockModelProvider implements DataProvider {
    // Rotation mutators matching vanilla BlockModelGenerators
    private static final VariantMutator NOP = p -> p;
    private static final VariantMutator UV_LOCK = VariantMutator.UV_LOCK.withValue(true);
    private static final VariantMutator X_ROT_90 = VariantMutator.X_ROT.withValue(Quadrant.R90);
    private static final VariantMutator X_ROT_180 = VariantMutator.X_ROT.withValue(Quadrant.R180);
    private static final VariantMutator X_ROT_270 = VariantMutator.X_ROT.withValue(Quadrant.R270);
    private static final VariantMutator Y_ROT_90 = VariantMutator.Y_ROT.withValue(Quadrant.R90);
    private static final VariantMutator Y_ROT_180 = VariantMutator.Y_ROT.withValue(Quadrant.R180);
    private static final VariantMutator Y_ROT_270 = VariantMutator.Y_ROT.withValue(Quadrant.R270);

    private final PackOutput output;
    private final String modid;
    private final Consumer<MKBlockModelProvider> addBlockModels;

    // Storage for generated data
    private final Map<Block, BlockModelDefinitionGenerator> blockStates = new LinkedHashMap<>();
    private final Map<ResourceLocation, ModelInstance> models = new HashMap<>();

    @ApiStatus.Internal
    public MKBlockModelProvider(PackOutput output, String modid, Consumer<MKBlockModelProvider> addBlockModels) {
        this.output = output;
        this.modid = modid;
        this.addBlockModels = addBlockModels;
    }

    // ==================== Original MKBlockModelProvider methods ====================

    public ModelReference file(ResourceLocation resourceLoc) {
        return new ModelReference(resourceLoc);
    }

    public ModelReference modFile(String path) {
        return this.file(this.modBlock(path));
    }

    public ModelReference mcFile(String path) {
        return this.file(this.mcBlock(path));
    }

    public ResourceLocation modBlock(String name) {
        return this.modLoc("block/" + name);
    }

    public ResourceLocation mcBlock(String name) {
        return this.mcLoc("block/" + name);
    }

    public ResourceLocation key(Block block) {
        if (BuiltInRegistries.BLOCK.containsValue(block)) {
            return BuiltInRegistries.BLOCK.getKey(block);
        } else {
            throw new IllegalStateException("Block " + block + " does not exist in block registry");
        }
    }

    public String name(Block block) {
        return this.key(block).getPath();
    }

    public ResourceLocation modLoc(String name) {
        return ResourceLocation.fromNamespaceAndPath(modid, name);
    }

    public ResourceLocation mcLoc(String name) {
        return ResourceLocation.withDefaultNamespace(name);
    }

    public ResourceLocation blockTexture(Block block) {
        ResourceLocation name = key(block);
        return ResourceLocation.fromNamespaceAndPath(name.getNamespace(), "block/" + name.getPath());
    }

    // ==================== Model creation methods ====================

    /**
     * Creates a cube_all model for the block and returns its location.
     */
    public ResourceLocation cubeAll(Block block) {
        ResourceLocation modelLoc = ModelLocationUtils.getModelLocation(block);
        TextureMapping textureMapping = TextureMapping.cube(block);
        ModelTemplates.CUBE_ALL.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    /**
     * Creates a cube_all model with a custom texture.
     */
    public ResourceLocation cubeAll(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.ALL, texture);
        ModelTemplates.CUBE_ALL.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    /**
     * Creates a cube_column model.
     */
    public ResourceLocation cubeColumn(String name, ResourceLocation side, ResourceLocation end) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.END, end);
        ModelTemplates.CUBE_COLUMN.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    /**
     * Creates a cube_column_horizontal model.
     */
    public ResourceLocation cubeColumnHorizontal(String name, ResourceLocation side, ResourceLocation end) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.END, end);
        ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    // ==================== Block state methods matching BlockStateProvider ====================

    public void simpleBlock(Block block) {
        ResourceLocation model = cubeAll(block);
        simpleBlock(block, model);
    }

    public void simpleBlock(Block block, ModelReference model) {
        simpleBlock(block, model.location());
    }

    public void simpleBlock(Block block, ResourceLocation model) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model)));
    }

    public void simpleBlockItem(Block block, ModelReference model) {
        simpleBlockItem(block, model.location());
    }

    /**
     * Creates an item model that uses the given block model as its parent.
     * The item model JSON will simply be: {"parent": "modid:block/blockname"}
     */
    public void simpleBlockItem(Block block, ResourceLocation model) {
        ResourceLocation itemModelLoc = ModelLocationUtils.getModelLocation(block.asItem());
        // Create a simple item model that just references the block model as parent
        ModelInstance itemModel = () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", model.toString());
            return json;
        };
        models.put(itemModelLoc, itemModel);
    }

    public void simpleBlockWithItem(Block block, ModelReference model) {
        simpleBlockWithItem(block, model.location());
    }

    public void simpleBlockWithItem(Block block, ResourceLocation model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    // ==================== Axis/Log blocks ====================

    public void axisBlock(RotatedPillarBlock block) {
        axisBlock(block, blockTexture(block));
    }

    public void logBlock(RotatedPillarBlock block) {
        axisBlock(block, blockTexture(block), extend(blockTexture(block), "_top"));
    }

    public void axisBlock(RotatedPillarBlock block, ResourceLocation baseName) {
        axisBlock(block, extend(baseName, "_side"), extend(baseName, "_end"));
    }

    public void axisBlock(RotatedPillarBlock block, ResourceLocation side, ResourceLocation end) {
        ResourceLocation vertical = cubeColumn(name(block), side, end);
        ResourceLocation horizontal = cubeColumnHorizontal(name(block) + "_horizontal", side, end);
        axisBlockWithModels(block, vertical, horizontal);
    }

    public void axisBlockWithModels(RotatedPillarBlock block, ResourceLocation vertical, ResourceLocation horizontal) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(RotatedPillarBlock.AXIS)
                        .select(Direction.Axis.Y, BlockModelGenerators.plainVariant(vertical))
                        .select(Direction.Axis.Z, BlockModelGenerators.plainVariant(horizontal).with(X_ROT_90))
                        .select(Direction.Axis.X, BlockModelGenerators.plainVariant(horizontal).with(X_ROT_90).with(Y_ROT_90))));
    }

    // ==================== Horizontal/Directional blocks ====================

    public void horizontalBlock(Block block, ResourceLocation side, ResourceLocation front, ResourceLocation top) {
        ResourceLocation model = orientable(name(block), side, front, top);
        horizontalBlock(block, model);
    }

    public void horizontalBlock(Block block, ModelReference model) {
        horizontalBlock(block, model.location());
    }

    public void horizontalBlock(Block block, ResourceLocation model) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING)
                        .select(Direction.NORTH, BlockModelGenerators.plainVariant(model).with(Y_ROT_180))
                        .select(Direction.SOUTH, BlockModelGenerators.plainVariant(model))
                        .select(Direction.WEST, BlockModelGenerators.plainVariant(model).with(Y_ROT_270))
                        .select(Direction.EAST, BlockModelGenerators.plainVariant(model).with(Y_ROT_90))));
    }

    public void directionalBlock(Block block, ModelReference model) {
        directionalBlock(block, model.location());
    }

    public void directionalBlock(Block block, ResourceLocation model) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.FACING)
                        .select(Direction.DOWN, BlockModelGenerators.plainVariant(model).with(X_ROT_90))
                        .select(Direction.UP, BlockModelGenerators.plainVariant(model).with(X_ROT_270))
                        .select(Direction.NORTH, BlockModelGenerators.plainVariant(model))
                        .select(Direction.SOUTH, BlockModelGenerators.plainVariant(model).with(Y_ROT_180))
                        .select(Direction.WEST, BlockModelGenerators.plainVariant(model).with(Y_ROT_270))
                        .select(Direction.EAST, BlockModelGenerators.plainVariant(model).with(Y_ROT_90))));
    }

    // ==================== Stairs ====================

    public void stairsBlock(StairBlock block, ResourceLocation texture) {
        stairsBlock(block, texture, texture, texture);
    }

    public void stairsBlock(StairBlock block, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        String baseName = name(block);
        ResourceLocation stairs = stairs(baseName, side, bottom, top);
        ResourceLocation stairsInner = stairsInner(baseName + "_inner", side, bottom, top);
        ResourceLocation stairsOuter = stairsOuter(baseName + "_outer", side, bottom, top);
        stairsBlockWithModels(block, stairs, stairsInner, stairsOuter);
    }

    public void stairsBlockWithModels(StairBlock block, ResourceLocation stairs, ResourceLocation stairsInner, ResourceLocation stairsOuter) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(StairBlock.FACING, StairBlock.HALF, StairBlock.SHAPE)
                        .generate((facing, half, shape) -> {
                            int yRot = (int) facing.getClockWise().toYRot();
                            if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                                yRot += 270;
                            }
                            if (shape != StairsShape.STRAIGHT && half == Half.TOP) {
                                yRot += 90;
                            }
                            yRot %= 360;

                            ResourceLocation model = switch (shape) {
                                case STRAIGHT -> stairs;
                                case INNER_LEFT, INNER_RIGHT -> stairsInner;
                                case OUTER_LEFT, OUTER_RIGHT -> stairsOuter;
                            };

                            var variant = BlockModelGenerators.plainVariant(model);
                            if (half == Half.TOP) {
                                variant = variant.with(X_ROT_180);
                            }
                            if (yRot != 0) {
                                variant = variant.with(yRotation(yRot));
                            }
                            if (yRot != 0 || half == Half.TOP) {
                                variant = variant.with(UV_LOCK);
                            }
                            return variant;
                        })));
    }

    // ==================== Slabs ====================

    public void slabBlock(SlabBlock block, ResourceLocation doubleslab, ResourceLocation texture) {
        slabBlock(block, doubleslab, texture, texture, texture);
    }

    public void slabBlock(SlabBlock block, ResourceLocation doubleslab, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        ResourceLocation slabBottom = slab(name(block), side, bottom, top);
        ResourceLocation slabTop = slabTop(name(block) + "_top", side, bottom, top);
        slabBlock(block, slabBottom, slabTop, doubleslab);
    }

    public void slabBlock(SlabBlock block, ResourceLocation bottom, ResourceLocation top, ResourceLocation doubleslab) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(SlabBlock.TYPE)
                        .select(SlabType.BOTTOM, BlockModelGenerators.plainVariant(bottom))
                        .select(SlabType.TOP, BlockModelGenerators.plainVariant(top))
                        .select(SlabType.DOUBLE, BlockModelGenerators.plainVariant(doubleslab))));
    }

    // ==================== Buttons ====================

    public void buttonBlock(ButtonBlock block, ResourceLocation texture) {
        ResourceLocation button = button(name(block), texture);
        ResourceLocation buttonPressed = buttonPressed(name(block) + "_pressed", texture);
        buttonBlock(block, button, buttonPressed);
    }

    public void buttonBlock(ButtonBlock block, ResourceLocation button, ResourceLocation buttonPressed) {
        blockStates.put(block, BlockModelGenerators.createButton(block,
                BlockModelGenerators.plainVariant(button),
                BlockModelGenerators.plainVariant(buttonPressed)));
    }

    // ==================== Pressure Plates ====================

    public void pressurePlateBlock(PressurePlateBlock block, ResourceLocation texture) {
        ResourceLocation up = pressurePlate(name(block), texture);
        ResourceLocation down = pressurePlateDown(name(block) + "_down", texture);
        pressurePlateBlock(block, up, down);
    }

    public void pressurePlateBlock(PressurePlateBlock block, ResourceLocation up, ResourceLocation down) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(PressurePlateBlock.POWERED)
                        .select(false, BlockModelGenerators.plainVariant(up))
                        .select(true, BlockModelGenerators.plainVariant(down))));
    }

    // ==================== Signs ====================

    public void signBlock(StandingSignBlock signBlock, WallSignBlock wallSignBlock, ResourceLocation texture) {
        ResourceLocation sign = sign(name(signBlock), texture);
        signBlockWithModel(signBlock, wallSignBlock, sign);
    }

    public void signBlockWithModel(StandingSignBlock signBlock, WallSignBlock wallSignBlock, ResourceLocation sign) {
        simpleBlock(signBlock, sign);
        simpleBlock(wallSignBlock, sign);
    }

    public void hangingSignBlock(CeilingHangingSignBlock hangingSignBlock, WallHangingSignBlock wallHangingSignBlock, ResourceLocation texture) {
        ResourceLocation sign = sign(name(hangingSignBlock), texture);
        hangingSignBlockWithModel(hangingSignBlock, wallHangingSignBlock, sign);
    }

    public void hangingSignBlockWithModel(CeilingHangingSignBlock hangingSignBlock, WallHangingSignBlock wallHangingSignBlock, ResourceLocation sign) {
        simpleBlock(hangingSignBlock, sign);
        simpleBlock(wallHangingSignBlock, sign);
    }

    // ==================== Fences ====================

    public void fenceBlock(FenceBlock block, ResourceLocation texture) {
        String baseName = name(block);
        ResourceLocation post = fencePost(baseName + "_post", texture);
        ResourceLocation side = fenceSide(baseName + "_side", texture);
        fenceBlock(block, post, side);
    }

    public void fenceBlock(FenceBlock block, ResourceLocation post, ResourceLocation side) {
        blockStates.put(block, BlockModelGenerators.createFence(block,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(side)));
    }

    // ==================== Fence Gates ====================

    public void fenceGateBlock(FenceGateBlock block, ResourceLocation texture) {
        String baseName = name(block);
        ResourceLocation gate = fenceGate(baseName, texture);
        ResourceLocation gateOpen = fenceGateOpen(baseName + "_open", texture);
        ResourceLocation gateWall = fenceGateWall(baseName + "_wall", texture);
        ResourceLocation gateWallOpen = fenceGateWallOpen(baseName + "_wall_open", texture);
        fenceGateBlock(block, gate, gateOpen, gateWall, gateWallOpen);
    }

    public void fenceGateBlock(FenceGateBlock block, ResourceLocation gate, ResourceLocation gateOpen, ResourceLocation gateWall, ResourceLocation gateWallOpen) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(FenceGateBlock.IN_WALL, FenceGateBlock.OPEN)
                        .select(false, false, BlockModelGenerators.plainVariant(gate))
                        .select(false, true, BlockModelGenerators.plainVariant(gateOpen))
                        .select(true, false, BlockModelGenerators.plainVariant(gateWall))
                        .select(true, true, BlockModelGenerators.plainVariant(gateWallOpen)))
                .with(PropertyDispatch.modify(FenceGateBlock.FACING)
                        .select(Direction.SOUTH, Y_ROT_180)
                        .select(Direction.WEST, Y_ROT_270)
                        .select(Direction.NORTH, NOP)
                        .select(Direction.EAST, Y_ROT_90))
                .with(UV_LOCK));
    }

    // ==================== Walls ====================

    public void wallBlock(WallBlock block, ResourceLocation texture) {
        String baseName = name(block);
        ResourceLocation post = wallPost(baseName + "_post", texture);
        ResourceLocation side = wallSide(baseName + "_side", texture);
        ResourceLocation sideTall = wallSideTall(baseName + "_side_tall", texture);
        wallBlock(block, post, side, sideTall);
    }

    public void wallBlock(WallBlock block, ResourceLocation post, ResourceLocation side, ResourceLocation sideTall) {
        blockStates.put(block, BlockModelGenerators.createWall(block,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(side),
                BlockModelGenerators.plainVariant(sideTall)));
    }

    // ==================== Doors ====================

    public void doorBlock(DoorBlock block, ResourceLocation bottom, ResourceLocation top) {
        String baseName = name(block);
        ResourceLocation bottomLeft = doorBottomLeft(baseName + "_bottom_left", bottom, top);
        ResourceLocation bottomLeftOpen = doorBottomLeftOpen(baseName + "_bottom_left_open", bottom, top);
        ResourceLocation bottomRight = doorBottomRight(baseName + "_bottom_right", bottom, top);
        ResourceLocation bottomRightOpen = doorBottomRightOpen(baseName + "_bottom_right_open", bottom, top);
        ResourceLocation topLeft = doorTopLeft(baseName + "_top_left", bottom, top);
        ResourceLocation topLeftOpen = doorTopLeftOpen(baseName + "_top_left_open", bottom, top);
        ResourceLocation topRight = doorTopRight(baseName + "_top_right", bottom, top);
        ResourceLocation topRightOpen = doorTopRightOpen(baseName + "_top_right_open", bottom, top);
        doorBlock(block, bottomLeft, bottomLeftOpen, bottomRight, bottomRightOpen, topLeft, topLeftOpen, topRight, topRightOpen);
    }

    public void doorBlock(DoorBlock block, ResourceLocation bottomLeft, ResourceLocation bottomLeftOpen,
                          ResourceLocation bottomRight, ResourceLocation bottomRightOpen,
                          ResourceLocation topLeft, ResourceLocation topLeftOpen,
                          ResourceLocation topRight, ResourceLocation topRightOpen) {
        blockStates.put(block, BlockModelGenerators.createDoor(block,
                BlockModelGenerators.plainVariant(topLeft),
                BlockModelGenerators.plainVariant(topLeftOpen),
                BlockModelGenerators.plainVariant(topRight),
                BlockModelGenerators.plainVariant(topRightOpen),
                BlockModelGenerators.plainVariant(bottomLeft),
                BlockModelGenerators.plainVariant(bottomLeftOpen),
                BlockModelGenerators.plainVariant(bottomRight),
                BlockModelGenerators.plainVariant(bottomRightOpen)));
    }

    // ==================== Trapdoors ====================

    public void trapdoorBlock(TrapDoorBlock block, ResourceLocation texture, boolean orientable) {
        String baseName = name(block);
        ResourceLocation bottom = orientable ? trapdoorOrientableBottom(baseName + "_bottom", texture) : trapdoorBottom(baseName + "_bottom", texture);
        ResourceLocation top = orientable ? trapdoorOrientableTop(baseName + "_top", texture) : trapdoorTop(baseName + "_top", texture);
        ResourceLocation open = orientable ? trapdoorOrientableOpen(baseName + "_open", texture) : trapdoorOpen(baseName + "_open", texture);
        trapdoorBlock(block, bottom, top, open, orientable);
    }

    public void trapdoorBlock(TrapDoorBlock block, ResourceLocation bottom, ResourceLocation top, ResourceLocation open, boolean orientable) {
        blockStates.put(block, MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(TrapDoorBlock.FACING, TrapDoorBlock.HALF, TrapDoorBlock.OPEN)
                        .generate((facing, half, isOpen) -> {
                            int yRot = ((int) facing.toYRot() + 180) % 360;
                            int xRot = 0;
                            if (orientable && isOpen && half == Half.TOP) {
                                xRot = 180;
                                yRot = (yRot + 180) % 360;
                            }
                            if (!orientable && !isOpen) {
                                yRot = 0;
                            }

                            ResourceLocation model = isOpen ? open : (half == Half.TOP ? top : bottom);
                            var variant = BlockModelGenerators.plainVariant(model);
                            if (xRot != 0) {
                                variant = variant.with(X_ROT_180);
                            }
                            if (yRot != 0) {
                                variant = variant.with(yRotation(yRot));
                            }
                            return variant;
                        })));
    }

    // ==================== Panes ====================

    public void paneBlock(IronBarsBlock block, ResourceLocation pane, ResourceLocation edge) {
        String baseName = name(block);
        ResourceLocation post = panePost(baseName + "_post", pane, edge);
        ResourceLocation side = paneSide(baseName + "_side", pane, edge);
        ResourceLocation sideAlt = paneSideAlt(baseName + "_side_alt", pane, edge);
        ResourceLocation noSide = paneNoSide(baseName + "_noside", pane);
        ResourceLocation noSideAlt = paneNoSideAlt(baseName + "_noside_alt", pane);
        paneBlock(block, post, side, sideAlt, noSide, noSideAlt);
    }

    public void paneBlock(IronBarsBlock block, ResourceLocation post, ResourceLocation side, ResourceLocation sideAlt, ResourceLocation noSide, ResourceLocation noSideAlt) {
        MultiPartGenerator builder = MultiPartGenerator.multiPart(block)
                .with(BlockModelGenerators.plainVariant(post));

        for (var entry : PipeBlock.PROPERTY_BY_DIRECTION.entrySet()) {
            Direction dir = entry.getKey();
            if (dir.getAxis().isHorizontal()) {
                boolean alt = dir == Direction.SOUTH;
                int yRot = dir.getAxis() == Direction.Axis.X ? 90 : 0;

                builder.with(BlockModelGenerators.condition().term(entry.getValue(), true),
                        BlockModelGenerators.plainVariant(alt || dir == Direction.WEST ? sideAlt : side)
                                .with(yRotation(yRot)));
                builder.with(BlockModelGenerators.condition().term(entry.getValue(), false),
                        BlockModelGenerators.plainVariant(alt || dir == Direction.EAST ? noSideAlt : noSide)
                                .with(yRotation(dir == Direction.WEST ? 270 : (dir == Direction.SOUTH ? 90 : 0))));
            }
        }

        blockStates.put(block, builder);
    }

    // ==================== Model template helpers ====================

    private ResourceLocation orientable(String name, ResourceLocation side, ResourceLocation front, ResourceLocation top) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.FRONT, front)
                .put(TextureSlot.TOP, top);
        ModelTemplates.CUBE_ORIENTABLE.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation stairs(String name, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.BOTTOM, bottom)
                .put(TextureSlot.TOP, top);
        ModelTemplates.STAIRS_STRAIGHT.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation stairsInner(String name, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.BOTTOM, bottom)
                .put(TextureSlot.TOP, top);
        ModelTemplates.STAIRS_INNER.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation stairsOuter(String name, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.BOTTOM, bottom)
                .put(TextureSlot.TOP, top);
        ModelTemplates.STAIRS_OUTER.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation slab(String name, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.BOTTOM, bottom)
                .put(TextureSlot.TOP, top);
        ModelTemplates.SLAB_BOTTOM.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation slabTop(String name, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.BOTTOM, bottom)
                .put(TextureSlot.TOP, top);
        ModelTemplates.SLAB_TOP.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation button(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.BUTTON.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation buttonPressed(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.BUTTON_PRESSED.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation pressurePlate(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.PRESSURE_PLATE_UP.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation pressurePlateDown(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.PRESSURE_PLATE_DOWN.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation sign(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.PARTICLE, texture);
        ModelTemplates.PARTICLE_ONLY.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation fencePost(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.FENCE_POST.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation fenceSide(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.FENCE_SIDE.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation fenceGate(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.FENCE_GATE_CLOSED.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation fenceGateOpen(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.FENCE_GATE_OPEN.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation fenceGateWall(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.FENCE_GATE_WALL_CLOSED.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation fenceGateWallOpen(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        ModelTemplates.FENCE_GATE_WALL_OPEN.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation wallPost(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.WALL, texture);
        ModelTemplates.WALL_POST.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation wallSide(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.WALL, texture);
        ModelTemplates.WALL_LOW_SIDE.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation wallSideTall(String name, ResourceLocation texture) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.WALL, texture);
        ModelTemplates.WALL_TALL_SIDE.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation doorBottomLeft(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_BOTTOM_LEFT);
    }

    private ResourceLocation doorBottomLeftOpen(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_BOTTOM_LEFT_OPEN);
    }

    private ResourceLocation doorBottomRight(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_BOTTOM_RIGHT);
    }

    private ResourceLocation doorBottomRightOpen(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN);
    }

    private ResourceLocation doorTopLeft(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_TOP_LEFT);
    }

    private ResourceLocation doorTopLeftOpen(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_TOP_LEFT_OPEN);
    }

    private ResourceLocation doorTopRight(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_TOP_RIGHT);
    }

    private ResourceLocation doorTopRightOpen(String name, ResourceLocation bottom, ResourceLocation top) {
        return createDoorModel(name, bottom, top, ModelTemplates.DOOR_TOP_RIGHT_OPEN);
    }

    private ResourceLocation createDoorModel(String name, ResourceLocation bottom, ResourceLocation top, ModelTemplate template) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.BOTTOM, bottom)
                .put(TextureSlot.TOP, top);
        template.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation trapdoorBottom(String name, ResourceLocation texture) {
        return createTrapdoorModel(name, texture, ModelTemplates.TRAPDOOR_BOTTOM);
    }

    private ResourceLocation trapdoorTop(String name, ResourceLocation texture) {
        return createTrapdoorModel(name, texture, ModelTemplates.TRAPDOOR_TOP);
    }

    private ResourceLocation trapdoorOpen(String name, ResourceLocation texture) {
        return createTrapdoorModel(name, texture, ModelTemplates.TRAPDOOR_OPEN);
    }

    private ResourceLocation trapdoorOrientableBottom(String name, ResourceLocation texture) {
        return createTrapdoorModel(name, texture, ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM);
    }

    private ResourceLocation trapdoorOrientableTop(String name, ResourceLocation texture) {
        return createTrapdoorModel(name, texture, ModelTemplates.ORIENTABLE_TRAPDOOR_TOP);
    }

    private ResourceLocation trapdoorOrientableOpen(String name, ResourceLocation texture) {
        return createTrapdoorModel(name, texture, ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN);
    }

    private ResourceLocation createTrapdoorModel(String name, ResourceLocation texture, ModelTemplate template) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.TEXTURE, texture);
        template.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation panePost(String name, ResourceLocation pane, ResourceLocation edge) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.PANE, pane)
                .put(TextureSlot.EDGE, edge);
        ModelTemplates.STAINED_GLASS_PANE_POST.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation paneSide(String name, ResourceLocation pane, ResourceLocation edge) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.PANE, pane)
                .put(TextureSlot.EDGE, edge);
        ModelTemplates.STAINED_GLASS_PANE_SIDE.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation paneSideAlt(String name, ResourceLocation pane, ResourceLocation edge) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.PANE, pane)
                .put(TextureSlot.EDGE, edge);
        ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation paneNoSide(String name, ResourceLocation pane) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.PANE, pane);
        ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    private ResourceLocation paneNoSideAlt(String name, ResourceLocation pane) {
        ResourceLocation modelLoc = modLoc("block/" + name);
        TextureMapping textureMapping = new TextureMapping().put(TextureSlot.PANE, pane);
        ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(modelLoc, textureMapping, this::acceptModel);
        return modelLoc;
    }

    // ==================== Utility methods ====================

    private ResourceLocation extend(ResourceLocation rl, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath() + suffix);
    }

    private VariantMutator yRotation(int degrees) {
        return switch (degrees) {
            case 0 -> NOP;
            case 90 -> Y_ROT_90;
            case 180 -> Y_ROT_180;
            case 270 -> Y_ROT_270;
            default -> throw new IllegalArgumentException("Invalid Y rotation: " + degrees);
        };
    }

    private void acceptModel(ResourceLocation location, ModelInstance model) {
        models.put(location, model);
    }

    // ==================== DataProvider implementation ====================

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        blockStates.clear();
        models.clear();

        addBlockModels.accept(this);

        Path blockStatePath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
        Path modelPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);

        CompletableFuture<?>[] futures = new CompletableFuture<?>[blockStates.size() + models.size()];
        int i = 0;

        // Save block states
        for (var entry : blockStates.entrySet()) {
            ResourceLocation blockName = key(entry.getKey());
            Path outputPath = blockStatePath.resolve(blockName.getNamespace())
                    .resolve("blockstates")
                    .resolve(blockName.getPath() + ".json");
            BlockModelDefinition definition = entry.getValue().create();
            JsonElement json = BlockModelDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow();
            futures[i++] = DataProvider.saveStable(cache, json, outputPath);
        }

        // Save models
        for (var entry : models.entrySet()) {
            ResourceLocation modelLoc = entry.getKey();
            Path outputPath = modelPath.resolve(modelLoc.getNamespace())
                    .resolve("models")
                    .resolve(modelLoc.getPath() + ".json");
            JsonElement json = entry.getValue().get();
            futures[i++] = DataProvider.saveStable(cache, json, outputPath);
        }

        return CompletableFuture.allOf(futures);
    }

    @Override
    public String getName() {
        return "ModKit Block Models for mod '" + this.modid + "'";
    }

    // ==================== ModelReference - replacement for ModelFile ====================

    /**
         * Simple wrapper around a model ResourceLocation, replacing the old ModelFile concept.
         */
        public record ModelReference(ResourceLocation location) {
    }
}

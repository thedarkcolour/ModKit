package thedarkcolour.modkit.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import thedarkcolour.modkit.data.model.SafeBlockModelProvider;

import java.util.Objects;
import java.util.function.Consumer;

public class MKBlockModelProvider extends BlockStateProvider {
    private final Lazy<MKItemModelProvider> itemModels;
    private final String modid;
    private final Consumer<MKBlockModelProvider> addBlockModels;

    private final SafeBlockModelProvider blockModels;

    protected MKBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper, DataHelper dataHelper, String modid, Consumer<MKBlockModelProvider> addBlockModels) {
        super(output, modid, existingFileHelper);
        this.itemModels = Lazy.of(() -> {
            if (dataHelper.itemModels == null) {
                dataHelper.createItemModels(false, false, false, null);
                dataHelper.event.getGenerator().addProvider(dataHelper.event.includeClient(), dataHelper.itemModels);
            }
            return dataHelper.itemModels;
        });
        this.modid = modid;
        this.addBlockModels = addBlockModels;
        this.blockModels = new SafeBlockModelProvider(output, modid, existingFileHelper);
    }

    public ModelFile.UncheckedModelFile file(ResourceLocation resourceLoc) {
        return new ModelFile.UncheckedModelFile(resourceLoc);
    }

    public ModelFile.UncheckedModelFile modFile(String path) {
        return file(modBlock(path));
    }

    public ModelFile.UncheckedModelFile mcFile(String path) {
        return file(mcBlock(path));
    }

    public ResourceLocation modBlock(String name) {
        return modLoc("block/" + name);
    }

    public ResourceLocation mcBlock(String name) {
        return mcLoc("block/" + name);
    }

    public ResourceLocation key(Block block) {
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block));
    }

    public String name(Block block) {
        return key(block).getPath();
    }

    @Override
    public BlockModelBuilder cubeAll(Block block) {
        return models().cubeAll(name(block), blockTexture(block));
    }

    @Override
    public void simpleBlockItem(Block block, ModelFile model) {
        itemModels.get().getBuilder(key(block).getPath()).parent(model);
    }

    /**
     * @deprecated Do not use this method, use the MKItemModelProvider from your DataHelper
     */
    @Override
    @Deprecated
    public final ItemModelProvider itemModels() {
        try {
            if (!Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()).isInstance(this)) {
                throw new UnsupportedOperationException("Do not use MKBlockModelProvider to generate item models");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return super.itemModels();
    }

    @Override
    public BlockModelProvider models() {
        return blockModels;
    }

    @Override
    protected void registerStatesAndModels() {
        addBlockModels.accept(this);
    }

    @Override
    public String getName() {
        return "ModKit Block Models for mod '" + modid + "'";
    }
}

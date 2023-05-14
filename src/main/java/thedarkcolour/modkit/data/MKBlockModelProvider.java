package thedarkcolour.modkit.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.function.Consumer;

public class MKBlockModelProvider extends BlockStateProvider {
    private final String modid;
    private final Consumer<MKBlockModelProvider> addBlockModels;

    public MKBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper, String modid, Consumer<MKBlockModelProvider> addBlockModels) {
        super(output, modid, existingFileHelper);
        this.modid = modid;
        this.addBlockModels = addBlockModels;
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

    @Override
    protected void registerStatesAndModels() {
        addBlockModels.accept(this);
    }

    @Override
    public String getName() {
        return "ModKit Block Models for mod '" + modid + "'";
    }
}

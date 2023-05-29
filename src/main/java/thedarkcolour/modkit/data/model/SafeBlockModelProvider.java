package thedarkcolour.modkit.data.model;

import com.google.common.base.Preconditions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import thedarkcolour.modkit.ModKit;

import java.util.concurrent.CompletableFuture;

public class SafeBlockModelProvider extends BlockModelProvider {
    public SafeBlockModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {}

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf();
    }

    public ResourceLocation extendWithFolder(ResourceLocation loc) {
        if (loc.getPath().contains("/")) {
            return loc;
        }
        return new ResourceLocation(loc.getNamespace(), folder + "/" + loc.getPath());
    }

    @Override
    public BlockModelBuilder getBuilder(String path) {
        Preconditions.checkNotNull(path, "Path must not be null");
        ResourceLocation outputLoc = extendWithFolder(path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(modid, path));
        this.existingFileHelper.trackGenerated(outputLoc, MODEL);
        return generatedModels.computeIfAbsent(outputLoc, loc -> new SafeBlockModelBuilder(loc, existingFileHelper));
    }

    @Override
    public BlockModelBuilder nested() {
        return new SafeBlockModelBuilder(new ResourceLocation("dummy:dummy"), existingFileHelper);
    }

    @Override
    public BlockModelBuilder withExistingParent(String name, ResourceLocation parent) {
        try {
            return super.withExistingParent(name, parent);
        } catch (IllegalStateException e) {
            ModKit.LOGGER.error(e.getMessage());
            return getBuilder(name).parent(new ModelFile.UncheckedModelFile(parent));
        }
    }
}

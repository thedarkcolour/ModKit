package thedarkcolour.modkit.data.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import thedarkcolour.modkit.ModKit;

public class SafeBlockModelBuilder extends BlockModelBuilder {
    public SafeBlockModelBuilder(ResourceLocation outputLocation, ExistingFileHelper existingFileHelper) {
        super(outputLocation, existingFileHelper);
    }

    // Ignore exceptions and generate models anyway
    @Override
    public BlockModelBuilder texture(String key, ResourceLocation texture) {
        try {
            return super.texture(key, texture);
        } catch (IllegalArgumentException e) {
            ModKit.LOGGER.error(e.getMessage());
            textures.put(key, texture.toString());
            return this;
        }
    }
}

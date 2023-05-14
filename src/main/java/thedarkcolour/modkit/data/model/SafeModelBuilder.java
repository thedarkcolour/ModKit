package thedarkcolour.modkit.data.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import thedarkcolour.modkit.ModKit;

public class SafeModelBuilder<T extends ModelBuilder<T>> extends ModelBuilder<T> {
    protected boolean errored;

    protected SafeModelBuilder(ResourceLocation outputLocation, ExistingFileHelper existingFileHelper) {
        super(outputLocation, existingFileHelper);
    }

    // Ignore exceptions and generate models anyway
    @SuppressWarnings("unchecked")
    @Override
    public T texture(String key, ResourceLocation texture) {
        try {
            return super.texture(key, texture);
        } catch (IllegalArgumentException e) {
            this.errored = true;
            ModKit.LOGGER.error(e.getMessage());
            textures.put(key, texture.toString());
            return (T) this;
        }
    }
}

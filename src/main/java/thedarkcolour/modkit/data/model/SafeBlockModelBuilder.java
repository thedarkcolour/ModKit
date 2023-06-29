package thedarkcolour.modkit.data.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;

public class SafeBlockModelBuilder extends BlockModelBuilder {
    private final Logger logger;

    public SafeBlockModelBuilder(ResourceLocation outputLocation, Logger logger, ExistingFileHelper existingFileHelper) {
        super(outputLocation, existingFileHelper);
        this.logger = logger;
    }

    // Ignore exceptions and generate models anyway
    @Override
    public BlockModelBuilder texture(String key, ResourceLocation texture) {
        try {
            return super.texture(key, texture);
        } catch (IllegalArgumentException e) {
            this.logger.error(e.getMessage());
            this.textures.put(key, texture.toString());
            return this;
        }
    }
}

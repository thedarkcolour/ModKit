package thedarkcolour.modkit.data.model;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.slf4j.Logger;
import thedarkcolour.modkit.ModKit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SafeItemModelBuilder extends ModelBuilder<SafeItemModelBuilder> {
    private final Logger logger;
    protected List<OverrideBuilder> overrides = new ArrayList<>();

    public SafeItemModelBuilder(ResourceLocation outputLocation, Logger logger, ExistingFileHelper existingFileHelper) {
        super(outputLocation, existingFileHelper);
        this.logger = logger;
    }

    public OverrideBuilder override() {
        OverrideBuilder ret = new OverrideBuilder();
        overrides.add(ret);
        return ret;
    }

    /**
     * Get an existing override builder
     *
     * @param index the index of the existing override builder
     * @return the override builder
     * @throws IndexOutOfBoundsException if {@code} index is out of bounds
     */
    public OverrideBuilder override(int index) {
        Preconditions.checkElementIndex(index, overrides.size(), "override");
        return overrides.get(index);
    }

    @Override
    public JsonObject toJson() {
        JsonObject root = super.toJson();
        if (!overrides.isEmpty()) {
            JsonArray overridesJson = new JsonArray();
            overrides.stream().map(OverrideBuilder::toJson).forEach(overridesJson::add);
            root.add("overrides", overridesJson);
        }
        return root;
    }

    // Ignore exceptions and generate models anyway
    @Override
    public SafeItemModelBuilder texture(String key, ResourceLocation texture) {
        try {
            return super.texture(key, texture);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            textures.put(key, texture.toString());
            return this;
        }
    }

    public class OverrideBuilder {
        private ModelFile model;
        private final Map<ResourceLocation, Float> predicates = new LinkedHashMap<>();

        public OverrideBuilder model(ModelFile model) {
            this.model = model;
            model.assertExistence();
            return this;
        }

        public OverrideBuilder predicate(ResourceLocation key, float value) {
            this.predicates.put(key, value);
            return this;
        }

        public SafeItemModelBuilder end() { return SafeItemModelBuilder.this; }

        JsonObject toJson() {
            JsonObject ret = new JsonObject();
            JsonObject predicatesJson = new JsonObject();
            predicates.forEach((key, val) -> predicatesJson.addProperty(key.toString(), val));
            ret.add("predicate", predicatesJson);
            ret.addProperty("model", model.getLocation().toString());
            return ret;
        }
    }
}

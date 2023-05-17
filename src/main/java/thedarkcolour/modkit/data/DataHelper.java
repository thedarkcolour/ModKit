package thedarkcolour.modkit.data;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Use this in your event handler for GatherDataEvent. To avoid requiring ModKit in-game,
 * GatherDataEvent should be in a separate class from mod code. See the examples package.
 */
public class DataHelper {
    private final String modid;
    private final GatherDataEvent event;

    @Nullable
    private MKItemModelProvider itemModels;

    public DataHelper(String modid, GatherDataEvent event) {
        this.modid = modid;
        this.event = event;
    }

    public void createEnglish(boolean generateNames, @Nullable Consumer<MKEnglishProvider> addNames) {
        var english = new MKEnglishProvider(event.getGenerator().getPackOutput(), modid, generateNames, addNames);
        event.getGenerator().addProvider(event.includeClient(), english);
    }

    /**
     * Tells the data generation helper to generate item models for your mod.
     *
     * @param generate3dBlockItems If true, BlockItems are given generic 3D item models (ex. dirt, diamond block)
     * @param generate2dItems If true, non-BlockItems are given generic 2D item models (ex. gold ingot)
     *                        and SwordItem/PickaxeItem/ShovelItem/etc. items will be given handheld item models (ex. wooden pickaxe)
     * @param generateSpawnEggs If true, SpawnEggItems are given spawn egg item models
     * @param addItemModels Function (nullable) to add/override generated models for items. If you are using generate3dBlockItems
     *                      or generate2dItems and you want to change which model an item is generated with, do so in
     *                      this function.
     */
    public void createItemModels(boolean generate3dBlockItems, boolean generate2dItems, boolean generateSpawnEggs, @Nullable Consumer<MKItemModelProvider> addItemModels) {
        if (itemModels != null) {
            throw new IllegalStateException("Item models already created!");
        }
        itemModels = new MKItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper(), modid, generate3dBlockItems, generate2dItems, generateSpawnEggs, addItemModels);
        event.getGenerator().addProvider(event.includeClient(), itemModels);
    }

    public void createBlockModels(Consumer<MKBlockModelProvider> addBlockModels) {
        if (itemModels == null) {
            throw new IllegalStateException("Item models must be created first");
        }
        var blockModels = new MKBlockModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper(), modid, addBlockModels);
        event.getGenerator().addProvider(event.includeClient(), blockModels);
    }

    public void createRecipes(BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes) {
        var recipes = new MKRecipeProvider(event.getGenerator().getPackOutput(), addRecipes);
        event.getGenerator().addProvider(event.includeServer(), recipes);
    }

    static <T> void forModRegistry(IForgeRegistry<T> registry, String modid, BiConsumer<ResourceLocation, T> consumer) {
        for (var entry : registry.getEntries()) {
            var id = entry.getKey().location();

            if (id.getNamespace().equals(modid)) {
                consumer.accept(id, entry.getValue());
            }
        }
    }
}

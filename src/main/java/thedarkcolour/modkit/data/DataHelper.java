package thedarkcolour.modkit.data;

import com.google.common.base.Preconditions;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Use this in your event handler for GatherDataEvent. To avoid requiring ModKit in-game,
 * GatherDataEvent should be in a separate class from mod code. See the examples package.
 * <p>
 * Methods which specify what to generate follow the naming scheme "createBlah" and always
 * have a consumer (usually nullable) as their last parameter which gets run at the appropriate
 * time during data generation. For example, while you'd normally write translation code in
 * LanguageProvider#addTranslations, you would write it in the consumer you pass as the last
 * argument for {@link #createEnglish}, either with a method reference to a static method
 * "addTranslations" in your own data generation class. If you do not need any additional
 * translations aside from what's automatically generated by ModKit, you may simply pass null.
 */
public class DataHelper {
    protected final String modid;
    protected final GatherDataEvent event;

    @Nullable
    protected MKEnglishProvider english;
    @Nullable
    protected MKItemModelProvider itemModels;
    @Nullable
    protected MKBlockModelProvider blockModels;

    @Nullable
    protected MKRecipeProvider recipes;
    //@Nullable
    //private MKLootTableProvider lootTables;
    //@Nullable
    //private MKBlockTagProvider blockTags;
    //@Nullable
    //private MKItemTagProvider itemTags;

    public DataHelper(String modid, GatherDataEvent event) {
        this.modid = modid;
        this.event = event;
    }

    /**
     * Generates English language translations for your mod.
     *
     * @param generateNames   Whether to automatically generate names based on registry names.
     *                        For example, "minecraft:gold_ingot" would become "Gold Ingot".
     *                        Cases where this does not work is if your registry names do not follow
     *                        proper convention, or if you use acronyms: "projecte:emc_gun" would be "Emc Gun"
     *                        instead of "EMC Gun" and "mymod:exampleblock" would be "Exampleblock" instead of "Example Block"
     * @param addTranslations If not null, a consumer ran in LanguageProvider.addTranslations AFTER names are autogenerated,
     *                        so you can add in names that were generated incorrectly or names for things like Creative Tabs.
     */
    @SuppressWarnings("SpellCheckingInspection")
    public void createEnglish(boolean generateNames, @Nullable Consumer<MKEnglishProvider> addTranslations) {
        checkNotCreated(english, "English language");

        this.english = new MKEnglishProvider(event.getGenerator().getPackOutput(), modid, generateNames, addTranslations);
        event.getGenerator().addProvider(event.includeClient(), english);
    }

    /**
     * Generates item models for your mod.
     *
     * @param generate3dBlockItems If true, BlockItems are given generic 3D item models (ex. dirt, diamond block)
     * @param generate2dItems      If true, non-BlockItems are given generic 2D item models (ex. gold ingot)
     *                             and SwordItem/PickaxeItem/ShovelItem/etc. items will be given handheld item models (ex. wooden pickaxe)
     * @param generateSpawnEggs    If true, SpawnEggItems are given spawn egg item models
     * @param addItemModels        Function (nullable) to add/override generated models for items. If you are using generate3dBlockItems
     *                             or generate2dItems and you want to change which model an item is generated with, do so in
     *                             this function.
     */
    public void createItemModels(boolean generate3dBlockItems, boolean generate2dItems, boolean generateSpawnEggs, @Nullable Consumer<MKItemModelProvider> addItemModels) {
        checkNotCreated(itemModels, "Item models");

        this.itemModels = new MKItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper(), modid, generate3dBlockItems, generate2dItems, generateSpawnEggs, addItemModels);
        event.getGenerator().addProvider(event.includeClient(), itemModels);
    }

    /**
     * Generates block models for your mod. The MKBlockModelProvider provides some template models
     * you can use, but if you need anything more complicated it's best to write your own methods and
     * use them in the method you pass in for addBlockModels.
     *
     * @param addBlockModels Non-null function which receives the MKBlockModelProvider, which inherits methods from
     *                       BlockStateProvider and has some other methods to generate models.
     */
    public void createBlockModels(Consumer<MKBlockModelProvider> addBlockModels) {
        checkNotCreated(blockModels, "Block models");

        this.blockModels = new MKBlockModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper(), this, modid, addBlockModels);
    }

    /**
     * Generates recipes of all kinds for your mod.
     *
     * @param addRecipes Non-null function which receives the finished recipe writer and MKRecipeProvider, which has
     *                   built-in methods for common recipe types. If you need something more advanced, you may write
     *                   methods using the given finished recipe writer and call them in your addRecipes function.
     */
    public void createRecipes(BiConsumer<Consumer<FinishedRecipe>, MKRecipeProvider> addRecipes) {
        checkNotCreated(recipes, "Recipes");

        this.recipes = new MKRecipeProvider(event.getGenerator().getPackOutput(), addRecipes);
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

    private void checkNotCreated(@Nullable Object obj, String provider) {
        if (obj != null) {
            throw new IllegalStateException(provider + " generation already created!");
        }
    }
}

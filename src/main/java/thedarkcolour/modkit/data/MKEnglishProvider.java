package thedarkcolour.modkit.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.MKUtils;
import thedarkcolour.modkit.ModKit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * English language generation.
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class MKEnglishProvider extends LanguageProvider {
    private static final Field FIELD_DATA;

    private final String modid;
    private final boolean generateNames;
    @Nullable
    private final Consumer<MKEnglishProvider> addNames;
    private final Map<String, String> data;
    private final Map<Class<?>, Function<Object, String>> registryObjectHandlers;

    protected MKEnglishProvider(PackOutput output, String modid, boolean generateNames, @Nullable Consumer<MKEnglishProvider> addNames) {
        super(output, modid, "en_us");
        this.modid = modid;
        this.generateNames = generateNames;
        this.addNames = addNames;

        try {
            data = (Map<String, String>) FIELD_DATA.get(this);
        } catch (IllegalAccessException ignored) {
            throw new IllegalStateException("Failed to create MKEnglishProvider");
        }

        // Default handlers
        this.registryObjectHandlers = new HashMap<>();
        addTranslationHandler(Block.class, Block::getDescriptionId);
        addTranslationHandler(Item.class, Item::getDescriptionId);
        addTranslationHandler(EntityType.class, EntityType::getDescriptionId);
        addTranslationHandler(MobEffect.class, MobEffect::getDescriptionId);
        addTranslationHandler(Enchantment.class, Enchantment::getDescriptionId);
        addTranslationHandler(ItemStack.class, ItemStack::getDescriptionId);
    }

    @Override
    protected void addTranslations() {
        if (generateNames) {
            MKUtils.forModRegistry(ForgeRegistries.ITEMS, modid, (id, item) -> {
                String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
                add(item, name);
            });
            MKUtils.forModRegistry(ForgeRegistries.BLOCKS, modid, (id, block) -> {
                String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
                add(block, name);
            });
            MKUtils.forModRegistry(ForgeRegistries.ENTITY_TYPES, modid, (id, entityType) -> {
                String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
                add(entityType, name);
            });
            MKUtils.forModRegistry(ForgeRegistries.ENCHANTMENTS, modid, (id, enchantment) -> {
                String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
                add(enchantment, name);
            });
        }

        if (addNames != null) {
            addNames.accept(this);
        }
    }

    @Override
    public String getName() {
        return "ModKit Language: en_us for mod '" + modid + "'";
    }

    @Override
    public void add(String key, String value) {
        String old = data.put(key, value);
        if (old != null && !old.equals(value)) {
            ModKit.LOGGER.info("Overridden/duplicate translation key '" + key + "' (old: '" + old + "' new: '" + value + "')");
        }
    }

    /**
     * If you have a translatable object not included by default for adding translation keys,
     * add a mapping function here so that {@link #add(Object, String)} can properly add
     * translation keys for your specific type of registry object.
     *
     * @param type The superclass to handle (ex. Block, Item)
     * @param translationKeys The mapping function (ex. Block.getDescriptionId)
     * @param <T> The generic type for the registry object to make the compiler happy
     */
    public <T> void addTranslationHandler(Class<T> type, Function<T, String> translationKeys) {
        registryObjectHandlers.put(type, (Function<Object, String>) translationKeys);
    }

    /**
     * If your mod adds its own registry, you can register it here so that English names
     * will be automatically generated for its members.
     * TODO
     */
    public void addRegistryForGeneration(IForgeRegistry<?> registry) {
        throw new UnsupportedOperationException("TODO");
    }

    public void add(Object key, String name) {
        for (var entry : registryObjectHandlers.entrySet()) {
            if (entry.getKey().isInstance(key)) {
                add(entry.getValue().apply(key), name);
                return;
            }
        }

        throw new IllegalArgumentException("Unsupported registry object type for translation keys");
    }

    public void addGeneric(RegistryObject<?> key, String name) {
        add(key.get(), name);
    }

    /**
     * Do not include the translation for this key.
     * @param key The key to exclude
     */
    public void exclude(String key) {
        data.remove(key);
    }

    /**
     * Do not include the translation for the translation key of this registry object.
     * @param key The registry object whose key should be excluded.
     * @throws IllegalArgumentException if any is not a supported registry object
     */
    public void exclude(Object key) {
        for (var entry : registryObjectHandlers.entrySet()) {
            if (entry.getKey().isInstance(key)) {
                data.remove(entry.getValue().apply(key));
                return;
            }
        }

        throw new IllegalArgumentException("Unsupported registry object type for translation keys");
    }

    public void excludeGeneric(RegistryObject<?> key) {
        exclude(key.get());
    }

    static {
        try {
            FIELD_DATA = LanguageProvider.class.getDeclaredField("data");
            FIELD_DATA.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reflect into LanguageProvider, overrides of generated names will not work!", e);
        }
    }
}

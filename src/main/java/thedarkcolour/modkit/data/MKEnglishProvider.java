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
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import thedarkcolour.modkit.MKUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * English language generation.
 */
@SuppressWarnings({"unchecked", "deprecation", "unused"})
public class MKEnglishProvider extends LanguageProvider {
    private static final Field FIELD_DATA;

    static {
        try {
            FIELD_DATA = LanguageProvider.class.getDeclaredField("data");
            FIELD_DATA.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reflect into LanguageProvider, overrides of generated names will not work!", e);
        }
    }

    private final String modid;
    private final Logger logger;
    private final boolean generateNames;
    @Nullable
    private final Consumer<MKEnglishProvider> addNames;
    private final Map<String, String> data;
    private final Map<Class<?>, Function<Object, String>> registryObjectHandlers;
    private final List<IForgeRegistry<?>> autoTranslatedRegistries;

    protected MKEnglishProvider(PackOutput output, String modid, Logger logger, boolean generateNames, @Nullable Consumer<MKEnglishProvider> addNames) {
        super(output, modid, "en_us");
        this.modid = modid;
        this.logger = logger;
        this.generateNames = generateNames;
        this.addNames = addNames;

        try {
            data = (Map<String, String>) FIELD_DATA.get(this);
        } catch (IllegalAccessException ignored) {
            throw new IllegalStateException("Failed to create MKEnglishProvider");
        }

        // Default translation key handlers
        this.registryObjectHandlers = new HashMap<>();
        addTranslationHandler(Block.class, Block::getDescriptionId);
        addTranslationHandler(Item.class, Item::getDescriptionId);
        addTranslationHandler(EntityType.class, EntityType::getDescriptionId);
        addTranslationHandler(MobEffect.class, MobEffect::getDescriptionId);
        addTranslationHandler(Enchantment.class, Enchantment::getDescriptionId);
        addTranslationHandler(ItemStack.class, ItemStack::getDescriptionId);

        // Registries which will have names generated automatically
        this.autoTranslatedRegistries = new ArrayList<>();
        autoTranslatedRegistries.add(ForgeRegistries.ITEMS);
        autoTranslatedRegistries.add(ForgeRegistries.BLOCKS);
        autoTranslatedRegistries.add(ForgeRegistries.ENTITY_TYPES);
        autoTranslatedRegistries.add(ForgeRegistries.ENCHANTMENTS);
    }

    @Override
    protected void addTranslations() {
        if (addNames != null) {
            addNames.accept(this);
        }

        if (this.generateNames) {
            for (IForgeRegistry<?> registry : this.autoTranslatedRegistries) {
                MutableInt i = new MutableInt();

                try {
                    MKUtils.forModRegistry(registry, this.modid, (id, obj) -> {
                        String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
                        String key = getTranslationKey(obj);

                        if (!data.containsKey(key)) {
                            add(key, name);
                            i.increment();
                        }
                    });
                } catch (IllegalArgumentException e) {
                    this.logger.error("No translation key handler registered by mod {} for registry {} (use MKEnglishProvider.addTranslationHandler)", this.modid, registry.getRegistryName());
                    continue;
                }

                if (i.intValue() > 0) {
                    this.logger.info("Automatically generated {} names for mod {}'s entries in registry {}", i, this.modid, registry.getRegistryName());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "ModKit Language: en_us for mod '" + this.modid + "'";
    }

    @Override
    public void add(String key, String value) {
        String old = this.data.put(key, value);
        if (old != null && !old.equals(value)) {
            this.logger.info("Overridden/duplicate translation key '" + key + "' (old: '" + old + "' new: '" + value + "')");
        }
    }

    /**
     * If you have a translatable object not included by default for adding translation keys,
     * add a mapping function here so that {@link #add(Object, String)} can properly add
     * translation keys for your specific type of registry object.
     *
     * @param type            The superclass to handle (ex. Block, Item)
     * @param translationKeys The mapping function, returns a translation key for the object (ex. Block::getDescriptionId)
     * @param <T>             The generic type for the registry
     */
    public <T> void addTranslationHandler(Class<T> type, Function<T, String> translationKeys) {
        this.registryObjectHandlers.put(type, (Function<Object, String>) translationKeys);
    }

    /**
     * If your mod adds its own registry, you can register it here so that English names
     * will be automatically generated for its members.
     * <p>
     * IMPORTANT: Make sure to call {@link #addTranslationHandler(Class, Function)} to register a translation key
     * handler for your registry objects, otherwise ModKit will not be able to translate them!
     *
     * @param registry        The registry to iterate for automatically generating English names
     * @param <T>             The generic type for the registry
     */
    public <T> void addRegistryForAutoTranslation(IForgeRegistry<T> registry) {
        if (!this.generateNames) {
            this.logger.error("Tried to automatically generate English names for registry {}, but {} MKEnglishProvider has 'generateNames' set to false!", registry.getRegistryName(), this.modid);
            throw new IllegalStateException("MKEnglishGenerator.generateNames is false");
        } else {
            this.autoTranslatedRegistries.add(registry);
        }
    }

    public void add(Object key, String name) {
        add(getTranslationKey(key), name);
    }

    public void addGeneric(RegistryObject<?> key, String name) {
        add(key.get(), name);
    }

    public String getTranslationKey(Object object) {
        for (var entry : registryObjectHandlers.entrySet()) {
            if (entry.getKey().isInstance(object)) {
                return entry.getValue().apply(object);
            }
        }

        throw new IllegalArgumentException("Unsupported registry object type for translation keys");
    }
}

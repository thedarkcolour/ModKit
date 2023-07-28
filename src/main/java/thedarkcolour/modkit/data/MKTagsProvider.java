package thedarkcolour.modkit.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.Lazy;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * If you are a Kotlin user, this class implements {@link java.util.function.Function} with its "tag" method, so
 * you can write more concisely if you use "tagProvider()" instead of "tagProvider.tag()" to generate tags.
 *
 * @param <T> The type of objects this tag provider is generating tags for.
 */
@SuppressWarnings("deprecation")
public class MKTagsProvider<T> extends TagsProvider<T> implements Function<TagKey<T>, DirectTagAppender<T>> {
    private static final Function<EntityType<?>, ResourceKey<EntityType<?>>> ENTITY_TYPE_KEY_GETTER;
    private static final Function<Item, ResourceKey<Item>> ITEM_KEY_GETTER;
    private static final Function<Block, ResourceKey<Block>> BLOCK_KEY_GETTER;
    private static final Function<GameEvent, ResourceKey<GameEvent>> GAME_EVENT_KEY_GETTER;
    private static final Function<Fluid, ResourceKey<Fluid>> FLUID_KEY_GETTER;
    private final Function<T, ResourceKey<T>> keyGetter;
    private final BiConsumer<MKTagsProvider<T>, HolderLookup.Provider> addTags;
    private final Logger logger;

    // only used for ITEMS registry
    private final Lazy<CompletableFuture<TagLookup<Block>>> blockTags;
    private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy;

    @SuppressWarnings("unchecked")
    protected MKTagsProvider(DataHelper helper, ResourceKey<? extends Registry<T>> registry, BiConsumer<MKTagsProvider<T>, HolderLookup.Provider> addTags) {
        super(helper.event.getGenerator().getPackOutput(), registry, helper.event.getLookupProvider(), helper.modid, helper.event.getExistingFileHelper());

        this.keyGetter = chooseKeyGetter(registry);
        this.addTags = addTags;
        this.logger = helper.logger;
        this.tagsToCopy = new HashMap<>();
        this.blockTags = Lazy.of(() -> {
            var blockTags = ((MKTagsProvider<Block>) helper.tags.get(Registries.BLOCK));
            if (blockTags == null) {
                return CompletableFuture.completedFuture(null);
            } else {
                return blockTags.contentsGetter();
            }
        });
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        this.addTags.accept(this, lookup);
    }

    @Override
    public DirectTagAppender<T> tag(TagKey<T> tag) {
        var builder = this.getOrCreateRawBuilder(tag);
        return new DirectTagAppender<>(builder, this.keyGetter, this.modId);
    }

    @Override
    public DirectTagAppender<T> apply(TagKey<T> tag) {
        return tag(tag);
    }

    public void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
        if (this.registryKey.equals(Registries.ITEM)) {
            this.tagsToCopy.put(blockTag, itemTag);
        } else {
            logger.warn("Tried to copy a block tag in a tag provider for registry " + registryKey.location());
        }
    }

    @Override
    protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
        if (registryKey.equals(Registries.ITEM)) {
            return super.createContentsProvider().thenCombineAsync(blockTags.get(), (itemTags, blockTags) -> {
                // if no block tags are registered, this will be null per the second MKTagsProvider constructor
                if (blockTags != null) {
                    this.tagsToCopy.forEach((blockTag, itemTag) -> {
                        @SuppressWarnings("unchecked")
                        var builder = this.getOrCreateRawBuilder((TagKey<T>) itemTag);
                        var blockTagBuilder = blockTags.apply(blockTag).orElseThrow(() -> new IllegalStateException("Missing block tag " + itemTag.location())).build();

                        for (var entry : blockTagBuilder) {
                            builder.add(entry);
                        }
                    });
                }
                return itemTags;
            });
        } else {
            return super.createContentsProvider();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Function<T, ResourceKey<T>> chooseKeyGetter(ResourceKey<? extends Registry<T>> registry) {
        Function keyGetter;

        if (registry.equals(Registries.ENTITY_TYPE)) {
            keyGetter = ENTITY_TYPE_KEY_GETTER;
        } else if (registry.equals(Registries.BLOCK)) {
            keyGetter = BLOCK_KEY_GETTER;
        } else if (registry.equals(Registries.ITEM)) {
            keyGetter = ITEM_KEY_GETTER;
        } else if (registry.equals(Registries.FLUID)) {
            keyGetter = FLUID_KEY_GETTER;
        } else if (registry.equals(Registries.GAME_EVENT)) {
            keyGetter = GAME_EVENT_KEY_GETTER;
        } else {
            keyGetter = registryKeyGetter(registry);
        }

        return keyGetter;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static <T> Function<T, ResourceKey<T>> registryKeyGetter(ResourceKey<? extends Registry<T>> registry) {
        return obj -> RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registryOrThrow(registry).getResourceKey(obj).get();
    }

    static {
        ENTITY_TYPE_KEY_GETTER = entityType -> entityType.builtInRegistryHolder().key();
        ITEM_KEY_GETTER = item -> item.builtInRegistryHolder().key();
        BLOCK_KEY_GETTER = block -> block.builtInRegistryHolder().key();
        GAME_EVENT_KEY_GETTER = gameEvent -> gameEvent.builtInRegistryHolder().key();
        FLUID_KEY_GETTER = fluid -> fluid.builtInRegistryHolder().key();
    }
}

package thedarkcolour.modkit.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.extensions.IForgeIntrinsicHolderTagAppender;
import thedarkcolour.modkit.impl.MKTagsProvider;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ITagsProvider<T> extends Function<TagKey<T>, MKTagsProvider.DirectTagAppender<T>> {
    MKTagsProvider.DirectTagAppender<T> tag(TagKey<T> tag);

    void copy(TagKey<Block> blockTag, TagKey<Item> itemTag);

    @Override
    default MKTagsProvider.DirectTagAppender<T> apply(TagKey<T> tag) {
        return tag(tag);
    }

    interface IDirectTagAppender<T> extends IForgeIntrinsicHolderTagAppender<T> {
        IDirectTagAppender<T> add(T obj);

        /**
         * Implementation is annotated @SafeVarargs, so ignore the warning!
         * @param objs The objects to add to this tag
         */
        @SuppressWarnings("unchecked")
        IDirectTagAppender<T> add(T... objs);

        IDirectTagAppender<T> add(Supplier<? extends T> obj);

        IDirectTagAppender<T> addKey(ResourceKey<T> key);

        /**
         * Implementation is annotated @SafeVarargs, so ignore the warning!
         * @param keys The resource keys of the objects to add to this tag
         */
        @SuppressWarnings("unchecked")
        IDirectTagAppender<T> addKey(ResourceKey<T>... keys);

        IDirectTagAppender<T> addOptional(ResourceLocation id);

        IDirectTagAppender<T> addTag(TagKey<T> tag);

        IDirectTagAppender<T> addOptionalTag(ResourceLocation tagId);

        IDirectTagAppender<T> add(TagEntry tag);

        TagBuilder getInternalBuilder();

        String getModID();
    }
}

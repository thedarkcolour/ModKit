/*
 * MIT License
 *
 * Copyright (c) 2026 thedarkcolour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package thedarkcolour.modkit.data;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;

import java.util.function.Function;
import java.util.function.Supplier;

public class DirectTagAppender<T> implements TagAppender<T> {
    private final TagBuilder builder;
    private final String modId;
    private final Function<T, ResourceKey<T>> keyGetter;

    public DirectTagAppender(TagBuilder builder, Function<T, ResourceKey<T>> keyGetter, String modId) {
        this.builder = builder;
        this.modId = modId;
        this.keyGetter = keyGetter;
    }

    @Override
    public final DirectTagAppender<T> add(ResourceKey<T> key) {
        this.builder.addElement(key.identifier());
        return this;
    }

    @Override
    @SafeVarargs
    public final DirectTagAppender<T> add(ResourceKey<T>... toAdd) {
        for (ResourceKey<T> resourcekey : toAdd) {
            this.builder.addElement(resourcekey.identifier());
        }
        return this;
    }

    @Override
    public DirectTagAppender<T> addOptional(ResourceKey<T> element) {
        this.builder.addOptionalElement(element.identifier());
        return this;
    }

    public final DirectTagAppender<T> add(T obj) {
        this.add(keyGetter.apply(obj));
        return this;
    }

    @SafeVarargs
    public final DirectTagAppender<T> add(T... objs) {
        for (var obj : objs) {
            this.add(keyGetter.apply(obj));
        }
        return this;
    }

    public final DirectTagAppender<T> add(Supplier<? extends T> obj) {
        this.add(keyGetter.apply(obj.get()));
        return this;
    }

    @SafeVarargs
    public final DirectTagAppender<T> add(Supplier<? extends T>... objs) {
        for (var obj : objs) {
            this.add(keyGetter.apply(obj.get()));
        }
        return this;
    }

    public DirectTagAppender<T> addKey(ResourceKey<T> key) {
        this.add(key);
        return this;
    }

    @SafeVarargs
    public final DirectTagAppender<T> addKey(ResourceKey<T>... keys) {
        this.add(keys);
        return this;
    }

    public DirectTagAppender<T> addOptional(Identifier location) {
        this.builder.addOptionalElement(location);
        return this;
    }

    @Override
    public DirectTagAppender<T> addTag(TagKey<T> tag) {
        this.builder.addTag(tag.location());
        return this;
    }

    public DirectTagAppender<T> addOptionalTag(Identifier location) {
        this.builder.addOptionalTag(location);
        return this;
    }

    @Override
    public DirectTagAppender<T> addOptionalTag(TagKey<T> value) {
        return this.addOptionalTag(value.location());
    }

    @Override
    public DirectTagAppender<T> add(TagEntry tag) {
        builder.add(tag);
        return this;
    }

    public TagBuilder getInternalBuilder() {
        return builder;
    }

    public String getModID() {
        return modId;
    }

    @Override
    @SafeVarargs
    public final DirectTagAppender<T> addTags(TagKey<T>... values) {
        for (TagKey<T> value : values) {
            this.addTag(value);
        }
        return this;
    }

    @Override
    @SafeVarargs
    public final DirectTagAppender<T> addOptionalTags(TagKey<T>... values) {
        for (TagKey<T> value : values) {
            this.addOptionalTag(value.location());
        }
        return this;
    }

    @Override
    public DirectTagAppender<T> replace() {
        return replace(true);
    }

    @Override
    public DirectTagAppender<T> replace(boolean value) {
        this.getInternalBuilder().setReplace(value);
        return this;
    }

    public DirectTagAppender<T> remove(T entry) {
        remove(keyGetter.apply(entry));
        return this;
    }

    @SafeVarargs
    public final DirectTagAppender<T> remove(final T first, final T... entries) {
        this.remove(first);
        for (T entry : entries) {
            this.remove(entry);
        }
        return this;
    }

    /**
     * Adds a single element's ID to the tag json's remove list. Callable during datageneration.
     *
     * @param location The ID of the element to remove
     * @return The builder for chaining
     */
    public DirectTagAppender<T> remove(final Identifier location) {
        this.getInternalBuilder().removeElement(location);
        return this;
    }

    /**
     * Adds multiple elements' IDs to the tag json's remove list. Callable during datageneration.
     *
     * @param locations The IDs of the elements to remove
     * @return The builder for chaining
     */
    public DirectTagAppender<T> remove(final Identifier first, final Identifier... locations) {
        this.remove(first);
        for (Identifier location : locations) {
            this.remove(location);
        }
        return this;
    }

    /**
     * Adds a resource key to the tag json's remove list. Callable during datageneration.
     *
     * @param resourceKey The resource key of the element to remove
     * @return The appender for chaining
     */
    @Override
    public DirectTagAppender<T> remove(final ResourceKey<T> resourceKey) {
        this.remove(resourceKey.identifier());
        return this;
    }

    /**
     * Adds multiple resource keys to the tag json's remove list. Callable during datageneration.
     *
     * @param resourceKeys The resource keys of the elements to remove
     * @return The appender for chaining
     */
    @Override
    @SafeVarargs
    public final DirectTagAppender<T> remove(final ResourceKey<T> firstResourceKey, final ResourceKey<T>... resourceKeys) {
        this.remove(firstResourceKey.identifier());
        for (ResourceKey<T> resourceKey : resourceKeys) {
            this.remove(resourceKey.identifier());
        }
        return this;
    }

    /**
     * Adds a tag to the tag json's remove list. Callable during datageneration.
     *
     * @param tag The ID of the tag to remove
     * @return The builder for chaining
     */
    @Override
    public DirectTagAppender<T> remove(TagKey<T> tag) {
        this.getInternalBuilder().removeTag(tag.location());
        return this;
    }

    /**
     * Adds multiple tags to the tag json's remove list. Callable during datageneration.
     *
     * @param tags The IDs of the tags to remove
     * @return The builder for chaining
     */
    @Override
    @SafeVarargs
    public final DirectTagAppender<T> remove(TagKey<T> first, TagKey<T>... tags) {
        this.remove(first);
        for (TagKey<T> tag : tags) {
            this.remove(tag);
        }
        return this;
    }
}

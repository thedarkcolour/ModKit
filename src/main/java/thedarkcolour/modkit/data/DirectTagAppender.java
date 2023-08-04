/*
 * MIT License
 *
 * Copyright (c) 2023 thedarkcolour
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

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.extensions.IForgeTagAppender;

import java.util.function.Function;
import java.util.function.Supplier;

public class DirectTagAppender<T> extends TagsProvider.TagAppender<T> implements IForgeTagAppender<T> {
    private final Function<T, ResourceKey<T>> keyGetter;

    public DirectTagAppender(TagBuilder builder, Function<T, ResourceKey<T>> keyGetter, String modId) {
        super(builder, modId);
        this.keyGetter = keyGetter;
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

    @Override
    public DirectTagAppender<T> addOptional(ResourceLocation p_176840_) {
        super.addOptional(p_176840_);
        return this;
    }

    @Override
    public DirectTagAppender<T> addOptionalTag(ResourceLocation p_176842_) {
        super.addOptionalTag(p_176842_);
        return this;
    }

    @Override
    public DirectTagAppender<T> add(TagEntry tag) {
        super.add(tag);
        return this;
    }

    @SafeVarargs
    @Override
    public final DirectTagAppender<T> addTags(TagKey<T>... values) {
        super.addTags(values);
        return this;
    }

    @Override
    public DirectTagAppender<T> addTag(TagKey<T> tag) {
        super.addTag(tag);
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

    @Override
    public DirectTagAppender<T> replace() {
        super.replace();
        return this;
    }

    @Override
    public DirectTagAppender<T> replace(boolean value) {
        super.replace(value);
        return this;
    }

    @Override
    public DirectTagAppender<T> remove(ResourceLocation location) {
        super.remove(location);
        return this;
    }

    @Override
    public DirectTagAppender<T> remove(ResourceLocation first, ResourceLocation... locations) {
        super.remove(first, locations);
        return this;
    }

    @Override
    public DirectTagAppender<T> remove(ResourceKey<T> resourceKey) {
        super.remove(resourceKey);
        return this;
    }

    @SafeVarargs
    @Override
    public final DirectTagAppender<T> remove(ResourceKey<T> firstResourceKey, ResourceKey<T>... resourceKeys) {
        super.remove(firstResourceKey, resourceKeys);
        return this;
    }

    @Override
    public DirectTagAppender<T> remove(TagKey<T> tag) {
        super.remove(tag);
        return this;
    }

    @SafeVarargs
    @Override
    public final DirectTagAppender<T> remove(TagKey<T> first, TagKey<T>... tags) {
        super.remove(first, tags);
        return this;
    }
}

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

package thedarkcolour.modkit.data.recipe;

import net.minecraft.core.component.DataComponentType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// Do not use outside of data gen code, ModKit code does not ship with your mod
// A typesafe alternative to Map<DataComponentType<?>, ?>
public class ItemDataMap {
    @SuppressWarnings("rawtypes")
    private final Map<DataComponentType, Entry<?>> entries = new HashMap<>();

    public static ItemDataMap of() {
        return new ItemDataMap();
    }

    public static <T0> ItemDataMap of(DataComponentType<T0> type, T0 value) {
        ItemDataMap map = new ItemDataMap();
        map.put(type, value);
        return map;
    }

    public static <T0, T1> ItemDataMap of(DataComponentType<T0> type0, T0 value0, DataComponentType<T1> type1, T1 value1) {
        ItemDataMap map = new ItemDataMap();
        map.put(type0, value0);
        map.put(type1, value1);
        return map;
    }

    public static <T0, T1, T2> ItemDataMap of(DataComponentType<T0> type0, T0 value0, DataComponentType<T1> type1, T1 value1, DataComponentType<T2> type2, T2 value2) {
        ItemDataMap map = new ItemDataMap();
        map.put(type0, value0);
        map.put(type1, value1);
        map.put(type2, value2);
        return map;
    }

    public static <T0, T1, T2, T3> ItemDataMap of(DataComponentType<T0> type0, T0 value0, DataComponentType<T1> type1, T1 value1, DataComponentType<T2> type2, T2 value2, DataComponentType<T3> type3, T3 value3) {
        ItemDataMap map = new ItemDataMap();
        map.put(type0, value0);
        map.put(type1, value1);
        map.put(type2, value2);
        map.put(type3, value3);
        return map;
    }

    public static <T0, T1, T2, T3, T4> ItemDataMap of(DataComponentType<T0> type0, T0 value0, DataComponentType<T1> type1, T1 value1, DataComponentType<T2> type2, T2 value2, DataComponentType<T3> type3, T3 value3, DataComponentType<T4> type4, T4 value4) {
        ItemDataMap map = new ItemDataMap();
        map.put(type0, value0);
        map.put(type1, value1);
        map.put(type2, value2);
        map.put(type3, value3);
        map.put(type4, value4);
        return map;
    }

    public <T> void put(DataComponentType<T> type, T value) {
        entries.put(type, new Entry<>(type, value));
    }

    public Collection<Entry<?>> entrySet() {
        return entries.values();
    }

    public record Entry<T>(DataComponentType<T> type, T value) {
    }
}

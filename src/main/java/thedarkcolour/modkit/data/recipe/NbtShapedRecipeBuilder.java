package thedarkcolour.modkit.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.impl.MKRecipeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class NbtShapedRecipeBuilder extends NbtResultRecipe<NbtShapedRecipeBuilder> {
    private final List<String> rows = new ArrayList<>();
    private final Char2ObjectMap<Ingredient> key = new Char2ObjectOpenHashMap<>(9);
    private boolean showNotification = true;

    public NbtShapedRecipeBuilder(RecipeCategory category, ItemLike result, int resultCount, @Nullable CompoundTag resultNbt) {
        super(category, result.asItem(), resultCount, resultNbt);
    }

    public static NbtShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
        return shaped(category, result, 1);
    }

    public static NbtShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
        return shaped(category, result, count, null);
    }

    public static NbtShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count, @Nullable CompoundTag nbt) {
        return new NbtShapedRecipeBuilder(category, result, count, nbt);
    }

    public NbtShapedRecipeBuilder define(char symbol, RegistryObject<? extends ItemLike> item) {
        return define(symbol, item.get());
    }

    public NbtShapedRecipeBuilder define(char symbol, ItemLike item) {
        return define(symbol, Ingredient.of(item));
    }

    public NbtShapedRecipeBuilder define(char symbol, TagKey<Item> tag) {
        return define(symbol, Ingredient.of(tag));
    }

    public NbtShapedRecipeBuilder define(char symbol, Ingredient ingredient) {
        if (key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        } else if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            key.put(symbol, ingredient);
            return this;
        }
    }

    public NbtShapedRecipeBuilder pattern(String pattern) {
        if (!rows.isEmpty() && pattern.length() != rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            rows.add(pattern);
            return this;
        }
    }

    /**
     * Whether this recipe displays a toast on the top right of the player's screen upon unlocking.
     * The only Vanilla recipe where this is ever set to false is the Crafting Table recipe, which
     * is the only recipe the player has unlocked by default.
     */
    public NbtShapedRecipeBuilder showNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }

    /**
     * Using the given ingredients, attempts to generate a recipe criterion instead of requiring each
     * recipe to have {@link #unlockedBy} with tons of extra information.
     */
    public void attemptAutoCriterion() {
        for (Ingredient ingredient : key.values()) {
            if (MKRecipeProvider.unlockedByHaving(this, ingredient)) {
                return;
            }
        }
    }

    @Override
    public void save(Consumer<FinishedRecipe> writer, ResourceLocation id) {
        ensureValid(id);
        advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(RequirementsStrategy.OR);
        writer.accept(new Result(id, category, result, resultCount, resultNbt, group, rows, key, advancement, id.withPrefix("recipes/" + category.getFolderName() + "/"), showNotification));
    }

    @Override
    protected void ensureValid(ResourceLocation id) {
        if (rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped recipe " + id + "!");
        } else {
            var set = new CharOpenHashSet(key.keySet());
            set.remove(' ');

            for (String s : rows) {
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);

                    if (!key.containsKey(c) && c != ' ') {
                        throw new IllegalStateException("Pattern in recipe " + id + " uses undefined symbol '" + c + "'");
                    }

                    set.remove(c);
                }
            }

            if (!set.isEmpty()) {
                throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + id);
            } else if (rows.size() == 1 && rows.get(0).length() == 1) {
                throw new IllegalStateException("Shaped recipe " + id + " only takes in a single item - should it be a shapeless recipe instead?");
            }
        }

        super.ensureValid(id);
    }

    public static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final RecipeCategory category;
        private final Item result;
        private final int resultCount;
        @Nullable
        private final CompoundTag resultNbt;
        @Nullable
        private final String group;
        private final List<String> pattern;
        private final Char2ObjectMap<Ingredient> key;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;
        private final boolean showNotification;

        public Result(ResourceLocation id, RecipeCategory category, Item result, int resultCount, @Nullable CompoundTag resultNbt, @Nullable String group, List<String> pattern, Char2ObjectMap<Ingredient> key, Advancement.Builder advancement, ResourceLocation advancementId, boolean showNotification) {
            this.id = id;
            this.category = category;
            this.result = result;
            this.resultCount = resultCount;
            this.resultNbt = resultNbt;
            this.group = group;
            this.pattern = pattern;
            this.key = key;
            this.advancement = advancement;
            this.advancementId = advancementId;
            this.showNotification = showNotification;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("category", getCategoryName(category));

            if (group != null) {
                json.addProperty("group", group);
            }

            JsonArray patternObj = new JsonArray();

            for (String s : pattern) {
                patternObj.add(s);
            }

            json.add("pattern", patternObj);
            JsonObject keyObj = new JsonObject();

            for (var entry : key.char2ObjectEntrySet()) {
                keyObj.add(String.valueOf(entry.getCharKey()), entry.getValue().toJson());
            }

            json.add("key", keyObj);
            json.add("result", serializeResult(result, resultCount, resultNbt));
            json.addProperty("show_notification", showNotification);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return advancement.serializeToJson();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return advancementId;
        }
    }
}

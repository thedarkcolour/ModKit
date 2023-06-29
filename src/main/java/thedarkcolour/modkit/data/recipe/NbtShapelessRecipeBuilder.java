package thedarkcolour.modkit.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Same as regular ShapelessRecipeBuilder but you can add an NBT tag to the crafting result,
 * which is supported by Forge but is not included in Minecraft's data generation.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class NbtShapelessRecipeBuilder extends NbtResultRecipe<NbtShapelessRecipeBuilder> {
    private final List<Ingredient> ingredients = new ArrayList<>();

    public NbtShapelessRecipeBuilder(RecipeCategory category, Item result, int resultCount, @Nullable CompoundTag resultNbt) {
        super(category, result, resultCount, resultNbt);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), 1, null);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, CompoundTag tag) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), 1, tag);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int resultCount) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), resultCount, null);
    }

    public static NbtShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int resultCount, CompoundTag tag) {
        return new NbtShapelessRecipeBuilder(category, result.asItem(), resultCount, tag);
    }

    public NbtShapelessRecipeBuilder requires(TagKey<Item> tag) {
        return requires(Ingredient.of(tag));
    }

    public NbtShapelessRecipeBuilder requires(ItemLike tag) {
        return requires(Ingredient.of(tag));
    }

    public NbtShapelessRecipeBuilder requires(ItemLike tag, int quantity) {
        return requires(Ingredient.of(tag), quantity);
    }

    public NbtShapelessRecipeBuilder requires(Ingredient ingredient) {
        return requires(ingredient, 1);
    }

    public NbtShapelessRecipeBuilder requires(Ingredient ingredient, int quantity) {
        for (int i = 0; i < quantity; i++) {
            ingredients.add(ingredient);
        }

        return this;
    }

    @Override
    public void save(Consumer<FinishedRecipe> writer, ResourceLocation id) {
        ensureValid(id);
        advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(RequirementsStrategy.OR);
        writer.accept(new Result(id, category, this.result, resultCount, resultNbt, this.group, this.ingredients, this.advancement, id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
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
        private final List<Ingredient> ingredients;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, RecipeCategory category, Item result, int resultCount, @Nullable CompoundTag resultNbt, @Nullable String group, List<Ingredient> ingredients, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.category = category;
            this.result = result;
            this.resultCount = resultCount;
            this.resultNbt = resultNbt;
            this.group = group;
            this.ingredients = ingredients;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("category", getCategoryName(category));

            if (group != null) {
                json.addProperty("group", group);
            }
            JsonArray array = new JsonArray();

            for (var ingredient : ingredients) {
                array.add(ingredient.toJson());
            }

            json.add("ingredients", array);
            json.add("result", serializeResult(result, resultCount, resultNbt));
        }

        @Override
        public RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPELESS_RECIPE;
        }

        @Override
        public ResourceLocation getId() {
            return id;
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

package thedarkcolour.modkit.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.modkit.data.model.SafeItemModelBuilder;

import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public class MKItemModelProvider extends ModelProvider<SafeItemModelBuilder> {
    private final boolean generate3dBlockItems;
    private final boolean generate2dItems;
    private final boolean generateSpawnEggs;
    @Nullable
    private final Consumer<MKItemModelProvider> addItemModels;

    protected MKItemModelProvider(PackOutput output,
                                  ExistingFileHelper helper,
                                  String modid,
                                  boolean generate3dBlockItems,
                                  boolean generate2dItems,
                                  boolean generateSpawnEggs,
                                  @Nullable Consumer<MKItemModelProvider> addItemModels) {
        super(output, modid, "item", SafeItemModelBuilder::new, helper);

        this.generate3dBlockItems = generate3dBlockItems;
        this.generate2dItems = generate2dItems;
        this.generateSpawnEggs = generateSpawnEggs;
        this.addItemModels = addItemModels;
    }

    @Override
    public String getName() {
        return "ModKit Item Models for mod '" + modid + "'";
    }

    @Override
    protected void registerModels() {
        if (generate3dBlockItems || generate2dItems || generateSpawnEggs) {
            DataHelper.forModRegistry(ForgeRegistries.ITEMS, modid, (id, item) -> {
                if (generate3dBlockItems && item instanceof BlockItem) {
                    generic3d(id);
                } else if (generateSpawnEggs && item instanceof SpawnEggItem) {
                    spawnEgg(id);
                } else if (generate2dItems) {
                    if (item instanceof ShovelItem || item instanceof SwordItem || item instanceof HoeItem || item instanceof AxeItem || item instanceof PickaxeItem) {
                        handheld(id);
                    } else {
                        generic2d(id);
                    }
                }
            });
        }
        if (addItemModels != null) {
            addItemModels.accept(this);
        }
    }

    // Makes a 2d single layer item like hopper, gold ingot, or redstone dust for item model
    public SafeItemModelBuilder generic2d(RegistryObject<? extends Item> supplier) {
        return generic2d(supplier.getId());
    }

    public SafeItemModelBuilder generic2d(ResourceLocation item) {
        return getBuilder(item.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", new ResourceLocation(item.getNamespace(), "item/" + item.getPath()));
    }

    // Makes a 2d single layer item with special rotations like pickaxe or sword for item model
    public SafeItemModelBuilder handheld(RegistryObject<? extends Item> supplier) {
        return handheld(supplier.getId());
    }

    public SafeItemModelBuilder handheld(ResourceLocation id) {
        String path = id.getPath();

        // make a json file in model/items/ + path
        return getBuilder(path)
                .parent(new ModelFile.UncheckedModelFile("item/handheld")) // handheld
                .texture("layer0", new ResourceLocation(id.getNamespace(), "item/" + path));
    }

    // Makes a 3d cube of a block for item model
    public SafeItemModelBuilder generic3d(RegistryObject<? extends Item> supplier) {
        String path = supplier.getId().getPath();
        return withExistingParent(path, new ResourceLocation(supplier.getId().getNamespace(), "block/" + path));
    }

    public SafeItemModelBuilder generic3d(ResourceLocation id) {
        return withExistingParent(id.getPath(), new ResourceLocation(id.getNamespace(), "block/" + id));
    }

    private SafeItemModelBuilder spawnEgg(RegistryObject<? extends Item> supplier) {
        return spawnEgg(supplier.getId());
    }

    private SafeItemModelBuilder spawnEgg(ResourceLocation id) {
        return getBuilder(id.getPath()).parent(new ModelFile.UncheckedModelFile("item/template_spawn_egg"));
    }
}

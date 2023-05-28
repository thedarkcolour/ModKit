package thedarkcolour.modkit;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thedarkcolour.modkit.item.ClearWandItem;
import thedarkcolour.modkit.item.CloneWandItem;
import thedarkcolour.modkit.item.DistanceWandItem;
import thedarkcolour.modkit.item.FillWandItem;

@Mod(ModKit.ID)
public class ModKit {
    public static final String ID = "modkit";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<Item> FILL_WAND = ITEMS.register("fill_wand", () -> new FillWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CLEAR_WAND = ITEMS.register("clear_wand", () -> new ClearWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DISTANCE_WAND = ITEMS.register("distance_wand", () -> new DistanceWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CLONE_WAND = ITEMS.register("clone_wand", () -> new CloneWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public ModKit() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(ModKit::addCreativeTab);
    }

    private static void addCreativeTab(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new ResourceLocation(ModKit.ID, ModKit.ID), builder -> {
            builder.icon(() -> new ItemStack(CLONE_WAND.get()));
            builder.title(Component.translatable("itemGroup.modkit"));
            builder.displayItems((params, output) -> {
                output.accept(FILL_WAND.get());
                output.accept(CLEAR_WAND.get());
                output.accept(DISTANCE_WAND.get());
                output.accept(CLONE_WAND.get());
            });
        });
    }
}

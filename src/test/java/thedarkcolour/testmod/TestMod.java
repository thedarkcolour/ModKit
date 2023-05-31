package thedarkcolour.testmod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thedarkcolour.testmod.data.DataGen;

@Mod(TestMod.ID)
public class TestMod {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TestMod.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TestMod.ID);

    public static final RegistryObject<Block> RED_BLOCK = BLOCKS.register("red_block", () -> new Block(BlockBehaviour.Properties.of(Material.CLAY).strength(2.0f).sound(SoundType.HONEY_BLOCK))) ;
    public static final RegistryObject<Item> RED_BLOCK_ITEM = ITEMS.register("red_block", () -> new BlockItem(RED_BLOCK.get(), new Item.Properties().stacksTo(3)));

    public static final RegistryObject<Item> ORANGE = ITEMS.register("orange", () -> new Item(new Item.Properties().stacksTo(6)));
    public static final RegistryObject<Block> ORANGE_BLOCK = BLOCKS.register("orange_block", () -> new Block(BlockBehaviour.Properties.of(Material.CLAY).strength(12.0f).sound(SoundType.HONEY_BLOCK))) ;
    public static final RegistryObject<Item> ORANGE_BLOCK_ITEM = ITEMS.register("orange_block", () -> new BlockItem(ORANGE_BLOCK.get(), new Item.Properties().stacksTo(10)));

    public static final String ID = "testmod";

    public TestMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modBus);
        ITEMS.register(modBus);

        modBus.addListener((GatherDataEvent event) -> DataGen.gatherData(event));
    }
}

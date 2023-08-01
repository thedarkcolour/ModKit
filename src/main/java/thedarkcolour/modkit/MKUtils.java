package thedarkcolour.modkit;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MKUtils {
    public static <T> void forModRegistry(Registry<T> registry, String modid, BiConsumer<ResourceLocation, T> consumer) {
        for (var entry : registry.entrySet()) {
            var id = entry.getKey().location();

            if (id.getNamespace().equals(modid)) {
                consumer.accept(id, entry.getValue());
            }
        }
    }

    public static void forInDevMods(Consumer<IModInfo> action) {
        for (IModFileInfo modsToml : ModList.get().getModFiles()) {
            for (IModInfo modInfo : modsToml.getMods()) {
                if (!modsToml.getFile().getFilePath().toAbsolutePath().toString().contains(".jar")) {
                    action.accept(modInfo);
                }
            }
        }
    }
}

package thedarkcolour.modkit.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;

import java.util.List;
import java.util.Set;

public class MKLootTableProvider extends LootTableProvider {
    public MKLootTableProvider(PackOutput pOutput, List<SubProviderEntry> pSubProviders) {
        super(pOutput, Set.of(), pSubProviders);
    }
}

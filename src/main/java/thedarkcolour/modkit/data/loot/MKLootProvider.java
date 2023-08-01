package thedarkcolour.modkit.data.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MKLootProvider extends LootTableProvider {
    private final List<SubProviderEntry> providers = new ArrayList<>();

    public MKLootProvider(PackOutput output) {
        super(output, Set.of(), null);
    }

    @Override
    public List<SubProviderEntry> getTables() {
        return providers;
    }
}

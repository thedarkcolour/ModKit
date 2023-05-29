package thedarkcolour.modkit.data;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;

public class MKTagProvider implements DataProvider {
    private final String modid;

    protected MKTagProvider(String modid) {
        this.modid = modid;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        return null;
    }

    @Override
    public String getName() {
        return "ModKit Tags for modid '" + modid + "'";
    }
}

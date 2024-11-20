package thedarkcolour.modkit.data;

import com.mojang.serialization.JsonOps;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MKDamageTypeProvider extends JsonCodecProvider<DamageType> {
    private final Consumer<MKDamageTypeProvider> addTypes;
    private final HashMap<ResourceLocation, DamageTypeBuilder> types = new HashMap<>();

    public MKDamageTypeProvider(PackOutput output, ExistingFileHelper helper, String modid, Consumer<MKDamageTypeProvider> addTypes) {
        super(output, helper, modid, JsonOps.INSTANCE, PackType.SERVER_DATA, "damage_type", DamageType.CODEC, Map.of());
        this.addTypes = addTypes;
    }

    public DamageTypeBuilder add(ResourceKey<DamageType> type) {
        return this.types.computeIfAbsent(type.location(), key -> new DamageTypeBuilder(this.modid + '.' + type.location().getPath()));
    }

    @Override
    protected void gather(BiConsumer<ResourceLocation, DamageType> consumer) {
        this.addTypes.accept(this);
        this.types.forEach((id, builder) -> consumer.accept(id, builder.createType()));
    }

    public static class DamageTypeBuilder {
        private final String msgId;

        private DamageScaling scaling = DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER;
        private float exhaustion = 0.0f;
        private DamageEffects effects = DamageEffects.HURT;
        private DeathMessageType deathMessageType = DeathMessageType.DEFAULT;

        public DamageTypeBuilder(String msgId) {
            this.msgId = msgId;
        }

        public DamageTypeBuilder scaling(DamageScaling scaling) {
            this.scaling = scaling;
            return this;
        }

        public DamageTypeBuilder exhaustion(float exhaustion) {
            this.exhaustion = exhaustion;
            return this;
        }

        public DamageTypeBuilder effects(DamageEffects effects) {
            this.effects = effects;
            return this;
        }

        public DamageTypeBuilder deathMessageType(DeathMessageType deathMessageType) {
            this.deathMessageType = deathMessageType;
            return this;
        }

        public DamageType createType() {
            return new DamageType(this.msgId, this.scaling, this.exhaustion, this.effects, this.deathMessageType);
        }
    }
}

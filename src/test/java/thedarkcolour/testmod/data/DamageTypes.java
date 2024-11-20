package thedarkcolour.testmod.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import thedarkcolour.modkit.data.MKDamageTypeProvider;
import thedarkcolour.testmod.TestMod;

public class DamageTypes {
    public static final ResourceKey<DamageType> TEST_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(TestMod.ID, "test_damage"));

    public static void addTypes(MKDamageTypeProvider types) {
        types.add(TEST_DAMAGE)
                .exhaustion(0.5f)
                .effects(DamageEffects.FREEZING)
                .deathMessageType(DeathMessageType.INTENTIONAL_GAME_DESIGN)
                .scaling(DamageScaling.NEVER);
    }
}

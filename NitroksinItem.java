package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class NitroksinItem extends PotionItem {

    /*
     * Nitroksin etkisinin süresi:
     *
     * 20 tick = 1 saniye
     * 20 * 30 = 30 saniye
     */
    private static final int EFFECT_DURATION = 20 * 30;

    /*
     * Lazerin maksimum menzili
     */
    private static final double LASER_RANGE = 30.0D;

    /*
     * Patlama gücü.
     * Creeper yaklaşık 3 civarındadır.
     */
    private static final float EXPLOSION_POWER = 2.5F;

    /*
     * Kırmızı parçacık ayarı.
     *
     * DustParticleOptions:
     * RGB değerleri 0.0 - 1.0 arasındadır.
     *
     * 1, 0, 0 = kırmızı
     */
    private static final DustParticleOptions RED_LASER =
            new DustParticleOptions(
                    1.0F,
                    0.0F,
                    0.0F,
                    1.5F
            );

    public NitroksinItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Oyuncu Nitroksin'i içtiğinde çalışır.
     */
    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ) {

        if (entity instanceof Player player) {

            /*
             * Blindness
             *
             * Süre: 30 saniye
             * Amplifier: 0 -> Seviye I
             */
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.BLINDNESS,
                            EFFECT_DURATION,
                            0,
                            false,
                            true,
                            true
                    )
            );

            /*
             * Night Vision
             *
             * Süre: 30 saniye
             */
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.NIGHT_VISION,
                            EFFECT_DURATION,
                            0,
                            false,
                            true,
                            true
                    )
            );
        }

        /*
         * PotionItem'in normal davranışına benzer şekilde
         * içilen şişenin yerine cam şişe veriyoruz.
         */
        if (entity instanceof Player player) {

            if (!player.getAbilities().instabuild) {

                stack.shrink(1);

                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);

                if (stack.isEmpty()) {
                    return bottle;
                }

                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }

            return stack;
        }

        stack.shrink(1);
        return stack;
    }

    /**
     * Oyuncu Nitroksin'i eline alıp sağ tıkladığında
     * içmeye başlamasını sağlar.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack = player.getItemInHand(hand);

        /*
         * Vanilla potion içme süresini kullanıyoruz.
         * 32 tick yaklaşık 1.6 saniyedir.
         */
        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    /**
     * Oyuncu item'i kullanırken içme animasyonunun
     * kaç tick süreceğini belirler.
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    /**
     * Oyuncunun baktığı noktayı bulur.
     */
    private static BlockHitResult getLookHit(
            Player player
    ) {

        Vec3 start = player.getEyePosition();

        Vec3 look = player.getViewVector(1.0F);

        Vec3 end = start.add(
                look.x * LASER_RANGE,
                look.y * LASER_RANGE,
                look.z * LASER_RANGE
        );

        return player.level().clip(
                new ClipContext(
                        start,
                        end,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );
    }

    /**
     * Oyuncunun baktığı noktaya kırmızı lazer parçacıkları
     * oluşturur.
     */
    private static void spawnLaser(
            ServerLevel level,
            Player player,
            Vec3 target
    ) {

        Vec3 start = player.getEyePosition();

        Vec3 direction = target.subtract(start);

        double distance = direction.length();

        if (distance <= 0.1D) {
            return;
        }

        direction = direction.normalize();

        /*
         * Parçacıkların aralığı.
         *
         * Daha küçük değer = daha yoğun lazer.
         */
        double particleStep = 0.25D;

        int particleCount =
                (int) (distance / particleStep);

        for (int i = 0; i <= particleCount; i++) {

            double currentDistance =
                    i * particleStep;

            Vec3 particlePosition =
                    start.add(
                            direction.x * currentDistance,
                            direction.y * currentDistance,
                            direction.z * currentDistance
                    );

            /*
             * ServerLevel#sendParticles:
             *
             * 1. parametre = parçacık tipi
             * 2-4 = konum
             * 5 = parçacık sayısı
             * 6-8 = rastgele dağılım
             * 9 = hız
             */
            level.sendParticles(
                    RED_LASER,
                    particlePosition.x,
                    particlePosition.y,
                    particlePosition.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    /**
     * Patlama oluşturur.
     */
    private static void createExplosion(
            ServerLevel level,
            Player player,
            Vec3 target
    ) {

        level.explode(
                player,

                // Patlamanın konumu
                target.x,
                target.y,
                target.z,

                // Patlama gücü
                EXPLOSION_POWER,

                /*
                 * Yangın çıkmasın.
                 */
                false,

                /*
                 * MOB:
                 *
                 * Patlama canlılara hasar verir fakat
                 * blokları oyuncu kaynaklı patlama gibi
                 * gereksiz şekilde kırmaz.
                 */
                Level.ExplosionInteraction.MOB
        );
    }

    /**
     * Nitroksin etkisi aktif olan oyuncuların
     * baktığı yere lazer gönderen sistem.
     *
     * Bu metodun Forge event sistemine bağlanması gerekir.
     */
    public static void handleNitroksinLaser(
            TickEvent.PlayerTickEvent event
    ) {

        /*
         * Sadece tick'in END aşamasında çalıştır.
         */
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        /*
         * Client tarafında patlama oluşturmuyoruz.
         * Patlama ve parçacık işlemlerini server yapıyor.
         */
        if (player.level().isClientSide()) {
            return;
        }

        /*
         * Nitroksin'in verdiği Blindness efekti var mı?
         *
         * Oyuncunun üzerinde Blindness varsa Nitroksin
         * aktif kabul edilir.
         */
        if (!player.hasEffect(MobEffects.BLINDNESS)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level =
                serverPlayer.serverLevel();

        /*
         * Oyuncunun baktığı bloğu bul.
         */
        BlockHitResult hit =
                getLookHit(serverPlayer);

        Vec3 target;

        if (hit.getType() == HitResult.Type.MISS) {

            /*
             * Oyuncu 30 blok içinde bir bloğa bakmıyorsa,
             * lazer 30 blok ileride biter.
             */
            Vec3 start =
                    serverPlayer.getEyePosition();

            Vec3 direction =
                    serverPlayer.getViewVector(1.0F);

            target = start.add(
                    direction.x * LASER_RANGE,
                    direction.y * LASER_RANGE,
                    direction.z * LASER_RANGE
            );

        } else {

            /*
             * Bloğun vurulan yüzeyinin konumunu al.
             */
            target = hit.getLocation();
        }

        /*
         * Kırmızı lazer parçacıkları.
         */
        spawnLaser(
                level,
                serverPlayer,
                target
        );

        /*
         * Her tick patlama oluşturmak yerine
         * yaklaşık yarım saniyede bir patlama yapıyoruz.
         *
         * 20 tick = 1 saniye
         * 10 tick = 0.5 saniye
         */
        if (serverPlayer.tickCount % 10 == 0) {

            createExplosion(
                    level,
                    serverPlayer,
                    target
            );
        }
    }
}
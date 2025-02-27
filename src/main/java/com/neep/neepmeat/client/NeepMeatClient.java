package com.neep.neepmeat.client;

import com.neep.meatlib.block.BaseBuildingBlock;
import com.neep.meatlib.block.PaintedBlockManager;
import com.neep.meatlib.graphics.client.GraphicsEffectClient;
import com.neep.meatweapons.particle.PhageRayGraphicsEffect;
import com.neep.neepmeat.NeepMeat;
import com.neep.neepmeat.api.processing.BlockCrushingRegistry;
import com.neep.neepmeat.api.processing.OreFatRegistry;
import com.neep.neepmeat.api.processing.OreFatSyncS2CPacket;
import com.neep.neepmeat.client.effect.ReminaGraphicsEvent;
import com.neep.neepmeat.client.fluid.NMFluidsClient;
import com.neep.neepmeat.client.hud.BigBlockPlacementHelper;
import com.neep.neepmeat.client.hud.GuideLookupThings;
import com.neep.neepmeat.client.hud.HUDOverlays;
import com.neep.neepmeat.client.instance.TreeVacuumInstance;
import com.neep.neepmeat.client.hud.FarmingScutterHudRenderer;
import com.neep.neepmeat.client.item.RockDrillItemClient;
import com.neep.neepmeat.client.model.GlassTankModel;
import com.neep.neepmeat.client.model.GlomeEntityModel;
import com.neep.neepmeat.client.model.entity.HoundEntityModel;
import com.neep.neepmeat.client.plc.PLCClient;
import com.neep.neepmeat.client.plc.PLCHudRenderer;
import com.neep.neepmeat.client.renderer.*;
import com.neep.neepmeat.client.renderer.block.AdvancedIntegratorRenderer;
import com.neep.neepmeat.client.renderer.entity.*;
import com.neep.neepmeat.client.screen.*;
import com.neep.neepmeat.client.screen.living_machine.ItemOutputPortScreen;
import com.neep.neepmeat.client.screen.living_machine.LivingMachineScreen;
import com.neep.neepmeat.client.screen.plc.PLCProgramScreen;
import com.neep.neepmeat.client.world.NMDimensionEffects;
import com.neep.neepmeat.entity.follower.FollowerRenderer;
import com.neep.neepmeat.init.*;
import com.neep.neepmeat.item.DosimeterItem;
import com.neep.neepmeat.item.NetworkingToolItem;
import com.neep.neepmeat.item.RockDrillItem;
import com.neep.neepmeat.machine.assembler.AssemblerRenderer;
import com.neep.neepmeat.machine.bottler.BottlerRenderer;
import com.neep.neepmeat.machine.casting_basin.CastingBasinRenderer;
import com.neep.neepmeat.machine.charnel_pump.CharnelPumpRenderer;
import com.neep.neepmeat.machine.crucible.AlembicRenderer;
import com.neep.neepmeat.machine.crucible.CrucibleRenderer;
import com.neep.neepmeat.machine.death_blades.DeathBladesRenderer;
import com.neep.neepmeat.machine.fabricator.FabricatorInstance;
import com.neep.neepmeat.machine.fabricator.FabricatorScreen;
import com.neep.neepmeat.machine.item_mincer.ItemMincerRenderer;
import com.neep.neepmeat.machine.live_machine.LivingMachines;
import com.neep.neepmeat.machine.mixer.MixerRenderer;
import com.neep.neepmeat.machine.multitank.MultiTankRenderer;
import com.neep.neepmeat.machine.phage_ray.PhageRayClientComponent;
import com.neep.neepmeat.machine.separator.SeparatorScreen;
import com.neep.neepmeat.machine.small_compressor.SmallCompressorScreen;
import com.neep.neepmeat.machine.small_compressor.SmallCompressorScreenHandler;
import com.neep.neepmeat.machine.small_trommel.SmallTrommelRenderer;
import com.neep.neepmeat.machine.synthesiser.SynthesiserRenderer;
import com.neep.neepmeat.machine.trough.TroughRenderer;
import com.neep.neepmeat.network.*;
import com.neep.neepmeat.plc.PLCBlocks;
import com.neep.neepmeat.transport.FluidTransport;
import com.neep.neepmeat.transport.ItemTransport;
import com.neep.neepmeat.transport.block.fluid_transport.FilterPipeBlock;
import com.neep.neepmeat.transport.block.fluid_transport.FluidPipeBlock;
import com.neep.neepmeat.transport.client.TransportClient;
import com.neep.neepmeat.transport.client.renderer.WindowPipeRenderer;
import dev.monarkhes.myron_neepmeat.api.Myron;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.MinecartEntityModel;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(value= EnvType.CLIENT)
public class NeepMeatClient implements ClientModInitializer
{
    public static final EntityModelLayer MODEL_GLASS_TANK_LAYER = new EntityModelLayer(new Identifier(NeepMeat.NAMESPACE, "glass_tank"), "main");
    public static final EntityModelLayer TANK_MINECART = new EntityModelLayer(new Identifier(NeepMeat.NAMESPACE, "tank_minecart"), "main");
    public static final EntityModelLayer SMALL_COMPRESSOR_MINECART = new EntityModelLayer(new Identifier(NeepMeat.NAMESPACE, "small_compressor_minecart"), "main");
    public static final EntityModelLayer GLOME = new EntityModelLayer(new Identifier(NeepMeat.NAMESPACE, "glome"), "main");
    public static final EntityModelLayer EGG = new EntityModelLayer(new Identifier(NeepMeat.NAMESPACE, "egg"), "main");

    @Override
    public void onInitializeClient()
    {
        Myron.registerNamespace(NeepMeat.NAMESPACE);

        NMClientComponents.init();

        registerRenderers();
        registerLayers();
        registerScreens();
        NMFluidsClient.registerFluidRenderers();
        HUDOverlays.init();
        MachineHudOverlay.init();
        FarmingScutterHudRenderer.init();
        BigBlockPlacementHelper.init();
        RockDrillItemClient.init();

        TransportClient.init();

        TankMessagePacket.Client.registerReceiver();
        ParticleSpawnS2C.Client.registerReceiver();
        BlockSoundPacket.Client.registerReceiver();
        ParticleSpawnS2C.Client.registerReceiver();
        PlayerImplantStatusS2CPacket.Client.registerReceiver();
        EntityAnimationS2C.Client.registerReceiver();
        MachineDiagnosticsRequest.Client.registerReceiver();
        ClientPlayNetworking.registerGlobalReceiver(OreFatRegistry.SYNC_ID, OreFatSyncS2CPacket.Client::onPacket);
        ClientPlayNetworking.registerGlobalReceiver(BlockCrushingRegistry.CHANNEL_ID, BlockCrushingRegistry.Client::onPacket);
        NMClientNetwork.init();
        NMKeys.registerKeybindings();

        GraphicsEffectClient.registerEffect(NMGraphicsEffects.REMINA, ReminaGraphicsEvent::new);
        GraphicsEffectClient.registerEffect(NMGraphicsEffects.PHAGE_RAY, PhageRayGraphicsEffect::new);

        NMDimensionEffects.init();

        PLCHudRenderer.init();
        PLCClient.init();

        NetworkingToolItem.Client.init();
        DosimeterItem.Client.init();

        PhageRayClientComponent.init();
        FabricatorInstance.initNetwork();
        TreeVacuumInstance.initNetwork();

        GuideLookupThings.init();

//        ImplantAttributes.register(Impla);
    }

    public static void registerRenderers()
    {
        EntityModelLayerRegistry.registerModelLayer(GLOME, GlomeEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(EGG, GlomeEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(HoundEntityRenderer.HOUND_LAYER, HoundEntityModel::getTexturedModelData);

        TexturedModelData keeperMainData = TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0), 64, 64);
        TexturedModelData keeperInnerLayer = TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(0.5f), 0), 64, 64);
        TexturedModelData keeperOuterLayer = TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(1f), 0), 64, 64);
        EntityModelLayerRegistry.registerModelLayer(KeeperEntityRenderer.KEEPER, () -> keeperMainData);
        EntityModelLayerRegistry.registerModelLayer(KeeperEntityRenderer.KEEPER_INNER, () -> keeperInnerLayer);
        EntityModelLayerRegistry.registerModelLayer(KeeperEntityRenderer.KEEPER_OUTER, () -> keeperOuterLayer);

//        EntityRendererRegistry.register(NMEntities.TANK_MINECART, ctx -> new TankMinecartRenderer(ctx, TANK_MINECART));
        EntityRendererRegistry.register(NMEntities.SMALL_COMPRESSOR_MINECART, ctx -> new SmallCompressorMinecartRenderer(ctx, SMALL_COMPRESSOR_MINECART));
        EntityRendererRegistry.register(NMEntities.GLOME, ctx -> new GlomeEntityRenderer(ctx, GLOME));
        EntityRendererRegistry.register(NMEntities.EGG, ctx -> new EggEntityRenderer(ctx, EGG));
        EntityRendererRegistry.register(NMEntities.LIMB, DummyEntityRenderer::new);
        EntityRendererRegistry.register(NMEntities.WORM, WormEntityRenderer::new);
        EntityRendererRegistry.register(NMEntities.KEEPER, KeeperEntityRenderer::new);
        EntityRendererRegistry.register(NMEntities.HOUND, HoundEntityRenderer::new);
        EntityRendererRegistry.register(NMEntities.BOVINE_HORROR, BovineHorrorRenderer::new);
        EntityRendererRegistry.register(NMEntities.ACID_SPRAY, DummyEntityRenderer::new);
        EntityRendererRegistry.register(NMEntities.PHAGE_RAY, DummyEntityRenderer::new);
        EntityRendererRegistry.register(NMEntities.FOLLOWER, FollowerRenderer::new);

        EntityRendererRegistry.register(NMEntities.FARMING_SCUTTER, DummyEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(TANK_MINECART, MinecartEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(SMALL_COMPRESSOR_MINECART, MinecartEntityModel::getTexturedModelData);

//        GeoArmorRenderer.regi(new GogglesArmourRenderer(new GenericModel<>(
//                NeepMeat.NAMESPACE,
//                "geo/goggles.geo.json",
//                "textures/entity/armour/goggles.png",
//                "animations/goggles.animation.json"
//                )), NMItems.GOGGLES);

        // Custom baked models
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(rm -> new NeepMeatModelProvider());
        ModelLoadingRegistry.INSTANCE.registerModelProvider(NMExtraModels.EXTRA_MODELS);
        NMExtraModels.init();
        NMParticles.Client.init();

        NMInstances.init();

        // BlockEntity renderers
        BlockEntityRendererFactories.register(NMBlockEntities.WINDOW_PIPE, WindowPipeRenderer::new);

        BlockEntityRendererFactories.register(NMBlockEntities.GLASS_TANK, GlassTankRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MODEL_GLASS_TANK_LAYER, GlassTankModel::getTexturedModelData);
        BlockEntityRendererFactories.register(NMBlockEntities.FLUID_BUFFER, FluidBufferRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.ITEM_BUFFER_BLOCK_ENTITY, c -> new ItemBlockEntityRenderer<>(
                c, be -> be.getStorage(null).getAsStack(), be ->
        {
            be.stackRenderDelta = MathHelper.lerp(0.1f, be.stackRenderDelta, be.getStorage(null).getAmount() <= 0 ? 0.3f : 0f);
            return 0.25f + be.stackRenderDelta;
        }));
//        BlockEntityRendererFactories.register(NMBlockEntities.TROMMEL, TrommelRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.SMALL_TROMMEL, SmallTrommelRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.INTEGRATOR, IntegratorEggRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.ADVANCED_INTEGRATOR, AdvancedIntegratorRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.BIG_LEVER, BigLeverRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.PNEUMATIC_PIPE, ItemPipeRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.ENCASED_PNEUMATIC_PIPE, ItemPipeRenderer::new);
        BlockEntityRendererFactories.register(ItemTransport.STORAGE_BUS_BE, ItemPipeRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.MERGE_ITEM_PIPE, MergePipeRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.ITEM_PUMP, ItemPumpRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.EJECTOR, EjectorRenderer::new);
//        BlockEntityRendererFactories.register(NMBlockEntities.CONVERTER, ConverterRenderer::new);
//        BlockEntityRendererFactories.register(NMBlockEntities.ADVANCED_MOTOR, AdvancedMotorRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.DEPLOYER, DeployerRenderer::new);
//        BlockEntityRendererFactories.register(NMBlockEntities.AGITATOR, AgitatorRenderer::new);
//        BlockEntityRendererFactories.register(NMBlockEntities.VAT_CONTROLLER, VatRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.MIXER, MixerRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.MULTI_TANK, MultiTankRenderer::new);

        BlockEntityRendererFactories.register(NMBlockEntities.CRUCIBLE, CrucibleRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.ALEMBIC, AlembicRenderer::new);
        BlockEntityRendererFactories.register(NMBlockEntities.CASTING_BASIN, CastingBasinRenderer::new);

        BlockEntityRendererFactories.register(NMBlockEntities.PEDESTAL, c -> new ItemBlockEntityRenderer<>(
                c, be -> be.getStorage(null).getResource().toStack(), be -> 0.67f));
        BlockEntityRendererFactories.register(NMBlockEntities.ASSEMBLER, AssemblerRenderer::new);

        BlockEntityRendererFactories.register(NMBlockEntities.DEATH_BLADES, DeathBladesRenderer::new);

        BlockEntityRendererFactories.register(NMBlockEntities.FEEDING_TROUGH, TroughRenderer::new);

        BlockEntityRendererFactories.register(NMBlockEntities.BOTTLER, BottlerRenderer::new);

//        BlockEntityRendererFactories.register(NMBlockEntities.PYLON, PylonInstance::new);

        BlockEntityRendererFactories.register(NMBlockEntities.SYNTHESISER, SynthesiserRenderer::new);

        BlockEntityRendererFactories.register(LivingMachines.CHARNEL_PUMP_BE, CharnelPumpRenderer::new);

//        BlockEntityRendererFactories.register(PLCBlocks.PLC_ENTITY, PLCInstance::new);

        BlockEntityRendererFactories.register(NMBlockEntities.ITEM_MINCER, ItemMincerRenderer::new);

//        BlockEntityRendererFactories.register(PLCBlocks.ROBOTIC_ARM_ENTITY, RoboticArmRenderer::new);

        BuiltinItemRendererRegistry.INSTANCE.register(NMItems.ROCK_DRILL, new RockDrillItemRenderer(MinecraftClient.getInstance()));


        // Coloured blocks
        for (PaintedBlockManager.PaintedBlock block : NMBlocks.SMOOTH_TILE.entries)
        {
            ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> block.getRawCol(), block);
            ColorProviderRegistry.ITEM.register((stack, tintIndex) -> block.getRawCol(), block.asItem());
        }

        // Pipes
        for (var pipe : FluidTransport.COLOURED_FLUID_PIPES.values())
        {
            ColorProviderRegistry.BLOCK.register(FluidPipeBlock::getTint, pipe);
            BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), pipe);
            ColorProviderRegistry.ITEM.register(FluidPipeBlock::getItemTint, pipe.asItem());
        }
        ColorProviderRegistry.BLOCK.register(FilterPipeBlock::getTint, FluidTransport.FILTER_PIPE);
    }

    public static void registerScreens()
    {
        HandledScreens.register(ScreenHandlerInit.BUFFER_SCREEN_HANDLER, BufferScreen::new);
        HandledScreens.register(ScreenHandlerInit.CONTENT_DETECTOR_SCREEN_HANDLER, ContentDetectorScreen::new);
        HandledScreens.register(ScreenHandlerInit.ROUTER, RouterScreen::new);
        HandledScreens.register(ScreenHandlerInit.STIRLING_ENGINE, StirlingEngineScreen::new);
        HandledScreens.register(ScreenHandlerInit.ALLOY_KILN, AlloyKilnScreen::new);
        HandledScreens.register(ScreenHandlerInit.ASSEMBLER, AssemblerScreen::new);
        HandledScreens.register(ScreenHandlerInit.WORKSTATION, WorkstationScreen::new);
//        HandledScreens.register(ScreenHandlerInit.GUIDE, GuideMainScreen::new);
        HandledScreens.register(ScreenHandlerInit.FLUID_RATIONER, FluidRationerScreen::new);
        HandledScreens.register(ScreenHandlerInit.SEPARATOR, SeparatorScreen::new);
        HandledScreens.register(ScreenHandlerInit.UPGRADE_MANAGER, UpgradeManagerScreen::new);
        HandledScreens.register(ScreenHandlerInit.DISPLAY_PLATE, DisplayPlateScreen::new);
        HandledScreens.register(ScreenHandlerInit.FABRICATOR, FabricatorScreen::new);
        HandledScreens.register(ScreenHandlerInit.SMALL_COMPRESSOR, SmallCompressorScreen::new);

        HandledScreens.register(ScreenHandlerInit.LIVING_MACHINE, LivingMachineScreen::new);
        HandledScreens.register(ScreenHandlerInit.ITEM_OUTPUT, ItemOutputPortScreen::new);

        HandledScreens.register(ScreenHandlerInit.PLC, PLCProgramScreen::new);
    }

    public static void registerLayers()
    {
        // Fluids
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), NMFluids.STILL_BLOOD, NMFluids.FLOWING_BLOOD);
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), NMFluids.STILL_PATINA_TREATMENT, NMFluids.FLOWING_PATINA_TREATMENT);
//        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), NMFluids.STILL_ENRICHED_BLOOD, NMFluids.FLOWING_ENRICHED_BLOOD);
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), NMFluids.STILL_ELDRITCH_ENZYMES, NMFluids.FLOWING_ELDRITCH_ENZYMES);

        // Other blocks
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.BASIC_GLASS_TANK);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.MULTI_TANK);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.FLUID_BUFFER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.MESH_PANE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.PUMP);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.RUSTED_BARS);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.PNEUMATIC_TUBE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ItemTransport.STORAGE_BUS);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.MERGE_ITEM_PIPE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.ITEM_PUMP);
//        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.CONVERTER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.DEPLOYER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.MIXER);
//        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.LEADED_GLASS);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.COLLECTOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.TRANSDUCER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.SMALL_TROMMEL);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), LivingMachines.LARGE_TROMMEL);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), LivingMachines.TREE_VACUUM);
//        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.CAUTION_TAPE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), NMBlocks.ASSEMBLER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.FABRICATOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BOTTLER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BLOOD_BUBBLE_SAPLING);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BLOOD_BUBBLE_LEAVES);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BLOOD_BUBBLE_LEAVES_FLOWERING);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BLOOD_BUBBLE_TRAPDOOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BLOOD_BUBBLE_DOOR);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.CAUTION_BLOCK_DOOR);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.WHISPER_WHEAT);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.FLESH_POTATO);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.SCAFFOLD_PLATFORM);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) NMBlocks.SCAFFOLD_PLATFORM.stairs);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) NMBlocks.SCAFFOLD_PLATFORM.slab);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.BLUE_SCAFFOLD);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) NMBlocks.BLUE_SCAFFOLD.stairs);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) NMBlocks.BLUE_SCAFFOLD.slab);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.YELLOW_SCAFFOLD);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) NMBlocks.YELLOW_SCAFFOLD.stairs);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) NMBlocks.YELLOW_SCAFFOLD.slab);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.POWER_FLOWER_SEEDS);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.ADVANCED_INTEGRATOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.LARGE_MOTOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.CHARNEL_PUMP);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.REINFORCED_GLASS);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) ((BaseBuildingBlock) NMBlocks.REINFORCED_GLASS).stairs);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), (Block) ((BaseBuildingBlock) NMBlocks.REINFORCED_GLASS).slab);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.SCAFFOLD_TRAPDOOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), NMBlocks.SCAFFOLD_TRAPDOOR);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.ENCASED_VASCULAR_CONDUIT);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.ENCASED_FLUID_PIPE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.ENCASED_PNEUMATIC_PIPE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.CHUTE);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.FLUID_DRAIN);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), PLCBlocks.ROBOTIC_ARM);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), FluidTransport.WINDOW_PIPE);

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.HOLDING_TRACK);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.CONTROL_TRACK);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.DUMPING_TRACK);


        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), NMBlocks.LARGE_FAN);
    }
}

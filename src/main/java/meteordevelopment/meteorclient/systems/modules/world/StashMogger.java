package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class StashMogger extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-blocks")
        .description("Ab wie vielen Behaeltern soll der Alarm ausloesen?")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> scanChests = sgGeneral.add(new BoolSetting.Builder()
        .name("scan-chests")
        .description("Sucht nach normalen Holz- und Doppelkisten.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> scanBarrels = sgGeneral.add(new BoolSetting.Builder()
        .name("scan-barrels")
        .description("Sucht nach Faessern.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> scanShulkers = sgGeneral.add(new BoolSetting.Builder()
        .name("scan-shulkers")
        .description("Sucht nach Shulkerboxen jeglicher Farbe.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> scanCopperChests = sgGeneral.add(new BoolSetting.Builder()
        .name("scan-copper-chests")
        .description("Sucht nach Kupferkisten (inkl. oxidierter Versionen).")
        .defaultValue(true)
        .build()
    );

    private final List<BlockPos> verifiedBases = new ArrayList<>();

    public StashMogger() {
        super(Categories.World, "Stash-Mogger", "Base-Finder mit Custom-Menue. Filtert perfekt nach Blockart.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        verifiedBases.clear(); 
        List<BlockPos> tempRealBlocks = new ArrayList<>(); 

        for (BlockEntity blockEntity : mc.world.blockEntities) {
            BlockPos pos = blockEntity.getPos();
            boolean isValidType = false;

            if (scanChests.get() && blockEntity instanceof ChestBlockEntity) isValidType = true;
            
            if (scanBarrels.get() && blockEntity instanceof BarrelBlockEntity) isValidType = true;
            
            if (scanShulkers.get() && blockEntity instanceof ShulkerBoxBlockEntity) isValidType = true;

            if (scanCopperChests.get()) {
                String blockName = Registries.BLOCK.getId(blockEntity.getCachedState().getBlock()).getPath();
                if (blockName.contains("copper_chest")) isValidType = true;
            }

            if (isValidType && !isAdminFakeBase(pos) && hasAirAround(pos)) {
                tempRealBlocks.add(pos); 
            }
        }

        for (BlockPos blockPos : tempRealBlocks) {
            int closeBlocks = 0; 
            for (BlockPos otherPos : tempRealBlocks) {
                if (blockPos.isWithinDistance(otherPos, 8)) {
                    closeBlocks++;
                }
            }

            if (closeBlocks >= minBlocks.get()) {
                verifiedBases.add(blockPos);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (verifiedBases.isEmpty()) return; 

        Color leuchtFarbe = new Color(0, 255, 0, 100); 
        Color fadenFarbe = new Color(0, 255, 0, 255);

        for (BlockPos pos : verifiedBases) {
            event.renderer.box(pos, leuchtFarbe, leuchtFarbe, ShapeMode.Both, 0);
            event.renderer.line(
                event.offsetX, event.offsetY, event.offsetZ, 
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                fadenFarbe
            );
        }
    }

    private boolean hasAirAround(BlockPos pos) {
        BlockPos[] offsets = { pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west() };
        for (BlockPos offset : offsets) {
            if (mc.world.getBlockState(offset).isAir()) return true;
        }
        return false; 
    }

    private boolean isAdminFakeBase(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() == Blocks.TRAPPED_CHEST) return true;

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = mc.world.getBlockState(pos.add(x, y, z)).getBlock();
                    if (block == Blocks.TNT || block == Blocks.OBSERVER || block == Blocks.SCULK_SENSOR || block == Blocks.COMMAND_BLOCK) {
                        return true; 
                    }
                }
            }
        }
        
        WorldChunk chunk = mc.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null && chunk.getInhabitedTime() < 36000) {
            return true; 
        }

        return false; 
    }
}
 

 
add(new StashMogger());

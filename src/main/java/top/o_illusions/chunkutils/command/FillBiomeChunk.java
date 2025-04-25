package top.o_illusions.chunkutils.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import top.o_illusions.chunkutils.utils.MiscellaneousUtil;

import java.util.ArrayList;
import java.util.List;


public class FillBiomeChunk implements ICommand {
    public FillBiomeChunk() {
    }

    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess)
    {
        dispatcher.register(CommandManager.literal("fillbiomechunk")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("biome", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.BIOME))
                        .then(CommandManager.argument("range", NumberRangeArgumentType.intRange())
                                .executes(context -> execute(context.getSource(),
                                        RegistryEntryReferenceArgumentType.getRegistryEntry(context, "biome", RegistryKeys.BIOME),
                                        NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "range"))
                                ))));
    }

    public int execute(ServerCommandSource source, RegistryEntry<Biome> biome, NumberRange.IntRange range) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null)
        {
            source.sendError(Text.literal("非玩家单位执行"));
            return 0;
        }
        BlockPos pos = player.getBlockPos();
        ServerWorld world = source.getWorld();

        int intRange = (int) (range.maxSquared().get()/ 2);
        Chunk centrChunk = world.getChunk(pos);
        List<Chunk> chunks = new ArrayList<>();
        ChunkPos centrChunkPos = centrChunk.getPos();

        BlockPos startChunkPos = new BlockPos((centrChunkPos.getStartX()- intRange)>> 4, 0, (centrChunkPos.getStartZ()- intRange)>> 4);
        BlockPos endChunkPos = new BlockPos((centrChunkPos.getEndX()+ intRange)>> 4, 0, (centrChunkPos.getEndZ()+ intRange)>> 4);
        BlockBox chunksBox = BlockBox.create(startChunkPos, endChunkPos);

        long chunkCount = 0;

        source.sendError(Text.literal(chunksBox.toString()));

        for (int i = startChunkPos.getX(); i <= endChunkPos.getX(); i++) {
            for (int j = startChunkPos.getZ(); j <= endChunkPos.getZ(); j++) {
                chunks.add(world.getChunk(i, j));
                chunkCount++;
            }
        }

        for (Chunk chunk : chunks) {
            FillBiome(world, chunk, biome);
        }

        long finalChunkCount = chunkCount;
        source.sendFeedback(() -> Text.literal("已填充%d个区块的群系".formatted(finalChunkCount)), false);

        world.getChunkManager().chunkLoadingManager.sendChunkBiomePackets(chunks);

        return 1;
    }

    public void FillBiome(ServerWorld world, Chunk chunk, RegistryEntry<Biome> biome) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos chunkStartPos = new BlockPos(chunkPos.getStartX(), chunk.getBottomY() , chunkPos.getStartZ());
        BlockPos chunkEndPos = new BlockPos(chunkPos.getEndX(), chunk.getTopY(), chunkPos.getEndZ());

        BlockBox box = BlockBox.create(chunkStartPos, chunkEndPos);
        MutableInt mutableInt = new MutableInt(0);
        BiomeSupplier supplier = MiscellaneousUtil.createBiomeSupplier(mutableInt, chunk, box, biome, (RegistryEntry) -> true);

        chunk.populateBiomes(supplier, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
        chunk.setNeedsSaving(true);
    }
}
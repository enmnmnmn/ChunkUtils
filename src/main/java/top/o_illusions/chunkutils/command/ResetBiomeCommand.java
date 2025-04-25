package top.o_illusions.chunkutils.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import top.o_illusions.chunkutils.utils.MiscellaneousUtil;

import java.util.ArrayList;
import java.util.List;

public class ResetBiomeCommand implements ICommand {
    public ResetBiomeCommand() {
    }

    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("resetbiome")
                .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("range", NumberRangeArgumentType.intRange())
                .executes(context -> execute(context.getSource(), NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "range"))
                )));
    }

    public void ResetBiome(ServerWorld world, Chunk chunk, ServerCommandSource source) {
        ServerChunkManager chunkManager = world.getChunkManager();
        MultiNoiseUtil.MultiNoiseSampler sampler = chunkManager.getNoiseConfig().getMultiNoiseSampler();
        MutableInt mutableInt = new MutableInt(0);

        ChunkPos chunkPos = chunk.getPos();
        BlockPos chunkStartPos = new BlockPos(chunkPos.getStartX(), chunk.getBottomY() , chunkPos.getStartZ());
        BlockPos chunkEndPos = new BlockPos(chunkPos.getEndX(), chunk.getTopY(), chunkPos.getEndZ());
        long biomeBoxCount = 0;

        for (int i = chunkStartPos.getY(); i < chunkEndPos.getY(); i += 4) {
            for (int j = chunkStartPos.getX(); j < chunkEndPos.getX(); j += 4) {
                for (int k = chunkStartPos.getZ(); k < chunkEndPos.getZ(); k += 4) {
                    biomeBoxCount++;
                    BlockBox box = BlockBox.create(new BlockPos(j, i, k), new BlockPos(j + 4, i + 4, k + 4));
                    BiomeSupplier supplier = MiscellaneousUtil.createBiomeSupplier(mutableInt,
                            chunk,
                            box,
                            MiscellaneousUtil.getOriginalBiome(world, new BlockPos(j, i, k)),
                            (RegistryEntry) -> true
                    );
                    chunk.populateBiomes(supplier, sampler);
                }
            }
        }
        long finalBiomeBoxCount = biomeBoxCount;
        source.sendFeedback(() -> Text.literal("已重置%d个群系块[ChunkPos:[x:%d,z:%d]".formatted(finalBiomeBoxCount, (chunkStartPos.getX()>> 4),
                (chunkStartPos.getZ()>> 4))), false);

        chunk.setNeedsSaving(true);
    }

    protected int execute(ServerCommandSource source, NumberRange.IntRange range) {
        ServerWorld world = source.getWorld();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("非玩家单位执行"));
            return 0;
        }
        BlockPos pos = player.getBlockPos();
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
            ResetBiome(world, chunk, source);
        }

        long finalChunkCount = chunkCount;
        source.sendFeedback(() -> Text.literal("已重置%d个区块的群系".formatted(finalChunkCount)), false);

        world.getChunkManager().chunkLoadingManager.sendChunkBiomePackets(chunks);

        return 1;
    }
}

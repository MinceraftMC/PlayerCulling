package de.pianoman911.playerculling.platformfabric1219.platform;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.pianoman911.playerculling.platformcommon.platform.command.BlockPosResolver;
import de.pianoman911.playerculling.platformcommon.platform.command.MultiPlayerResolver;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformArgument;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformArgumentProvider;
import de.pianoman911.playerculling.platformcommon.platform.command.ResultConverter;
import de.pianoman911.playerculling.platformcommon.platform.command.SinglePlayerResolver;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class FabricArgumentsProvider implements PlatformArgumentProvider {

    private final FabricPlatform platform;
    private final Set<FabricWrapperArgumentType<?, ArgumentType<?>, ?>> registered = new HashSet<>();

    private final FabricWrapperArgumentType<Coordinates, ArgumentType<Coordinates>, BlockPosResolver> blockPos;
    private final FabricWrapperArgumentType<EntitySelector, ArgumentType<EntitySelector>, SinglePlayerResolver> player;
    private final FabricWrapperArgumentType<EntitySelector, ArgumentType<EntitySelector>, MultiPlayerResolver> players;

    public FabricArgumentsProvider(FabricPlatform platform) {
        this.platform = platform;

        this.blockPos = this.buildWrappedArgument(BlockPosArgument.blockPos(), nms -> src -> {
            BlockPos blockPos = nms.getBlockPos(((FabricCommandSourceStack) src).getFabricSourceStack());
            if (blockPos == null) {
                return null;
            }
            return new Vec3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }, arg -> arg instanceof BlockPosArgument);

        this.player = this.buildWrappedArgument(EntityArgument.player(), nms -> src -> {
            Player player = nms.findSinglePlayer(((FabricCommandSourceStack) src).getFabricSourceStack());
            return player == null ? null : this.platform.providePlayer(player);
        }, arg -> arg instanceof EntityArgument entity && entity.single);

        this.players = this.buildWrappedArgument(EntityArgument.players(), nms -> src -> {
            List<ServerPlayer> resolved = nms.findPlayers(((FabricCommandSourceStack) src).getFabricSourceStack());
            if (resolved == null) {
                return null;
            }
            List<PlatformPlayer> players = new ArrayList<>(resolved.size());
            for (ServerPlayer player : resolved) {
                players.add(this.platform.providePlayer(player));
            }
            return players;
        }, arg -> arg instanceof EntityArgument entity && !entity.single);
    }

    @Override
    public ArgumentType<BlockPosResolver> blockPos() {
        return this.blockPos;
    }

    @Override
    public ArgumentType<SinglePlayerResolver> player() {
        return this.player;
    }

    @Override
    public ArgumentType<MultiPlayerResolver> players() {
        return this.players;
    }

    @SuppressWarnings("unchecked")
    private <O, V extends ArgumentType<O>, C> FabricWrapperArgumentType<O, V, C> buildWrappedArgument(V delegate, ResultConverter<O, C> converter, Predicate<ArgumentType<?>> compatible) {
        FabricWrapperArgumentType<O, V, C> argument = new FabricWrapperArgumentType<>(delegate, converter, compatible);
        this.registered.add((FabricWrapperArgumentType<?, ArgumentType<?>, ?>) argument);
        return argument;
    }

    @Override
    public ArgumentType<?> mapFromNms(ArgumentType<?> instance) {
        for (FabricWrapperArgumentType<?, ArgumentType<?>, ?> wrapper : this.registered) {
            if (wrapper.compatible.test(instance)) {
                return wrapper;
            }
        }
        return instance;
    }

    @Override
    public ArgumentType<?> mapToNms(ArgumentType<?> instance) {
        if (instance instanceof FabricWrapperArgumentType<?, ?, ?> wrapped) {
            return wrapped.getDelegate();
        }
        return instance;
    }

    public static final class FabricWrapperArgumentType<O, V extends ArgumentType<O>, P> implements ArgumentType<P>, PlatformArgument<O, P> {

        private final V delegate;
        private final ResultConverter<O, P> converter;
        private final Predicate<ArgumentType<?>> compatible;

        private FabricWrapperArgumentType(V delegate, ResultConverter<O, P> converter, Predicate<ArgumentType<?>> compatible) {
            this.delegate = delegate;
            this.converter = converter;
            this.compatible = compatible;
        }

        @Override
        public P parse(final StringReader reader) throws CommandSyntaxException {
            return this.converter.convert(this.delegate.parse(reader));
        }

        @Override
        public <S> P parse(final StringReader reader, final S source) throws CommandSyntaxException {
            return this.converter.convert(this.delegate.parse(reader, source));
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
            return this.delegate.listSuggestions(context, builder);
        }

        @Override
        public Collection<String> getExamples() {
            return this.delegate.getExamples();
        }

        @Override
        public P convertToPlatform(O obj) throws CommandSyntaxException {
            return this.converter.convert(obj);
        }

        public V getDelegate() {
            return this.delegate;
        }
    }
}

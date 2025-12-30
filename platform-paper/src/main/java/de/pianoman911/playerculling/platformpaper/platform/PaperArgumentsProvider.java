package de.pianoman911.playerculling.platformpaper.platform;

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
import de.pianoman911.playerculling.platformcommon.platform.command.SingleEntityResolver;
import de.pianoman911.playerculling.platformcommon.platform.command.SinglePlayerResolver;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.math.BlockPosition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class PaperArgumentsProvider implements PlatformArgumentProvider {

    private final PaperPlatform platform;

    public PaperArgumentsProvider(PaperPlatform platform) {
        this.platform = platform;
    }

    @Override
    public ArgumentType<BlockPosResolver> blockPos() {
        return this.wrap(ArgumentTypes.blockPosition(), paper -> src -> {
            BlockPosition resolved = paper.resolve(((PaperCommandSourceStack) src).getPaperSourceStack());
            return new Vec3i(resolved.blockX(), resolved.blockY(), resolved.blockZ());
        });
    }

    @Override
    public ArgumentType<SinglePlayerResolver> player() {
        return this.wrap(ArgumentTypes.player(), paper -> src -> {
            Player player = paper.resolve(((PaperCommandSourceStack) src).getPaperSourceStack()).getFirst();
            return (PlatformPlayer) this.platform.provideEntity(player);
        });
    }

    @Override
    public ArgumentType<MultiPlayerResolver> players() {
        return this.wrap(ArgumentTypes.players(), paper -> src -> {
            List<Player> resolved = paper.resolve(((PaperCommandSourceStack) src).getPaperSourceStack());
            List<PlatformPlayer> players = new ArrayList<>(resolved.size());
            for (Player player : resolved) {
                players.add((PlatformPlayer) this.platform.provideEntity(player));
            }
            return players;
        });
    }

    @Override
    public ArgumentType<SingleEntityResolver> entity() {
        return this.wrap(ArgumentTypes.entity(), paper -> src -> {
            Entity first = paper.resolve(((PaperCommandSourceStack) src).getPaperSourceStack()).getFirst();
            return this.platform.provideEntity(first);
        });
    }

    private <B, C> ArgumentType<C> wrap(final ArgumentType<B> base, final ResultConverter<B, C> converter) {
        return new PaperWrapperArgumentType<>(base, converter);
    }

    @NullMarked
    public static final class PaperWrapperArgumentType<P, C> implements CustomArgumentType<C, P>, PlatformArgument<P, C> {

        private final ArgumentType<P> paperBase;
        private final ResultConverter<P, C> converter;

        private PaperWrapperArgumentType(final ArgumentType<P> nmsBase, final ResultConverter<P, C> converter) {
            this.paperBase = nmsBase;
            this.converter = converter;
        }

        @Override
        public C parse(final StringReader reader) throws CommandSyntaxException {
            return this.converter.convert(this.paperBase.parse(reader));
        }

        @Override
        public <S> C parse(final StringReader reader, final S source) throws CommandSyntaxException {
            return this.converter.convert(this.paperBase.parse(reader, source));
        }

        @Override
        public ArgumentType<P> getNativeType() {
            return this.paperBase;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
            return this.paperBase.listSuggestions(context, builder);
        }

        @Override
        public C convertToPlatform(P obj) throws CommandSyntaxException {
            return this.converter.convert(obj);
        }
    }
}

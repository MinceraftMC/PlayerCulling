package de.pianoman911.playerculling.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformArgument;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// Converts between the implementation of the command source stack and the PlayerCulling command source stack
// A full command tree remap is needed
public class CommandConversionHandler<P> {

    private static final MethodHandle ARGUMENT_GETTER = ReflectionUtil.getGetter(CommandContext.class, Map.class, 1);

    private final Function<PlatformCommandSourceStack, P> converter;
    private final Function<P, PlatformCommandSourceStack> reverseConverter;
    private final @Nullable UnaryOperator<ArgumentType<?>> argumentWrapper;
    private final @Nullable UnaryOperator<ArgumentType<?>> argumentUnwrapper;

    public CommandConversionHandler(
            Function<PlatformCommandSourceStack, P> converter,
            Function<P, PlatformCommandSourceStack> reverseConverter,
            @Nullable UnaryOperator<ArgumentType<?>> argumentWrapper,
            @Nullable UnaryOperator<ArgumentType<?>> argumentUnwrapper
    ) {
        this.converter = converter;
        this.reverseConverter = reverseConverter;
        this.argumentWrapper = argumentWrapper;
        this.argumentUnwrapper = argumentUnwrapper;
    }

    @SuppressWarnings("unchecked")
    private <T, F> T convertCss(F css) {
        if (css instanceof PlatformCommandSourceStack internal) {
            return (T) this.converter.apply(internal);
        } else {
            // This can only be the P command source stack
            return (T) this.reverseConverter.apply((P) css);
        }
    }

    @SuppressWarnings("unchecked")
    private <T, F> Map<String, ParsedArgument<T, ?>> convertArguments(
            Map<String, ParsedArgument<F, ?>> arguments,
            Map<String, ArgumentType<?>> argumentTypes
    ) throws CommandSyntaxException {
        Map<String, ParsedArgument<T, ?>> converted = new HashMap<>(arguments.size());
        for (Map.Entry<String, ParsedArgument<F, ?>> entry : arguments.entrySet()) {
            String name = entry.getKey();
            ParsedArgument<F, ?> value = entry.getValue();

            // convert back to platform type
            ArgumentType<?> type = argumentTypes.get(name);
            Object result;
            if (type instanceof PlatformArgument<?, ?> arg) {
                result = ((PlatformArgument<Object, Object>) arg).convertToPlatform(value.getResult());
            } else {
                result = value.getResult();
            }

            ParsedArgument<T, ?> convertedValue = new ParsedArgument<>(
                    value.getRange().getStart(),
                    value.getRange().getEnd(),
                    result
            );
            converted.put(name, convertedValue);
        }
        return converted;
    }

    @SuppressWarnings("unchecked")
    private <T, F> CommandContext<T> convertCtx(CommandContext<F> ctx) throws CommandSyntaxException {
        if (ctx == null) { // Recursive child can be null
            return null;
        }

        Map<String, ParsedArgument<F, ?>> arguments;
        try {
            arguments = (Map<String, ParsedArgument<F, ?>>) ARGUMENT_GETTER.invoke(ctx);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }

        List<ParsedCommandNode<F>> nodes = ctx.getNodes();
        Map<String, ArgumentType<?>> argumentTypes = new HashMap<>(nodes.size());
        for (ParsedCommandNode<F> node : nodes) {
            if (!(node.getNode() instanceof ArgumentCommandNode<?, ?> argNode)) {
                continue;
            }
            ArgumentType<?> argType = this.argumentWrapper == null ? argNode.getType() :
                    this.argumentWrapper.apply(argNode.getType());
            argumentTypes.put(argNode.getName(), argType);
        }

        return new CommandContext<>(
                this.convertCss(ctx.getSource()),
                ctx.getInput(),
                this.convertArguments(arguments, argumentTypes),
                this.remapCommand(ctx.getCommand()),
                null,
                this.convertNodes(ctx.getNodes()),
                ctx.getRange(),
                this.convertCtx(ctx.getChild()),
                null,
                ctx.isForked()
        );
    }

    private <T, F> List<ParsedCommandNode<T>> convertNodes(List<ParsedCommandNode<F>> nodes) {
        List<ParsedCommandNode<T>> converted = new ArrayList<>(nodes.size());
        for (ParsedCommandNode<F> node : nodes) {
            converted.add(new ParsedCommandNode<>(this.remapCommand(node.getNode()), node.getRange()));
        }
        return converted;
    }

    private <T, F> Command<T> remapCommand(Command<F> command) {
        return ctx -> {
            CommandContext<F> stack = this.convertCtx(ctx);
            return command.run(stack);
        };
    }

    private <T, F> ArgumentBuilder<T, ?> createArgumentBuilder(ArgumentCommandNode<F, ?> node) {
        ArgumentType<?> argumentType = this.argumentUnwrapper != null ? this.argumentUnwrapper.apply(node.getType()) : node.getType();
        RequiredArgumentBuilder<T, ?> argument = RequiredArgumentBuilder.argument(node.getName(), argumentType);
        if (node.getCustomSuggestions() == null) {
            return argument;
        }
        argument.suggests((ctx, builder) -> {
            CommandContext<F> other = this.convertCtx(ctx);
            return node.getCustomSuggestions().getSuggestions(other, builder);
        });

        return argument;
    }

    private <T, F> ArgumentBuilder<T, ?> createLiteralBuilder(LiteralCommandNode<F> node) {
        return LiteralArgumentBuilder.literal(node.getName());
    }

    private <T, F> ArgumentBuilder<T, ?> createRootBuilder(RootCommandNode<F> node) {
        return LiteralArgumentBuilder.literal(node.getName());
    }

    @SuppressWarnings("unchecked")
    public <I extends CommandNode<F>, O extends CommandNode<T>, T, F> O remapCommand(I node) {
        ArgumentBuilder<T, ?> builder = switch ((CommandNode<F>) node) {
            case LiteralCommandNode<F> lnode -> createLiteralBuilder(lnode);
            case ArgumentCommandNode<F, ?> anode -> createArgumentBuilder(anode);
            case RootCommandNode<F> rnode -> createRootBuilder(rnode);
            default -> throw new UnsupportedOperationException("Unsupported command node type: " + node.getClass());
        };

        // convert requirement
        Predicate<F> requirement = node.getRequirement();
        if (requirement != null) {
            builder.requires(css -> requirement.test(this.convertCss(css)));
        }
        // convert executor
        Command<F> command = node.getCommand();
        if (command != null) {
            builder.executes(this.remapCommand(node.getCommand()));
        }
        // convert redirect
        CommandNode<F> redirect = node.getRedirect();
        if (redirect != null) {
            builder.redirect(this.remapCommand(redirect));
        }
        // copy children (recursively)
        for (CommandNode<F> child : node.getChildren()) {
            builder.then(this.remapCommand(child));
        }
        return (O) builder.build();
    }
}

package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.commands.AbstractCommand;
import com.gmail.subnokoii78.gpcore.commands.arguments.AbstractEnumerationArgument;
import com.gmail.subnokoii78.gpcore.commands.arguments.CommandArgumentableEnumeration;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ConsoleCommandSender;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public class BlockPropertyAccessorCommand extends AbstractCommand {
    private BlockPropertyAccessorCommand() {}

    @Override
    protected LiteralCommandNode<CommandSourceStack> getCommandNode() {
        return Commands.literal("blockpropertyaccessor")
            .requires(stack -> stack.getSender() instanceof ConsoleCommandSender)
            .then(
                Commands.argument("datapack_location", FinalOutputDirectorySuggestionArgument.finalOutputDirectorySuggestion())
                    .executes(context -> {
                        return generate(
                            context,
                            Path.of(context.getArgument("datapack_location", String.class)).resolve(BlockPropertyAccessor.getBlockPropertyAccessor().getDatapackFileName())
                        );
                    })
            )
            .executes(context -> {
                return generate(
                    context,
                    Path.of(BlockPropertyAccessor.getBlockPropertyAccessor().getDatapackFileName())
                );
            })
            .build();
    }

    @Override
    protected String getDescription() {
        return "データパック " + BlockPropertyAccessor.BLOCK_PROPERTY_ACCESSOR + " を生成します";
    }

    private int generate(CommandContext<CommandSourceStack> context, Path finalOutput) {
        context.getSource().getSender().sendMessage(Component.text("データパックを生成しています..."));

        final DatapackGenerator datapackGenerator = BlockPropertyAccessor.getBlockPropertyAccessor().getDatapackGenerator();

        if (datapackGenerator.startGenerating(finalOutput)) {
            context.getSource().getSender().sendMessage(Component.text("データパックの生成に成功しました").color(NamedTextColor.GREEN));
            return 1;
        }
        else {
            return failure(context, new RuntimeException(
                "現在生成中のため重複して実行することができません"
            ));
        }
    }

    public enum FinalOutputDirectorySuggestion implements CommandArgumentableEnumeration {
        PLUGINS("plugins");

        private final String value;

        FinalOutputDirectorySuggestion(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public static final class FinalOutputDirectorySuggestionArgument extends AbstractEnumerationArgument<FinalOutputDirectorySuggestion> {
        private FinalOutputDirectorySuggestionArgument() {}

        @Override
        protected Class<FinalOutputDirectorySuggestion> getEnumClass() {
            return FinalOutputDirectorySuggestion.class;
        }

        public static FinalOutputDirectorySuggestionArgument finalOutputDirectorySuggestion() {
            return new FinalOutputDirectorySuggestionArgument();
        }
    }

    public static final BlockPropertyAccessorCommand BLOCK_PROPERTY_ACCESSOR_COMMAND = new BlockPropertyAccessorCommand();
}

package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.commands.AbstractCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ConsoleCommandSender;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlockPropertyAccessorCommand extends AbstractCommand {
    private BlockPropertyAccessorCommand() {}

    @Override
    protected LiteralCommandNode<CommandSourceStack> getCommandNode() {
        return Commands.literal("blockpropertyaccessor")
            .requires(stack -> stack.getSender() instanceof ConsoleCommandSender)
            .then(
                Commands.argument("require_type_traits", BoolArgumentType.bool())
                    .then(
                        Commands.argument("require_data_traits", BoolArgumentType.bool())
                            .executes(this::generateWithRequirements)
                    )
            )
            .executes(context -> generate(context, true, true))
            .build();
    }

    @Override
    protected String getDescription() {
        return "データパック " + BlockPropertyAccessor.BLOCK_PROPERTY_ACCESSOR + " を生成します";
    }

    private int generate(CommandContext<CommandSourceStack> context, boolean requireTypeTraits, boolean requireDataTraits) {
        context.getSource().getSender().sendMessage(Component.text("データパックを生成しています..."));

        final DatapackGenerator datapackGenerator = BlockPropertyAccessor.getBlockPropertyAccessor().getDatapackGenerator();

        if (datapackGenerator.startGenerating(requireTypeTraits, requireDataTraits)) {
            context.getSource().getSender().sendMessage(Component.text("データパックの生成に成功しました").color(NamedTextColor.GREEN));
            return 1;
        }
        else {
            return failure(context, new RuntimeException(
                "現在生成中のため重複して実行することができません"
            ));
        }
    }

    private int generateWithRequirements(CommandContext<CommandSourceStack> context) {
        return generate(context, context.getArgument("require_type_traits", Boolean.class), context.getArgument("require_data_traits", Boolean.class));
    }

    public static final BlockPropertyAccessorCommand BLOCK_PROPERTY_ACCESSOR_COMMAND = new BlockPropertyAccessorCommand();
}

package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.commands.AbstractCommand;
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
            .executes(this::generate)
            .build();
    }

    @Override
    protected String getDescription() {
        return "データパック " + BlockPropertyAccessor.BLOCK_PROPERTY_ACCESSOR + " を生成します";
    }

    private int generate(CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage(Component.text("データパックを生成しています..."));

        final DatapackGenerator datapackGenerator = BlockPropertyAccessor.getBlockPropertyAccessor().getDatapackGenerator();

        if (datapackGenerator.startGenerating()) {
            context.getSource().getSender().sendMessage(Component.text("データパックの生成に成功しました").color(NamedTextColor.GREEN));
            return 1;
        }
        else {
            return failure(context, new RuntimeException(
                "現在生成中のため重複して実行することができません"
            ));
        }
    }

    public static final BlockPropertyAccessorCommand BLOCK_PROPERTY_ACCESSOR_COMMAND = new BlockPropertyAccessorCommand();
}

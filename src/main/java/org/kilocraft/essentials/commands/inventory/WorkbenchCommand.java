package org.kilocraft.essentials.commands.inventory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.container.BlockContext;
import net.minecraft.container.Container;
import net.minecraft.container.CraftingTableContainer;
import net.minecraft.container.SimpleNamedContainerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.TranslatableText;
import org.kilocraft.essentials.CommandPermission;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.chat.KiloChat;

public class WorkbenchCommand extends EssentialCommand {

    public WorkbenchCommand() {
        super("workbench", CommandPermission.WORKBENCH, new String[]{"craftingbench", "craft"});
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        argumentBuilder.executes(WorkbenchCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        KiloChat.sendLangMessageTo(context.getSource(), "general.open_container", "CraftingTable");

        player.openContainer(new SimpleNamedContainerFactory(WorkbenchCommand::createContainer, new TranslatableText("container.crafting")));
        player.incrementStat(Stats.INTERACT_WITH_CRAFTING_TABLE);

        return 1;
    }

    public static Container createContainer(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new CraftingTableContainer(syncId, inventory, BlockContext.EMPTY);
    }

}

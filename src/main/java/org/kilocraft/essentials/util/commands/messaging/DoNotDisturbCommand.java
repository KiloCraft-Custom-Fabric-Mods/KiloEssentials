package org.kilocraft.essentials.util.commands.messaging;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.OnlineUser;
import org.kilocraft.essentials.api.user.preference.Preference;
import org.kilocraft.essentials.user.ServerUser;
import org.kilocraft.essentials.user.preference.Preferences;

public class DoNotDisturbCommand extends EssentialCommand {
    private final Preference<Boolean> Preference = Preferences.DON_NOT_DISTURB;

    public DoNotDisturbCommand() {
        super("donotdisturb", new String[]{"toggledisturb"});
        this.withUsage("command.donotdisturb.usage");
    }

    @Override
    public final void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        this.argumentBuilder.executes(this::toggle);
    }

    private int toggle(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ServerUser user = (ServerUser) this.getOnlineUser(ctx);

        user.getPreferences().set(this.Preference, !user.getPreference(this.Preference));

        if (user.getPreference(this.Preference)) {
            ((OnlineUser) user).sendLangMessage("command.donotdisturb.on");
        } else {
            ((OnlineUser) user).sendLangMessage("command.donotdisturb.off");
        }

        return SUCCESS;
    }

}

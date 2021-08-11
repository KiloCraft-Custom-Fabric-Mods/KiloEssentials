package org.kilocraft.essentials.util.settings.values;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import org.kilocraft.essentials.util.settings.values.util.ConfigurableSetting;
import org.kilocraft.essentials.util.settings.values.util.RangeSetting;

import java.util.function.Consumer;

public class IntegerSetting extends ConfigurableSetting<Integer> implements RangeSetting<Integer> {

    private Integer from = Integer.MIN_VALUE;
    private Integer to = Integer.MAX_VALUE;

    public IntegerSetting(Integer value, String id) {
        super(value, id);
    }

    @Override
    public ArgumentType<Integer> valueArgumentType() {
        return IntegerArgumentType.integer(from, to);
    }

    @Override
    public void setValueFromCommand(CommandContext<ServerCommandSource> ctx) {
        this.setValue(Integer.parseInt(StringArgumentType.getString(ctx, commandArgumentValue)));
    }

    @Override
    public String getFormattedValue() {
        return "<gold>" +  getValue();
    }

    @Override
    protected void setValue(NbtCompound tag) {
        tag.putInt("value", this.getValue());
    }

    @Override
    protected Integer getValue(NbtCompound tag) {
        return tag.getInt("value");
    }

    @Override
    public IntegerSetting onChanged(Consumer<Integer> consumer) {
        return (IntegerSetting) super.onChanged(consumer);
    }

    @Override
    public IntegerSetting range(Integer from, Integer to) {
        this.from = from;
        this.to = to;
        return this;
    }
}

package org.kilocraft.essentials.util.commands.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.kilocraft.essentials.api.command.ArgumentSuggestions;
import org.kilocraft.essentials.api.command.EssentialCommand;
import org.kilocraft.essentials.api.user.CommandSourceUser;
import org.kilocraft.essentials.util.CommandPermission;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import net.minecraft.commands.CommandSourceStack;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CalculateCommand extends EssentialCommand {
    public CalculateCommand() {
        super("calculate", CommandPermission.CALCULATE, new String[]{"calc"});
        this.withUsage("command.calculate.usage", "input");
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, String> inputArgument = this.argument("input", StringArgumentType.greedyString())
                .suggests(this::operations)
                .executes(this::execute);

        this.commandNode.addChild(inputArgument.build());
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceUser src = this.getCommandSource(ctx);
        String input = StringArgumentType.getString(ctx, "input");
//        StringUtils.Calculator calculator = new StringUtils.Calculator(input);
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        Double result = null;
        try {
            if (input.matches("[\\d+-\\/*()%]+")) {
                result = Double.valueOf(String.valueOf(engine.eval(input)));
            } else {
                throw new ScriptException("");
            }
        } catch (ScriptException e) {
            src.sendLangError("command.calculate.syntax");
            return 1;
        }

//        if (Arrays.stream(StringUtils.Calculator.operations()).parallel().noneMatch(input::contains)) {
//            src.sendLangError("command.calculate.no_operators");
//            return FAILED;
//        }

//        try {
//            calculator.calculate();
//            result = (int) calculator.result();
//        } catch (Exception e) {
//            src.sendError(Texter.exceptionToText(e, true));
//            return FAILED;
//        }
        DecimalFormat df = new DecimalFormat("#.##");
        src.sendLangMessage("command.calculate.result", input, df.format(result));
        return result.intValue();
    }

    private CompletableFuture<Suggestions> operations(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        ArrayList<String> operations = new ArrayList<>();
//        List<String> commands = Arrays.asList(super.getAlias());
        List<String> commands = new LinkedList<String>(Arrays.asList(super.getAlias()));
        commands.add(super.getLabel());
        for (String command : commands) {
            if (ctx.getInput().matches("\\/" + command + " [(]*[0-9]+([+][(]*[0-9]+[)]*)*")) {
                operations.add("+");
                operations.add("-");
                operations.add("*");
                operations.add("/");
                operations.add("%");
            }
        }
//        operations.add(ctx.getInput());
        operations.add("(");
        operations.add(")");


        return ArgumentSuggestions.suggestAtCursor(operations, ctx);
    }
}

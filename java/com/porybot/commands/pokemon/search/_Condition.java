package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.porybot.GameElements.Enums.Target;

import Utils.Methods;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public interface _Condition extends Predicate<PairBox>{
	public String getEmoteText();

	public boolean eval(PairBox o);

	@Override
	public default boolean test(PairBox o) {
		return eval(o);
	}

	static Target parseTarget(OptionMapping op) {
		String target = Methods.getOptionValue(op, null);
		Target t = target != null ? Target.valueOf(target) : null;
		return t;
	}

	public static List<Choice> generateTargetChoices(){
		return Arrays.asList(Target.values()).stream().skip(1).map(t -> new Command.Choice(t.getName(), t.name())).collect(Collectors.toList());
	}
}
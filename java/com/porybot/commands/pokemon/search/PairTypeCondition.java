package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.Enums.Type;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class PairTypeCondition implements _Condition {
	private Type type;

	private PairTypeCondition(Type type) { this.type = type; }
	public static PairTypeCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("pair_type") == null)
			return null;
		return new PairTypeCondition(Type.valueOf(event.getOption("pair_type").getAsString()));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Type.values()).stream().skip(1).map(t -> new Command.Choice(t.getName(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() { return BotType.getPokemonInstance().getImages().getEmoteText(type.getEmote()) + " Type"; }
	@Override
	public boolean eval(PairBox o) { return o.getPair().getType() == type; }
}
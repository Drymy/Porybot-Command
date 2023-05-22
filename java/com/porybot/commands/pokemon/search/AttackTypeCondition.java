package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.Move;
import com.porybot.GameElements.Enums.Category;
import com.porybot.GameElements.Enums.Target;
import com.porybot.GameElements.Enums.Type;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class AttackTypeCondition implements _Condition {
	private Type type;
	private Target target;

	private AttackTypeCondition(Type type, Target target) {
		this.type = type;
		this.target = target;
	}
	public static AttackTypeCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("attack_type") == null)
			return null;
		return new AttackTypeCondition(Type.valueOf(event.getOption("attack_type").getAsString()), _Condition.parseTarget(event.getOption("target")));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Type.values()).stream().skip(1).map(t -> new Command.Choice(t.getName(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() {
		return BotType.getPokemonInstance().getImages().getEmoteText(type.getEmote()) + (target != null ? target.getEmoteText() : "") + " " +
				(target != null ? target.getName() + " " : "") + type.getName() + " Move";
	}
	private boolean isValidTarget(Move m) { return target == null || m.getTarget() == target; }
	@Override
	public boolean eval(PairBox sp) {
		sp.filterMoves(sp.getMoveList().stream()
				.filter(m -> isValidTarget(m) && sp.getPair().getMoveTypeWithPassives(m, null) == type
						&& (m.getCategory().equals(Category.Physical) || m.getCategory().equals(Category.Special)))
				.collect(Collectors.toList()));
		return !sp.getMoveList().isEmpty();
	}
}
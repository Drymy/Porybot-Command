package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.BoardPassive;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.Enums.Stat;
import com.porybot.GameElements.Enums.Target;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class StatDownCondition implements _Condition {
	private Stat stat;
	private Target target;

	private StatDownCondition(Stat stat, Target target) {
		this.stat = stat;
		this.target = target;
	}
	public static StatDownCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("stat_down") == null)
			return null;
		return new StatDownCondition(Stat.valueOf(event.getOption("stat_down").getAsString()), _Condition.parseTarget(event.getOption("target")));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Stat.values()).stream().skip(1).map(t -> new Command.Choice(t.getName(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() {
		return BotType.getPokemonInstance().getImages().getEmoteText(stat.name() + "_DOWN") + (target != null ? target.getEmoteText() : "") + " " +
				(target != null ? target.getName() + " " : "") + stat.getName() + " Down";
	}
	private boolean isValidTarget(Passive p) {
		boolean ret = target == null || (p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(target.name())) ||
				(target == Target.OPPONENTSINGLE && p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(Target.OPPONENTALL.name()))) ||
				(p.getTags().stream().flatMap(List::stream).noneMatch(t -> Target.getFromTag(t) != null) && p instanceof BoardPassive && ((BoardPassive)p).getMove() != null
						&& ((target == ((BoardPassive)p).getMove().getTarget()) ||
								(Target.OPPONENTSINGLE == target && Target.OPPONENTALL == ((BoardPassive)p).getMove().getTarget()))));
		return ret;
	}
	@Override
	public boolean eval(PairBox o) {
		// if(o.getPair().getName().contains("Zinnia & Rayquaza"))
		// System.out.println();
		boolean specialPassive = o.getPair().getAllPassives(null).stream().flatMap(p -> p.getTags().stream().flatMap(List::stream))
				.anyMatch(tt -> tt.getTag().equals("TARGET_RIPPLE"));
		o.filterMoves(o.getMoveList().stream().filter(m -> m.getTags().stream().anyMatch(tt -> {
			// if(m.getName().contains("Ascent"))
			// System.out.println();
			return ((target == null || specialPassive || tt.stream().anyMatch(t -> target.equals(Target.getFromTag(t))) ||

					(o.getPair().getMoveTargetWithPassives(m, null, false).equals(target))

					|| (o.getPair().getMoveTargetWithPassives(m, null, false).equals(Target.OPPONENTALL) && target == Target.OPPONENTSINGLE))
					&& tt.stream().anyMatch(t -> stat.equals(Stat.getFromDownTag(t))));
		})).collect(Collectors.toList()));
		o.filterPassives(o.getPassiveList().stream()
				.filter(p -> isValidTarget(p) && p.getTags().stream().anyMatch(tt -> tt.stream().anyMatch(t -> stat.equals(Stat.getFromDownTag(t))))).collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
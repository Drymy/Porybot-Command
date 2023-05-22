package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.BoardPassive;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.Enums.Target;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class PositiveConditionsCondition implements _Condition {
	public enum Condition {
		STATUSREGEN("Regen", "CONDITION_REGEN"),
		STATUSENDURE("Endure", "CONDITION_ENDURE"),
		STATUSFREEMOVENEXT("Free Move Next", "CONDITION_FREE_MOVE"),
		STATUSCRITHITNEXT("Critical Hit Next", "CONDITION_SURECRIT"),
		STATUSSUREHITNEXT("Sure Hit Next", "CONDITION_SUREHIT"),
		STATUSDAMAGEGUARD("Damage Guard Next", "CONDITION_DAMAGE_GUARD"),
		STATUSSUPEREFFNEXT("Supereffective ↑ Next", "CONDITION_SUPEREFFECTICEUP"),
		STATUSPHYSDMGUPNEXT("Physical Moves ↑ Next", "CONDITION_PHYSICAL_UP"),
		STATUSSPECDMGUPNEXT("Special Moves ↑ Next", "CONDITION_SPECIAL_UP"),
		STATUSNULLCONDITIONS("Condition Nullification", "CONDITION_CONDITION_NULL"),
		STATUSNULLSTATUS("Status Condition Defense", "CONDITION_CONDITION_NULL"),
		STATUSNULLSTATSDOWN("Stat Reduction Defense", "CONDITION_CONDITION_NULL");

		private String description;
		private String emote;

		private Condition(String desc, String emote) {
			description = desc;
			this.emote = emote;
		}
		public String getDescription() { return description; }
		public String getEmoteText() { return emote; }
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
	}

	private Condition condition;
	private Target target;

	private PositiveConditionsCondition(Condition condition, Target target) {
		this.condition = condition;
		this.target = target;
	}
	public static PositiveConditionsCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("positive_condition") == null)
			return null;
		return new PositiveConditionsCondition(Condition.valueOf(event.getOption("positive_condition").getAsString()), _Condition.parseTarget(event.getOption("target")));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Condition.values()).stream().map(t -> new Command.Choice(t.getDescription(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() {
		return BotType.getPokemonInstance().getImages().getEmoteText(condition.getEmoteText()) + (target != null ? target.getEmoteText() : "") + " " +
				(target != null ? target.getName() + " " : "") + condition.getDescription();
	}
	private boolean isValidTarget(Passive p) {
		return target == null || (p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(target.name())) ||
				(target == Target.OPPONENTSINGLE && p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(Target.OPPONENTALL.name()))) ||
				(p instanceof BoardPassive && ((BoardPassive)p).getMove() != null && ((target == ((BoardPassive)p).getMove().getTarget()) ||
						(Target.OPPONENTSINGLE == target && Target.OPPONENTALL == ((BoardPassive)p).getMove().getTarget()))));
	}
	@Override
	public boolean eval(PairBox o) {
		o.filterMoves(o.getMoveList().stream()
				.filter(m -> m.getTags().stream().anyMatch(tt -> (target == null || tt.stream().anyMatch(t -> target.equals(Target.getFromTag(t)))
						|| o.getPair().getMoveTargetWithPassives(m, null, false).equals(target)
						|| o.getPair().getMoveTargetWithPassives(m, null, false).equals(Target.OPPONENTALL) && target == Target.OPPONENTSINGLE)
						&& tt.stream()
								.anyMatch(t -> condition.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(condition.getDescription()))))))
				.collect(Collectors.toList()));
		o.filterPassives(o
				.getPassiveList().stream().filter(
						p -> isValidTarget(p) && p.getTags().stream()
								.anyMatch(tt -> tt.stream().anyMatch(t -> condition.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(condition.getDescription()))))))
				.collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
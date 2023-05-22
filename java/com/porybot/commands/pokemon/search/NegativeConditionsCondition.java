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

public class NegativeConditionsCondition implements _Condition {
	public enum Condition {
		CONDIPOISON("Poison", "STATUS_POISON", true),
		CONDIBADPOISON("Bad Poison", "STATUS_BAD_POISON", true),
		CONDIPARALYSIS("Paralysis", "STATUS_PARALYZE", true),
		CONDISLEEP("Sleep", "STATUS_SLEEP", true),
		CONDIBURN("Burn", "STATUS_BURN", true),
		CONDIFREEZE("Freeze", "STATUS_FREEZE", true),
		STATUSFLINCH("Flinch", "CONDITION_FLINCH", false),
		STATUSCONFUSE("Confusion", "CONDITION_CONFUSE", false),
		STATUSTRAP("Trap", "CONDITION_TRAP", false),
		STATUSRESTRAIN("Restrain", "CONDITION_RESTRAINED", false),
		STATUSNOEVADE("No Evasion", "CONDITION_NODODGE", false),
		ANYCONDITION("Any Condition", "CONDITION_ANY", true),
		ANYINTERFERENCE("Any Interference", "INTERFERENCE_ANY", false);

		private String description;
		private String emote;
		private boolean viral;

		private Condition(String desc, String emote, boolean viral) {
			description = desc;
			this.emote = emote;
			this.viral = viral;
		}
		public String getDescription() { return description; }
		public String getEmoteText() { return emote; }
		public boolean isViral() { return viral; }
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
		public static List<Condition> allConditions() { return Arrays.asList(values()).stream().limit(6).collect(Collectors.toList()); }
		public static List<Condition> allInterferences() { return Arrays.asList(values()).stream().skip(6).limit(3).collect(Collectors.toList()); }
	}

	private Condition condition;
	private Target target;

	private NegativeConditionsCondition(Condition condition, Target target) {
		this.condition = condition;
		this.target = target;
	}
	public static NegativeConditionsCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("negative_condition") == null)
			return null;
		return new NegativeConditionsCondition(Condition.valueOf(event.getOption("negative_condition").getAsString()), _Condition.parseTarget(event.getOption("target")));
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
		boolean specialPassive = condition.isViral() && o.getPair().getAllPassives(null).stream().flatMap(p -> p.getTags().stream().flatMap(List::stream))
				.anyMatch(tt -> tt.getTag().equals("TARGET_GOVIRAL") || tt.getTag().equals("TARGET_HITVIRAL"));
		o.filterMoves(o.getMoveList().stream().filter(m -> m.getTags().stream().anyMatch(tt -> (target == null || specialPassive
				|| tt.stream().anyMatch(t -> target.equals(Target.getFromTag(t))) ||
				o.getPair().getMoveTargetWithPassives(m, null, false).equals(target) ||
				(target == Target.OPPONENTSINGLE && o.getPair().getMoveTargetWithPassives(m, null, false).equals(Target.OPPONENTALL)))
				&& tt.stream().anyMatch(t -> (condition.equals(Condition.ANYCONDITION)
						? Condition.allConditions().stream().anyMatch(c -> c.name().equals(t.getTag()))
						: (condition.equals(Condition.ANYINTERFERENCE)
								? Condition.allInterferences().stream().anyMatch(c -> c.name().equals(t.getTag()))
								: condition.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(condition.getDescription()))))))))
				.collect(Collectors.toList()));
		o.filterPassives(o.getPassiveList().stream()
				.filter(p -> isValidTarget(p) && p.getTags().stream().anyMatch(tt -> tt.stream().anyMatch(t -> (condition.equals(Condition.ANYCONDITION)
						? Condition.allConditions().stream().anyMatch(c -> c.name().equals(t.getTag()))
						: (condition.equals(Condition.ANYINTERFERENCE)
								? Condition.allInterferences().stream().anyMatch(c -> c.name().equals(t.getTag()))
								: condition.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(condition.getDescription()))))))))
				.collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
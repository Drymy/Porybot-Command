package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.Enums.Target;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class FieldCondition extends FieldEffectCondition {
	public enum Field implements FieldEffect {
		FIELDPHYSDMGREDUC("Physical Damage Reduction Field", "FIELD_PHYSICAL_RESIST"),
		FIELDSPECDMGREDUC("Special Damage Reduction Field", "FIELD_SPECIAL_RESIST"),
		FIELDGAUGEACCEL("Move Gauge Acceleration Field", "FIELD_MOVE_RATE_UP"),
		FIELDCONDIDEFEND("Status Move Defense Field", "FIELD_CONDITION_GUARD"),
		FIELDCONDIREFLECT("Status Condition Reflection Field", "FIELD_CONDITION_REFLECT"),
		FIELDSTATREDUCDEFENSE("Stat Reduction Defense Field", "FIELD_DEBUFF_GUARD"),
		FIELDCRITHITDEFENSE("Critical-Hit Defense Field", "FIELD_CRITICAL_GUARD"),
		FIELDVARIABLE("Damage Field", "FIELD_DAMAGE");

		private String description;
		private String emote;

		private Field(String desc, String emote) {
			description = desc;
			this.emote = emote;
		}
		@Override
		public String getDescription() { return description; }
		@Override
		public String getEmoteText() { return emote; }
		@Override
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
	}

	private Field obj;
	private Target target;

	private FieldCondition(Field obj, Target target) {
		this.obj = obj;
		this.target = target;
	}
	public static FieldCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("field_effects") == null || !event.getOption("field_effects").getAsString().startsWith("FIELD"))
			return null;
		return new FieldCondition(Field.valueOf(event.getOption("field_effects").getAsString()), _Condition.parseTarget(event.getOption("target")));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Field.values()).stream().map(t -> new Command.Choice(t.getDescription(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() {
		return BotType.getPokemonInstance().getImages().getEmoteText(obj.getEmoteText()) + (target != null ? target.getEmoteText() : "") + " " +
				(target != null ? target.getName() + " " : "") + obj.getDescription();
	}
	private boolean isValidTarget(Passive p) {
		return target == null || (p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(target.name())) ||
				((target == Target.OPPONENTFIELD || target == Target.ALLYFIELD)
						&& p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(Target.ENTIREFIELD.name()))));
	}
	@Override
	public boolean eval(PairBox o) {
		o.filterMoves(o.getMoveList().stream().filter(m -> m.getTags().stream().anyMatch(tt -> (target == null || tt.stream().anyMatch(t -> target.equals(Target.getFromTag(t))))
				&& tt.stream().anyMatch(t -> obj.name().equals(t.getTag())))).collect(Collectors.toList()));
		o.filterPassives(o.getPassiveList().stream().filter(p -> {
			try {
				return isValidTarget(p) && p.getTags().stream().filter(tt -> tt != null && tt.size() > 0).anyMatch(
						tt -> tt.stream().anyMatch(t -> obj.name().contains(t.getTag()) || (t.getTagId() != null && obj.getDescription().contains(t.getRealValue(t.getTagId())))));
			} catch(Exception e) {
				System.out.println();
				throw e;
			}
		}).collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
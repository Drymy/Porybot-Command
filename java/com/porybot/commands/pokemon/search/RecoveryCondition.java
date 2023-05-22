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

public class RecoveryCondition implements _Condition {
	public enum Recovery {
		RECOVER("Recover HP", "RECOVER_HP"),
		ABSORBHP("Absorb HP", "RECOVER_HP"),
		RECOVEROWNMP("Recover MP", "RECOVER_MP"),
		REMOVECONDITIONS("Remove Conditions", "RECOVER_CLEANSE"),
		REMOVESTATUS("Remove Status", "RECOVER_CLEANSE"),
		CHARGEGAUGE("Gauge Charge", "RECOVER_GAUGE"),
		ACCELGAUGE("Gauge Acceleration", "RECOVER_GAUGE_ACCEL");

		private String description;
		private String emote;

		private Recovery(String desc, String emote) {
			description = desc;
			this.emote = emote;
		}
		public String getDescription() { return description; }
		public String getEmoteText() { return emote; }
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
	}

	private Recovery recovery;
	private Target target;

	private RecoveryCondition(Recovery recovery, Target target) {
		this.recovery = recovery;
		this.target = target;
	}
	public static RecoveryCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("recovery") == null)
			return null;
		return new RecoveryCondition(Recovery.valueOf(event.getOption("recovery").getAsString()), _Condition.parseTarget(event.getOption("target")));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Recovery.values()).stream().map(t -> new Command.Choice(t.getDescription(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() {
		return BotType.getPokemonInstance().getImages().getEmoteText(recovery.name()) + (target != null ? target.getEmoteText() : "") + " " +
				(target != null ? target.getName() + " " : "") + recovery.getDescription();
	}
	private boolean isValidTarget(Passive p) {
		return target == null || (p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(target.name())) ||
				((target == Target.ALLYSINGLE || target == Target.SELF) && p.getTags().stream().flatMap(List::stream).anyMatch(t -> t.getTag().equals(Target.ALLYALL.name()))));
	}
	@Override
	public boolean eval(PairBox o) {
		o.filterMoves(o.getMoveList().stream().filter(m -> m.getTags().stream().anyMatch(tt -> (target == null || tt.stream().anyMatch(t -> target.equals(Target.getFromTag(t))))
				&& tt.stream()
						.anyMatch(t -> recovery.name().equals(t.getTag())
								|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(recovery.getDescription()))))))
				.collect(Collectors.toList()));
		o.filterPassives(o
				.getPassiveList().stream().filter(
						p -> isValidTarget(p) && p.getTags().stream()
								.anyMatch(tt -> tt.stream().anyMatch(t -> recovery.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(recovery.getDescription()))))))
				.collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
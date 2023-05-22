package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class SpecialEffectsCondition implements _Condition {
	public enum Effect {
		SYNCACCEL("Sync Acceleration", "CONDITION_SYNC"),
		PROVOKE("Provoke", "SPECIAL_PROVOKE"),
		PROTECT("Defensive Posture", "CONDITION_DEFENDING"),
		SURE_HIT("Sure Hit", "CONDITION_NODODGE"),
		STATCLEANSE("Stat Cleanse", "REMOVESTATUS");

		private String description;
		private String emote;

		private Effect(String desc, String emote) {
			description = desc;
			this.emote = emote;
		}
		public String getDescription() { return description; }
		public String getEmoteText() { return emote; }
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
	}

	private Effect effect;

	private SpecialEffectsCondition(Effect effect) { this.effect = effect; }
	public static SpecialEffectsCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("special_effect") == null)
			return null;
		return new SpecialEffectsCondition(Effect.valueOf(event.getOption("special_effect").getAsString()));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Effect.values()).stream().map(t -> new Command.Choice(t.getDescription(), t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() { return BotType.getPokemonInstance().getImages().getEmoteText(effect.getEmoteText()) + " " + effect.getDescription(); }
	@Override
	public boolean eval(PairBox o) {
		o.filterMoves(o
				.getMoveList().stream().filter(
						m -> m.getTags().stream()
								.anyMatch(tt -> tt.stream().anyMatch(t -> effect.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(effect.getDescription()))))))
				.collect(Collectors.toList()));
		o.filterPassives(o
				.getPassiveList().stream().filter(
						p -> p.getTags().stream()
								.anyMatch(tt -> tt.stream().anyMatch(t -> effect.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(effect.getDescription()))))))
				.collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
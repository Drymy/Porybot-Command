package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.Enums.Type;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class ZoneCondition extends FieldEffectCondition {
	public enum Zone implements FieldEffect {
		ZONEPOISON(Type.POISON.getName(), null),
		ZONEGROUND(Type.GROUND.getName(), null),
		ZONEFLYING(Type.FLYING.getName(), null),
		ZONEBUG(Type.BUG.getName(), null),
		ZONEGHOST(Type.GHOST.getName(), null),
		ZONEDRAGON(Type.DRAGON.getName(), null),
		ZONEDARK(Type.DARK.getName(), null),
		ZONESTEEL(Type.STEEL.getName(), null),
		ZONEFAIRY(Type.FAIRY.getName(), null);

		private String description;
		private String emote;

		private Zone(String desc, String emote) {
			description = desc;
			this.emote = emote;
		}
		@Override
		public String getDescription() { return description; }
		@Override
		public String getEmoteText() { return emote != null ? emote : "ZONE_" + description.toUpperCase(); }
		@Override
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
	}

	private Zone obj;

	private ZoneCondition(Zone obj) { this.obj = obj; }
	public static ZoneCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("field_effects") == null || !event.getOption("field_effects").getAsString().startsWith("ZONE"))
			return null;
		return new ZoneCondition(Zone.valueOf(event.getOption("field_effects").getAsString()));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Zone.values()).stream().map(t -> new Command.Choice(t.getDescription() + (t.getDescription().contains("Zone") ? "" : " Zone"), t.name()))
				.collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() {
		return BotType.getPokemonInstance().getImages().getEmoteText(obj.getEmoteText()) + " " + obj.getDescription() + (obj.getDescription().contains("Zone") ? "" : " Zone");
	}
	@Override
	public boolean eval(PairBox o) {
		if(o.getPair().getTrainer().getCharacterId().equals("10282000000"))
			return false;
		o.filterMoves(o.getMoveList().stream()
				.filter(m -> m.getTags().stream()
						.anyMatch(tt -> tt.stream()
								.anyMatch(t -> obj.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(obj.getDescription()))))))
				.collect(Collectors.toList()));
		o.filterPassives(o.getPassiveList().stream()
				.filter(p -> p.getTags().stream()
						.anyMatch(tt -> tt.stream()
								.anyMatch(t -> obj.name().equals(t.getTag())
										|| (t.getTag().equals("TAG") && t.getRealValues().values().stream().anyMatch(tag -> tag.equalsIgnoreCase(obj.getDescription()))))))
				.collect(Collectors.toList()));
		return !o.getMoveList().isEmpty() || !o.getPassiveList().isEmpty();
	}
}
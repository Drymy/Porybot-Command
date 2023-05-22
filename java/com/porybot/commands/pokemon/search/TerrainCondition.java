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

public class TerrainCondition extends FieldEffectCondition {
	public enum Terrain implements FieldEffect {
		TERRAINELECTRIC(Type.ELECTRIC.getName(), null),
		TERRAINGRASS(Type.GRASS.getName(), null),
		TERRAINPSYCHIC(Type.PSYCHIC.getName(), null);

		private String description;
		private String emote;

		private Terrain(String desc, String emote) {
			description = desc;
			this.emote = emote;
		}
		@Override
		public String getDescription() { return description; }
		@Override
		public String getEmoteText() { return emote != null ? emote : "TERRAIN_" + description.toUpperCase(); }
		@Override
		public RichCustomEmoji getEmote() { return BotType.getPokemonInstance().getImages().getEmoteClassByName(getEmoteText()); }
	}

	private Terrain obj;

	private TerrainCondition(Terrain obj) { this.obj = obj; }
	public static TerrainCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("field_effects") == null || !event.getOption("field_effects").getAsString().startsWith("TERRAIN"))
			return null;
		return new TerrainCondition(Terrain.valueOf(event.getOption("field_effects").getAsString()));
	}
	public static List<Choice> generateChoices() {
		return Arrays.asList(Terrain.values()).stream().map(t -> new Command.Choice(t.getDescription() + " Terrain", t.name())).collect(Collectors.toList());
	}
	@Override
	public String getEmoteText() { return BotType.getPokemonInstance().getImages().getEmoteText(obj.getEmoteText()) + " " + obj.getDescription() + " Terrain"; }
	@Override
	public boolean eval(PairBox o) {
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
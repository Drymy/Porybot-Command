package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.porybot.GameElements.Enums.Region;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class RegionCondition implements _Condition {
	private Region region;

	private RegionCondition(Region region) { this.region = region; }
	public static List<Choice> generateChoices() {
		return Arrays.asList(Region.values()).stream().filter(r -> r.getTicketEmote() != null)
				.map(r -> new Command.Choice(r.getText(), r.name())).collect(Collectors.toList());
	}
	public static RegionCondition of(Region region) { return new RegionCondition(region); }
	@Override
	public String getEmoteText() { return BotType.getPokemonInstance().getImages().getEmoteText("THEME_REGION") + " " + region.name() + " Region"; }
	@Override
	public boolean eval(PairBox o) { return o.getPair().getRegion() == region; }
}
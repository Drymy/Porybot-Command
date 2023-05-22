package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import com.manager.BotType;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class RarityCondition implements _Condition {
	private String minRarity;

	private RarityCondition(String minRarity) { this.minRarity = minRarity; }
	public static List<Choice> generateChoices() {
		return Arrays.asList(new Choice("Eggmons Only", "0"), new Choice("1* and above", "1"), new Choice("2* and above", "2"),
				new Choice("3* and above", "3"), new Choice("4* and above", "4"), new Choice("5* and above", "5"), new Choice("EX Only", "6"));
	}
	public static RarityCondition of(String minRarity) { return new RarityCondition(minRarity); }
	@Override
	public String getEmoteText() {
		return (minRarity.equals("0")
				? BotType.getPokemonInstance().getImages().getEmoteText("RARITY_EGG") + " Eggmons Only"
				: minRarity.equals("6")
						? BotType.getPokemonInstance().getImages().getEmoteText("STAREX_1") + " EX Only"
						: BotType.getPokemonInstance().getImages().getEmoteText("RARITY_" + minRarity) + " " + minRarity + "* Min Rarity");
	}
	@Override
	public boolean eval(PairBox o) {
		return minRarity.equals("0")
				? o.getPair().getTrainer().getRarity().longValue() == 1L
				: minRarity.equals("6") ? o.getPair().getTrainer().isEX() : o.getPair().getTrainer().getRarity().longValue() >= Long.parseLong(minRarity);
	}
}
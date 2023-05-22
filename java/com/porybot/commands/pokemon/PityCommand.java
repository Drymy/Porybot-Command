package com.porybot.commands.pokemon;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import Utils.BotException;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PityCommand extends _PokemonCommand {
	public PityCommand() { super("pity", "Calculate pity"); }
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		Long points = Methods.getOptionValue(event.getOption("points"), 0L);
		Long gems = Methods.getOptionValue(event.getOption("gems"), 0L);
		String desc = "";
		if(gems < 0)
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(),
					"> You have " + gems + " gems? You better ask DeNA customer support for help, that's not normal.");
		if(gems > 0) {
			Long gemsCalc = gems;
			Long pointsTotal = 0L;
			while(gemsCalc >= 3000) {
				pointsTotal += 33;
				gemsCalc -= 3000;
			}
			pointsTotal += (gemsCalc / 100L);
			NumberFormat nf2 = NumberFormat.getInstance(new Locale("en", "US"));
			desc = "> If you spend **" + nf2.format(gems) + "** gems, you'll have **" + pointsTotal + "** scout points." + System.lineSeparator();
			points = pointsTotal;
		}
		if(points >= 400) {
			if(points > 432L)
				return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(),
						"> You have " + points + " points? You better ask DeNA customer support for help, that's not normal.");
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "> You have enough for pity.");
		}
		if(points < 0)
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(),
					"> You have " + points + " points? You better ask DeNA customer support for help, that's not normal.");
		Long pointsRequired = 400 - points;
		Long multis = 0L;
		while(pointsRequired >= 12) {
			multis++;
			pointsRequired -= 33;
		}
		NumberFormat nf2 = NumberFormat.getInstance(new Locale("en", "US"));
		if(multis >= 1)
			desc += "> To reach pity, you'll need to spend **" + nf2.format(multis * 3000) + "** gems on **" + multis + "** Multis Scouts to reach **" + (400 - pointsRequired)
					+ "** scout points." + System.lineSeparator() +
					(pointsRequired <= 0 ? "" : "> You'll then need an additional **" + pointsRequired + "** scout points." + System.lineSeparator());
		else
			desc += "> You'll need an additional **" + pointsRequired + "** scout points to reach **400** scout points." + System.lineSeparator();
		desc += "> 100 **Paid-Only Gems** Daily Discount Scout gives __**1**__ Scout Point." + System.lineSeparator() +
				"> 300 **Gems** Single Scout gives __**3**__ Scout Points.";
		return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), desc);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od3 = new OptionData(OptionType.INTEGER, "points", "How many scout points you have", false);
		OptionData od4 = new OptionData(OptionType.INTEGER, "gems", "How many gems you have to spend", false);
		cd.addOptions(od3, od4);
		return cd;
	}
}
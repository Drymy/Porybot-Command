package com.porybot.commands.pokemon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import Utils.BotException;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class LodgeCommand extends _PokemonCommand {
	public LodgeCommand() { super("lodge", "Shows infographic for a pair's topics"); }
	public static Embed createEmbed(String trainer, String imageURL) {
		Embed eb = new Embed(BotType.getPokemonInstance());
		eb.setImage(imageURL);
		eb.setFooter("Credits to Zamcio for the infographs" + System.lineSeparator() + "https://twitter.com/zamcio/");
		return eb;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String trainer = event.getOption("name").getAsString().toLowerCase();
		String newData = Methods.getOptionValue(event.getOption("url"), null);
		if(newData != null)
			try {
				BotType.getPokemonInstance().getSQL().executeInsert("REPLACE INTO Lodge VALUES(?, ?)", trainer, newData);
				return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "Updated");
			} catch(BotException e) {
				return BotType.getPokemonInstance().getMessages().sendStatusMessageError(event.getHook(), "Error Updating");
			}
		String image = get(trainer);
		if(image == null)
			return BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), "Trainer with that name isn't registered.");
		Embed eb = createEmbed(trainer, image);
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), eb);
	}
	public static String get(String name) {
		try(ResultSet rs = BotType.getPokemonInstance().getSQL().executeSelect("SELECT * FROM Lodge WHERE name = ?", name)) {
			if(rs.next())
				return rs.getString("url");
		} catch(BotException | SQLException e) {
			BotType.getPokemonInstance().getMessages().sendReport("Unable to load simple commands");
		}
		return null;
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od = new OptionData(OptionType.STRING, "name", "Trainer's Name", true);
		return cd.addOptions(od);
	}
}
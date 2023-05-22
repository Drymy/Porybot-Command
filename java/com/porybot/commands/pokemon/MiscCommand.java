package com.porybot.commands.pokemon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import Utils.BotException;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import Utils.Methods.MiscInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class MiscCommand extends _PokemonCommand {
	public MiscCommand() { super("misc", "Lots of easily accessible information"); }
	public static String getOwner(Embed eb, String owner) {
		if(StringUtils.isNumeric(owner))
			owner = BotType.getPokemonInstance().getClient().getUserById(owner).getName() + "#" + BotType.getPokemonInstance().getClient().getUserById(owner).getDiscriminator();
		if(eb != null && owner != null && owner.contains("#"))
			eb.setFooter("Credits to " + BotType.getPokemonInstance().getClient().getUserById(owner).getAvatarUrl(), owner);
		else if(eb != null && owner != null)
			eb.setFooter("Credits to " + owner);
		return owner;
	}
	public static Embed createEmbed(MiscInfo mi) {
		Embed eb = new Embed(BotType.getPokemonInstance());
		eb.setImage(mi.data);
		getOwner(eb, mi.owner);
		return eb;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String info = event.getOption("info").getAsString();
		String newData = Methods.getOptionValue(event.getOption("data"), null);
		MiscInfo mi = MiscInfo.get(info);
		if(newData != null)
			try {
				BotType.getPokemonInstance().getSQL().executeInsert("UPDATE Commands SET data = ? WHERE name = ?", newData, info);
				return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "Updated");
			} catch(BotException e) {
				return BotType.getPokemonInstance().getMessages().sendStatusMessageError(event.getHook(), "Error Updating");
			}
		if(mi == null) {
			BotType.getPokemonInstance().getMessages().sendReport("Error loading data '" + info + "' from DB");
			return BotType.getPokemonInstance().getMessages().sendStatusMessageError(event.getHook(), "Error loading data, please try again later");
		}
		if(!Methods.isImage(mi.data))
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(),
					mi.data + (mi.owner != null ? System.lineSeparator() + "Credits to " + getOwner(null, mi.owner) : ""));
		Embed eb = createEmbed(mi);
		if(info.equalsIgnoreCase("Candy Suggestions"))
			eb.setDescription("Order inside the tiers  take in account how good and how much units gain from grid." + System.lineSeparator()
					+ "Never candy a non-limited 5* pair.");
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), eb);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od = new OptionData(OptionType.STRING, "info", "What info to show", true);
		List<MiscInfo> infos = new LinkedList<>();
		if(infos.isEmpty())
			try(ResultSet rs = BotType.getPokemonInstance().getSQL().executeSelect("SELECT * FROM Commands")) {
				while(rs.next()) {
					MiscInfo mi = new MiscInfo();
					mi.name = rs.getString("name");
					infos.add(mi);
				}
			} catch(BotException | SQLException e) {
				BotType.getPokemonInstance().getMessages().sendReport("Unable to load simple commands");
			}
		for(MiscInfo mi : infos)
			od.addChoice(mi.name.toLowerCase(), mi.name);
		return cd.addOptions(od);
	}
}
package com.porybot.commands.pokemon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot.GameElements.Enums.LuckyCookie;
import Utils.BotException;
import Utils.MessageUtils;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class CookieCommand extends _PokemonCommand {
	public CookieCommand() { super("cookie", "Show Lucky Skills information"); }
	private static Embed createTypeEmbed(ResultSet rs, String type) throws SQLException {
		LuckyCookie lc = LuckyCookie.valueOf(type);
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(lc.getDescription(),
				"https://www.serebii.net/pokemonmasters/luckyskills.shtml#" + lc.getDescription().replace("*", "star").replace(" ", "").toLowerCase(),
				lc.getEmoteClass().getImageUrl());
		String desc = "";
		String lastDesc = "";
		while(rs.next()) {
			desc += System.lineSeparator() + "**__" + rs.getString(1) + "__ (" + rs.getString(3) + ")**";
			String newDesc = rs.getString(4).replace("by 2 stat ranks ", "").replace("by 1 stat rank ", "");
			if(!newDesc.equals(lastDesc))
				desc += " - " + (lastDesc = newDesc);
			// else
			// System.out.print("");
		}
		embed.setDescription(desc.trim());
		return embed;
	}
	private static Embed createNameEmbed(ResultSet rs, String name) throws SQLException {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor("Searching for \"" + name + "\"");
		TreeMap<LuckyCookie, List<String>> map = new TreeMap<>();
		String lastDesc = null;
		LuckyCookie lastCookie = null;
		while(rs.next()) {
			LuckyCookie lc = LuckyCookie.valueOf(rs.getString(2));
			map.putIfAbsent(lc, new LinkedList<>());
			String desc = "**" + rs.getString(1) + " (" + rs.getString(3) + ")**";
			if(!(rs.getString(4).equals(lastDesc) && lc == lastCookie)) {
				desc += " - " + (lastDesc = rs.getString(4));
				lastCookie = lc;
			}
			map.get(lc).add(desc);
		}
		for(LuckyCookie lc : map.keySet()) {
			boolean first = true;
			String desc = "";
			for(String str : map.get(lc))
				if(desc.length() + str.length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
					embed.addField(first ? lc.getEmoteText() + " " + lc.getDescription() : "", desc.trim(), false);
					desc = "";
					first = false;
				} else
					desc += System.lineSeparator() + str;
			if(!desc.isBlank())
				embed.addField(first ? lc.getEmoteText() + " " + lc.getDescription() : "", desc.trim(), false);
		}
		return embed;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String type = Methods.getOptionValue(event.getOption("type"), null);
		String name = Methods.getOptionValue(event.getOption("name"), null);
		if((type == null && name == null) || (type != null && name != null))
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "Please insert either 'type' or 'name' (and not both)");
		if(name != null && Methods.removeNonAlphanumeric(name).length() < 4)
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "Name must be at least 4 characters long");
		String query = "SELECT * FROM Lucky_Skills WHERE " + (type != null ? "cookieType = ?" : "LOWER(name) LIKE ?");
		String value = type != null ? type : "%" + Methods.removeNonAlphanumeric(name) + "%";
		try(ResultSet rs = BotType.getPokemonInstance().getSQL().executeSelect(query, value.toLowerCase())) {
			if(type != null)
				return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), createTypeEmbed(rs, type));
			return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), createNameEmbed(rs, name));
		} catch(BotException | SQLException e) {
			return BotType.getPokemonInstance().getMessages().sendStatusMessageError(event.getHook(), "Error obtaining data. Try again later.");
		}
	}
	@Override
	public boolean isEtherealReply(SlashCommandInteractionEvent event) {
		return event.getOption("type") == event.getOption("name") || (event.getOption("name") != null && event.getOption("type") != null);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od2 = new OptionData(OptionType.STRING, "type", "Type of cookie", false);
		OptionData od3 = new OptionData(OptionType.STRING, "name", "Skill Name", false);
		Arrays.asList(LuckyCookie.values()).stream().forEach(r -> od2.addChoices(new Command.Choice(r.getDescription(), r.name())));
		cd.addOptions(od2, od3);
		return cd;
	}
}
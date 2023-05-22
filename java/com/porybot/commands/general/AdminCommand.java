package com.porybot.commands.general;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.Acquisition;
import com.porybot.commands.pokemon.LodgeCommand;
import com.porybot.commands.pokemon.MiscCommand;
import Utils.BotException;
import Utils.Constants;
import Utils.Dual;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import Utils.Methods.MiscInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

public class AdminCommand extends _PokemonCommand {
	public AdminCommand() { super("admin", "Update information"); }
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		CompletableFuture<Message> ret = null;
		String message = "Nothing done";
		switch(event.getSubcommandGroup()) {
			case "operation": {
				switch(event.getSubcommandName()) {
					case "update":
						BotType.PORYBOT.getInstance().getSQL().setKeyValue("OPERATION", "1");
						message = "Update Queued";
						break;
				}
				break;
			}
			case "patreon": {
				if(PatreonCommand.updatePatreonKey(event.getMember(), event.getOption("key").getAsString()))
					message = "Patreon API Key Updated";
				else
					message = "Error updating Patreon API Key";
				break;
			}
			case "emotes": {
				List<SyncPair> lsp = _Library.get(event.getOption("name").getAsString());
				if(lsp.size() != 1)
					message = "Please write a specific name";
				else {
					switch(event.getSubcommandName()) {
						case "add":
							try {
								InputStream is = event.getOption("emote").getAsAttachment().getProxy().download().get();
								Icon trimmedIcon = Methods.imageResizer(is);

								List<Guild> lg = instance.getClient().getGuilds().stream()
										.filter(s -> s.getOwnerIdLong() == Constants.QUETZ2_ID && s.getIdLong() != Constants.PORYBOT_SERVER_ID)
										.filter(g -> g.getName().startsWith("PoryphoneBot Characters ")).collect(Collectors.toList());
								// .filter(g -> g.getEmojis().size() <
								// g.getMaxEmojis()).findFirst().orElse(null);

								Dual<Integer, Integer> counts = lg.stream().map(g -> new Dual<Integer, Integer>(g.getEmojis().size(), g.getMaxEmojis()))
										.reduce(new Dual<Integer, Integer>(0, 0), (d1, d2) -> {
											d1.setValue1(d1.getValue1() + d2.getValue1());
											d1.setValue2(d1.getValue2() + d2.getValue2());
											return d1;
										});
								Guild gg = lg.stream().filter(g -> g.getEmojis().size() < g.getMaxEmojis()).findFirst().orElse(null);
								if(gg == null) {
									message = "No more emote spaces available, please create a new server!";
								} else {
									RichCustomEmoji emoteNew = gg.createEmoji(lsp.get(0).getTrainer().getCharacterId(), trimmedIcon).complete();
									message = "Emote Space Used: " + counts.getValue1() + "/" + counts.getValue2() + System.lineSeparator()
											+ "Emote Created on " + gg.getName()
											+ System.lineSeparator() + "New Emote: " + emoteNew.getImageUrl();
								}
							} catch(InterruptedException | ExecutionException | IOException e) {
								message = "Error parsing image";
							}
							break;
						case "update":
							RichCustomEmoji emote = instance.getImages().getEmoteClassByName(lsp.get(0).getTrainer().getCharacterId());
							if(emote != null) {
								Guild g = emote.getGuild();
								try {
									InputStream is = event.getOption("emote").getAsAttachment().getProxy().download().get();
									Icon trimmedIcon = Methods.imageResizer(is);
									emote.delete().complete();
									RichCustomEmoji emoteNew = g.createEmoji(lsp.get(0).getTrainer().getCharacterId(), trimmedIcon).complete();
									message = "Emote Changed on " + g.getName() + System.lineSeparator() + "Previous Emote: " + emote.getImageUrl()
											+ System.lineSeparator() + "New Emote: " + emoteNew.getImageUrl();
								} catch(InterruptedException | ExecutionException | IOException e) {
									message = "Error parsing image";
								}
							}
							break;
					}
				}
				break;
			}
			case "styles": {
				List<SyncPair> lsp = _Library.get(event.getOption("name").getAsString());
				if(lsp.size() != 1)
					message = "Please write a specific name";
				else
					switch(event.getSubcommandName()) {
						case "add":
							instance.getSQL().executeInsert("INSERT INTO Styles VALUES(?, ?, ?)", lsp.get(0).getTrainer().getCharacterId(),
									event.getOption("main").getAsString(), event.getOption("ex").getAsString());
							message = "Styles added for " + lsp.get(0).getName() + "\"";
							break;
						case "remove":
							instance.getSQL().executeInsert("DELETE FROM Styles WHERE characterId = ?", lsp.get(0).getTrainer().getCharacterId());
							message = "Styles removed for \"" + lsp.get(0).getName() + "\"";
							break;
					}
				break;
			}
			case "nicknames": {
				switch(event.getSubcommandName()) {
					case "add":
						String nick = event.getOption("nickname").getAsString();
						List<SyncPair> lsp = _Library.get(event.getOption("name").getAsString());
						if(lsp.size() != 1)
							message = "Please write a specific name";
						else {
							if(!_Library.get(nick).isEmpty())
								message = "Nickname already found, but adding it anyway";
							instance.getSQL().executeInsert("INSERT INTO Nicknames VALUES (?, ?)", lsp.get(0).getName(), nick);
							message = "Nickname added";
						}
						break;
					case "remove":
						String nick2 = event.getOption("nickname").getAsString();
						if(_Library.get(nick2).isEmpty())
							message = "Nickname not found";
						else {
							List<String> units = instance.getSQL().getUnitNamesFromNickname(nick2);
							instance.getSQL().executeInsert("DELETE FROM Nicknames WHERE nickname = ?", nick2);
							message = "Nickname removed from unit(s):" + units.stream().reduce((o1, o2) -> o1 + "/" + o2).get();
						}
						break;
					case "list":
						try {
							HashMap<String, String> nicks = instance.getSQL().getNicknames();
							File f = new File("Nicknames.txt");
							FileWriter fw = new FileWriter(f);
							List<Entry<String, String>> list = nicks.entrySet().stream().sorted((o1, o2) -> {
								int res = o1.getValue().compareTo(o2.getValue());
								return res == 0 ? o1.getKey().compareTo(o2.getKey()) : res;
							}).collect(Collectors.toList());
							for(Entry<String, String> e : list)
								fw.write(e.getValue() + " - " + e.getKey() + System.lineSeparator());
							fw.close();
							ret = instance.getMessages().sendFile(event.getHook(), "Nicknames.txt", FileUtils.readFileToByteArray(f));
						} catch(IOException e) {
							throw new BotException(instance, e);
						}
						break;
				}
				break;
			}
			case "acquisition": {
				List<SyncPair> lsp = _Library.get(event.getOption("name").getAsString());
				Acquisition acq = Acquisition.valueOf(event.getOption("type").getAsString());
				if(lsp.size() != 1)
					message = "Please write a specific name";
				else
					switch(event.getSubcommandName()) {
						case "add": {
							if(lsp.get(0).getAcquisition().contains(acq))
								message = "Pair already has this acquisition";
							else {
								instance.getSQL().executeInsert("INSERT INTO Acquisition VALUES (?, ?)", lsp.get(0).getTrainer().getCharacterId(), acq.getEmoteKey());
								message = "Acquisition added";
								lsp.get(0).getAcquisition().add(acq);
							}
						}
							break;
						case "remove": {
							if(!lsp.get(0).getAcquisition().contains(acq))
								message = "Pair doesn't have this acquisition";
							else {
								instance.getSQL().executeInsert("DELETE FROM Acquisition WHERE characterId = ? AND emoteKey = ?",
										lsp.get(0).getTrainer().getCharacterId(), acq.getEmoteKey());
								message = "Acquisition removed";
								lsp.get(0).getAcquisition().remove(acq);
							}
						}
					}
				break;
			}
			case "misc": {
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
							mi.data + (mi.owner != null ? System.lineSeparator() + "Credits to " + MiscCommand.getOwner(null, mi.owner) : ""));
				Embed eb = MiscCommand.createEmbed(mi);
				if(info.equalsIgnoreCase("Candy Suggestions"))
					eb.setDescription("Order inside the tiers  take in account how good and how much units gain from grid." + System.lineSeparator()
							+ "Never candy a non-limited 5* pair.");
				ret = BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), eb);
				break;
			}
			case "lodge": {
				String trainer = event.getOption("name").getAsString().toLowerCase();
				String newData = Methods.getOptionValue(event.getOption("url"), null);
				if(newData != null)
					try {
						BotType.getPokemonInstance().getSQL().executeInsert("REPLACE INTO Lodge VALUES(?, ?)", trainer, newData);
						return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "Updated");
					} catch(BotException e) {
						return BotType.getPokemonInstance().getMessages().sendStatusMessageError(event.getHook(), "Error Updating");
					}
				String image = LodgeCommand.get(trainer);
				if(image == null)
					return BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), "Trainer with that name isn't registered.");
				ret = BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), LodgeCommand.createEmbed(trainer, image));
				break;
			}
		}
		if(ret != null)
			return ret;
		else
			return instance.getMessages().sendMessage(event.getHook(), message);
	}
	@Override
	public SlashCommandData getCommandData() { return null; }
	@Override
	public SlashCommandData getAdminCommandData() {
		SlashCommandData cd = super.getCommandData();
		SubcommandGroupData scgd1 = new SubcommandGroupData("styles", "Manage Styles");
		SubcommandData scd11 = new SubcommandData("add", "Add Style");
		scd11.addOptions(new OptionData(OptionType.STRING, "name", "Sync Pair name (Must be exact)", true));
		scd11.addOptions(new OptionData(OptionType.STRING, "main", "Main Style", true));
		scd11.addOptions(new OptionData(OptionType.STRING, "ex", "EX Style", true));
		scgd1.addSubcommands(scd11);
		SubcommandGroupData scgd2 = new SubcommandGroupData("nicknames", "Manage Nicknames");
		SubcommandData scd21 = new SubcommandData("add", "Add Nickname");
		scd21.addOptions(new OptionData(OptionType.STRING, "name", "Sync Pair name (Must be exact)", true));
		scd21.addOptions(new OptionData(OptionType.STRING, "nickname", "Nickname to add", true));
		scgd2.addSubcommands(scd21);
		SubcommandData scd22 = new SubcommandData("remove", "Remove Nickname");
		scd22.addOptions(new OptionData(OptionType.STRING, "nickname", "Nickname to remove", true));
		scgd2.addSubcommands(scd22);
		SubcommandData scd23 = new SubcommandData("list", "List Nicknames");
		scgd2.addSubcommands(scd23);
		SubcommandGroupData scgd4 = new SubcommandGroupData("acquisition", "Manage Acquisitions");
		SubcommandData scd41 = new SubcommandData("add", "Add Acquisition");
		scd41.addOptions(new OptionData(OptionType.STRING, "name", "Sync Pair name (Must be exact)", true));
		scd41.addOptions(new OptionData(OptionType.STRING, "type", "Acquisition Type", true)
				.addChoices(Arrays.asList(Acquisition.values()).stream()
						.map(a -> new Command.Choice(a.getDescription(), a.getEmoteKey())).collect(Collectors.toList())));
		scgd4.addSubcommands(scd41);
		SubcommandData scd42 = new SubcommandData("remove", "Remove Acquisition");
		scd42.addOptions(new OptionData(OptionType.STRING, "name", "Sync Pair name (Must be exact)", true));
		scd42.addOptions(new OptionData(OptionType.STRING, "type", "Acquisition Type", true)
				.addChoices(Arrays.asList(Acquisition.values()).stream()
						.map(a -> new Command.Choice(a.getDescription(), a.getEmoteKey())).collect(Collectors.toList())));
		scgd4.addSubcommands(scd42);
		SubcommandGroupData scgd3 = new SubcommandGroupData("patreon", "Manage Patreon");
		SubcommandData scd31 = new SubcommandData("updatekey", "Update Patreon Key");
		scd31.addOptions(new OptionData(OptionType.STRING, "key", "Key", true));
		scgd3.addSubcommands(scd31);
		SubcommandGroupData scgd5 = new SubcommandGroupData("operation", "Perform operation");
		SubcommandData scd51 = new SubcommandData("update", "Update data");
		scgd5.addSubcommands(scd51);
		SubcommandGroupData scgd6 = new SubcommandGroupData("emotes", "Manage Emotes");
		SubcommandData scd61 = new SubcommandData("add", "Add emote");
		scd61.addOptions(new OptionData(OptionType.STRING, "name", "Sync Pair name (Must be exact)", true));
		scd61.addOptions(new OptionData(OptionType.ATTACHMENT, "emote", "Emote to replace", true));
		SubcommandData scd62 = new SubcommandData("update", "Update emote");
		scd62.addOptions(new OptionData(OptionType.STRING, "name", "Sync Pair name (Must be exact)", true));
		scd62.addOptions(new OptionData(OptionType.ATTACHMENT, "emote", "Emote to replace", true));
		scgd6.addSubcommands(scd61, scd62);
		SubcommandGroupData scgd7 = new SubcommandGroupData("misc", "Lots of easily accessible information");
		SubcommandData scd71 = new SubcommandData("update", "Update Misc Data");
		OptionData od = new OptionData(OptionType.STRING, "info", "What info to update", true);
		try(ResultSet rs = BotType.getPokemonInstance().getSQL().executeSelect("SELECT * FROM Commands")) {
			while(rs.next())
				if(rs.getBoolean("updateable"))
					od.addChoice(rs.getString("name").toLowerCase(), rs.getString("name"));
		} catch(BotException | SQLException e) {
			BotType.getPokemonInstance().getMessages().sendReport("Unable to load simple commands");
		}
		scd71.addOptions(od);
		scd71.addOptions(new OptionData(OptionType.STRING, "data", "New data", true));
		scgd7.addSubcommands(scd71);
		SubcommandGroupData scgd8 = new SubcommandGroupData("lodge", "Shows infographic for a pair's topics");
		SubcommandData scd81 = new SubcommandData("add", "Add");
		scd81.addOptions(new OptionData(OptionType.STRING, "name", "Trainer's Name", true));
		scd81.addOptions(new OptionData(OptionType.STRING, "url", "Image URL", true));
		scgd8.addSubcommands(scd81);
		return cd.addSubcommandGroups(scgd1, scgd2, scgd3, scgd4, scgd5, scgd6, scgd7, scgd8);
	}
}
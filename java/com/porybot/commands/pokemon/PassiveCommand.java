package com.porybot.commands.pokemon;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.common.collect.Streams;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.BoardPassive;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.Type;
import Utils.MessageUtils;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class PassiveCommand extends _PokemonCommand {
	public PassiveCommand() { super("passive", "Shows specified passive information and who has it"); }
	private Embed parse(GenericInteractionCreateEvent event, Long id, String name, String desc, boolean eggmons) {
		List<Passive> passivesThatMatch = new LinkedList<>();
		List<SyncPair> res = _Library.get(eggmons).stream()
				.filter(sp -> {
					if(sp.getName().contains("Rachel"))
						System.out.println();
					return Streams.concat(
							sp.getPassives().stream(),
							sp.getVariationPassives(true, -1).stream(),
							sp.getSyncBoards().stream().map(BoardPassive::getPassive))// .collect(Collectors.toList()).get(36))
							.filter(p -> p != null)
							.filter(p -> id == null || p.getId().equals(id))
							.filter(p -> name == null || Methods.containIgnoreSymbols(p.getName(), name))
							.filter(p -> desc == null || Methods.containIgnoreSymbols(p.getDescription(), desc))
							.peek(p -> passivesThatMatch.add(p))
							.count() > 0;
				}).collect(Collectors.toList());
		List<Passive> passivesThatMatch2 = passivesThatMatch.stream().distinct().sorted((p1, p2) -> p1.getName().compareTo(p2.getName())).collect(Collectors.toList());
		if(passivesThatMatch2.isEmpty())
			return null;
		if(passivesThatMatch2.size() == 1)
			return createEmbed(passivesThatMatch2.get(0), res);
		return createEmbed(passivesThatMatch2, eggmons);
	}
	private static Embed createEmbed(Passive passive, List<SyncPair> l) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(passive.getName());
		embed.setDescription(passive.getDescription());
		Map<Type, List<SyncPair>> tl = l.stream().collect(Collectors.groupingBy(SyncPair::getType));
		List<String> ret = tl.keySet().stream().sorted().flatMap(t -> tl.get(t).stream()
				.map(sp -> BotType.getPokemonInstance().getImages().getEmoteText(sp.getTrainer().getCharacterId()) + " " + sp.getName() +
						(sp.getTrainer().getRarity() == 1 ? " (" + sp.getTrainer().getRole().getEmoteText() + " " + sp.getTrainer().getRole().getName() + ")" : "") +
						sp.getSyncBoards().stream().filter(sbp -> sbp.getPassive() != null && sbp.getPassive().getName().equals(passive.getName()))
								.map(sbp -> sbp.getType().getEmoteText()).distinct().sorted().reduce((s1, s2) -> s1 + s2).orElse("")))
				.collect(Collectors.toList());
		String desc = "";
		boolean first = true;
		for(String sp : ret) {
			if((desc + System.lineSeparator() + sp).trim().length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
				embed.addField(first ? "Sync Pairs with Passive: " : MessageUtils.S, desc.trim(), true);
				first = false;
				desc = "";
			}
			desc += System.lineSeparator() + sp;
		}
		if(!desc.trim().isBlank())
			embed.addField(first ? "Sync Pairs with Passive: " : MessageUtils.S, desc.trim(), true);
		return embed;
	}
	private static Embed createEmbed(List<Passive> passivesThatMatch, boolean eggmons) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor("Multiple results found");
		String desc = "";
		int count = 0;
		for(Passive p : passivesThatMatch) {
			if((desc + System.lineSeparator() + p.getName()).length() > MessageUtils.FIELD_MESSAGE_LIMIT || count == 25) {
				embed.addField(MessageUtils.S, desc.trim(), true);
				desc = "";
				count = 0;
			}
			desc += System.lineSeparator() + p.getName();
			count++;
		}
		if(!desc.isBlank())
			embed.addField(MessageUtils.S, desc.trim(), true);
		embed.createMenu(passivesThatMatch.stream()
				.map(p -> embed.createMenuOption(p.getName(),
						"passive" + MessageUtils.SEPARATOR + eggmons + MessageUtils.SEPARATOR + p.getId().toString()))
				.collect(Collectors.toList()).toArray(new SelectOption[0]));
		return embed;
	}
	private String acronyms(String val) {
		if(val == null)
			return null;
		if(val.equalsIgnoreCase("mgr"))
			return "MP Refresh";
		if(val.equalsIgnoreCase("oar"))
			return "On a Roll";
		if(val.equalsIgnoreCase("mga"))
			return "Move Gauge Acceleration";
		return val;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) {
		String name = acronyms(Methods.getOptionValue(event.getOption("name")));
		String description = acronyms(Methods.getOptionValue(event.getOption("description")));
		if(name == null && description == null || name != null && description != null)
			return BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), "Please insert either 'name' or 'description' (and not both)");
		boolean eggMon = Methods.getOptionValue(event.getOption("eggmons"), false);
		Embed embed = parse(event, null, name, description, eggMon);
		if(embed == null)
			return BotType.getPokemonInstance().getMessages().sendStatusMessageInfo(event.getHook(), "No passive found with that name");
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), embed);
	}
	@Override
	public void doStuff(StringSelectInteractionEvent event) {
		String[] vals = event.getSelectedOptions().get(0).getValue().split(MessageUtils.SEPARATOR);
		Embed embed = parse(event, Long.parseLong(vals[2]), null, null, Boolean.parseBoolean(vals[1]));
		if(embed != null)
			BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), embed);
		else
			BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), "Error loading passive data");
	}
	@Override
	public boolean isEtherealReply(SlashCommandInteractionEvent event) {
		return event.getOption("name") == event.getOption("description") || (event.getOption("name") != null && event.getOption("description") != null);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od3 = new OptionData(OptionType.STRING, "name", "Passive Name", false);
		OptionData od5 = new OptionData(OptionType.STRING, "description", "Passive Description", false);
		OptionData od4 = new OptionData(OptionType.BOOLEAN, "eggmons", "Show Eggmons?", false);
		cd.addOptions(od3, od5, od4);
		return cd;
	}
}
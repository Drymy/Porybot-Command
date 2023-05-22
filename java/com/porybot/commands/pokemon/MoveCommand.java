package com.porybot.commands.pokemon;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.common.collect.Streams;
import com.manager.BotType;
import com.manager.Main;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.Move;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.Group;
import com.porybot.GameElements.Enums.MoveTag;
import com.porybot.GameElements.Enums.Type;
import Utils.BotException;
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

public class MoveCommand extends _PokemonCommand {
	public MoveCommand() { super("move", "Shows specified move information and who has it"); }
	private Embed parse(GenericInteractionCreateEvent event, Long id, String name, String desc, boolean eggmons) {
		List<Move> movesThatMatch = new LinkedList<>();
		List<SyncPair> res = _Library.get(eggmons).stream()
				.filter(sp -> Streams.concat(
						sp.getMoves().stream(),
						sp.getVariationMoves(true, -1).stream(),
						Arrays.asList(sp.getSyncMove(), sp.getVariationSyncMove(true, -1)).stream())
						.filter(mo -> mo != null)
						.filter(mo -> id == null || mo.getId().equals(id))
						.filter(mo -> name == null || Methods.containIgnoreSymbols(mo.getName(), name))
						.filter(mo -> desc == null || Methods.containIgnoreSymbols(mo.getDescription(), desc))
						.peek(mo -> movesThatMatch.add(mo))
						.count() > 0)
				.collect(Collectors.toList());
		final Comparator<Move> comp = ((Comparator<Move>)(m1, m2) -> Integer.compare(m1.getGroup().getOrder(), m2.getGroup().getOrder()))
				.thenComparing((m1, m2) -> m1.getCategory().compareTo(m2.getCategory()))
				.thenComparing((m1, m2) -> m1.getType().compareTo(m2.getType()))
				.thenComparing((m1, m2) -> m1.getName().compareTo(m2.getName()));
		List<Move> movesThatMatch2 = movesThatMatch.stream().distinct().sorted(comp).collect(Collectors.toList());
		if(movesThatMatch2.isEmpty())
			return null;
		if(Main.isDebug())
			System.out.println(movesThatMatch2.stream().map(m -> m.getId() + " - " + m.getName()).reduce((m1, m2) -> m1 + System.lineSeparator() + m2).get());
		if(movesThatMatch2.size() == 1)
			return createEmbed(movesThatMatch2.get(0), res);
		return createEmbed(movesThatMatch2, eggmons);
	}
	private static Embed createEmbed(Move m, List<SyncPair> l) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setColor(m.getType().getColor());
		Map<Type, List<SyncPair>> tl = l.stream().collect(Collectors.groupingBy(SyncPair::getType));
		List<String> ret = tl.keySet().stream().sorted().flatMap(t -> tl.get(t).stream()
				.map(sp -> BotType.getPokemonInstance().getImages().getEmoteText(sp.getTrainer().getCharacterId()) + " " + sp.getName() +
						(sp.getTrainer().getRarity() == 1 ? " (" + sp.getTrainer().getRole().getEmoteText() + " " + sp.getTrainer().getRole().getName() + ")" : "")))
				.collect(Collectors.toList());
		if(m.getGroup().equals(Group.Regular))
			embed.addField((m.getCategory().getEmoteText() + " " + m.getType().getEmoteText()).trim() + " " + m.getName(),
					"Gauge: " + MessageUtils.zero(m.getCost()) + " | Power: " + MessageUtils.zero(m.getPower()) +
							" | Accuracy: " + MessageUtils.zero(m.getAccuracy()) + " | Uses: " + MessageUtils.zero(m.getUses()) + System.lineSeparator() +
							"Target: " + m.getTarget().getName() + System.lineSeparator() +
							"Effect Tag: " + m.getMoveTags().stream().map(MoveTag::getName).reduce((m1, m2) -> m1 + ", " + m2).orElse("-") + System.lineSeparator() +
							m.getDescription(),
					false);
		else
			embed.addField((m.getCategory().getEmoteText() + " " + m.getType().getEmoteText() + " " + m.getGroup().getEmote()).trim() + " " + m.getName(),
					"Power: " + MessageUtils.zero(m.getPower()) + System.lineSeparator() +
							"Target: " + m.getTarget().getName() + System.lineSeparator() +
							"Effect Tag: " + m.getMoveTags().stream().map(MoveTag::getName).reduce((m1, m2) -> m1 + ", " + m2).orElse("-") + System.lineSeparator() +
							m.getDescription(),
					false);
		String desc = "";
		boolean first = true;
		for(String sp : ret) {
			if((desc + System.lineSeparator() + sp).trim().length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
				embed.addField(first ? "Sync Pairs with Move: " : MessageUtils.S, desc.trim(), true);
				first = false;
				desc = "";
			}
			desc += System.lineSeparator() + sp;
		}
		if(!desc.trim().isBlank())
			embed.addField(first ? "Sync Pairs with Move: " : MessageUtils.S, desc.trim(), true);
		return embed;
	}
	private static Embed createEmbed(List<Move> movesThatMatch, boolean eggmons) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor("Multiple results found");
		String desc = "";
		int count = 0;
		for(Move m : movesThatMatch) {
			String newLine = m.getCategory().getEmoteText() + " " + m.getType().getEmoteText() + m.getName();
			if((desc + System.lineSeparator() + newLine).length() > MessageUtils.FIELD_MESSAGE_LIMIT || count == 10) {
				embed.addField(MessageUtils.S, desc.trim(), true);
				desc = "";
				count = 0;
			}
			desc += System.lineSeparator() + newLine;
			count++;
		}
		if(!desc.isBlank())
			embed.addField(MessageUtils.S, desc.trim(), true);
		embed.createMenu(movesThatMatch.stream()
				.map(m -> embed.createMenuOption(m.getName(),
						"move" + MessageUtils.SEPARATOR + eggmons + MessageUtils.SEPARATOR + m.getId().toString()))
				.collect(Collectors.toList()).toArray(new SelectOption[0]));
		return embed;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String name = Methods.getOptionValue(event.getOption("name"));
		String description = Methods.getOptionValue(event.getOption("description"));
		if(name == null && description == null || name != null && description != null)
			return BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), "Please insert either 'name' or 'description' (and not both)");
		boolean eggMon = Methods.getOptionValue(event.getOption("eggmons"), false);
		Embed embed = parse(event, null, name, description, eggMon);
		if(embed == null)
			return BotType.getPokemonInstance().getMessages().sendStatusMessageInfo(event.getHook(), "No move found with that name");
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), embed);
	}
	@Override
	public void doStuff(StringSelectInteractionEvent event) throws BotException {
		String[] vals = event.getSelectedOptions().get(0).getValue().split(MessageUtils.SEPARATOR);
		Embed embed = parse(event, Long.parseLong(vals[2]), null, null, Boolean.parseBoolean(vals[1]));
		if(embed != null)
			BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), embed);
		else
			BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), "Error loading move data");
	}
	@Override
	public boolean isEtherealReply(SlashCommandInteractionEvent event) {
		return event.getOption("name") == event.getOption("description") || (event.getOption("name") != null && event.getOption("description") != null);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od3 = new OptionData(OptionType.STRING, "name", "Move Name", false);
		OptionData od5 = new OptionData(OptionType.STRING, "description", "Move Description", false);
		OptionData od4 = new OptionData(OptionType.BOOLEAN, "eggmons", "Show Eggmons?", false);
		cd.addOptions(od3, od5, od4);
		return cd;
	}
}
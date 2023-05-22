package com.porybot.commands.pokemon;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.Region;
import com.porybot.GameElements.Enums.Type;
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

public class TicketCommand extends _PokemonCommand {
	public TicketCommand() { super("ticket", "Show Sync Pairs that each Ticket can drop"); }
	private static Embed createEmbed(List<SyncPair> l, Region r) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(r.getText() + " Tickets", null, r.getTicketEmote().getImageUrl());
		Map<Type, List<SyncPair>> tl = l.stream().collect(Collectors.groupingBy(SyncPair::getType));
		tl.keySet().stream().sorted().forEach(t -> embed.addField(t.getEmoteText() + " " + t.getName(), tl.get(t).stream()
				.map(sp -> BotType.getPokemonInstance().getImages().getEmoteText(sp.getTrainer().getCharacterId()) + " " + sp.getName())
				.reduce(Methods::reduceToList).get(), tl.keySet().size() > 3));
		if(tl.keySet().size() > 3 && tl.keySet().size() % 3 == 2)
			embed.addField(MessageUtils.S, MessageUtils.S, true);
		return embed;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String type = event.getOption("type").getAsString();
		Region r = Region.get(type);
		List<SyncPair> res = _Library.get(false).stream()
				.filter(sp -> sp.getRegion() == r && sp.getAcquisition().stream().anyMatch(a -> a.getEmoteKey().equals(r.name() + "Ticket")))
				.collect(Collectors.toList());
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), createEmbed(res, r));
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od3 = new OptionData(OptionType.STRING, "type", "Type of ticket", true);
		Arrays.asList(Region.values()).stream().filter(r -> r.getTicketEmoteText() != null).forEach(r -> od3.addChoices(new Command.Choice(r.getText(), r.getText())));
		cd.addOptions(od3);
		return cd;
	}
}
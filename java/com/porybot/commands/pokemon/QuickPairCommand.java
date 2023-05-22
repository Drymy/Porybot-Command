package com.porybot.commands.pokemon;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.manager.Main;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.SyncPair;
import Utils.BotException;
import Utils.MessageUtils;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class QuickPairCommand extends _PokemonCommand {
	public QuickPairCommand() { super("qp", "(QuickPair) Show minimalist information on a specific Sync Pair data"); }
	private static Embed createShortEmbed(SyncPair sp, Boolean isVar, int formId) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getName(),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setThumbnail(BotType.getPokemonInstance().getImages().getEmoteClassByName(sp.getTrainer().getCharacterId()).getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		embed.addField("Acquisition:", sp.getAcquisition().stream()
				.map(t -> BotType.getPokemonInstance().getImages().getEmoteText(t.getEmote()) + " " + t.getDescription())
				.reduce(Methods::reduceToList).orElse("None"), true);
		embed.addField("Alternates:", _Library.getByBaseTrainer(sp.getTrainer().getBaseName()).stream()
				.filter(t -> !t.equals(sp))
				.map(t -> BotType.getPokemonInstance().getImages().getEmoteText(t.getTrainer().getCharacterId()) + " " + t.getName())
				.reduce((s1, s2) -> s1 + System.lineSeparator() + s2).orElse("None"), false);
		embed.setFooter("");
		return embed;
	}
	private static Embed createListChoose(List<SyncPair> sp) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor("Multiple results found");
		embed.setDescription(sp.stream().distinct()
				.map(t -> BotType.getPokemonInstance().getImages().getEmoteText(t.getTrainer().getCharacterId()) + " " + t.getName() +
						(t.getTrainer().getBaseName().equals("Player") ? " (" + t.getTrainer().getRole().getEmoteText() + " " + t.getTrainer().getRole().getName() + ")" : ""))
				.reduce(Methods::reduceToList).orElse("None"));
		embed.createMenu(sp.stream().distinct()
				.map(t -> embed.createMenuOption(t.getName() + (t.getTrainer().getRarity() == 1 ? " (" + t.getTrainer().getRole().getName() + ")" : ""),
						"qp" + MessageUtils.SEPARATOR + "TRAINERSWITCH" + MessageUtils.SEPARATOR + t.getTrainer().getCharacterId(),
						BotType.getPokemonInstance().getImages().getEmoteClassByName(t.getTrainer().getCharacterId())))
				.collect(Collectors.toList()).toArray(new SelectOption[0]));
		return embed;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String name = event.getOption("name").getAsString().trim().toLowerCase();
		List<SyncPair> res = _Library.get(name);
		if(name.contains("player"))
			res = _Library.getByBaseTrainer("Player");
		if(res.isEmpty())
			return BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), "No Sync Pairs found with specified name");
		if(Main.isDebug())
			System.out.println(res.stream().map(sp -> sp.getTrainer().getCharacterId() + " - " + sp.getName()).reduce(Methods::reduceToList).get());
		if(res.size() != 1)
			return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), createListChoose(res));
		SyncPair sp = res.get(0);
		Embed embed = createShortEmbed(sp, false, -1);
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), embed);
	}
	@Override
	public void doStuff(StringSelectInteractionEvent event) throws BotException {
		String[] vals = event.getSelectedOptions().get(0).getValue().split(MessageUtils.SEPARATOR);
		SyncPair sp = _Library.getById(vals[2]);
		Embed embed = createShortEmbed(sp, false, -1);
		embed.setDeleteActions(true);
		BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), embed);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od2 = new OptionData(OptionType.STRING, "name", "Sync Pair name", true);
		cd.addOptions(od2);
		return cd;
	}
}
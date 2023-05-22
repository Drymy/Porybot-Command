package com.porybot.commands.pokemon;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.common.collect.Streams;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.Enums.Stat;
import Utils.BotException;
import Utils.Dual;
import Utils.MessageUtils;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class TopStatCommand extends _PokemonCommand {
	public TopStatCommand() { super("topstat", "Show the top Sync Pairs for a specified stat"); }
	
	
	
	private static Embed createEmbed(List<Dual<String, Long>> l, Stat stat, String role) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setDeleteActions(true);
		embed.setAuthor("Top Sync Pairs for " + stat.getName() + " stat" + (role != null ? " (" + role + ")" : ""));
		embed.setDescription("Stats at L140, 5* with 0/20 powerups");
		String desc = "";
		int count = -1;
		for(String sp : l.stream().map(d -> d.getValue1() + " (" + d.getValue2() + ")").collect(Collectors.toList()))
			if((++count == 8) || (desc + System.lineSeparator() + sp).trim().length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
				embed.addField(MessageUtils.S, desc.trim(), false);
				desc = sp;
				count = 0;
			} else
				desc += System.lineSeparator() + sp;
		if(!desc.trim().isBlank())
			embed.addField(MessageUtils.S, desc.trim(), true);
		return embed;
	}
	
	
	private static Embed doStuff(Stat stat, boolean eggmons, boolean megas, int count, String role) {
		int roleId = role != null ? role.equals("Striker") ? 1 : role.equals("Tech") ? 2 : 3 : 0;
		// SyncPair sp = null;
		// Streams.concat(
		// (megas && sp.getVariation() != null) ? sp.getVariations().stream().map(v -> new
		// Dual<>(ImageUtils.getEmoteText(sp.getTrainer().getCharacterId()) + "" +
		// ImageUtils.getEmoteText(v.getFormType().getEmote()), 0L)) : Arrays.asList((Dual<String,
		// Long>)null).stream(),
		// Arrays.asList(new Dual<>(ImageUtils.getEmoteText(sp.getTrainer().getCharacterId()) + " "
		// +
		// (megas ? sp.getVariationName(false, -1) : sp.getName()), sp.getVariationStat(stat, false,
		// -1))).stream());

		List<Dual<String, Long>> res = _Library.get(eggmons).stream()
				.filter(sp -> roleId > 0 ? sp.getTrainer().getRole().isRoleById(roleId) : true)
				.flatMap(sp -> Streams.concat(
						// (megas && sp.getVariation() != null && sp.getVariations().size() == 1) ?
						// null : null,
						// (megas && sp.getVariation() != null && sp.getVariations().size() > 1)
						(megas && sp.getVariation() != null)
								? sp.getVariations().stream().map(v -> new Dual<>(BotType.getPokemonInstance().getImages().getEmoteText(sp.getTrainer().getCharacterId()) + "" +
										BotType.getPokemonInstance().getImages().getEmoteText(v.getFormType().getEmote()) + " "
										+ sp.getVariationName(true, v.getFormType().ordinal() - sp.getVariation().getFormType().ordinal()),
										sp.getVariationStat(stat, true, v.getFormType().ordinal() - sp.getVariation().getFormType().ordinal())))
								: Arrays.asList((Dual<String, Long>)null).stream(),
						Arrays.asList(new Dual<>(BotType.getPokemonInstance().getImages().getEmoteText(sp.getTrainer().getCharacterId()) + " " +
								(megas ? sp.getVariationName(false, -1) : sp.getName()), sp.getVariationStat(stat, false, -1))).stream()))
				.filter(o -> o != null)
				.sorted((d1, d2) -> Long.compare(d2.getValue2(), d1.getValue2()))
				.limit(count)
				.collect(Collectors.toList());
		Embed embed = createEmbed(res, stat, role);
		if(!stat.equals(Stat.HP))
			embed.createRow(embed.createButton(megas ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, "topstat", stat.name() + MessageUtils.SEPARATOR + eggmons +
					MessageUtils.SEPARATOR + megas + MessageUtils.SEPARATOR + count + MessageUtils.SEPARATOR + role, "Forme Change", "REACTION_FORME_CHANGE"));
		return embed;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		Stat stat = Stat.valueOf(event.getOption("stat").getAsString());
		boolean eggmons = Methods.getOptionValue(event.getOption("eggmons"), false);
		boolean megas = Methods.getOptionValue(event.getOption("alternateforms"), false);
		int count = Methods.getOptionValue(event.getOption("paircount"), 15L, 0L, 30L).intValue();
		String role = Methods.getOptionValue(event.getOption("role"));
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), doStuff(stat, eggmons, megas, count, role));
	}
	@Override
	public void doStuff(ButtonInteractionEvent event) throws BotException {
		String[] vals = event.getButton().getId().split(MessageUtils.SEPARATOR);
		Stat stat = Stat.valueOf(vals[1]);
		boolean eggmons = Boolean.parseBoolean(vals[2]);
		boolean megas = Boolean.parseBoolean(vals[3]);
		int count = Integer.parseInt(vals[4]);
		String role = vals[5].equals("null") ? null : vals[5];
		BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), doStuff(stat, eggmons, !megas, count, role));
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od3 = new OptionData(OptionType.STRING, "stat", "What stat to search", true);
		Arrays.asList(Stat.values()).stream().limit(6).forEach(r -> od3.addChoices(new Command.Choice(r.getName(), r.name())));
		OptionData od4 = new OptionData(OptionType.STRING, "role", "What role to limit search to", false);
		od4.addChoices(new Command.Choice("Striker", "Striker"));
		od4.addChoices(new Command.Choice("Tech", "Tech"));
		od4.addChoices(new Command.Choice("Support", "Support"));
		OptionData od6 = new OptionData(OptionType.INTEGER, "paircount", "How many pairs to show (Default: 15, Max: 30)", false);
		OptionData od5 = new OptionData(OptionType.BOOLEAN, "alternateforms", "Show Alternate Formes? (Defaults to false)", false);
		OptionData od2 = new OptionData(OptionType.BOOLEAN, "eggmons", "Show Eggmons? (Defaults to false)", false);
		cd.addOptions(od3, od4, od6, od5, od2);
		return cd;
	}
}
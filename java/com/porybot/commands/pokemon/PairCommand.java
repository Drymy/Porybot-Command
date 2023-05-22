package com.porybot.commands.pokemon;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import com.manager.BotType;
import com.manager.Main;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.Move;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.FormType;
import com.porybot.GameElements.Enums.MoveTag;
import com.porybot.GameElements.Enums.Stat;
import com.porybot.GameElements.Enums.SyncBoardCondition;
import com.porybot.GameElements.Enums.SyncBoardType;
import Utils.BotException;
import Utils.Dual;
import Utils.MessageUtils;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

public class PairCommand extends _PokemonCommand {
	public PairCommand() { super("pair", "Search for specific Sync Pair data"); }
	private static Embed createProfileEmbed(SyncPair sp, Boolean isVar, int formId) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getVariationName(isVar, formId),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setThumbnail(BotType.getPokemonInstance().getImages().getEmoteClassByName(sp.getTrainer().getCharacterId()).getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		String stars = StringUtils.repeat(BotType.getPokemonInstance().getImages().getEmoteText("STAR_" + sp.getTrainer().getRarity().intValue()),
				sp.getTrainer().getRarity().intValue())
				+ StringUtils.repeat(BotType.getPokemonInstance().getImages().getEmoteText("STAR_0"), 5 - sp.getTrainer().getRarity().intValue())
				+ (sp.getTrainer().isEX() ? BotType.getPokemonInstance().getImages().getEmoteText("STAREX_0") : "");
		embed.setDescription(stars + System.lineSeparator() + MessageUtils.S2 + "Type: " + sp.getTrainer().getType().getEmoteText() + MessageUtils.empty(2) +
				"Weak: " + sp.getTrainer().getWeakType().getEmoteText());
		embed.addField("Level 140 Stats:",
				"HP: " + sp.getVariationStat(Stat.HP, isVar, formId) + System.lineSeparator() +
						"ATK: " + sp.getVariationStat(Stat.ATK, isVar, formId) + " | DEF: " + sp.getVariationStat(Stat.DEF, isVar, formId) + System.lineSeparator() +
						"SPA: " + sp.getVariationStat(Stat.SPA, isVar, formId) + " | SPD: " + sp.getVariationStat(Stat.SPD, isVar, formId) + System.lineSeparator() +
						"SPE: " + sp.getVariationStat(Stat.SPE, isVar, formId),
				true);
		embed.addField("Acquisition:", sp.getAcquisition().stream()
				.map(t -> BotType.getPokemonInstance().getImages().getEmoteText(t.getEmote()) + " " + t.getDescription())
				.reduce(Methods::reduceToList).orElse("None"), true);
		embed.addField("Alternates:", _Library.getByBaseTrainer(sp.getTrainer().getBaseName()).stream()
				.filter(t -> !t.equals(sp))
				.map(t -> BotType.getPokemonInstance().getImages().getEmoteText(t.getTrainer().getCharacterId()) + " " + t.getName())
				.reduce((s1, s2) -> s1 + System.lineSeparator() + s2).orElse("None"), false);
		String recommends = "";
		String lucky = "";
		if(sp.getSyncBoards().size() > 6) {
			List<Dual<String, String>> list = BotType.getPokemonInstance().getDocs().getSyncGridBuilds(sp.getName());
			if(list != null && !list.isEmpty())
				for(Dual<String, String> dual : list)
					if(dual.getValue2() == null)
						lucky += System.lineSeparator() + dual.getValue1();
					else {
						recommends += System.lineSeparator() + (dual.getValue1().startsWith("+") ? MessageUtils.tab() : "")
								+ ("[" + dual.getValue1() + "](" + dual.getValue2() + ")").trim();
						lucky = "";
					}
			if(recommends.isBlank())
				recommends = "Error loading recommendations" + System.lineSeparator() +
						"[Default Link here](https://docs.google.com/document/d/1vF42uzF-xpkcfIU2gVEY4Dl7sS_I3ITj8g5X2lo1usA)";
			recommends = recommends + System.lineSeparator() + lucky;
			String rec = "";
			boolean first = true;
			for(String s : recommends.split(System.lineSeparator())) {
				if((rec + System.lineSeparator() + s).length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
					embed.addField(first ? "Sync Grid Recommendations:" : MessageUtils.S, rec, false);
					rec = s;
					first = false;
				}
				rec += System.lineSeparator() + s;
			}
			if(!rec.isBlank())
				embed.addField(first ? "Sync Grid Recommendations:" : MessageUtils.S, rec.trim(), false);
		}
		return embed;
	}
	private static Embed createMoveEmbed(SyncPair sp, Boolean isVar, int formId) {
		// if(Constants.DEBUG)
		// System.out.println(sp.getMoves().stream().map(m -> m.getId() + " - " + m.getName() + " -
		// " + m.getDescription()).reduce(Methods::reduceToList).get());
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getVariationName(isVar, formId),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		for(Move m : sp.getVariationMoves(isVar, formId))
			embed.addField((m.getCategory().getEmoteText() + " " + sp.getMoveTypeWithPassives(m, isVar).getEmoteText()).trim() + " " + m.getName(),
					"Gauge: " + MessageUtils.zero(sp.getMoveCostWithPassives(m, isVar)) + " | Power: " + MessageUtils.zero(sp.getMovePowerWithPassives(m, isVar)) +
							" | Accuracy: " + MessageUtils.zero(sp.getMoveAccuracyWithPassives(m, isVar)) + " | Uses: " + MessageUtils.zero(m.getUses()) + System.lineSeparator() +
							"Target: " + sp.getMoveTargetWithPassives(m, isVar).getName() + System.lineSeparator() +
							"Effect Tag: " + m.getMoveTags().stream().map(MoveTag::getName).reduce((m1, m2) -> m1 + ", " + m2).orElse("-") + System.lineSeparator() +
							m.getDescription(),
					false);
		Move m = sp.getVariationSyncMove(isVar, formId);
		embed.addField(
				(m.getCategory().getEmoteText() + " " + m.getType().getEmoteText() + " " + BotType.getPokemonInstance().getImages().getEmoteText("CONDITION_SYNC")).trim() + " "
						+ m.getName(),
				"Power: " + MessageUtils.zero(m.getPower()) + System.lineSeparator() +
						"Target: " + m.getTarget().getName() + System.lineSeparator() +
						"Effect Tag: " + m.getMoveTags().stream().map(MoveTag::getName).reduce((m1, m2) -> m1 + ", " + m2).orElse("-") + System.lineSeparator() +
						m.getDescription(),
				false);
		return embed;
	}
	private static Embed createPassiveEmbed(SyncPair sp, Boolean isVar, int formId) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getVariationName(isVar, formId),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		for(Passive p : sp.getVariationPassives(isVar, formId).stream().filter(pp -> !pp.isTheme()).collect(Collectors.toList()))
			embed.addField((p.isMaster() ? BotType.getPokemonInstance().getImages().getEmoteText("MASTER_PAIR") + " " : "") + p.getName() + ":", p.getDescription(), false);
		String themes = sp.getTrainer().getPassives().stream()
				.filter(Passive::isTheme)
				.map(Passive::getTheme).filter(t -> t != null)
				.map(t -> t.getThemeEmote() + " " + t.getText())
				.reduce((s1, s2) -> (((s1 + " | " + s2).length() > 150 && !s1.contains(System.lineSeparator())) ? (s1 + System.lineSeparator() + s2) : (s1 + " | " + s2)))
				.orElse(null);
		embed.addField("Themes:", themes, false);
		return embed;
	}
	private static Embed createGridsEmbed(SyncPair sp, Boolean isVar, int formId) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getVariationName(isVar, formId),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		if(sp.getSyncBoards().size() <= 6) {
			embed.setDescription("This poor sync pair has no grids." + System.lineSeparator() + "DeNA plz");
			return embed;
		}
		// Sync Tiles
		String desc = sp.getSyncBoards().stream().filter(bp -> sp.getPokemon().getSyncMove().equals(bp.getMove()))
				.map(bp -> "(" + SyncBoardCondition.getRequiredLevelByConditions(bp.getConditions()) + ") **" + bp.getName() + "**")
				.reduce(Methods::reduceToList).orElse("");
		if(desc.length() > 0)
			embed.addField(BotType.getPokemonInstance().getImages().getEmoteText("GRID_SYNC") + " __Sync Tiles:__", desc, false);
		// Move Tiles
		boolean first = true;
		for(int i = 1; i <= 5; i++) {
			int ii = i;
			desc = "";
			for(String line : sp.getSyncBoards().stream().filter(bp -> bp.getMove() != null).filter(bp -> !sp.getPokemon().getSyncMove().equals(bp.getMove()))
					.filter(bp -> SyncBoardType.Move.equals(bp.getType()) || (SyncBoardType.PowerIncrease.equals(bp.getType()) && bp.getValue().intValue() > 10))
					.filter(bp -> SyncBoardCondition.getRequiredLevelByConditions(bp.getConditions()) == ii)
					.sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
					.map(bp -> "(" + SyncBoardCondition.getRequiredLevelByConditions(bp.getConditions()) + ") **" + bp.getName() + "** - " + bp.getDescription())
					.collect(Collectors.toList())) {
				if((desc + System.lineSeparator() + line).length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
					embed.addField(first ? SyncBoardType.Move.getEmoteText() + " __Move Tiles:__" : MessageUtils.E, desc, first = false);
					desc = "";
				}
				desc += System.lineSeparator() + line;
			}
			if(!desc.isBlank())
				embed.addField(first ? SyncBoardType.Move.getEmoteText() + " __Move Tiles:__" : MessageUtils.E, desc, first = false);
		}
		first = true;
		// Modifier Tiles
		for(int i = 1; i <= 5; i++) {
			int ii = i;
			desc = "";
			for(String line : sp.getSyncBoards().stream()
					.filter(bp -> SyncBoardType.Passive.equals(bp.getType()))
					.filter(bp -> SyncBoardCondition.getRequiredLevelByConditions(bp.getConditions()) == ii)
					.sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
					.map(bp -> "(" + SyncBoardCondition.getRequiredLevelByConditions(bp.getConditions()) + ") **" + bp.getName() + "** - " + bp.getDescription())
					.collect(Collectors.toList())) {
				if((desc + System.lineSeparator() + line).length() > MessageUtils.FIELD_MESSAGE_LIMIT) {
					embed.addField(first ? SyncBoardType.Passive.getEmoteText() + " __Passive Tiles:__" : MessageUtils.E, desc.trim(), first = false);
					desc = "";
				}
				desc += System.lineSeparator() + line;
			}
			if(!desc.isBlank())
				embed.addField(first ? SyncBoardType.Passive.getEmoteText() + " __Passive Tiles:__" : MessageUtils.E, desc, first = false);
		}
		return embed;
	}
	private static Embed createStyleEmbed(SyncPair sp, Boolean isVar, int formId) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getVariationName(isVar, formId),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		if(sp.getTrainer().isEX()) {
			embed.setDescription("Base Style →" + System.lineSeparator() + "EX Style ↓");
			String mainStyle = "https://github.com/gamepress/jsons/raw/master/image/" + sp.getTrainer().getActorKeywordId() + "_expose_1024.ktx.png";
			String exStyle = "https://github.com/gamepress/jsons/raw/master/image/" + sp.getTrainer().getActorKeywordId() + "_01_expose_1024.ktx.png";
			try {
				ResultSet rs = BotType.getPokemonInstance().getSQL().executeSelect("SELECT * FROM Styles WHERE characterId = ?", sp.getTrainer().getCharacterId());
				if(rs.next()) {
					mainStyle = rs.getString("main");
					exStyle = rs.getString("ex");
				} else {
					HttpURLConnection huc = (HttpURLConnection)new java.net.URL(mainStyle).openConnection();
					if(huc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
						embed.setDescription("Error loading Styles" + System.lineSeparator() + "Try again later");
						BotType.getPokemonInstance().getMessages().sendReport("Missing image for \"" + sp.getName() + "\"");
					}
				}
			} catch(IOException | BotException | SQLException e) {}
			embed.setThumbnail(mainStyle);
			embed.setImage(exStyle);
		} else {
			embed.setDescription("Base Style ↓" + System.lineSeparator() + "No EX yet");
			embed.setImage("https://github.com/gamepress/jsons/raw/master/image/" + sp.getTrainer().getActorKeywordId() + "_1024.ktx.png");
		}
		return embed;
	}
	private static Embed createBannersEmbed(SyncPair sp, Boolean isVar, int formId) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setAuthor(sp.getVariationName(isVar, formId),
				"https://gamepress.gg/pokemonmasters/pokemon/" + Methods.urlize(sp.getName()),
				sp.getTrainer().getRole().getEmoteClass().getImageUrl());
		embed.setColor(sp.getTrainer().getType().getColor());
		List<String> banners = BotType.getPokemonInstance().getSheets().getBannerData(sp.getTrainer().getCharacterId());
		if(banners == null || banners.isEmpty())
			embed.setDescription("No data available");
		else {
			embed.addField("Banner Availability", banners.stream().reduce((b1, b2) -> b1 + System.lineSeparator() + b2).orElse("Error"), false);
		}
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
						"pair" + MessageUtils.SEPARATOR + "TRAINERSWITCH" + MessageUtils.SEPARATOR + t.getTrainer().getCharacterId(),
						BotType.getPokemonInstance().getImages().getEmoteClassByName(t.getTrainer().getCharacterId())))
				.collect(Collectors.toList()).toArray(new SelectOption[0]));
		return embed;
	}
	private static Boolean isVar(ButtonInteractionEvent event) {
		if(event.getMessage().getActionRows().size() > 2)
			return event.getMessage().getActionRows().get(2).getButtons().stream().anyMatch(b -> b.getStyle().equals(ButtonStyle.PRIMARY));
		return null;
	}
	private static int getFormeId(ButtonInteractionEvent event) {
		for(Button b : event.getMessage().getActionRows().get(1).getButtons()) {
			if(b.getStyle().equals(ButtonStyle.PRIMARY)) {
				String formName = b.getId().split(MessageUtils.SEPARATOR)[2];
				if(formName.contains("Deoxys"))
					return FormType.getDeoxys(formName);
			}
		}
		return -1;
	}
	private static Embed getEmbedByType(SyncPair sp, String type, Boolean isVar, int formId) {
		switch(type) {
			case "PROFILE":
				return createProfileEmbed(sp, isVar, formId);
			case "MOVES":
				return createMoveEmbed(sp, isVar, formId);
			case "PASSIVES":
				return createPassiveEmbed(sp, isVar, formId);
			case "GRIDS":
				return createGridsEmbed(sp, isVar, formId);
			case "BANNERS":
				return createBannersEmbed(sp, isVar, formId);
			case "STYLES":
				return createStyleEmbed(sp, isVar, formId);
			default:
				throw new RuntimeException("Unknown Screen Type: " + type);
		}
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String name = event.getOption("name").getAsString().trim().toLowerCase();
		String type = Methods.getOptionValue(event.getOption("page"), "PROFILE").toUpperCase();
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
		Embed embed = getEmbedByType(sp, type, false, -1);
		embed.createRow(
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "PROFILE", sp.getTrainer().getCharacterId(), "Profile", "REACTION_PROFILE"),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "MOVES", sp.getTrainer().getCharacterId(), "Moves", "REACTION_MOVES"),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "PASSIVES", sp.getTrainer().getCharacterId(), "Passives",
						"REACTION_PASSIVES"),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "GRIDS", sp.getTrainer().getCharacterId(), "Grids", "REACTION_GRIDS"));
		List<String> banners = BotType.getPokemonInstance().getSheets().getBannerData(sp.getTrainer().getCharacterId());
		embed.createRow(
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "BANNERS", sp.getTrainer().getCharacterId(), "Availability",
						"REACTION_BANNERS", banners != null && !banners.isEmpty()),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "STYLES", sp.getTrainer().getCharacterId(), "Styles", "REACTION_STYLES"));
		if(sp.getTrainer().getCharacterId().equals("10090100000")) { // Deoxys Exclusive
			embed.createRow(
					embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + type + MessageUtils.SEPARATOR + FormType.DeoxysN.name(),
							sp.getTrainer().getCharacterId(), FormType.DeoxysN.getName(), FormType.DeoxysN.getEmote()),
					embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + type + MessageUtils.SEPARATOR + FormType.DeoxysA.name(),
							sp.getTrainer().getCharacterId(), FormType.DeoxysA.getName(), FormType.DeoxysA.getEmote()),
					embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + type + MessageUtils.SEPARATOR + FormType.DeoxysS.name(),
							sp.getTrainer().getCharacterId(), FormType.DeoxysS.getName(), FormType.DeoxysS.getEmote()),
					embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + type + MessageUtils.SEPARATOR + FormType.DeoxysD.name(),
							sp.getTrainer().getCharacterId(), FormType.DeoxysD.getName(), FormType.DeoxysD.getEmote()));
		} else if(sp.getVariation() != null && sp.getVariation().getFormType().getName() != null)
			embed.createRow(
					embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + type,
							sp.getTrainer().getCharacterId(), sp.getVariation().getFormType().getName(), sp.getVariation().getFormType().getEmote()));
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), embed);
	}
	@SuppressWarnings("unused")
	@Override
	public void doStuff(ButtonInteractionEvent event) throws BotException {
		String[] vals = event.getButton().getId().split(MessageUtils.SEPARATOR);
		Boolean isVar = event.getButton().getStyle() == ButtonStyle.PRIMARY ? Boolean.TRUE : isVar(event);

		int formId = event.getButton().getStyle() == ButtonStyle.PRIMARY
				? -1
				: (vals.length == 4 ? FormType.getDeoxys(vals[2]) : event.getMessage().getButtons().size() > 7 ? getFormeId(event) : -1);
		SyncPair sp = _Library.getById(vals.length == 4 ? vals[3] : vals[2]);
		Embed embed = null;;
		if(isVar != null) {
			if(vals[1].contains("SWITCH")) {
				vals[1] = vals[1].substring(6);
				embed = getEmbedByType(sp, vals[1].toUpperCase(), event.getButton().getStyle() != ButtonStyle.PRIMARY, formId);
				ActionRow ar = event.getMessage().getActionRows().get(0);
				ActionRow ar2 = event.getMessage().getActionRows().get(1);
				ActionRow ar3 = null;
				if(sp.getTrainer().getCharacterId().equals("10090100000")) { // Deoxys Exclusive
					ar3 = ActionRow.of(
							embed.createButton(formId == FormType.DeoxysN.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysN.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysN.getName(), FormType.DeoxysN.getEmote()),
							embed.createButton(formId == FormType.DeoxysA.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysA.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysA.getName(), FormType.DeoxysA.getEmote()),
							embed.createButton(formId == FormType.DeoxysS.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysS.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysS.getName(), FormType.DeoxysS.getEmote()),
							embed.createButton(formId == FormType.DeoxysD.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysD.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysD.getName(), FormType.DeoxysD.getEmote()));
				} else if(isVar) // Change to Normal
					ar3 = ActionRow.of(embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1],
							sp.getTrainer().getCharacterId(), sp.getVariation().getFormType().getName(), sp.getVariation().getFormType().getEmote()));
				else // Change to Variation
					ar3 = ActionRow.of(embed.createButton(ButtonStyle.PRIMARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1],
							sp.getTrainer().getCharacterId(), sp.getVariation().getFormType().getName(), sp.getVariation().getFormType().getEmote()));
				embed.setDeleteActions(true);
				embed.createRow(ar);
				embed.createRow(ar2);
				embed.createRow(ar3);
			} else {
				embed = getEmbedByType(sp, vals[1].toUpperCase(), isVar, formId);
				ActionRow ar = event.getMessage().getActionRows().get(0);
				ActionRow ar2 = event.getMessage().getActionRows().get(1);
				ActionRow ar3 = null;
				if(sp.getTrainer().getCharacterId().equals("10090100000")) { // Deoxys Exclusive
					ar3 = ActionRow.of(
							embed.createButton(formId == FormType.DeoxysN.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysN.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysN.getName(), FormType.DeoxysN.getEmote()),
							embed.createButton(formId == FormType.DeoxysA.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysA.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysA.getName(), FormType.DeoxysA.getEmote()),
							embed.createButton(formId == FormType.DeoxysS.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysS.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysS.getName(), FormType.DeoxysS.getEmote()),
							embed.createButton(formId == FormType.DeoxysD.ordinal() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
									this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1] + MessageUtils.SEPARATOR + FormType.DeoxysD.name(),
									sp.getTrainer().getCharacterId(), FormType.DeoxysD.getName(), FormType.DeoxysD.getEmote()));
				} else if(!isVar) // Keep Normal
					ar3 = ActionRow.of(embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1],
							sp.getTrainer().getCharacterId(), sp.getVariation().getFormType().getName(), sp.getVariation().getFormType().getEmote()));
				else // Keep Variation
					ar3 = ActionRow.of(embed.createButton(ButtonStyle.PRIMARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + vals[1],
							sp.getTrainer().getCharacterId(), sp.getVariation().getFormType().getName(), sp.getVariation().getFormType().getEmote()));
				embed.setDeleteActions(true);
				embed.createRow(ar);
				embed.createRow(ar2);
				embed.createRow(ar3);
			}
		} else
			embed = getEmbedByType(sp, vals[1].toUpperCase(), isVar, formId);
		BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), embed);
	}
	@Override
	public void doStuff(StringSelectInteractionEvent event) throws BotException {
		String[] vals = event.getSelectedOptions().get(0).getValue().split(MessageUtils.SEPARATOR);
		String type = "PROFILE";
		SyncPair sp = _Library.getById(vals[2]);
		Embed embed = getEmbedByType(sp, type, false, -1);
		embed.setDeleteActions(true);
		embed.createRow(
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "PROFILE", sp.getTrainer().getCharacterId(), "Profile", "REACTION_PROFILE"),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "MOVES", sp.getTrainer().getCharacterId(), "Moves", "REACTION_MOVES"),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "PASSIVES", sp.getTrainer().getCharacterId(), "Passives",
						"REACTION_PASSIVES"),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "GRIDS", sp.getTrainer().getCharacterId(), "Grids", "REACTION_GRIDS"));
		List<String> banners = BotType.getPokemonInstance().getSheets().getBannerData(sp.getTrainer().getCharacterId());
		embed.createRow(
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "BANNERS", sp.getTrainer().getCharacterId(), "Availability",
						"REACTION_BANNERS", banners != null && !banners.isEmpty()),
				embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "STYLES", sp.getTrainer().getCharacterId(), "Styles", "REACTION_STYLES"));
		if(sp.getVariation() != null && sp.getVariation().getFormType().getName() != null)
			embed.createRow(
					embed.createButton(ButtonStyle.SECONDARY, this.getCommand() + MessageUtils.SEPARATOR + "SWITCH" + type,
							sp.getTrainer().getCharacterId(), sp.getVariation().getFormType().getName(), sp.getVariation().getFormType().getEmote()));
		BotType.getPokemonInstance().getMessages().editMessage(event.getMessage(), embed);
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od2 = new OptionData(OptionType.STRING, "name", "Sync Pair name", true);
		OptionData od3 = new OptionData(OptionType.STRING, "page", "Starting page", false)
				.addChoices(new Command.Choice("Profile", "Profile"))
				.addChoices(new Command.Choice("Moves", "Moves"))
				.addChoices(new Command.Choice("Passives", "Passives"))
				.addChoices(new Command.Choice("Grids", "Grids"))
				.addChoices(new Command.Choice("Availability", "Availability"))
				.addChoices(new Command.Choice("Styles", "Styles"));
		cd.addOptions(od2, od3);
		return cd;
	}
}
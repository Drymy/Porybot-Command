package com.porybot.commands.pokemon;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.Move;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.Region;
import com.porybot.commands.pokemon.search.AcquisitionCondition;
import com.porybot.commands.pokemon.search.AttackTypeCondition;
import com.porybot.commands.pokemon.search.CategoryCondition;
import com.porybot.commands.pokemon.search.FieldEffectCondition;
import com.porybot.commands.pokemon.search.NegativeConditionsCondition;
import com.porybot.commands.pokemon.search.PairBox;
import com.porybot.commands.pokemon.search.PairTypeCondition;
import com.porybot.commands.pokemon.search.PositiveConditionsCondition;
import com.porybot.commands.pokemon.search.RarityCondition;
import com.porybot.commands.pokemon.search.RecoveryCondition;
import com.porybot.commands.pokemon.search.RegionCondition;
import com.porybot.commands.pokemon.search.RoleCondition;
import com.porybot.commands.pokemon.search.SpecialEffectsCondition;
import com.porybot.commands.pokemon.search.StatDownCondition;
import com.porybot.commands.pokemon.search.StatUpCondition;
import com.porybot.commands.pokemon.search._Condition;
import Utils.BotException;
import Utils.Dual;
import Utils.MessageUtils;
import Utils.MessageUtils.Embed;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class SearchCommand extends _PokemonCommand {
	private static class Search {
		public List<_Condition> conditions = new LinkedList<>();
		public List<PairBox> pairs = new LinkedList<>();

		public Search(List<_Condition> c) { conditions = c; }
	}

	private static final LoadingCache<Dual<Long, Long>, Dual<Message, List<Search>>> CHAIN_SEARCHES = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
			.removalListener(l -> {
				if(l.getCause().equals(RemovalCause.EXPIRED)) {
					@SuppressWarnings("unchecked")
					Dual<Message, List<Search>> value = (Dual<Message, List<Search>>)l.getValue();
					BotType.getPokemonInstance().getMessages().editMessage(value.getValue1(), createEmbed(value.getValue2(), false));
				}
			})
			.build(new CacheLoader<Dual<Long, Long>, Dual<Message, List<Search>>>() {
				@Override
				public Dual<Message, List<Search>> load(Dual<Long, Long> key) throws Exception { return new Dual<>(null, new LinkedList<>()); }
			});

	public static void checkCache() { CHAIN_SEARCHES.cleanUp(); }
	public SearchCommand() { super("search", "Search for any sync pair that fulfils the conditions you input"); }
	private static Embed createEmbed(List<Search> l, boolean createButtons) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setTitle("Conditions:");
		String condi = "";
		HashMap<SyncPair, Dual<List<Move>, List<Passive>>> validPairs = new HashMap<>();
		_Library.get(true).stream().forEach(sp -> validPairs.put(sp, new Dual<>(new LinkedList<>(), new LinkedList<>())));
		int condiCounter = 1;
		for(Search s : l) {
			String newC = s.conditions.stream().map(_Condition::getEmoteText).reduce((s1, s2) -> s1 + " | " + s2).orElse("");
			if(!newC.isBlank())
				condi += System.lineSeparator() + condiCounter++ + ": " + newC;
			Iterator<SyncPair> iter = validPairs.keySet().iterator();
			while(iter.hasNext()) {
				SyncPair p = iter.next();
				if(s.pairs.stream().noneMatch(pb -> pb.getPair() == p))
					iter.remove();
			}
			s.pairs.stream().filter(p -> validPairs.containsKey(p.getPair())).forEach(sp -> {
				validPairs.get(sp.getPair()).getValue1().addAll(sp.getFinalMoveList());
				validPairs.get(sp.getPair()).getValue2().addAll(sp.getFinalPassiveList());
			});
		}
		String desc = "";
		embed.setDescription(condi + MessageUtils.S);
		embed.setDeleteActions(true);
		if(createButtons)
			embed.createRow(embed.createButton(ButtonStyle.SECONDARY, "search", "rollback", "Rollback", "UI_ROLLBACK"),
					embed.createButton(ButtonStyle.DANGER, "search", "finish", "Finish Search", "UI_FINISH"));
		boolean first = true;
		for(SyncPair key : validPairs.keySet().stream().sorted((k1, k2) -> k1.getName().compareTo(k2.getName())).collect(Collectors.toList())) {
			String pair = BotType.getPokemonInstance().getImages().getEmoteText(key.getTrainer().getCharacterId()) + " **" + key.getName() + "**"
					+ (key.getTrainer().getBaseName().equals("Player") ? " (" + key.getTrainer().getRole().getEmoteText() + " " + key.getTrainer().getRole().getName() + ")" : "");
			String moveList = validPairs.get(key).getValue1().isEmpty()
					? ""
					: validPairs.get(key).getValue1().stream().distinct().map(Move::getName).reduce((m1, m2) -> m1 + ", " + m2).orElse("");
			String passiveList = validPairs.get(key).getValue2().isEmpty()
					? ""
					: validPairs.get(key).getValue2().stream().distinct().map(Passive::getName).reduce((p1, p2) -> p1 + ", " + p2).orElse("");
			if(!moveList.isBlank())
				pair += System.lineSeparator() + MessageUtils.tab() + "⤷" + moveList;
			if(!passiveList.isBlank())
				pair += System.lineSeparator() + MessageUtils.tab() + "⤷*" + passiveList + "*";
			if((desc.length() + System.lineSeparator().length() + pair.length()) > MessageUtils.FIELD_MESSAGE_LIMIT) {
				embed.addField(first ? "------------------------------" : MessageUtils.S, desc.trim(), false);
				first = false;
				desc = "";
			}
			desc += System.lineSeparator() + pair;
		}
		if(!desc.isBlank())
			embed.addField(first ? "------------------------------" : MessageUtils.S, desc, false);
		if(embed.length() > MessageUtils.EMBED_LIMIT) {
			embed.getFields().clear();
			embed.addField("------------------------------", "Too many pairs to show, run more search commands to limit search and get more results", false);
		}
		return embed;
	}
	private List<Search> parseArgs(SlashCommandInteractionEvent event, List<Search> searchList) {
		// Add new conditions to searchList
		List<_Condition> conditions = new LinkedList<>();
		if(event.getOption("rarity") != null) {
			searchList.forEach(s -> s.conditions.removeIf(c -> c.getClass() == RarityCondition.class));
			conditions.add(RarityCondition.of(event.getOption("rarity").getAsString()));
		} else if(searchList.isEmpty())
			conditions.add(RarityCondition.of("3"));
		if(event.getOption("availability") != null) {
			searchList.forEach(s -> s.conditions.removeIf(c -> c.getClass() == AcquisitionCondition.class));
			conditions.add(AcquisitionCondition.of(event.getOption("availability").getAsString().equals("TRUE")));
		} else if(searchList.isEmpty())
			conditions.add(AcquisitionCondition.of(true));
		if(event.getOption("region") != null) {
			searchList.forEach(s -> s.conditions.removeIf(c -> c.getClass() == RegionCondition.class));
			conditions.add(RegionCondition.of(Region.valueOf(event.getOption("region").getAsString())));
		}
		if(event.getOption("role") != null) {
			searchList.forEach(s -> s.conditions.removeIf(c -> c.getClass() == RoleCondition.class));
			conditions.add(RoleCondition.of(event.getOption("role").getAsString()));
		}
		conditions.add(PairTypeCondition.of(event));
		conditions.add(AttackTypeCondition.of(event));
		conditions.add(CategoryCondition.of(event));
		conditions.add(StatUpCondition.of(event));
		conditions.add(StatDownCondition.of(event));
		conditions.add(PositiveConditionsCondition.of(event));
		conditions.add(NegativeConditionsCondition.of(event));
		conditions.add(RecoveryCondition.of(event));
		conditions.add(SpecialEffectsCondition.of(event));
		conditions.add(FieldEffectCondition.of(event));
		// conditions.add(FieldCondition.of(event));
		// conditions.add(WeatherCondition.of(event));
		// conditions.add(TerrainCondition.of(event));
		// conditions.add(ZoneCondition.of(event));
		searchList.add(new Search(conditions.stream().filter(c -> c != null).collect(Collectors.toList())));
		return doTheFilter(searchList);
	}
	private List<Search> doTheFilter(List<Search> searchList) {
		for(Search s : searchList) {
			Stream<PairBox> res = _Library.get(true).stream().map(PairBox::new);
			for(_Condition condi : s.conditions)
				res = res.filter(condi);
			s.pairs = res.sorted((sp1, sp2) -> sp1.getPair().getName().compareTo(sp2.getPair().getName())).collect(Collectors.toList());
		}
		return searchList;
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		Dual<Long, Long> key = new Dual<>(event.getChannel().getIdLong(), event.getUser().getIdLong());
		Dual<Message, List<Search>> allSearches = CHAIN_SEARCHES.getUnchecked(key);
		Message originalMessage = allSearches.getValue1() != null ? allSearches.getValue1() : null;
		List<Search> res = parseArgs(event, allSearches.getValue2());
		if(originalMessage != null) {
			BotType.getPokemonInstance().getMessages().editMessage(originalMessage, createEmbed(res, true));
			return BotType.getPokemonInstance().getMessages().sendMessage(event.getHook(), "Search Updated");
		}
		return BotType.getPokemonInstance().getMessages().sendEmbed(event.getHook(), createEmbed(res, true))
				.thenApply(m -> {
					allSearches.setValue1(m);
					return m;
				});
	}
	@Override
	public boolean isEditButton(ButtonInteractionEvent event) {
		return CHAIN_SEARCHES.getIfPresent(new Dual<>(event.getChannel().getIdLong(), event.getUser().getIdLong())) != null;
	}
	@Override
	public void doStuff(ButtonInteractionEvent event) throws BotException {
		String op = event.getButton().getId().split(MessageUtils.SEPARATOR)[1];
		Dual<Long, Long> key = new Dual<>(event.getChannel().getIdLong(), event.getUser().getIdLong());
		Dual<Message, List<Search>> allSearches = null;
		allSearches = CHAIN_SEARCHES.getIfPresent(key);
		if(allSearches == null)
			BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), "No search available to undo");
		else if(op.equalsIgnoreCase("rollback")) {
			if(allSearches.getValue2().size() <= 1) {
				allSearches.getValue1().delete().queue();
				CHAIN_SEARCHES.invalidate(key);
			} else {
				allSearches.setValue2(allSearches.getValue2().subList(0, allSearches.getValue2().size() - 1));
				BotType.getPokemonInstance().getMessages().editMessage(allSearches.getValue1(), createEmbed(allSearches.getValue2(), true));
			}
		} else if(op.equalsIgnoreCase("finish")) {
			CHAIN_SEARCHES.invalidate(key);
			BotType.getPokemonInstance().getMessages().editMessage(allSearches.getValue1(), createEmbed(allSearches.getValue2(), false));
		}
	}
	@Override
	public boolean isEtherealReply(SlashCommandInteractionEvent event) {
		Dual<Long, Long> key = new Dual<>(event.getChannel().getIdLong(), event.getUser().getIdLong());
		return CHAIN_SEARCHES.getIfPresent(key) != null && CHAIN_SEARCHES.getIfPresent(key).getValue1() != null;
	}
	@Override
	public boolean isEtherealReply(ButtonInteractionEvent event) { return true; }
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		List<OptionData> lod = new LinkedList<>();
		lod.add(new OptionData(OptionType.STRING, "pair_type", "Restricts to type").addChoices(PairTypeCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "role", "Restricts to role").addChoices(RoleCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "region", "Restricts to region").addChoices(RegionCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "category", "Restricts to category").addChoices(CategoryCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "attack_type", "Restricts to move type").addChoices(AttackTypeCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "stat_up", "Restricts to stat increase").addChoices(StatUpCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "stat_down", "Restricts to stat decrease").addChoices(StatDownCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "positive_condition", "Restricts to condition").addChoices(PositiveConditionsCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "negative_condition", "Restricts to condition").addChoices(NegativeConditionsCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "recovery", "Restricts to action").addChoices(RecoveryCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "target", "Applies to move-related targetting, stat changes, conditions and fields")
				.addChoices(_Condition.generateTargetChoices()));
		// lod.add(new OptionData(OptionType.STRING, "field", "Restricts to
		// field").addChoices(FieldCondition.generateChoices()));
		// lod.add(new OptionData(OptionType.STRING, "weather", "Restricts to
		// weather").addChoices(WeatherCondition.generateChoices()));
		// lod.add(new OptionData(OptionType.STRING, "terrain", "Restricts to
		// terrain").addChoices(TerrainCondition.generateChoices()));
		// lod.add(new OptionData(OptionType.STRING, "zone", "Restricts to
		// zone").addChoices(ZoneCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "field_effects", "Field-wide options").addChoices(FieldEffectCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "special_effect", "Restricts to special effects").addChoices(SpecialEffectsCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "rarity", "Minimum Rarity (Defaults to '3* and above')").addChoices(RarityCondition.generateChoices()));
		lod.add(new OptionData(OptionType.STRING, "availability", "Pair availability (Defaults to 'Everyone')").addChoices(AcquisitionCondition.generateChoices()));
		cd.addOptions(lod.toArray(new OptionData[lod.size()]));
		return cd;
	}
}
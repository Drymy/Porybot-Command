package com.porybot.commands.pokemon;
import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.porybot._Library;
import com.porybot.GameElements.SyncPair;
import com.porybot.GameElements.Enums.Acquisition;
import IO.SQL.SQLAccess.UserData;
import Utils.BotException;
import Utils.Constants;
import Utils.CooldownManager;
import Utils.MessageUtils.Embed;
import Utils.Methods;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PullCommand extends _PokemonCommand {
	private static final String BASIC_PULL_GIF = "https://cdn.discordapp.com/attachments/961656376701095936/1104880127566938232/pull_no_spark.gif";
	private static final String SPECIAL_PULL_GIF = "https://cdn.discordapp.com/attachments/961656376701095936/1104880128250630144/pull_spark.gif";
	private static final String QUEUE_GIF = "https://media.discordapp.net/attachments/961656376701095936/1104858822738202765/poryqueue.gif";
	
	private static final List<Long> INFINITE_USES_USERS = Arrays.asList(
			Constants.QUETZ_ID,
			Constants.DREAMY_ID,
			643654685856628761L, //Annie Green
			895250065109712917L, //Gakon
			227518848641925121L, //Sages
			301390140377399297L  //ThunderNick20
		);

	private static class PullInfo {
		public Message message;
		public InteractionHook hook;
		public BannerType type;
		public List<SyncPair> featuredPairs;
		public List<SyncPair> pulls;
		public UserData userData;
	}

	private static HashMap<Long, Lock> channelLock = new HashMap<>();

	private static synchronized Lock getChannelLock(Long channelId) {
		channelLock.computeIfAbsent(channelId, k -> new ReentrantLock(true));
		return channelLock.get(channelId);
	}

	private enum BannerType {
		SPOTLIGHT1("Spotlight", 1, 0.02, 0.07, 0.2, "Spotlight Scout", new Color(255, 204, 102), Acquisition.General),
        SPOTLIGHT2("Spotlight", 2, 0.015, 0.07, 0.2, "Spotlight Scout", new Color(255, 204, 102), Acquisition.General),
        SPOTLIGHT3("Spotlight", 3, 0.01, 0.07, 0.2, "Spotlight Scout", new Color(255, 204, 102), Acquisition.General),
        SEASONAL1("Seasonal", 1, 0.02, 0.07, 0.2, "Seasonal Scout", new Color(153, 255, 51), Acquisition.Seasonal),
        SEASONAL2("Seasonal", 2, 0.015, 0.07, 0.2, "Seasonal Scout", new Color(153, 255, 51), Acquisition.Seasonal),
        SEASONAL3("Seasonal", 3, 0.01, 0.07, 0.2, "Seasonsal Scout", new Color(153, 255, 51), Acquisition.Seasonal),
        VARIETY1("Variety", 1, 0.02, 0.07, 0.2, "Variety Scout", new Color(153, 51, 0), Acquisition.Variety),
        VARIETY2("Variety", 2, 0.015, 0.07, 0.2, "Variety Scout", new Color(153, 51, 0), Acquisition.Variety),
        VARIETY3("Variety", 3, 0.01, 0.07, 0.2, "Variety Scout", new Color(153, 51, 0), Acquisition.Variety),
        POKEFAIR1("PokeFair", 1, 0.02, 0.1, 0.3, "PokéFair Scout", new Color(51, 153, 255), Acquisition.PokeFair),
        POKEFAIR2("PokeFair", 2, 0.015, 0.1, 0.3, "PokéFair Scout", new Color(51, 153, 255), Acquisition.PokeFair),
        POKEFAIR3("PokeFair", 3, 0.01, 0.1, 0.3, "PokéFair Scout", new Color(51, 153, 255), Acquisition.PokeFair),
        MASTERFAIR1("MasterFair", 1, 0.01, 0.12, 0.2, "MasterFair Scout", new Color(204, 0, 255), Acquisition.Master),
        MASTERFAIR2("MasterFair", 2, 0.0075, 0.12, 0.2, "MasterFair Scout", new Color(204, 0, 255), Acquisition.Master),
        MASTERFAIR3("MasterFair", 3, 0.005, 0.12, 0.2, "MasterFair Scout", new Color(204, 0, 255), Acquisition.Master);

		private String code, name;
		private int featuredNum;
		private double pF, p5, p4;
		private Color color;
		private Acquisition validAcquisition;

		private BannerType(String code, int featuredNum, double pF, double p5, double p4, String name, Color color, Acquisition validAcquisition) {
			this.code = code;
			this.featuredNum = featuredNum;
			this.pF = pF;
			this.p5 = p5;
			this.p4 = p4;
			this.name = name;
			this.color = color;
			this.validAcquisition = validAcquisition;
		}
		public String getName() { return name; }
		public Color getColor() { return color; }
		public double getFeaturedRate() { return pF; }
		public double getFiveStarRate() { return p5; }
		public double getFourStarRate() { return p4; }
		public void isValidPair(SyncPair sp) throws Exception {
			if(sp == null)
				return;
			if(sp.getTrainer().getRarity().intValue() != 5)
				throw new Exception("'" + sp.getName() + "'" + " is not a 5 Star Pair!");
			if(!sp.getAcquisition().contains(validAcquisition))
				throw new Exception("'" + sp.getName() + "'" + " is not a " + validAcquisition.getDescription() + "!");
		}
		public static BannerType getType(String code, int featuredNum) {
			return Arrays.asList(BannerType.values()).stream().filter(bt -> bt.code.equals(code) && bt.featuredNum == featuredNum).findFirst().orElse(null);
		}
	}

	public PullCommand() { super("pull", "Simulate pulls!"); }
	private static SyncPair getSpecificPair(String pairName) throws Exception {
		if(pairName == null)
			return null;
		pairName = pairName.trim().toLowerCase();
		List<SyncPair> res = _Library.get(pairName, false);
		if(pairName.contains("player"))
			res = _Library.getByBaseTrainer("Player");
		if(res.isEmpty())
			throw new Exception("'" + pairName + "'" + " is not a valid Sync Pair.");
		if(res.size() != 1)
			throw new Exception("Please input the full pair name, example : 'Selene & Decidueye'." + System.lineSeparator()
					+ "Alternatively use the Pokemon name or abbreviations such as 'ss selene' to prevent possible alts.");
		return res.get(0);
	}
	private static Embed createEmbed(PullInfo pull, int step) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		if(step == -1) {
			embed.setFooter("");
			if(pull.pulls.stream().anyMatch(sp -> sp.getTrainer().getRarity().intValue() == 5))
				embed.setImage(Methods.RNG.nextBoolean() ? SPECIAL_PULL_GIF : BASIC_PULL_GIF);
			else
				embed.setImage(BASIC_PULL_GIF);
		} else {
			embed.setAuthor(pull.type.getName());
			embed.setColor(pull.type.getColor());
			embed.setImage(null);
			embed.setFooter(null);
			embed.setDescription(BotType.getPokemonInstance().getImages().getEmoteText("GEMS_PULL") + "Gems used total: " + pull.userData.totalGems + System.lineSeparator() +
					BotType.getPokemonInstance().getImages().getEmoteText("GEMS_PULL") + "Gems used today: " + pull.userData.todayGems
					//+ System.lineSeparator() + BotType.getPokemonInstance().getImages().getEmoteText("GEMS_PULL") + "Gems used since last featured: " + (step == 11 ? "36000" : "?????")
					);
			embed.addField("Featured Pairs: ",
					pull.featuredPairs.stream().map(sp -> sp.getFeaturedStar() + " " + sp.getEmoteText() + " " + sp.getName()).reduce(Methods::reduceToList).orElse("None"), false);
			String message = pull.pulls.stream().limit(step)
					.map(spp -> (pull.featuredPairs.contains(spp) ? spp.getFeaturedStar() : spp.getStar()) + " " + spp.getEmoteText() + " " + spp.getName())
					.reduce(Methods::reduceToList).orElse("");
			if(step > 0)
				message += System.lineSeparator();
			message += pull.pulls.stream().skip(step)
					.map(spp -> BotType.getPokemonInstance().getImages().getEmoteText("STAR_1") + " " + BotType.getPokemonInstance().getImages().getEmoteText("EMPTY_TILE")
							+ "?????? & ??????")
					.reduce(Methods::reduceToList).orElse("");
			embed.addField("Scout Results: ", message, false);
		}
		return embed;
	}
	private CompletableFuture<Message> doPull(PullInfo pull) throws Exception {
		CompletableFuture<Message> ret = null;
		if(pull.message == null)
			ret = BotType.getPokemonInstance().getMessages().sendEmbed(pull.hook, createEmbed(pull, -1));
		else
			ret = BotType.getPokemonInstance().getMessages().editMessage(pull.message, createEmbed(pull, -1));
		ret.get();
		Methods.sleep(3000);
		for(int i = 0; i <= 11; i++) {
			ret = BotType.getPokemonInstance().getMessages().editMessage(ret, createEmbed(pull, i));
			ret.get();
			Methods.sleep(1000);
		}
		return ret;
	}
	private CompletableFuture<Message> doQueue(PullInfo pull) {
		Embed embed = new Embed(BotType.getPokemonInstance());
		embed.setImage(QUEUE_GIF);
		embed.setFooter("");
		return BotType.getPokemonInstance().getMessages().sendEmbed(pull.hook, embed);
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		try {
			UserData ud = BotType.getPokemonInstance().getSQL().getUserData(event.getMember().getUser().getIdLong());
			if(ud.todayGems >= 36000 && ud.today.equals(LocalDate.now()) && !INFINITE_USES_USERS.contains(ud.userId)) {
				throw new Exception("You have reached the daily cap of 36000 gems used." + System.lineSeparator() + 
									"If you wish to keep using this feature with no limit, please join our Patreon on https://patreon.com/PoryphoneBot" + System.lineSeparator() + 
									"Your support is valuable to keep the bot running and cover its monthly costs, we would be grateful for your participation!");
			}
			String type = Methods.getOptionValue(event.getOption("type"));
			SyncPair pair1 = getSpecificPair(Methods.getOptionValue(event.getOption("pair1"), null));
			SyncPair pair2 = getSpecificPair(Methods.getOptionValue(event.getOption("pair2"), null));
			SyncPair pair3 = getSpecificPair(Methods.getOptionValue(event.getOption("pair3"), null));

			List<SyncPair> threeStarPairs = _Library.get(false).stream().filter(a -> a.getAcquisition().contains(Acquisition.General))
					.filter(r -> r.getTrainer().getRarity().intValue() == 3).collect(Collectors.toList());
			List<SyncPair> fourStarPairs = _Library.get(false).stream().filter(a -> a.getAcquisition().contains(Acquisition.General))
					.filter(r -> r.getTrainer().getRarity().intValue() == 4).collect(Collectors.toList());
			List<SyncPair> fiveStarPairs = _Library.get(false).stream().filter(a -> a.getAcquisition().contains(Acquisition.General))
					.filter(r -> r.getTrainer().getRarity().intValue() == 5).collect(Collectors.toList());
			List<SyncPair> featuredPairs = new ArrayList<>();
			if(pair1 != null)
				featuredPairs.add(pair1);
			if(pair2 != null)
				featuredPairs.add(pair2);
			if(pair3 != null)
				featuredPairs.add(pair3);

			// the damn rates
			final Random random = new Random();
			final int NUM_ROLLS = 11;
			BannerType scoutRate = BannerType.getType(type, featuredPairs.size());
			// In case there was a combination that isn't valid
			if(scoutRate == null)
				throw new Exception("Invalid combination given for the specified scout type");

			// // the short ass part to counter user stupidity
			for(SyncPair sp : featuredPairs)
				scoutRate.isValidPair(sp);

			List<SyncPair> pullPairs = new ArrayList<>();
			for(int i = 0; i < NUM_ROLLS; i++) {
				double roll = random.nextDouble();
				if(roll < scoutRate.getFiveStarRate()) {
					if(roll < (scoutRate.getFeaturedRate() * featuredPairs.size())) {
						// featured 5-star piece
						pullPairs.add(featuredPairs.get(random.nextInt(featuredPairs.size())));
					} else {
						// regular 5-star piece
						pullPairs.add(fiveStarPairs.get(random.nextInt(fiveStarPairs.size())));
					}
				} else if(roll < scoutRate.getFiveStarRate() + scoutRate.getFourStarRate()) {
					// 4-star piece
					pullPairs.add(fourStarPairs.get(random.nextInt(fourStarPairs.size())));
				} else {
					// 3-star piece
					pullPairs.add(threeStarPairs.get(random.nextInt(threeStarPairs.size())));
				}
			}
			PullInfo pull = new PullInfo();
			pull.pulls = pullPairs;
			pull.featuredPairs = featuredPairs;
			pull.type = scoutRate;
			pull.hook = event.getHook();
			pull.userData = ud;
//			pull.userId = event.getMember().getIdLong();
//			pull.guildId = event.getGuild().getIdLong();
//			pull.channelId = event.getChannel().getIdLong();
			Lock lock = getChannelLock(event.getChannel().getIdLong());
			try {
				if(!lock.tryLock()) {
					pull.message = doQueue(pull).get();
					lock.lock();
					BotType.getPokemonInstance().getMessages()
							.sendMessage(event.getChannel(), event.getMember().getAsMention() + ", your pull is ready: " + System.lineSeparator() +
									"https://discord.com/channels/" + event.getGuild().getId() + "/" + event.getChannel().getId() + "/" + pull.message.getId())
							.thenAccept(mm -> { Methods.sleep(20000); mm.delete().queue(); });
				}
				if(!ud.today.equals(LocalDate.now())){
					ud.today = LocalDate.now();
					ud.todayGems = 0;
				}
				ud.todayGems += 3000;
				ud.totalGems += 3000;
				BotType.getPokemonInstance().getSQL().saveUserData(ud);
				return doPull(pull);
			} finally {
				lock.unlock();
			}
		} catch(Exception e) {
			return BotType.getPokemonInstance().getMessages().sendStatusMessageWarn(event.getHook(), e.getMessage());
		}
	}
	@Override
	public boolean isEtherealReply(SlashCommandInteractionEvent event) {
		try {
			UserData ud = BotType.getPokemonInstance().getSQL().getUserData(event.getMember().getUser().getIdLong());
			if(ud.todayGems == 36000 && ud.today.equals(LocalDate.now())) return true;
			SyncPair pair1 = getSpecificPair(Methods.getOptionValue(event.getOption("pair1"), null));
			SyncPair pair2 = getSpecificPair(Methods.getOptionValue(event.getOption("pair2"), null));
			SyncPair pair3 = getSpecificPair(Methods.getOptionValue(event.getOption("pair3"), null));
			List<SyncPair> featuredPairs = new ArrayList<>();
			if(pair1 != null)
				featuredPairs.add(pair1);
			if(pair2 != null)
				featuredPairs.add(pair2);
			if(pair3 != null)
				featuredPairs.add(pair3);
			BannerType scoutRate = BannerType.getType(Methods.getOptionValue(event.getOption("type")), featuredPairs.size());
			if(scoutRate == null)
				return true;
			for(SyncPair sp : featuredPairs)
				scoutRate.isValidPair(sp);
			return false;
		} catch(Exception e) {
			return true;
		}
	}
	@Override
	public CooldownManager.Type getCooldown(SlashCommandInteractionEvent event) { 
		return CooldownManager.Type.PULL;
	}
	@Override
	public SlashCommandData getCommandData() {
		SlashCommandData cd = super.getCommandData();
		OptionData od3 = new OptionData(OptionType.STRING, "type", "The Banner Type", true);
		od3.addChoices(new Command.Choice("Spotlight", "Spotlight"));
		od3.addChoices(new Command.Choice("Seasonal", "Seasonal"));
		od3.addChoices(new Command.Choice("Variety", "Variety"));
		od3.addChoices(new Command.Choice("PokéFair", "PokeFair"));
		od3.addChoices(new Command.Choice("MasterFair", "MasterFair"));
		OptionData od4 = new OptionData(OptionType.STRING, "pair1", "First Featured Pair", true);
		OptionData od5 = new OptionData(OptionType.STRING, "pair2", "Second Featured Pair", false);
		OptionData od6 = new OptionData(OptionType.STRING, "pair3", "Third Featured Pair", false);
		cd.addOptions(od3, od4, od5, od6);
		return cd;
	}
}
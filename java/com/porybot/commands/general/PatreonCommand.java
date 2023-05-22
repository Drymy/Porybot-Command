package com.porybot.commands.general;
import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.manager.BotType;
import com.manager.commands._PokemonCommand;
import com.patreon.PatreonAPI;
import com.patreon.resources.Campaign;
import com.patreon.resources.Pledge;
import com.patreon.resources.Reward;
import com.porybot.GameElements.Enums.Acquisition;
import Utils.BotException;
import Utils.Constants;
import Utils.ImageUtils.Emotes;
import Utils.MessageUtils;
import Utils.Methods;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PatreonCommand extends _PokemonCommand {
	private static final String PATREON_LINK = "https://www.patreon.com/PoryphoneBot";
	private static final int NUMBER_OF_VISIBLE_PATRONS = 10;
	private static HashMap<String, String> REPLACES = new HashMap<>();

	static {
		// REPLACES.put("Sae-Hon Kim", "Alhazard");
	}

	private static final String replaceName(String name) { return REPLACES.containsKey(name) ? REPLACES.get(name) : name; }
	public PatreonCommand() { super("patreon", "Shows Patreon Supporters for PoryphoneBot <3"); }
	public static boolean isUserPatreon(User u) {
		Guild poryServer = u.getJDA().getGuildById(Constants.PORYBOT_SERVER_ID);
		return u.getIdLong() == Constants.QUETZ_ID ||
				Stream.of(poryServer.getMembersWithRoles(poryServer.getRoleById(961651092209434644L)),
						poryServer.getMembersWithRoles(poryServer.getRoleById(961650907240607834L)))
						.flatMap(List::stream).distinct().anyMatch(m -> m.getUser().equals(u));
	}
	private EmbedBuilder build(String bannerURL, List<String> patrons, String oldest, String mostRecent, int totalPatrons) {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("PoryphoneBot Patreon");
		builder.setThumbnail(instance.getClient().getSelfUser().getAvatarUrl());
		builder.setColor(new Color(249, 104, 84));
		builder.setImage(bannerURL);
		builder.addField("Link", PATREON_LINK, false);
		builder.addField("Special Mentions",
				"Oldest Patron: " + oldest + System.lineSeparator() +
						"Newest Patron: " + mostRecent + System.lineSeparator(),
				true);
		builder.addField("Patron Count", totalPatrons + " Patreons", true);
		int i = 0;
		while((i * NUMBER_OF_VISIBLE_PATRONS) < patrons.size()) {
			builder.addField(i == 0 ? "Patrons" : MessageUtils.E,
					patrons.subList(
							i * NUMBER_OF_VISIBLE_PATRONS,
							(i * NUMBER_OF_VISIBLE_PATRONS + NUMBER_OF_VISIBLE_PATRONS) >= patrons.size() ? patrons.size() : i * NUMBER_OF_VISIBLE_PATRONS + NUMBER_OF_VISIBLE_PATRONS)
							.stream().reduce(Methods::reduceToList).orElse("---"),
					false);
			i++;
		}
		return builder;
	}
	public static long getPatreonCount() {
		try {
			PatreonAPI apiClient = new PatreonAPI(BotType.getPokemonInstance().getSQL().getKeyValue(Constants.PATREON_ACCESS_TOKEN).trim());
			Campaign campaign = apiClient.fetchCampaigns().get().get(0);
			List<Pledge> pledges = apiClient.fetchAllPledges(campaign.getId());
			return pledges.size();
		} catch(IOException e) {
			return -1;
		}
	}
	public final static void updatePatrons() throws BotException {
		Guild poryServer = BotType.getPokemonInstance().getClient().getGuildById(Constants.PORYBOT_SERVER_ID);
		final GuildMessageChannel patreonChannel = poryServer.getTextChannelById(961655706740748390L);
		final GuildMessageChannel privateChannel = poryServer.getTextChannelById(964529784484937798L);
		try {
			final PatreonAPI apiClient = new PatreonAPI(BotType.getPokemonInstance().getSQL().getKeyValue(Constants.PATREON_ACCESS_TOKEN).trim());
			Campaign campaign = apiClient.fetchCampaigns().get().get(0);
			final List<Pledge> pledges = apiClient.fetchAllPledges(campaign.getId());
			final List<Long> patreonDiscordIdsT2 = new LinkedList<>();
			final List<Long> patreonDiscordIdsT3 = new LinkedList<>();
			final List<String> patronsMissingDiscordTag = new LinkedList<>();
			final List<Long> patreonDiscordLongSub = new LinkedList<>();
			for(Pledge p : pledges)
				if(p.getPatron().getSocialConnections() != null) {
					if(p.getPatron().getSocialConnections().getDiscord() != null) {
						long monthsPatron = ChronoUnit.MONTHS.between(LocalDate.parse(p.getCreatedAt().substring(0, 10),
								DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalDate.now());
						if(monthsPatron >= 6) { patreonDiscordLongSub.add(Long.parseLong(p.getPatron().getSocialConnections().getDiscord().getUser_id())); }
						if(p.getReward().getTitle().equals("Spotlight Pair") || p.getReward().getTitle().equals("Seasonal Pair") || p.getReward().getTitle().equals("Pokéfair Pair")) {
							patreonDiscordIdsT2.add(Long.parseLong(p.getPatron().getSocialConnections().getDiscord().getUser_id()));
						} else if(p.getReward().getTitle().equals("Masterfair Pair")) {
							patreonDiscordIdsT2.add(Long.parseLong(p.getPatron().getSocialConnections().getDiscord().getUser_id()));
							patreonDiscordIdsT3.add(Long.parseLong(p.getPatron().getSocialConnections().getDiscord().getUser_id()));
						}
					} else if(p.getReward().getTitle().equals("Spotlight Pair") || p.getReward().getTitle().equals("Seasonal Pair") || p.getReward().getTitle().equals("Pokéfair Pair") || p.getReward().getTitle().equals("Masterfair Pair"))
						patronsMissingDiscordTag.add(p.getPatron().getFullName() + " - " + p.getReward().getTitle());
				}
			final Role patron = poryServer.getRoleById(961651092209434644L);
			final Role highPatron = poryServer.getRoleById(961650907240607834L);
			poryServer.findMembersWithRoles(highPatron).onSuccess(lm -> lm.stream().filter(m -> !patreonDiscordIdsT3.contains(m.getIdLong())).forEach(m -> poryServer.removeRoleFromMember(m, highPatron).submit()
					.thenAccept(v -> BotType.getPokemonInstance().getMessages().sendMessage(privateChannel, m.getEffectiveName() + " is no longer a Master Patron.")))).onSuccess(v1 -> poryServer.findMembersWithRoles(patron).onSuccess(lm -> lm.stream().filter(m -> !patreonDiscordIdsT2.contains(m.getIdLong())).forEach(
							m -> poryServer.removeRoleFromMember(m, patron).submit()
									.thenAccept(v -> BotType.getPokemonInstance().getMessages().sendMessage(privateChannel, m.getEffectiveName() + " is no longer a Patron.")))))
					.onSuccess(v1 -> poryServer.retrieveMembersByIds(patreonDiscordIdsT2).onSuccess(lm -> lm.stream().filter(m -> !m.getRoles().contains(patron))
							.forEach(m -> poryServer.addRoleToMember(m, patron).submit()
									.thenAccept(v -> BotType.getPokemonInstance().getMessages().sendMessage(privateChannel, m.getEffectiveName() + " is a new Patron."))
									.thenAccept(v -> BotType.getPokemonInstance().getMessages().sendMessage(patreonChannel, m.getEffectiveName() + " is a new Patron.")))))
					.onSuccess(v1 -> poryServer.retrieveMembersByIds(patreonDiscordIdsT3).onSuccess(lm -> lm.stream().filter(m -> !m.getRoles().contains(highPatron))
							.forEach(m -> poryServer.addRoleToMember(m, patron).submit()
									.thenAccept(v -> poryServer.addRoleToMember(m, highPatron).submit())
									.thenAccept(v -> BotType.getPokemonInstance().getMessages().sendMessage(privateChannel, m.getEffectiveName() + " is a new Master Patron."))
									.thenAccept(v -> BotType.getPokemonInstance().getMessages().sendMessage(patreonChannel, m.getEffectiveName() + " is a new Master Patron.")))))
					.onSuccess(v1 -> {
						;// MessageUtils.sendMessageToChannel(troupeNotes, "TT Patreon successfully
							// executed");
					});
		} catch(IOException e) {
			BotType.getPokemonInstance().getMessages().sendReport("Patreon Key is probably dead, please refresh." + System.lineSeparator() +
					"https://www.patreon.com/portal/registration/register-clients");
			throw new BotException(BotType.getPokemonInstance(), "Patreon Key is dead", e);
		}
	}
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		try {
			PatreonAPI apiClient = new PatreonAPI(instance.getSQL().getKeyValue(Constants.PATREON_ACCESS_TOKEN).trim());
			final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
			Campaign campaign = apiClient.fetchCampaigns().get().get(0);
			List<Pledge> pledges = apiClient.fetchAllPledges(campaign.getId());
			int totalPatrons = pledges.size();
			String mostRecent = pledges.stream().sorted((p1, p2) -> {
				try {
					return f.parse(p2.getCreatedAt()).compareTo(f.parse(p1.getCreatedAt()));
				} catch(ParseException e) {
					;
				}
				return 0;
			}).findFirst().map(p -> (p.getReward() == null ? instance.getImages().getEmoteText(Emotes.UNKNOWN_EMOTE.get()) : instance.getImages().getEmoteText(getTierEmote(p.getReward()))) + " " + p.getPatron().getFullName()).orElse(null);
			String oldest = pledges.stream().sorted((p1, p2) -> {
				try {
					return f.parse(p1.getCreatedAt()).compareTo(f.parse(p2.getCreatedAt()));
				} catch(ParseException e) {
					;
				}
				return 0;
			}).findFirst().map(p -> (p.getReward() == null ? instance.getImages().getEmoteText(Emotes.UNKNOWN_EMOTE.get()) : instance.getImages().getEmoteText(getTierEmote(p.getReward()))) + " " + p.getPatron().getFullName()).orElse(null);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			List<String> patrons = pledges.stream()
					.sorted((p1, p2) -> p2.getAmountCents() - p1.getAmountCents())
					.filter(p -> replaceName(p.getPatron().getFullName()) != null)
					.map(p -> (p.getReward() == null ? instance.getImages().getEmoteText(Emotes.UNKNOWN_EMOTE.get()) : instance.getImages().getEmoteText(getTierEmote(p.getReward())))
							+ " " + replaceName(p.getPatron().getFullName()) +
							" (" + ChronoUnit.MONTHS.between(LocalDate.parse(p.getCreatedAt().substring(0, 10), formatter), LocalDate.now()) + " months)")
					.collect(Collectors.toList());
			return instance.getMessages().sendEmbed(event.getHook(), build(campaign.getImageUrl(), patrons, oldest, mostRecent, totalPatrons));
		} catch(IOException e) {
			instance.getMessages().sendReport("Patreon Key is dead, please refresh." + System.lineSeparator() +
					"https://www.patreon.com/portal/registration/register-clients" + System.lineSeparator() +
					"Use '/admin patreon updatekey key:<key>' to update the key");
			throw new BotException(instance, "Patreon Key is dead", e);
		}
	}
	private static final String getTierEmote(Reward r) {
		switch(r.getTitle()) {
			case "Spotlight Pair":
				return Acquisition.General.getEmoteText();
			case "Seasonal Pair":
				return Acquisition.Seasonal.getEmoteText();
			case "Pokéfair Pair":
				return Acquisition.PokeFair.getEmoteText();
			case "Masterfair Pair":
				return Acquisition.Master.getEmoteText();
		}
		return null;
	}
	public static final boolean updatePatreonKey(Member member, String key) throws BotException {
		if(member.getIdLong() == Constants.QUETZ_ID) {
			BotType.getPokemonInstance().getSQL().setKeyValue(Constants.PATREON_ACCESS_TOKEN, key);
			return true;
		}
		return false;
	}
}
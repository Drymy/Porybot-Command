package com.porybot.commands.general;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import com.manager.commands._PokemonCommand;

import Utils.BotException;
import Utils.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class AboutCommand extends _PokemonCommand{
	public AboutCommand() { super("about", "Shows info about who created this bot."); }

	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setAuthor("About PoryphoneBot", "https://discord.gg/C8EYQRzPyW", event.getJDA().getSelfUser().getAvatarUrl());
		builder.addField("Developer",
				"**Quetz** - Owner and developer of [PoryphoneBot](https://discord.gg/C8EYQRzPyW)", false);
		builder.addField("Main Contributors",
				"**Dreamy** - First incentive to create the bot, Big Opinionator, Emote Designer and Game Connoisseur " + System.lineSeparator() +
				"**[GamePress](https://github.com/gamepress/jsons)** - Source of all the bot's data" + System.lineSeparator() +
				"[Sages](https://tiermaker.com/create/pokmon-masters-ex-sages-type-colored-1132341) - Sync Pair Image Maker" + System.lineSeparator() +
				"", false);
		builder.addField("Other Contributors",
				"**Spark** - Author of [Grid Suggestions](https://docs.google.com/document/d/1vF42uzF-xpkcfIU2gVEY4Dl7sS_I3ITj8g5X2lo1usA/edit) shown by the bot" + System.lineSeparator() +
				"**shiro-kenri** - Event timeline and Gem Distribution" + System.lineSeparator() +
				"**[Absol-utely](https://twitter.com/absolutelypm)** - Pair Infographs and Champium Stadium Schedule" + System.lineSeparator() +
				"**Runner** - Guides & Datamine resources" + System.lineSeparator() +
				"**Ropalme1914** - Guides & Datamine resources" + System.lineSeparator() + System.lineSeparator() +
				MessageUtils.tab() + "and all others that contribute to the information provided by the /misc command" + System.lineSeparator() +
				"", false);
		builder.addField("Special Mentions", "All the patrons that monetarily support the bot's cost through [Patreon](https://patreon.com/PoryphoneBot)! Check them out with /patreon", false);
		builder.setFooter("");
		return instance.getMessages().sendEmbed(event.getHook(), builder);
	}

	@Override
	public boolean isEtherealReply(@Nonnull SlashCommandInteractionEvent event) { return event.getOption("update") != null; }
}
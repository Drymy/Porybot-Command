package com.porybot.commands.general;
import java.util.concurrent.CompletableFuture;
import com.manager.commands._PokemonCommand;
import Utils.BotException;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class InviteCommand extends _PokemonCommand{
	public InviteCommand() { super("invite", "Shows invite links for PoryphoneBot Server and the Bot's Invite URL."); }

	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException {
		String text = "PoryphoneBot Support Server: https://discord.gg/C8EYQRzPyW" + System.lineSeparator() +
				"PoryphoneBot Invite Link: [Invite Link Here](https://discord.com/oauth2/authorize?client_id=960544603575050310&permissions=259912944704&scope=bot%20applications.commands)";
		return instance.getMessages().sendMessage(event.getHook(), text);
	}
}
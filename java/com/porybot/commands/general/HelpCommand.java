package com.porybot.commands.general;
import java.util.concurrent.CompletableFuture;
import com.manager.commands._PokemonCommand;

import Utils.BotException;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class HelpCommand extends _PokemonCommand {
	public HelpCommand() { super("help", "Hopefully helpful text"); }
	
	@Override
	public CompletableFuture<Message> doStuff(SlashCommandInteractionEvent event) throws BotException { 
		return instance.getMessages().sendMessage(event.getHook(), instance.getSQL().getKeyValue("HELP"));
	}
}
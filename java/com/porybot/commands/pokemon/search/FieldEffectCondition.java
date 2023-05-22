package com.porybot.commands.pokemon.search;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public abstract class FieldEffectCondition implements _Condition {
	private static final List<Class<? extends FieldEffectCondition>> conds = Arrays.asList(WeatherCondition.class, TerrainCondition.class, FieldCondition.class,
			ZoneCondition2.class);

	public static interface FieldEffect {
		public String name();
		public String getDescription();
		public String getEmoteText();
		public RichCustomEmoji getEmote();
	}

	public static FieldEffectCondition of(SlashCommandInteractionEvent event) {
		FieldEffectCondition ret = null;
		if(event.getOption("field_effects") == null)
			return ret;
		for(Class<? extends FieldEffectCondition> c : conds) {
			Object o;
			try {
				o = c.getMethod("of", SlashCommandInteractionEvent.class).invoke(null, event);
				if(o != null)
					return (FieldEffectCondition)o;
			} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				return null;
			}
		}
		return ret;
	}
	@SuppressWarnings("unchecked")
	public static List<Choice> generateChoices() {
		return conds.stream().flatMap(c -> {
			try {
				return ((List<Choice>)c.getMethod("generateChoices").invoke(null)).stream();
			} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				return null;
			}
		}).collect(Collectors.toList());
	}
}
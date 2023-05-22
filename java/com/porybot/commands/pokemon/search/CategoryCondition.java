package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.Streams;
import com.porybot.GameElements.Move;
import com.porybot.GameElements.Enums.Category;
import com.porybot.GameElements.Enums.Target;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class CategoryCondition implements _Condition {
	private Category category;
	private Category category2;
	private Target target;

	private CategoryCondition(Category category, Category category2, Target target) {
		this.category = category;
		this.category2 = category2;
		this.target = target;
	}

	public static CategoryCondition of(SlashCommandInteractionEvent event) {
		if(event.getOption("category") == null) return null;
		return of(event.getOption("category").getAsString(), _Condition.parseTarget(event.getOption("target")));
	}

	public static List<Choice> generateChoices(){
		return Streams.concat(	Arrays.asList(Category.values()).stream().limit(3).map(t -> new Command.Choice(t.name(), t.name())),
								Arrays.asList(	new Command.Choice("Physical or Special", "PhysicalSpecial"),
												new Command.Choice("Physical or Status", "PhysicalStatus"),
												new Command.Choice("Special or Status", "SpecialStatus"),
												new Command.Choice("Physical and Special", "PhysicalANDSpecial"),
												new Command.Choice("Only Moves", "OnlyMoves"),
												new Command.Choice("Only Passives", "OnlyPassives")).stream()).collect(Collectors.toList());
	}

	private static CategoryCondition of(String category, Target target) {
		switch(category) {
			case "PhysicalSpecial": return new CategoryCondition(Category.Physical, Category.Special, target);
			case "PhysicalANDSpecial": return new CategoryCondition(Category.PhysicalSpecial, null, target);
			case "PhysicalStatus": return new CategoryCondition(Category.Physical, Category.Status, target);
			case "SpecialStatus": return new CategoryCondition(Category.Special, Category.Status, target);
			default: return new CategoryCondition(Category.valueOf(category), null, target);
		}
	}

	@Override
	public String getEmoteText() {
		String text = category.getEmoteText() + (category2 != null ? category2.getEmoteText() : "") + (target != null ? target.getEmoteText() : "") + " " +
				(target != null ? target.getName() + " " : "") + category.name() + (category2 != null ? "/" + category2.name() : "");
		return text.contains("Only") ? text.replace("Only", "Only ") : (text + " Move");
	}
	private boolean isValidTarget(Move m) {
		return target == null || m.getTarget() == target;
	}
	public boolean isOnlyMoves() { return Category.OnlyMoves == category; }
	public boolean isOnlyPassives() { return Category.OnlyPassives == category; }

	@Override
	public boolean eval(PairBox sp) {
		if(Category.OnlyMoves == category)
			sp.filterPassives(sp.getPassiveList().stream().filter(m -> false).collect(Collectors.toList()));
		else if(Category.OnlyPassives == category)
			sp.filterMoves(sp.getMoveList().stream().filter(m -> false).collect(Collectors.toList()));
		else if(Category.PhysicalSpecial == category)
			if( sp.getPair().getAllMoves().stream().anyMatch(m -> m.getCategory() == Category.Physical) &&
				sp.getPair().getAllMoves().stream().anyMatch(m -> m.getCategory() == Category.Special))
					sp.filterMoves(sp.getMoveList().stream().filter(m ->
						isValidTarget(m) && (m.getCategory() == Category.Physical || m.getCategory() == Category.Special))
					.collect(Collectors.toList()));
			else
				return false;
		else {
			sp.filterMoves(sp.getMoveList().stream().filter(m ->
				isValidTarget(m) && (m.getCategory() == category || (category2 != null && m.getCategory() == category2)))
			.collect(Collectors.toList()));
			sp.filterPassives(sp.getPassiveList().stream().filter(m -> false).collect(Collectors.toList()));
		}
		return !sp.getMoveList().isEmpty();
	}
}
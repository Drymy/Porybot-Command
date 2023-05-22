package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.Streams;
import com.porybot.GameElements.Enums.Role;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class RoleCondition implements _Condition {
	private Role role;
	private Role role2;
	private Role role3;

	private RoleCondition(Role role, Role role2, Role role3) {
		this.role = role;
		this.role2 = role2;
		this.role3 = role3;
	}

	public static List<Choice> generateChoices(){
		return Streams.concat(Arrays.asList(Role.values()).stream().limit(4).map(t -> new Command.Choice(t.getName(), t.name())),
				Arrays.asList(	new Command.Choice("Strikers (Physical or Special)", "PhysicalSpecial"),
						new Command.Choice("Physical or Tech", "PhysicalTech"), new Command.Choice("Special or Tech", "SpecialTech"),
						new Command.Choice("Physical or Support", "PhysicalSupport"), new Command.Choice("Special or Support", "SpecialSupport"),
						new Command.Choice("Support or Tech", "TechSupport"), new Command.Choice("Strikers or Tech", "StrikerTech"),
						new Command.Choice("Strikers or Support", "StrikerSupport")).stream()).collect(Collectors.toList());
	}
	public static RoleCondition of(String role) {
		switch(role) {
			case "PhysicalSpecial": return new RoleCondition(Role.StrikePhysical, Role.StrikeSpecial, null);
			case "PhysicalTech": return new RoleCondition(Role.StrikePhysical, Role.Tech, null);
			case "SpecialTech": return new RoleCondition(Role.StrikeSpecial, Role.Tech, null);
			case "PhysicalSupport": return new RoleCondition(Role.StrikePhysical, Role.Support, null);
			case "SpecialSupport": return new RoleCondition(Role.StrikeSpecial, Role.Support, null);
			case "TechSupport": return new RoleCondition(Role.Tech, Role.Support, null);
			case "StrikerTech": return new RoleCondition(Role.StrikePhysical, Role.StrikeSpecial, Role.Tech);
			case "StrikerSupport": return new RoleCondition(Role.StrikePhysical, Role.StrikeSpecial, Role.Support);
			default: return new RoleCondition(Role.valueOf(role), null, null);
		}
	}

	@Override
	public String getEmoteText() {
		return role.getEmoteText() + (role2 != null ? role2.getEmoteText() : "") + (role3 != null ? role3.getEmoteText() : "") + " " +
				role.getName() + (role2 != null ? "/" + role2.getName() : "") + (role3 != null ? "/" + role3.getName() : "") + " " + "Role";
	}

	@Override
	public boolean eval(PairBox o) {
		return 				  o.getPair().getTrainer().getRole() == role ||
			(role2 != null && o.getPair().getTrainer().getRole() == role2) ||
			(role3 != null && o.getPair().getTrainer().getRole() == role3);
	}
}
package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import com.manager.BotType;
import com.porybot.GameElements.Enums.Acquisition;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class AcquisitionCondition implements _Condition {
	private boolean everyone;

	private AcquisitionCondition(boolean everyone) { this.everyone = everyone; }
	public static List<Choice> generateChoices() {
		return Arrays.asList(new Choice("Permanent Pairs (General Pool/Story/BP)", "FALSE"), new Choice("All Pairs (Limited Included)", "TRUE"));
	}
	public static AcquisitionCondition of(boolean everyone) { return new AcquisitionCondition(everyone); }
	@Override
	public String getEmoteText() {
		return everyone
				? BotType.getPokemonInstance().getImages().getEmoteText("ACQUISITION_EVERYONE") + " All Pairs (Limited Included)"
				: BotType.getPokemonInstance().getImages().getEmoteText(Acquisition.MainStory.getEmoteText()) + " Permanent Pairs (General Pool/Story/BP)";
	}
	@Override
	public boolean eval(PairBox sp) {
		if(everyone)
			return true;
		return sp.getPair().getAcquisition().stream().anyMatch(
				a -> Arrays.asList(Acquisition.General, Acquisition.MainStory, Acquisition.BattlePoints, Acquisition.Egg, Acquisition.TrainerLodge, Acquisition.ScoutTicket,
						Acquisition.KantoTicket, Acquisition.JohtoTicket, Acquisition.HoennTicket, Acquisition.SinnohTicket,
						Acquisition.UnovaTicket, Acquisition.KalosTicket, Acquisition.AlolaTicket, Acquisition.GalarTicket).contains(a));
	}
}
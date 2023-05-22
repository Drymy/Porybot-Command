package com.porybot.commands.pokemon.search;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.porybot.GameElements.Move;
import com.porybot.GameElements.Passive;
import com.porybot.GameElements.SyncPair;

public class PairBox {
	private SyncPair pair;
	private List<Move> moveList;
	private List<Passive> passiveList;
	
	public PairBox(SyncPair sp) { pair = sp; }
	
	public SyncPair getPair() { return pair; }
	public List<Move> getRealFinalMoveList() { return moveList; }
	public List<Passive> getRealFinalPassiveList() { return passiveList; }
	public List<Move> getFinalMoveList() { return moveList == null ? Arrays.asList() : moveList; }
	public List<Passive> getFinalPassiveList() { return passiveList == null ? Arrays.asList() : passiveList; }
	public List<Move> getMoveList() { return moveList == null ? moveList = pair.getAllMoves() : moveList; }
	public List<Passive> getPassiveList() { return passiveList == null ? passiveList = pair.getAllPassives() : passiveList; }

	public void filterMoves(List<Move> moves) {
		moveList = !moveList.isEmpty() ? moveList.stream().distinct().filter(moves::contains).collect(Collectors.toList()) : moves;
	}
	public void filterPassives(List<Passive> passives) {
		passiveList = !passiveList.isEmpty() ? passiveList.stream().distinct().filter(passives::contains).collect(Collectors.toList()) : passives;
	}
}
package com.github.kilianB.gameManager;

/**
 * @author Kilian
 *
 */
public enum PickBanStrategy {
	/** Blind pick */
	BlindPick(1, "SkipBanStrategy", "SimulPickStrategy"),
	DraftPick(2, "StandardBanStrategy", "DraftModeSinglePickStrategy"),
	Random(4, "SkipBanStrategy", "AllRandomPickStrategy"),
	TournamentDraft(6, "TournamentBanStrategy", "TournamentPickStrategy");

	public int id;
	public String banStrategyName;
	public String pickStrategyName;

	private PickBanStrategy(int id, String banStrategyName, String pickStrategyName) {
		this.id = id;
		this.banStrategyName = banStrategyName;
		this.pickStrategyName = pickStrategyName;
	}
}

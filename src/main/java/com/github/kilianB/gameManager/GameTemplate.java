package com.github.kilianB.gameManager;

import com.github.kilianB.dtos.MapID;
import com.github.kilianB.dtos.SpectatorPolicy;

public class GameTemplate {

	public MapID map;
	public int teamSize;
	public SpectatorPolicy spectatorPolicy;
	public PickBanStrategy pickBan;
	
	//TODO differentiate between custom game and lobby
	
	private GameTemplate(Builder builder) {
		this.map = builder.map;
		this.teamSize = builder.teamSize;
		this.spectatorPolicy = builder.spectatorPolicy;
		this.pickBan = builder.pickBan;
	}
	/**
	 * Creates builder to build {@link GameTemplate}.
	 * @return created builder
	 */
	public static IMapStage builder() {
		return new Builder();
	}
	public interface IMapStage {
		public ITeamSizeStage withMap(MapID map);
	}
	public interface ITeamSizeStage {
		public ISpectatorPolicyStage withTeamSize(int teamSize);
	}
	public interface ISpectatorPolicyStage {
		public IPickBanStage withSpectatorPolicy(SpectatorPolicy spectatorPolicy);
	}
	public interface IPickBanStage {
		public IBuildStage withPickBan(PickBanStrategy pickBan);
	}
	public interface IBuildStage {
		public GameTemplate build();
	}
	/**
	 * Builder to build {@link GameTemplate}.
	 */
	public static final class Builder
			implements IMapStage, ITeamSizeStage, ISpectatorPolicyStage, IPickBanStage, IBuildStage {
		private MapID map;
		private int teamSize;
		private SpectatorPolicy spectatorPolicy;
		private PickBanStrategy pickBan;

		private Builder() {
		}

		@Override
		public ITeamSizeStage withMap(MapID map) {
			this.map = map;
			return this;
		}

		@Override
		public ISpectatorPolicyStage withTeamSize(int teamSize) {
			this.teamSize = teamSize;
			return this;
		}

		@Override
		public IPickBanStage withSpectatorPolicy(SpectatorPolicy spectatorPolicy) {
			this.spectatorPolicy = spectatorPolicy;
			return this;
		}

		@Override
		public IBuildStage withPickBan(PickBanStrategy pickBan) {
			this.pickBan = pickBan;
			return this;
		}

		@Override
		public GameTemplate build() {
			return new GameTemplate(this);
		}
	}

	
	
	
	
}
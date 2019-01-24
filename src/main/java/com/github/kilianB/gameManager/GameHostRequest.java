package com.github.kilianB.gameManager;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.kilianB.chatBot.chatRooms.LobbyRequester;
import com.github.kilianB.gameManager.lobbies.GameLobby;

/**
 * @author Kilian
 *
 */
public class GameHostRequest {

	private static final AtomicInteger idProvider = new AtomicInteger();
	
	private int uniqueId;
	// May be empty. Its in the responsibility for the hoster to choose a unique
	// name
	private String lobbyName;
	// May be empty.
	private String lobbyPassword;

	private GameTemplate template;
	
	private Class<? extends GameLobby> gameLobbyType;
	
	private LobbyRequester requester;

	
	private GameHostRequest(Builder builder) {
		this.template = builder.template;
		this.requester = builder.requester;
		this.uniqueId = idProvider.getAndIncrement();
		this.lobbyName = builder.lobbyName;
		this.lobbyPassword = builder.lobbyPassword;
		this.gameLobbyType = builder.gameLobbyType ;
	}

	/**
	 * @return the uniqueId
	 */
	public int getUniqueId() {
		return uniqueId;
	}

	/**
	 * @return the lobbyName
	 */
	public String getLobbyName() {
		return lobbyName;
	}

	/**
	 * @param lobbyName the lobbyName to set
	 */
	public void setLobbyName(String lobbyName) {
		this.lobbyName = lobbyName;
	}

	/**
	 * @return the lobbyPassword
	 */
	public String getLobbyPassword() {
		return lobbyPassword;
	}

	/**
	 * @param lobbyPassword the lobbyPassword to set
	 */
	public void setLobbyPassword(String lobbyPassword) {
		this.lobbyPassword = lobbyPassword;
	}

	/**
	 * @return the template
	 */
	public GameTemplate getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(GameTemplate template) {
		this.template = template;
	}

	/**
	 * @return the requester
	 */
	public LobbyRequester getRequester() {
		return requester;
	}

	/**
	 * @param requester the requester to set
	 */
	public void setRequester(LobbyRequester requester) {
		this.requester = requester;
	}
	
	

	/**
	 * @return the gameLobbyType
	 */
	public Class<? extends GameLobby> getGameLobbyType() {
		return gameLobbyType;
	}

	/**
	 * Creates builder to build {@link GameHostRequest}.
	 * @return created builder
	 */
	public static ITemplateStage builder(LobbyRequester requester) {
		return new Builder(requester);
	}

	public interface ITemplateStage {
		public ILobbyTypeStage withTemplate(GameTemplate template);
	}

	public interface ILobbyTypeStage{
		public IBuildStage withLobbyType(Class<? extends GameLobby> lobbyClass);
	}

	public interface IBuildStage {
		
		
		
		public IBuildStage withLobbyName(String lobbyName);

		public IBuildStage withLobbyPassword(String lobbyPassword);

		public GameHostRequest build();
	}

	/**
	 * Builder to build {@link GameHostRequest}.
	 */
	public static final class Builder implements ITemplateStage, ILobbyTypeStage, IBuildStage {
		public Class<? extends GameLobby> gameLobbyType;
		private GameTemplate template;
		private LobbyRequester requester;
		private String lobbyName;
		private String lobbyPassword;

		private Builder(LobbyRequester requester) {
			this.requester = requester;
		}

		@Override
		public ILobbyTypeStage withTemplate(GameTemplate template) {
			this.template = template;
			return this;
		}

		@Override
		public IBuildStage withLobbyName(String gameName) {
			this.lobbyName = gameName;
			return this;
		}

		@Override
		public IBuildStage withLobbyPassword(String lobbyPassword) {
			this.lobbyPassword = lobbyPassword;
			return this;
		}

		@Override
		public IBuildStage withLobbyType(Class<? extends GameLobby> lobbyClass) {
			this.gameLobbyType = lobbyClass;
			return this;
		}
		
		@Override
		public GameHostRequest build() {
			return new GameHostRequest(this);
		}

		
		
	}

}

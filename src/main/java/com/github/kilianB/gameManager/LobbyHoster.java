package com.github.kilianB.gameManager;

import java.io.IOException;
import java.util.List;

import com.github.kilianB.dtos.Summoner;
import com.github.kilianB.gameManager.lobbies.GameLobby;

/**
 * @author Kilian
 *
 */
public abstract class LobbyHoster {
	
	protected GameLobby currentLobby;
//	
	public abstract void host(GameHostRequest gameHostRequest) throws IOException;
	
	public abstract void moveToSpectate() throws IOException;

	/**
	 * @param summonerToInvite the summoner toInvite
	 */
	public abstract void inviteSummoner(Summoner summonerToInvite) throws IOException;

	public abstract void startGame() throws IOException;
	
	public abstract void disbandLobby() throws IOException;
	

	/**
	 * @param sum
	 * @return 
	 * @throws IOException 
	 */
	public abstract List<Integer> getChampionsOfSummoner(Summoner sum) throws IOException;

	/**
	 * @param sum
	 * @return 
	 * @throws IOException 
	 */
	public abstract List<Integer> getChampionsOfSummoner(long sumId) throws IOException;

	

	/**
	 * @param chatId 
	 * @throws IOException 
	 * 
	 */
	public abstract void writeInLobby(String message, String chatId) throws IOException;


}

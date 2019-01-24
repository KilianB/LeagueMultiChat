package com.github.kilianB.gameManager.lobbies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.kilianB.chatBot.chatRooms.LobbyRequester;
import com.github.kilianB.dtos.Summoner;
import com.github.kilianB.gameManager.GameTemplate;
import com.github.kilianB.gameManager.LobbyHoster;
import com.github.kilianB.util.DaemonThreadFactory;

/**
 * @author Kilian
 *
 */
public abstract class GameLobby {

	public static final AtomicInteger idProvider = new AtomicInteger();

	// Name of the game lobby used as reference when notyfing the player
	protected String name;
	protected long lobbyId;
	protected GameTemplate template;
	protected LobbyHoster lobbyHoster;
	
	//Chatroom
	protected String xmppChatId;
	protected String xmppChatPassword;
	

	protected List<Summoner> team0 = new ArrayList<>();
	protected List<Summoner> team1 = new ArrayList<>();
	protected List<Summoner> spectators = new ArrayList<>();

	protected List<Summoner> invited = new ArrayList<>();

	protected boolean hosterInSpectate = false;

	private ScheduledExecutorService scheduledExecutor = Executors
			.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
	
	/**
	 * @param name
	 * @param template
	 * @param lobbyHoster
	 */
	public GameLobby(String name, GameTemplate template, LobbyHoster lobbyHoster,String xmppChatId, String xmppChatPassword) {
		super();
		this.name = name;
		this.template = template;
		this.lobbyHoster = lobbyHoster;
		this.lobbyId = idProvider.getAndIncrement();
		this.xmppChatId = xmppChatId;
		this.xmppChatPassword = xmppChatPassword;
	}


	public void updateLobbyMembers(List<Summoner> team0, List<Summoner> team1, List<Summoner> spectators) throws IOException {
		this.team0 = team0;
		this.team1 = team1;
		this.spectators = spectators;

		int sumsInLobby = team0.size() + team1.size();

		if (sumsInLobby >= 7 && !hosterInSpectate) {
			//TODO handle this. we need to make sure people are all invited.
			lobbyHoster.moveToSpectate();
		}

		// We have enough members lets start the game.
		if (sumsInLobby == template.teamSize) {
			// The lobby gets disbanded as soon as we are in spectator and the last person
			// leaves.

		}

	}

	/**
	 * Invites a summoner into the lobby
	 * 
	 * @param timeoutInSeconds     the number of seconds until the invite becomes
	 *                             invalid and will be revoked
	 * @param invalidationListener
	 * @return true if the summoner got invited, false if the lobby is full
	 * @throws IOException 
	 */
	public boolean inviteSummoner(Summoner summonerToInvite, int timeoutInSeconds, LobbyRequester providerCallback) throws IOException {
		if (isFull()) {
			return false;
		} else {
			lobbyHoster.inviteSummoner(summonerToInvite);
			
			// Check if the summoner accepted the invitation
			scheduledExecutor.schedule(() -> {
				if (!team0.contains(summonerToInvite) && !team1.contains(summonerToInvite)) {
					providerCallback.requestSummoner(this);
					try {
						summonerToInvite.sendMessageAdmin("You did not accept the gamerequest for " + name + " "
								+ lobbyId + ". removed from queue");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, timeoutInSeconds, TimeUnit.SECONDS);
			return true;
		}
	}

	/**
	 * @return
	 */
	public boolean isFull() {
		return (summonersInLobbyExcludingSpectate() / 2d) >= template.teamSize;
	}

	public int summonersInLobbyExcludingSpectate() {
		return team0.size() + team1.size();
	}

	public void shutdown() {
		scheduledExecutor.shutdown();
	}
	
	public final void writeMessageToChat(String message) throws IOException {
		lobbyHoster.writeInLobby(message,xmppChatId);
	}
	

	/*
	 * @param summonerId
	 * @param teamId
	 */
	public abstract void memberUpdate(Summoner summoner, Integer teamId);

}

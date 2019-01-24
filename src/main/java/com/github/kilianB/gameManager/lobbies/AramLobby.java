package com.github.kilianB.gameManager.lobbies;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.kilianB.dtos.Summoner;
import com.github.kilianB.gameManager.GameTemplate;
import com.github.kilianB.gameManager.LobbyHoster;

/**
 * @author Kilian
 *
 */
public class AramLobby extends GameLobby {

	protected Map<Long, List<Integer>> availableChampions = new HashMap<>();

	protected Set<Integer> champsOwnedByEveryone = new HashSet<>();

	/**
	 * @param name
	 * @param template
	 * @param lobbyHoster
	 */
	public AramLobby(String name, GameTemplate template, LobbyHoster lobbyHoster, String xmppChatId,
			String xmppChatPassword) {
		super(name, template, lobbyHoster, xmppChatId, xmppChatPassword);
	}

	@Override
	public void memberUpdate(Summoner summoner, Integer teamId) {

		long summonerId = summoner.summonerId();
		
		System.out.println("Member update: " + summonerId + " Team:" + teamId);
		// TODO he left?
		if (teamId == -1) {
			try {
				summonerLeft();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (!availableChampions.containsKey(summonerId)) {
				summonerJoined(summonerId);
			} else {
				// He simply changed teams. Do we need to handle spectator role?
			}
		}

	}

	protected void summonerJoined(long sumId) {
		// Estimate the champions a summoner has. All champs with a mastery level > 3

		try {
			updateAvailableChampions(sumId);
			writeMessageToChat("Debug: available champs by everyone: " + champsOwnedByEveryone);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Debug print available champs
		// TODO Do we constrain the champs on one team or do all players have to have
		// the same champs?
	}

	protected void summonerLeft() throws IOException {
		// Recalculate available champs
		champsOwnedByEveryone.clear();
		for (Summoner sum : this.team0) {
			updateAvailableChampions(sum.summonerId());
		}
		for (Summoner sum : this.team1) {
			updateAvailableChampions(sum.summonerId());
		}

	}

	private void updateAvailableChampions(long sumId) throws IOException {
		List<Integer> ownedChamps = lobbyHoster.getChampionsOfSummoner(sumId);
		availableChampions.put(sumId, ownedChamps);

		// TODO rebuild on leave
		if (champsOwnedByEveryone.isEmpty()) {
			champsOwnedByEveryone.addAll(ownedChamps);
		} else {
			champsOwnedByEveryone.retainAll(ownedChamps);

			if (champsOwnedByEveryone.isEmpty()) {
				// Oh oh.
				// TODO handle
				this.writeMessageToChat("Upps: No suitable champ combination found...");
			}

		}
	}

}

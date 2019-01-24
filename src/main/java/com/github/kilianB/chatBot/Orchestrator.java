package com.github.kilianB.chatBot;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.kilianB.MiscUtil;
import com.github.kilianB.StringUtil;
import com.github.kilianB.chatBot.chatRooms.ChatRoom;
import com.github.kilianB.chatBot.chatRooms.LookingForGroupChatRoom;
import com.github.kilianB.dtos.ChatRoomEntry;
import com.github.kilianB.dtos.Summoner;
import com.github.kilianB.dtos.SummonerMessageSetting;
import com.github.kilianB.gameManager.GameHostRequest;
import com.github.kilianB.gameManager.LobbyHoster;
import com.sun.management.OperatingSystemMXBean;

/**
 * Chat and logic orchestrator. Does not directly connect to league but serves
 * as central entity coordinating the different accounts and defining the
 * behavior of the overall system.
 * 
 * Tasks are propagated to {@link com.github.kilianB.chatBot.SlaveBot SlaveBots}
 * which represent a connection to the league ecosystem. It's up to the
 * individuals to keep the friend list in a consistent state.
 * 
 * @author Kilian
 * 
 * @see https://github.com/kilianB/MultiChat
 */
public class Orchestrator {

	private static final Logger LOGGER = Logger.getLogger(Orchestrator.class.getSimpleName());

	/*
	 * Global Chat Commands
	 */

	/** List all available public chat rooms */
	private final String LIST_ROOMS = "!rooms";
	/** Mute an individual player or show which players are muted */
	private final String MUTE_PLAYER = "!mute";
	/** Unmute an individual player */
	private final String UNMUTE_PLAYER = "!unmute";
	/** Join or create a chat room */
	private final String JOIN_ROOM = "!join";
	/** Set or show the current chat setting. When shall the bot send a message */
	private final String CHAT_VISIBILITY = "!mode";
	/** Display chat command help */
	private final String HELP = "!help";

	/*
	 * Settings
	 */

	/**
	 * Give the room name a length limit so !rooms does not destroy the entire
	 * "layout" if someone is funny and decides to put a 300 character name room
	 * into existence.
	 */
	private final int roomNameLengthLimit = 25;

	/*
	 * Internal state
	 */

	/** A dummy account claiming all fixed public chat rooms. */
	private final Summoner dummyAdmin;

	/** List of all league accounts registered by this bot */
	private List<SlaveBot> slaves = new ArrayList<SlaveBot>();

	/** All summoners associated with the bot mapped to it's summoner id. */
	private HashMap<Long, Summoner> summonerMap = new HashMap<>();

	/** Chat room mapped to it's name representation for fast retrieval */
	private HashMap<String, ChatRoom> rooms = new LinkedHashMap<>();

	/** Look for room creation and deletion */
	private Lock roomLock = new ReentrantLock();

	/** Flag indicating when the room cache needs to be recomputed */
	private boolean roomsInvalidated = true;

	/** Cache for !rooms request. True sticky rooms, false custom roomss */
	private Map<Boolean, List<ChatRoom>> filteredRoomCache;

	/** Lobbys waiting to be hosted */
	private LinkedBlockingQueue<GameHostRequest> gameHostRequets = new LinkedBlockingQueue<>();

	// Keep a weak or phantom reference to game queues?

	// TODO a client has to take an object from the lobby queue

	private String welcomeMessage = "Beep Boop. Welcome to the league multi bot. To get started type !help.\n"
			+ "An extended manual can be found here: https://github.com/KilianB/LeagueMultiChat . If you have any feature requests or bug reports please open an issue "
			+ "at github and/or add Patex (EUW). ";

	private String helpMessage;
	{
		helpMessage = new StringBuilder().append("Available commands:\n").append(HELP).append(
				": Display this help. Typing the command inside a room may display room specific commands \n----\n")
				.append(LIST_ROOMS).append(": Display all public chatrooms\n ---- \n").append(JOIN_ROOM)
				.append(" [roomname] (password) : Join a room or create it if it doesn't exist. If the room is created grant admin permission. If password is ommited the room will be public. Leaves the current room you are in. \n ---- \n")
				.append(MUTE_PLAYER).append(" : Display all currently muted players \n ---- \n").append(MUTE_PLAYER)
				.append(" [playername]: No longer recieve messages send by this player\n ---- \n").append(UNMUTE_PLAYER)
				.append(" [playername]: Remove the player from the mute list.\n ---- \n").append(CHAT_VISIBILITY)
				.append(": Display the current chat mode\n ---- \n").append(CHAT_VISIBILITY)
				.append(" [chat/away/dnd/mobile/offline]: Set the current chatmode. \n \n").toString();
	}

	/**
	 * Summoner id mapped to the display name of summoner (don't have to be
	 * connected to the bot e.g. in case of bans and mutes).
	 */
	// private HashMap<Long, String> summonerNameCache = new HashMap<>();
	// TODO bidi map

	// Settings

	public Orchestrator() {
		dummyAdmin = new Summoner("Admin", -101, null);
		// Register default chatrooms
		//Not entirely ready yet
		registerChatRoomAdmin(new LookingForGroupChatRoom("Lfg","",dummyAdmin,this));
		//registerChatroomAdmin("Lfg", "");
		registerChatroomAdmin("Offtopic", "");
	}

	/**
	 * Adds a slave bot to the chat admin.
	 * 
	 * <p>
	 * Once a slave bot is registered friend requests may be propagated to the slave
	 * as well as it's expected to take care of lobby creation and invitational
	 * duties for custom game modes.
	 * 
	 * @param slave may not be null
	 */
	public void registerSlaveBot(SlaveBot slave) {
		if (!slaves.contains(slave)) {
			slaves.add(slave);
			slave.registerAdmin(this);
		}
	}

	/**
	 * Prototype method useful to implement blocking of spam content at a global
	 * level e.g. links to elo boosting services, account selling advertisement.
	 * 
	 * @implnote As long as no abuse is seen this method will be a NOP to improve
	 *           performance otherwise benchmark which implementation is performant
	 *           and scalable.
	 * 
	 * @param input to check if the string contains malicious content
	 * @return true if flagged content was found. false if not.
	 */
	private boolean containsBlockedContent(String input) {

		/*
		 * @formatter:off
		 * 
	
		 * A note to everyone considering to spam: Be my guest and try, but make sure
		 * that it's worth your effort. You even get to see the code used to combat
		 * malicious activity. I enjoy this arm race and game of cat and mouse since my
		 * library currently lacks utility methods to handle these cases this will be a
		 * great opportunity to work on them.
		 * 
		 * ---------------------
		 * 
		 * - Links:
		 * 
		 * 	If the url contains uncommon words simply search and block the entire host of
		 * 	the url. (discard scheme, path...) If this is not possible match the broader
		 * 	url signature. Combat shenanigans by some use of fuzzy string matching e.g.
		 * 	levenshtein distances.
		 * 	
		 * 	Some worthy considerations are 
		 * 		BitAp https://en.wikipedia.org/wiki/Bitap_algorithm
		 * 		FuzzyWuzzy https://github.com/xdrop/fuzzywuzzy
		 * 		Lucene probably overkill, but it's already present in the h2 depenency if we use SQL databases
		 * 	
		 *  - Build the blocklist by introducing a !report command and cross reference messages.
		 *  - Use github/issues to maintain a public blocklist which will be updated consistently
		 * 
		 * Use a CircularHashSet to keep track of the last x messages send by a user (or a count hashset)
		 * to keep track of global messages and flag users which post similar messages. cross reference 
		 * with bans and mutes.
		 *
		 * Some performance optimization
		 *  	Build a ngram index (tree) for quicker matching.
		 * 		We are working with a limited document length maybe it's possible to extract dominant features
		 * 		and feed a bloom filter before computing heavy metrics. 
		 *
		 * If everything else not work use regex to disable links all together (mask them?) or shut 
		 * down the bot.
		 *
		 * For "bad word" detection we could hook the riot internal sanitizer plugin, but since users
		 * are free to turn it on by themselves which will also sanitize the messages send by this bot 
		 * it's probably just an additional unnecessary layer.
		 *
		 *@formatter:on
		 */
		// Not related but still want to try it out somewhere:
		// https://dzone.com/articles/super-fast-estimates-of-levenshtein-distance
		return false;
	}

	/**
	 * Process a text message received by any of the slaves. Text entries are either
	 * expected to contain commands "!xxx" or text messages which will be send to
	 * all other users in the same channel
	 * 
	 * @param fromId The summoner who send this text message
	 * @param text   the content body of the text
	 * @throws IOException if an error occurs interacting with the lol api
	 */
	public void propergateMessage(long fromId, String text) throws IOException {

		// Get the summoner who send this message
		Summoner initiator = summonerMap.get(fromId);
		assert initiator != null;

		// Sanitize message

		// Trim additional whitespaces
		text = text.trim();

		if (!containsBlockedContent(text)) {
			// Parse text message
			if (text.startsWith(LIST_ROOMS)) {
				handleListRooms(initiator);
			} else if (text.startsWith(HELP)) {
				handleShowHelp(initiator);
			} else if (text.startsWith(JOIN_ROOM)) {
				handleJoinRoom(text, initiator);
			} else if (text.startsWith(MUTE_PLAYER)) {
				handleMutePlayer(text, initiator);
			} else if (text.startsWith(UNMUTE_PLAYER)) {
				handleUnmutePlayer(text, initiator);
			} else if (text.startsWith(CHAT_VISIBILITY)) {
				handleSetChatMode(text, initiator);
			} else {
				// Requires being inside a room
				ChatRoom activeRoom = initiator.getActiveChatroom();
				if (activeRoom != null) {
					activeRoom.handleMessage(text, initiator);
				} else {
					initiator.sendMessageAdmin(
							"Can not perform request. You need to be part of a chatroom to issue specific commands. Did you misstype your request?");
				}
			}
		} else {
			initiator.sendMessageAdmin("Failed to send message. Part of the message are part of the blocklist.");
		}

	}

	/**
	 * Received a query or set of chat mode availability. The char mode dictates
	 * when the bot is allowed to send summoners a message. This is necessary due to
	 * the fact that we don't want the bot to spam the ingame chat.
	 * 
	 * @param text      the text query send
	 * @param initiator the summoner who send the request
	 * @throws IOException if an error occurs interacting with the lol api
	 */
	private void handleSetChatMode(String text, Summoner initiator) throws IOException {

		String newM = text.substring(CHAT_VISIBILITY.length()).trim();

		if (!newM.isEmpty()) {
			try {
				SummonerMessageSetting newMode = SummonerMessageSetting.valueOf(newM.toUpperCase());
				initiator.setChatMode(newMode);
			} catch (IllegalArgumentException e) {
				initiator.sendMessageAdmin("Failed to set new chat state. Allowed values: "
						+ Arrays.toString(SummonerMessageSetting.values()));
			}
		} else {
			initiator.sendMessageAdmin("Current Mode: " + initiator.getChatMode());
		}

	}

	/**
	 * Received a request to unmute a certain summoner. Mute behaviour is player
	 * bound and only reflects changed for the requesting summoner.
	 * 
	 * @param text      the text query send
	 * @param initiator the summoner who send the request
	 * @throws IOException if an error occurs interacting with the lol api
	 */
	private void handleUnmutePlayer(String text, Summoner initiator) throws IOException {
		String unmutePlayerName = text.substring(UNMUTE_PLAYER.length()).trim();

		SlaveBot bot = initiator.getBot();

		// Get the id of the player
		if (!unmutePlayerName.isEmpty() && text.indexOf(" ") != -1) {
			long sToUnMute = bot.getSummoner(unmutePlayerName).summonerId();

			if (initiator.unmuteSummoner(sToUnMute)) {
				initiator.sendMessageAdmin(unmutePlayerName + " sucessfully unmuted");
			} else {
				initiator
						.sendMessageAdmin("Failed to unmute: " + unmutePlayerName + " player wasn't previously muted.");
			}
		} else {
			initiator.sendMessageAdmin("No player name supplied. Could not unmute anyone");
		}
	}

	/**
	 * Received a request to mute a certain summoner or display all summoners
	 * currently muted. Mute behaviour is player bound and only reflects changed for
	 * the requesting summoner.
	 * 
	 * @param text      the text query send
	 * @param initiator the summoner who send the request
	 * @throws IOException if an error occurs interacting with the lol api
	 */
	private void handleMutePlayer(String text, Summoner initiator) throws IOException {
		String mutePlayerName = text.substring(MUTE_PLAYER.length()).trim();

		SlaveBot bot = initiator.getBot();

		// Get the id of the player

		System.out.println(mutePlayerName + " " + mutePlayerName.isEmpty() + " " + mutePlayerName.indexOf(" "));

		if (!(mutePlayerName.isEmpty() || text.indexOf(" ") == -1)) {
			/*
			 * We need to get via the bot since the summoner may not be currently connected.
			 * This is really permissive as we also allow to mute players who are not even
			 * connected, but this also prevents users from quickly leaving and being unable
			 * to mute them.
			 */
			Summoner sToMute = bot.getSummoner(mutePlayerName);

			if (!sToMute.equals(initiator)) {
				initiator.addMutedSummoner(sToMute);
				initiator.sendMessageAdmin(mutePlayerName + " sucessfully muted");
			} else {
				initiator.sendMessageAdmin("Yikes: You can't mute yourself silly!");
			}
		} else {

			Set<Long> mutedSums = initiator.getMutedIds();
			StringBuilder mutedPlayers = new StringBuilder("Muted Players:\n");

			for (long id : mutedSums) {
				mutedPlayers.append("\t").append(summonerMap.get(id).getDisplayName()).append("\n");
			}
			initiator.sendMessageAdmin(mutedPlayers.toString());
		}
	}

	/**
	 * Handle the request to join a room. If the room does not exist create it and
	 * be promoted to being channel admin.
	 * 
	 * <p>
	 * The patter expected is !join roomname [optional_password]
	 * 
	 * @implnote currently we limited users to only ever being part of a single
	 *           room. Joining a room successfully means leaving another.
	 * 
	 * @param text      the text query send
	 * @param initiator the summoner who send the request
	 * @throws IOException if an error occurs interacting with the lol api
	 */
	private void handleJoinRoom(String text, Summoner initiator) throws IOException {
		String roomName = text.substring(JOIN_ROOM.length()).trim();
		String password = "";
		int lastWhitespace = roomName.lastIndexOf(" ");
		if (lastWhitespace > 0) {
			password = roomName.substring(lastWhitespace + 1);
			roomName = roomName.substring(0, lastWhitespace);
		}

		if (roomName.isEmpty()) {
			initiator.sendMessageAdmin("Failed. Room name may not be empty");
			return;
		}

		if (roomName.length() > roomNameLengthLimit) {
			initiator.sendMessageAdmin(
					"Failed. Room names may have a maximum length of " + roomNameLengthLimit + " characters");
			return;
		}

		switch (joinChatroom(roomName, password, initiator)) {
		case DOES_NOT_EXIST:
			initiator.sendMessageAdmin("Room: " + roomName + " did not exist. Created and joined.\n"
					+ StringUtil.centerText(roomName, "-", 80));
			break;
		case SUCCESS:
			initiator.sendMessageAdmin(StringUtil.centerText(roomName, "-", 80));
			break;
		case INCORRECT_PASSWORD:
			initiator.sendMessageAdmin("Failed to join: " + roomName + " wrong password.");
			break;
		case BANNED:
			initiator.sendMessageAdmin("Failed to join: " + roomName + ". You are banned");
			break;
		}
	}

	/**
	 * Handle a request a user asking to list all public chat rooms.
	 * 
	 * @param initiator the summoner who requested the information.
	 * @throws IOException
	 */
	private void handleListRooms(Summoner initiator) throws IOException {
		StringBuilder roomBuilder = new StringBuilder("Chatrooms:\n");

		try {
			roomLock.lock();
			if (roomsInvalidated) {
				filteredRoomCache = rooms.values().stream().filter(c -> !c.isProtected())
						.collect(Collectors.groupingBy(room -> room.isFixed));
				roomsInvalidated = false;
			}
		} finally {
			roomLock.unlock();
		}

		// Sort by sticky and by number of people online
		try {
			// Do we run into issues here if summoners leave or join DURING the sorting
			// process?
			// This will probably lead to a IllegalArgumentException. In this case lets not
			// worry
			// about the actual order and simply return the order of the room as they
			// currently are.
			Collections.sort(filteredRoomCache.get(Boolean.TRUE));
			// Only do this if we have public rooms available
			if (filteredRoomCache.containsKey(Boolean.FALSE)) {
				Collections.sort(filteredRoomCache.get(Boolean.FALSE));
			}

		} catch (IllegalArgumentException e) {
			LOGGER.info("Room comperation failed. Room user count changed during sorting. Ignore and procceed");
		}

		// First display fixed rooms
		for (var room : filteredRoomCache.get(Boolean.TRUE)) {
			roomBuilder.append(room.getName()).append(" (").append(room.getSummonerCount()).append(" online)\n");
		}
		roomBuilder.append("---\n");

		// Display custom rooms
		if (filteredRoomCache.containsKey(Boolean.FALSE)) {
			for (var room : filteredRoomCache.get(Boolean.FALSE)) {
				roomBuilder.append(room.getName()).append(" (").append(room.getSummonerCount()).append(" online)\n");
			}
		}

		initiator.sendMessageAdmin(roomBuilder.toString());
	}

	private ChatRoomEntry joinChatroom(String chatroom, String password, Summoner initiator) {
		chatroom = chatroom.toLowerCase();
		// If doesn't exist create one. Else join it
		if (registerChatroom(chatroom, password, initiator)) {
			// Room did not exist and we created it successfully
			return ChatRoomEntry.DOES_NOT_EXIST;
		} else {
			// Room did exist
			return initiator.joinChatroom(rooms.get(chatroom), password);
		}
	}

	private void handleShowHelp(Summoner initiator) throws IOException {

		ChatRoom chatRoom = initiator.getActiveChatroom();
		if (chatRoom != null) {
			initiator.sendMessageAdmin(helpMessage + chatRoom.getHelpMessage());
		}else {
			initiator.sendMessageAdmin(helpMessage);
		}
		
		
	}

	/**
	 * Registers a chat room bound to the user. Once everyone leaves the room is
	 * invalidated
	 * 
	 * @param name     The name of the chatroom
	 * @param password The passwort of the room (may be empty for public rooms)
	 * @param owner    The summoner who will have admin privileges
	 * @return true if the room was sucessfull created, false if a room with the
	 *         name already existed
	 */
	public boolean registerChatroom(String name, String password, Summoner owner) {

		try {
			roomLock.lock();
			if (rooms.containsKey(name.toLowerCase())) {
				return false;
			} else {
				ChatRoom chatRoom = new ChatRoom(name, password, owner, this);
				rooms.put(name.toLowerCase(), chatRoom);
				owner.joinChatroom(chatRoom, password);
				roomsInvalidated = true;
				return true;
			}
		} finally {
			roomLock.unlock();
		}
	}

	/**
	 * Register a fixed chat room which will be claimed by a dummy admin. Fixed chat
	 * rooms do not disappear once empty.
	 * 
	 * @param name     The name of the channel
	 * @param password The password of the channel. may be empty
	 */
	private void registerChatroomAdmin(String name, String password) {
		ChatRoom chatRoom = new ChatRoom(name, "", dummyAdmin, true, this);
		try {
			roomLock.lock();
			rooms.put(name.toLowerCase(), chatRoom);
		} finally {
			roomLock.unlock();
		}
	}
	
	private void registerChatRoomAdmin(ChatRoom room) {
		try {
			roomLock.lock();
			rooms.put(room.getName().toLowerCase(), room);
		} finally {
			roomLock.unlock();
		}
	}

	/**
	 * Delete a chat room
	 * 
	 * @param chatRoom the chat room to delete
	 */
	public void unregisterChatroom(ChatRoom chatRoom) {
		try {
			roomLock.lock();
			rooms.remove(chatRoom.getName().toLowerCase());
			roomsInvalidated = true;
		} finally {
			roomLock.unlock();
		}
	}

	/**
	 * Register a summoner to the bot. Registered summoners can be found by other
	 * slaves.
	 * 
	 * @param summoner The summoner to register
	 * @throws IOException if an error occurs communicating with the league api
	 */
	public void registerSummoner(Summoner summoner) throws IOException {
		summonerMap.put(summoner.summonerId(), summoner);
		summoner.sendMessageAdmin(welcomeMessage);
	}

	/**
	 * A summoner was removed from the friendlist. Probably because he went offline
	 * 
	 * @param id the summoner who went offline.
	 */
	public void propergateSummonerUnfriended(Summoner summoner) {
		summoner.setBot(null);
		System.out.println("Room left: " + summoner.leaveChatRoom());
	}

	/**
	 * Try to distribute the friend request to another slave account. We could call
	 * this an attempt of load balancing between the different accounts...We may
	 * distribute our bots across different computers if desired
	 * 
	 * @implnote How do we proceed if we don't have any friend list space left? We
	 *           can't add a summoner, notify him and remove him again due to
	 *           messages disappearing as soon as you remove someone from the friend
	 *           list.
	 * @param id The summoner id of the requestor
	 * @return false if no space is available on the chat anymore. Currently we do
	 *         not handle this situation.
	 */
	public boolean propagateFriendRequest(long id) {

		// Do some kind of "load balancing". Fill up slaves evenly.
		SlaveBot bestSlave = null;
		int bestSlots = Integer.MIN_VALUE;

		for (SlaveBot slave : slaves) {
			try {
				int availableSlot = slave.getAvaiableFriendSlotLimit();

				if (availableSlot > bestSlots) {
					bestSlots = availableSlot;
					bestSlave = slave;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (bestSlots > SlaveBot.FRIEND_LIST_SAFETY_MARGIN_SOFT_CAP) {
			try {
				System.out.println("Send friend request to : " + id + " " + bestSlave);
				bestSlave.sendFriendRequest(id);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * @param id
	 */
	public void propergateSummonerUnfriended(long id) {
		propergateSummonerUnfriended(summonerMap.get(id));
	}

	/**
	 * Check if the summoner is already registered
	 * 
	 * @param id
	 */
	public boolean isSummonerRegistered(long id) {
		if (summonerMap.containsKey(id)) {
			return summonerMap.get(id).getBot() != null;
		}
		return false;
	}
	

	public Summoner getSummoner(long id) {
		return summonerMap.get(id);
	}

	double nanoToMilis = 1e6;
	double bytesToGib = 1073741824;

	public void statReport() {

		OperatingSystemMXBean sunOSBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		MiscUtil.OS.getOS().toString();

		System.out.println("Guaranteed Heap: " + sunOSBean.getCommittedVirtualMemorySize() / bytesToGib + " GiB");
		System.out.println("RAM: " + sunOSBean.getFreePhysicalMemorySize() / bytesToGib + " GiB");
		System.out.println("Available Swap: " + sunOSBean.getFreeSwapSpaceSize() / bytesToGib + " GiB");
		System.out.println("CPU Utilization: " + sunOSBean.getProcessCpuLoad() * 100 + "%");
		System.out.println("CPU Time: " + sunOSBean.getProcessCpuTime() / nanoToMilis + "ms");
	}

	/**
	 * Send a text message to all summoners currently connected to the chat This
	 * method will be used to
	 * 
	 * @param message The message to send.
	 * @throws IOException
	 */
	public void sendGLobalMessage(String message) throws IOException {
		LOGGER.info("Send global message: " + message);
		for (Summoner s : summonerMap.values()) {
			s.sendMessageAdmin(message);
		}
	}

	/*
	 * Lobby hosting
	 */

	/**
	 * Callback for slave bots or any class which allows lobby hosting to provide
	 * their service.
	 * 
	 * @param lobbyHoster The hoster to anounce availablility
	 */
	public void anounceAvailableToHostLobby(LobbyHoster lobbyHoster) {
		new Thread(() -> {
			try {
				lobbyHoster.host(gameHostRequets.take());
			} catch (IOException | InterruptedException e) {
				LOGGER.warning("Intterupted while awaiting lobby");
				// If interrupted reschedule
				this.anounceAvailableToHostLobby(lobbyHoster);
			}
		}).start();
	}

	/**
	 * Requests a game lobby to be hosted. The lobby will be hosted as soon as a
	 * lobby hoster is available and the requester will be notified via callback.
	 * 
	 * @param gameRequest The request
	 */
	public void hostLobby(GameHostRequest gameRequest) {
		gameHostRequets.add(gameRequest);
	}

}

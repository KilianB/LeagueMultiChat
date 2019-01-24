package com.github.kilianB.dtos;

import static com.github.kilianB.dtos.ChatAvailability.away;
import static com.github.kilianB.dtos.ChatAvailability.chat;
import static com.github.kilianB.dtos.ChatAvailability.dnd;
import static com.github.kilianB.dtos.ChatAvailability.offline;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.kilianB.chatBot.SlaveBot;
import com.github.kilianB.chatBot.chatRooms.ChatRoom;

/**
 * @author Kilian
 *
 */
public class Summoner {

	/**
	 * The display name of the summoner. The display name may not be equivalent to
	 * the internal name used by the riot api. (e.g. after a name change).
	 * 
	 * <p>
	 * Be aware that the display name and login name are different values. The displayed
	 * name may change. Be careful since riot requires different values for different 
	 * api endpoints.
	 */
	private transient String summonerDisplayName;

	/**
	 * The summoner id of this summoner. This value stays consistent throughout the time and
	 * may be used to reference and track summoners.
	 */
	private long id;

	// TODO update upon reloads
	private transient ChatAvailability chatAvailability = ChatAvailability.unknown;

	private SummonerMessageSetting messageMode = SummonerMessageSetting.AWAY;

	// List<ChatRoom> chatrooms = new ArrayList<>();
	private transient ChatRoom activeRoom = null;

	/**
	 * Summoner id's of muted players. Here we work with id's since the summoner may
	 * or may not currently exist in the bot ecosystem but we want to block incoming
	 * messages if he connects at a later stage.
	 */
	private Set<Long> muted = new HashSet<>();

	/**
	 * The client responsible for this summoner. The account this summoner is
	 * represented on this account friend list.
	 * 
	 * @implnote once redundancy backup is implemented this field needs to be
	 *           updated (e.g. in case we loose connectivity to one account)...
	 */
	private transient SlaveBot bot;

	/**
	 * @param owner
	 */
	public Summoner(String summonerName, long summonerId, SlaveBot bot) {
		this.summonerDisplayName = summonerName;
		this.id = summonerId;
		this.bot = bot;
		// TODO this can be awful. try to get the correct availablilty. Maybe change the
		// constructor.
		this.chatAvailability = ChatAvailability.chat;
	}

	public void updateChatAvailablility(ChatAvailability newAvailability) {
		this.chatAvailability = newAvailability;
	}

	/**
	 * Get the summoner id of this summoner
	 * 
	 * @return
	 */
	public long summonerId() {
		return this.id;
	}

	/**
	 * Mute a player for this summoner
	 * 
	 * @param summonerToMute The summoner that will be muted
	 */
	public void addMutedSummoner(Summoner summonerToMute) {
		muted.add(summonerToMute.summonerId());
	}

	/**
	 * Mute a player for this summoner
	 * 
	 * @param summonerToMute The summoner that will be muted
	 */
	public void addMutedSummoner(long sumId) {
		muted.add(sumId);
	}

	public boolean unmuteSummoner(long sumId) {
		return muted.remove(sumId);
	}

	public boolean unmuteSummoner(Summoner summonerToUnmute) {
		return muted.remove(summonerToUnmute.summonerId());
	}

	/**
	 * Get the id's of the muted summoners by this summoner
	 * 
	 * @return the id's of all summoners muted by this summoner
	 */
	public Set<Long> getMutedIds() {
		return muted;
	}

	/**
	 * Send a message to this player
	 * 
	 * @param message
	 * @param initiator
	 * @throws IOException
	 */
	public void sendMessage(String message, Summoner initiator) throws IOException {
		if (bot != null && !isMuted(initiator)) {

			switch (messageMode) {
			case CHAT:
				if (!chatAvailability.equals(chat)) {
					return;
				}
				break;
			case AWAY:
				if (!(chatAvailability.equals(chat) || chatAvailability.equals(away))) {
					return;
				}
				break;
			case INGAME:
				if (!(chatAvailability.equals(chat) || chatAvailability.equals(away) || chatAvailability.equals(dnd))) {
					return;
				}
				break;
			case MOBILE:
				if (chatAvailability.equals(offline)) {
					return;
				}
				break;
			}
			bot.sendMessage(id, message);
		}
	}

	/**
	 * Send a message to this player
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void sendMessageAdmin(String message) throws IOException {
		if (bot != null) {
			bot.sendMessage(id, "/me \n" + message);
		}
	}

	public SlaveBot getBot() {
		return bot;
	}

	/**
	 * 
	 */
	public ChatRoom getActiveChatroom() {
		return activeRoom;
	}

	/**
	 * @return
	 */
	public ChatAvailability getChatActivity() {
		return chatAvailability;
	}

	/**
	 * @return
	 */
	public String getDisplayName() {
		return summonerDisplayName;
	}

	/**
	 * @param chatRoom
	 * @param password
	 * @return
	 */
	public ChatRoomEntry joinChatroom(ChatRoom chatRoom, String password) {
		ChatRoomEntry returnValue = chatRoom.join(this, password);
		if (returnValue.equals(ChatRoomEntry.SUCCESS)) {
			if (activeRoom != null) {
				activeRoom.leave(this);
			}
			// TODO shall we notify the people in the chatroom that someone joined? Probably
			// not
			this.activeRoom = chatRoom;
		}
		return returnValue;
	}

	/**
	 * 
	 */
	public ChatRoom leaveChatRoom() {
		if (activeRoom != null) {
			activeRoom.leave(this);
		}
		ChatRoom temp = activeRoom;
		activeRoom = null;
		return temp;
	}

	/**
	 * @param initiator
	 */
	private boolean isMuted(Summoner initiator) {
		return muted.contains(initiator.summonerId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Summoner other = (Summoner) obj;
		if (id != other.id)
			return false;
		return true;
	}

	/**
	 * @param slaveBot
	 */
	public void setBot(SlaveBot slaveBot) {
		this.bot = slaveBot;
	}

	/**
	 * @param newMode
	 */
	public void setChatMode(SummonerMessageSetting newMode) {
		messageMode = newMode;
	}

	/**
	 * @return
	 */
	public SummonerMessageSetting getChatMode() {
		return messageMode;
	}

	@Override
	public String toString() {
		return "Summoner [id=" + id + ", messageMode=" + messageMode + ", muted=" + muted + "]";
	}
	
	

}

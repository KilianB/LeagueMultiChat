package com.github.kilianB.dtos;

/**
 * Return values when  attempting to join a chatroom
 * @author Kilian
 *
 */
public enum ChatRoomEntry {
	/** Successfully joined the chat room */
	SUCCESS, 
	/** Incorrect password supplied */
	INCORRECT_PASSWORD, 
	/** Summoner was banned from the room and may not enter it */
	BANNED,
	/** The room does not exist. Used at the chat admin level */
	DOES_NOT_EXIST
}
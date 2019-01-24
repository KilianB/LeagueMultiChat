package com.github.kilianB.dtos;

/**
 * @author Kilian
 *
 */
public enum ChatAvailability {
	/**
	 * The summoner is available to chat and logged in (green dot)
	 */
	chat,
	/**
	 * The summoner is away. (red dot)
	 */
	away,
	/**
	 * The summoner is ingame or in champ select. Detailed information about the current
	 * state can be gained by inspecting the "lol" object returned by a query to the league 
	 * service
	 */
	dnd, 
	/**
	 * The summoner is offline but available to chat via mobile
	 */
	mobile, 
	/**
	 * The summoner is offline
	 */
	offline,

	/**
	 * We have no information about this summoner. Not an official lol status but
	 * useful since we might want to map summoners who are not on our friendlist to
	 * represent banned and muted summoners.1
	 */
	unknown;

	// TODO what about mobile?
}

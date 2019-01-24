package com.github.kilianB.dtos;

/**
 * @author Kilian
 *
 */
public enum MapID {
	SR_OLD(1),
	SR_OLD_AUTUM(2),
	TUTORIAL(3),
	TT_OLD(4),
	CRYSTAL_SCAR(5),
	TT(10),
	SR(11),
	ARAM(12),
	ARAM_BUTCHER(14),
	/**
	 * Dark star
	 */
	COSMIC_RUINS(16),
	/**
	 * Star guardian invasion map
	 */
	VALORAN_CITY_PARK(18),
	/**
	 * PROJECT
	 */
	SUBSTRUCTURE_43(19),
	/**
	 * Odyssey
	 */
	CRASH_SITE(20),
	NEXUS_BLITZ(21);
	
	private MapID(int id) {
		this.id = id;
	}
	
	public int id;
	
}

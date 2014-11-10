package net.pferdimanzug.hearthstone.analyzer.game.utils;

import net.pferdimanzug.hearthstone.analyzer.game.GameTag;

public class GameTagUtils {
	
	public static String getTagName(GameTag tag) {
		return tag.toString();
	}

	public static TagValueType getTagValueType(GameTag tag) {
		switch (tag) {
		case ARMOR:
		case ATTACK_BONUS:
		case AURA_ATTACK_BONUS:
		case AURA_HP_BONUS:
		case BASE_ATTACK:
		case BASE_HP:
		case DURABILITY:
		case HP:
		case HP_BONUS:
		case COMBO:
		case DIED_ON_TURN:
		case FATIGUE:
		case MAX_HP:
		case NUMBER_OF_ATTACKS:
		case OVERLOAD:
		case SPELL_AMPLIFY_MULTIPLIER:
		case SPELL_POWER:
		case WEAPON_DAMAGE:
			return TagValueType.INTEGER;
		case ATTACK_EQUALS_HP:
		case CANNOT_ATTACK:
		case CANNOT_REDUCE_HP_BELOW_1:
		case CHARGE:
		case CHOOSE_ONE:
		case CONSUME_DAMAGE_INSTEAD_OF_DURABILITY_ON_MINIONS:
		case COUNTERED:
		case DIVINE_SHIELD:
		case DOUBLE_DEATHRATTLES:
		case ENRAGED:
		case FROZEN:
		case IMMUNE:
		case IMMUNE_WHILE_ATTACKING:
		case INVERT_HEALING:
		case SECRET:
		case SILENCED:
		case STEALTHED:
		case SUMMONING_SICKNESS:
		case TAUNT:
		case UNTARGETABLE_BY_SPELLS:
		case WINDFURY:
			return TagValueType.BOOLEAN;
		case BATTLECRY:
		case DEATHRATTLES:
		case DEBUG:
		case ENRAGE_SPELL:
		case MANA_COST_MODIFIER:
		case RACE:
		case UNIQUE_MINION:
			return TagValueType.OTHER;
		default:
			break;
		}
		return TagValueType.OTHER;
	}
	
	private GameTagUtils() {
		
	}

}
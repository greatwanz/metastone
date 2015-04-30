package net.demilich.metastone.tests;

import java.util.ArrayList;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.GameTag;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.actions.PhysicalAttackAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.IChooseOneCard;
import net.demilich.metastone.game.cards.MinionCard;
import net.demilich.metastone.game.cards.SpellCard;
import net.demilich.metastone.game.cards.concrete.druid.Wrath;
import net.demilich.metastone.game.cards.concrete.neutral.AbusiveSergeant;
import net.demilich.metastone.game.cards.concrete.neutral.AmaniBerserker;
import net.demilich.metastone.game.cards.concrete.neutral.KoboldGeomancer;
import net.demilich.metastone.game.cards.concrete.priest.MindBlast;
import net.demilich.metastone.game.cards.concrete.priest.Thoughtsteal;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.spells.SetHpSpell;
import net.demilich.metastone.game.spells.SilenceSpell;
import net.demilich.metastone.game.spells.desc.SpellDesc;
import net.demilich.metastone.game.targeting.TargetSelection;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AdvancedMechanicTests extends BasicTests {

	@Test
	public void testChooseOne() {
		GameContext context = createContext(HeroClass.DRUID, HeroClass.WARRIOR);
		Player player = context.getPlayer1();
		Player opponent = context.getPlayer2();
		player.getHand().removeAll();

		context.endTurn();
		TestMinionCard minionCard = new TestMinionCard(1, 4);
		playCard(context, opponent, minionCard);
		context.endTurn();

		player.getHero().getHeroPower().setUsed(true);
		Card wrath = new Wrath();
		IChooseOneCard wrathChooseOne = (IChooseOneCard) wrath;
		context.getLogic().receiveCard(player.getId(), wrath);
		player.setMana(wrath.getBaseManaCost() + 1);
		List<GameAction> validActions = context.getLogic().getValidActions(player.getId());
		Assert.assertEquals(validActions.size(), 2);
		Assert.assertEquals(player.getHand().getCount(), 1);

		GameAction playWrath = wrathChooseOne.playOption1();
		playWrath.setTarget(getSingleMinion(opponent.getMinions()));
		context.getLogic().performGameAction(player.getId(), playWrath);

		validActions = context.getLogic().getValidActions(player.getId());
		Assert.assertEquals(validActions.size(), 0);
		Assert.assertEquals(player.getHand().getCount(), 0);
	}

	@Test
	public void testCopyCards() {
		GameContext context = createContext(HeroClass.PRIEST, HeroClass.WARRIOR);
		Player player = context.getPlayer1();
		Player opponent = context.getPlayer2();
		player.getHand().removeAll();

		int cardsInHand = player.getHand().getCount();
		int cardsInOpponentsDeck = opponent.getDeck().getCount();
		Card thoughtsteal = new Thoughtsteal();
		context.getLogic().receiveCard(player.getId(), thoughtsteal);
		context.getLogic().performGameAction(player.getId(), thoughtsteal.play());
		Assert.assertEquals(opponent.getDeck().getCount(), cardsInOpponentsDeck);
		Assert.assertEquals(player.getHand().getCount(), cardsInHand + 2);
	}

	@Test
	public void testDivineShield() {
		GameContext context = createContext(HeroClass.MAGE, HeroClass.WARRIOR);
		Player mage = context.getPlayer1();
		mage.setMana(10);
		Player warrior = context.getPlayer2();
		warrior.setMana(10);

		MinionCard minionCard1 = new TestMinionCard(2, 2, GameTag.DIVINE_SHIELD);
		context.getLogic().receiveCard(mage.getId(), minionCard1);
		context.getLogic().performGameAction(mage.getId(), minionCard1.play());

		MinionCard minionCard2 = new TestMinionCard(5, 5);
		context.getLogic().receiveCard(warrior.getId(), minionCard2);
		context.getLogic().performGameAction(warrior.getId(), minionCard2.play());

		Actor attacker = getSingleMinion(mage.getMinions());
		Actor defender = getSingleMinion(warrior.getMinions());

		GameAction attackAction = new PhysicalAttackAction(attacker.getReference());
		attackAction.setTarget(defender);

		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(attacker.getHp(), attacker.getMaxHp());
		Assert.assertEquals(defender.getHp(), defender.getMaxHp() - attacker.getAttack());
		Assert.assertEquals(attacker.isDead(), false);

		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(attacker.getHp(), attacker.getMaxHp() - defender.getAttack());
		Assert.assertEquals(defender.getHp(), defender.getMaxHp() - attacker.getAttack() * 2);
		Assert.assertEquals(attacker.isDead(), true);
	}

	@Test
	public void testEnrage() {
		GameContext context = createContext(HeroClass.MAGE, HeroClass.PRIEST);
		Player mage = context.getPlayer1();
		mage.setMana(10);
		Player priest = context.getPlayer2();
		priest.setMana(10);

		playCard(context, priest, new AmaniBerserker());

		playCard(context, mage, new TestMinionCard(1, 10));

		Entity attacker = getSingleMinion(mage.getMinions());
		Actor defender = getSingleMinion(priest.getMinions());

		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK);
		Assert.assertEquals(defender.hasStatus(GameTag.ENRAGED), false);

		// attack once, should apply the enrage attack bonus
		GameAction attackAction = new PhysicalAttackAction(attacker.getReference());
		attackAction.setTarget(defender);
		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK + AmaniBerserker.ENRAGE_ATTACK_BONUS);
		Assert.assertEquals(defender.hasStatus(GameTag.ENRAGED), true);
		// attack second time, enrage bonus should not increase
		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK + AmaniBerserker.ENRAGE_ATTACK_BONUS);

		// heal - enrage attack bonus should be gone
		GameAction healAction = priest.getHero().getHeroPower().play();
		healAction.setTarget(defender);
		context.getLogic().performGameAction(priest.getId(), healAction);
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK);
		Assert.assertEquals(defender.hasStatus(GameTag.ENRAGED), false);
	}

	@Test
	public void testOverload() {
		GameContext context = createContext(HeroClass.SHAMAN, HeroClass.WARRIOR);
		Player player = context.getPlayer1();
		int playerId = player.getId();

		context.getLogic().startTurn(playerId);
		Assert.assertEquals(player.getMana(), 1);
		context.getLogic().endTurn(playerId);
		context.getLogic().startTurn(playerId);
		Assert.assertEquals(player.getMana(), 2);

		Card overloadCard = new TestMinionCard(1, 1);
		overloadCard.setTag(GameTag.OVERLOAD, 2);
		context.getLogic().receiveCard(playerId, overloadCard);
		context.getLogic().performGameAction(playerId, overloadCard.play());
		context.getLogic().endTurn(playerId);
		context.getLogic().startTurn(playerId);
		Assert.assertEquals(player.getMana(), 1);

		context.getLogic().endTurn(playerId);
		context.getLogic().startTurn(playerId);
		Assert.assertEquals(player.getMana(), 4);
	}

	@Test
	public void testSetHpPlusSilence() {
		GameContext context = createContext(HeroClass.HUNTER, HeroClass.WARRIOR);
		Player player = context.getPlayer1();
		Player opponent = context.getPlayer2();

		int baseHp = 5;
		// summon a minion and check the base hp
		playCard(context, opponent, new TestMinionCard(4, baseHp));
		Actor minion = getSingleMinion(opponent.getMinions());
		Assert.assertEquals(minion.getHp(), baseHp);

		int modifiedHp = 1;
		// cast a spell on the minion which modifies the hp
		SpellDesc setHpSpell = SetHpSpell.create(modifiedHp);
		SpellCard spellCard = new TestSpellCard(setHpSpell);
		spellCard.setTargetRequirement(TargetSelection.MINIONS);
		context.getLogic().receiveCard(player.getId(), spellCard);
		GameAction playSpellCard = spellCard.play();
		playSpellCard.setTarget(minion);
		context.getLogic().performGameAction(player.getId(), playSpellCard);
		Assert.assertEquals(minion.getHp(), modifiedHp);
		Assert.assertEquals(minion.getMaxHp(), modifiedHp);

		// silence the creature - hp should be back to original value
		SpellDesc silenceSpell = SilenceSpell.create();
		spellCard = new TestSpellCard(silenceSpell);
		spellCard.setTargetRequirement(TargetSelection.MINIONS);
		context.getLogic().receiveCard(player.getId(), spellCard);
		playSpellCard = spellCard.play();
		playSpellCard.setTarget(minion);
		context.getLogic().performGameAction(player.getId(), playSpellCard);
		Assert.assertEquals(minion.getHp(), baseHp);
	}

	@Test
	public void testShorttermBuffs() {
		GameContext context = createContext(HeroClass.MAGE, HeroClass.WARRIOR);
		Player mage = context.getPlayer1();
		mage.setMana(10);
		Player warrior = context.getPlayer2();
		warrior.setMana(10);

		int baseAttack = 1;
		mage.setBehaviour(new Behaviour() {

			@Override
			public String getName() {
				return "Select-First";
			}

			@Override
			public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
				return new ArrayList<Card>();
			}

			@Override
			public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
				System.out.println("Action: " + validActions.get(0));
				return validActions.get(0);
			}

		});

		playCard(context, mage, new TestMinionCard(baseAttack, 1));
		Actor testSubject = getSingleMinion(mage.getMinions());
		Assert.assertEquals(testSubject.getAttack(), baseAttack);

		playCard(context, mage, new AbusiveSergeant());
		Assert.assertEquals(testSubject.getAttack(), baseAttack + AbusiveSergeant.ATTACK_BONUS);
		context.getLogic().endTurn(mage.getId());
		Assert.assertEquals(testSubject.getAttack(), baseAttack);
	}

	@Test
	public void testSpellpower() {
		GameContext context = createContext(HeroClass.PRIEST, HeroClass.WARRIOR);
		Player priest = context.getPlayer1();
		priest.setMana(10);
		Player warrior = context.getPlayer2();
		warrior.setMana(10);

		Assert.assertEquals(warrior.getHero().getHp(), warrior.getHero().getMaxHp());
		SpellCard damageSpell = new MindBlast();
		context.getLogic().receiveCard(priest.getId(), damageSpell);

		context.getLogic().performGameAction(priest.getId(), damageSpell.play());
		Assert.assertEquals(warrior.getHero().getHp(), warrior.getHero().getMaxHp() - MindBlast.DAMAGE);

		MinionCard spellPowerMinionCard = new KoboldGeomancer();
		context.getLogic().receiveCard(priest.getId(), spellPowerMinionCard);
		context.getLogic().performGameAction(priest.getId(), spellPowerMinionCard.play());
		context.getLogic().receiveCard(priest.getId(), damageSpell);
		context.getLogic().performGameAction(priest.getId(), damageSpell.play());
		int spellPower = getSingleMinion(priest.getMinions()).getTagValue(GameTag.SPELL_POWER);
		Assert.assertEquals(warrior.getHero().getHp(), warrior.getHero().getMaxHp() - 2 * MindBlast.DAMAGE - spellPower);
	}
}
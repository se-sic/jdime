

package net.sf.freecol.client.control;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.panel.MonarchPanel;
import net.sf.freecol.client.gui.panel.VictoryPanel;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public final class InGameInputHandler extends InputHandler {

    private static final Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    private Unit lastAnimatedUnit = null;

    
    public InGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }

    
    @Override
    public Element handle(Connection connection, Element element) {
        Element reply = null;

        if (element != null) {
            String type = element.getTagName();

            logger.log(Level.FINEST, "Received message " + type);

            if (type.equals("update")) {
                reply = update(element);
            } else if (type.equals("remove")) {
                reply = remove(element);
            } else if (type.equals("animateMove")) {
                reply = animateMove(element);
            } else if (type.equals("opponentAttack")) {
                reply = opponentAttack(element);
            } else if (type.equals("setCurrentPlayer")) {
                reply = setCurrentPlayer(element);
            } else if (type.equals("newTurn")) {
                reply = newTurn(element);
            } else if (type.equals("setDead")) {
                reply = setDead(element);
            } else if (type.equals("gameEnded")) {
                reply = gameEnded(element);
            } else if (type.equals("chat")) {
                reply = chat(element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(element);
            } else if (type.equals("error")) {
                reply = error(element);
            } else if (type.equals("chooseFoundingFather")) {
                reply = chooseFoundingFather(element);
            } else if (type.equals("indianDemand")) {
                reply = indianDemand(element);
            } else if (type.equals("reconnect")) {
                reply = reconnect(element);
            } else if (type.equals("setAI")) {
                reply = setAI(element);
            } else if (type.equals("monarchAction")) {
                reply = monarchAction(element);
            } else if (type.equals("removeGoods")) {
                reply = removeGoods(element);
            } else if (type.equals("setStance")) {
                reply = setStance(element);
            } else if (type.equals("newConvert")) {
                reply = newConvert(element);
            } else if (type.equals("diplomacy")) {
                reply = diplomacy(element);
            } else if (type.equals("addPlayer")) {
                reply = addPlayer(element);
            } else if (type.equals("spanishSuccession")) {
                reply = spanishSuccession(element);
            } else if (type.equals("addMessages")) {
                reply = addMessages(element);
            } else if (type.equals("addHistory")) {
                reply = addHistory(element);
            } else if (type.equals("multiple")) {
                reply = multiple(connection, element);
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }

            logger.log(Level.FINEST, "Handled message " + type);
        } else {
            throw new RuntimeException("Received empty (null) message! - should never happen");
        }

        return reply;
    }

    
    private Element reconnect(Element element) {
        logger.finest("Entered reconnect...");
        if (new ShowConfirmDialogSwingTask(null, "reconnect.text", "reconnect.yes", "reconnect.no").confirm()) {
            logger.finest("User wants to reconnect, do it!");
            new ReconnectSwingTask().invokeLater();
        } else {
            
            
            logger.finest("No reconnect, quit.");
            getFreeColClient().quit();
        }
        return null;
    }

    
    public Element update(Element updateElement) {
        
        updateGameObjects(updateElement.getChildNodes());
        
        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    
    private void updateGameObjects(NodeList nodeList) {
        Game game = getGame();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = game.getFreeColGameObjectSafely(element.getAttribute("ID"));

            if (fcgo == null) {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
            } else {
                fcgo.readFromXMLElement(element);
            }
        }
    }
    
    
    private Element remove(Element removeElement) {

        NodeList nodeList = removeElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            FreeColGameObject fcgo = getGame().getFreeColGameObject(element.getAttribute("ID"));

            if (fcgo != null) {
                fcgo.dispose();
            } else {
                logger.warning("Could not find 'FreeColGameObject' with ID: " + element.getAttribute("ID"));
            }
        }

        new RefreshCanvasSwingTask().invokeLater();
        return null;
    }

    
    private Element animateMove(Element element) {
        FreeColClient client = getFreeColClient();
        Game game = getGame();
        String unitId = element.getAttribute("unit");
        Unit unit = (Unit) game.getFreeColGameObjectSafely(unitId);
        if (unit == null) {
            if (element.getFirstChild() == null) {
                logger.warning("Animation"
                    + " for: " + client.getMyPlayer().getId()
                    + " incorrectly omitted unit: " + unitId
                    + " from: " + element.getAttribute("oldTile")
                    + " to: " + element.getAttribute("newTile")
                    );
                return null;
            }
            unit = new Unit(game, (Element) element.getFirstChild());
        }
        ClientOptions options = client.getClientOptions();
        boolean ourUnit = unit.getOwner() == client.getMyPlayer();
        String key = (ourUnit) ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        if (!client.isHeadless() && options.getInteger(key) > 0) {
            String oldTileId = element.getAttribute("oldTile");
            Tile oldTile = (Tile) game.getFreeColGameObjectSafely(oldTileId);
            String newTileId = element.getAttribute("newTile");
            Tile newTile = (Tile) game.getFreeColGameObjectSafely(newTileId);
            if (newTile == null || oldTile == null || unit == null) {
                throw new IllegalStateException("animateMove"
                                                + ((unit == null) ? ": null unit" : "")
                                                + ((oldTile == null) ? ": null oldTile" : "")
                                                + ((newTile == null) ? ": null newTile" : "")
                                                );
            }

            
            
            try {
                new UnitMoveAnimationCanvasSwingTask(unit, oldTile, newTile,
                                                     unit != lastAnimatedUnit)
                    .invokeSpecial();
            } catch (Exception exception) {
                logger.warning("UnitMoveAnimationCanvasSwingTask raised "
                               + exception.toString());
            }
            lastAnimatedUnit = unit;
        }
        return null;
    }

    
    private Element animateAttack(Element element) {
        FreeColClient client = getFreeColClient();
        if (client.isHeadless()) return null;
        Game game = getGame();
        Unit unit = (Unit) game.getFreeColGameObjectSafely(element.getAttribute("unit"));
        Unit defender = (Unit) game.getFreeColGameObjectSafely(element.getAttribute("defender"));
        CombatResultType result = Enum.valueOf(CombatResultType.class, element.getAttribute("result"));
        if (unit == null || defender == null) {
            throw new IllegalStateException("animateAttack"
                                            + ((unit == null) ? ": null unit" : "")
                                            + ((defender == null) ? ": null defender" : "")
                                            );
        }

        
        
        try {
            new UnitAttackAnimationCanvasSwingTask(unit, defender, result,
                                                   unit != lastAnimatedUnit)
                .invokeSpecial();
        } catch (Exception exception) {
            logger.warning("UnitAttackAnimationCanvasSwingTask raised "
                           + exception.toString());
        }
        lastAnimatedUnit = unit;
        return null;
    }


    
    private Element opponentAttack(final Element opponentAttackElement) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Unit unit = (Unit) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("unit"));
                    Colony colony = (Colony) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("colony"));
                    Unit defender = (Unit) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("defender"));

                    CombatResultType result = Enum.valueOf(CombatResultType.class, opponentAttackElement.getAttribute("result"));
                    int damage = Integer.parseInt(opponentAttackElement.getAttribute("damage"));
                    int plunderGold = Integer.parseInt(opponentAttackElement.getAttribute("plunderGold"));
                    Location repairLocation = (Location) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("repairIn"));

                    if (opponentAttackElement.hasAttribute("update")) {
                        String updateAttribute = opponentAttackElement.getAttribute("update");
                        if (updateAttribute.equals("unit")) {
                            Element unitElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                            unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                            if (unit == null) {
                                unit = new Unit(getGame(), unitElement);
                            } else {
                                unit.readFromXMLElement(unitElement);
                            }
                            if (unit.getTile() == null) {
                                throw new NullPointerException("unit.getTile() == null");
                            }
                            unit.setLocation(unit.getTile());
                        } else if (updateAttribute.equals("defender")) {
                            final Tile defenderTile = (Tile) getGame().getFreeColGameObjectSafely(opponentAttackElement.getAttribute("defenderTile"));
                            final Element defenderTileElement = Message.getChildElement(opponentAttackElement, Tile
                                    .getXMLElementTagName());
                            if (defenderTileElement != null) {
                                final Tile checkTile = (Tile) getGame().getFreeColGameObject(defenderTileElement.getAttribute("ID"));
                                if (checkTile != defenderTile) {
                                    throw new IllegalStateException("Trying to update another tile than the defending unit's tile.");
                                }
                                defenderTile.readFromXMLElement(defenderTileElement);
                            }
                            Element defenderElement = Message.getChildElement(opponentAttackElement, Unit.getXMLElementTagName());
                            defender = (Unit) getGame().getFreeColGameObject(defenderElement.getAttribute("ID"));
                            if (defender == null) {
                                defender = new Unit(getGame(), defenderElement);
                            } else {
                                defender.readFromXMLElement(defenderElement);
                            }
                            defender.setLocationNoUpdate(defenderTile);
                        } else if (updateAttribute.equals("tile")) {
                            Element tileElement = Message.getChildElement(opponentAttackElement, Tile
                                    .getXMLElementTagName());
                            Tile tile = (Tile) getGame().getFreeColGameObject(tileElement.getAttribute("ID"));
                            if (tile == null) {
                                tile = new Tile(getGame(), tileElement);
                            } else {
                                tile.readFromXMLElement(tileElement);
                            }
                            colony = tile.getColony();
                        } else {
                            throw new IllegalStateException("Unknown update " + updateAttribute);
                        }
                    }

                    if (unit == null && colony == null) {
                        throw new NullPointerException("unit == null && colony == null");
                    }

                    if (defender == null) {
                        throw new NullPointerException("defender == null");
                    }
                    
                    if (colony != null) {
                        getGame().getCombatModel().bombard(colony, defender, new CombatResult(result, damage), repairLocation);
                    } else {
                    	Animations.unitAttack(getFreeColClient().getCanvas(), unit, defender, result);
                        unit.getGame().getCombatModel().attack(unit, defender, new CombatResult(result, damage), plunderGold, repairLocation);
                        if (!unit.isDisposed() &&
                                (unit.getLocation() == null ||
                                        !unit.isVisibleTo(getFreeColClient().getMyPlayer()))) {
                            unit.dispose();
                        }
                    }

                    if (!defender.isDisposed()
                            && (defender.getLocation() == null || !defender.isVisibleTo(getFreeColClient().getMyPlayer()))) {
                        if (result == CombatResultType.DONE_SETTLEMENT && defender.getColony() != null
                                && !defender.getColony().isDisposed()) {
                            defender.getColony().setUnitCount(defender.getColony().getUnitCount());
                        }
                        defender.dispose();
                    }
                }
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while handling opponentAttack message.", e);
        }
        return null;
    }

    
    private Element setCurrentPlayer(Element setCurrentPlayerElement) {
        Game game = getGame();
        
        
        final boolean popDown = getFreeColClient().getMyPlayer().equals(game.getCurrentPlayer());

        final Player currentPlayer = (Player) game.getFreeColGameObject(setCurrentPlayerElement.getAttribute("player"));

        logger.finest("About to set currentPlayer to " + currentPlayer.getName());
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if (popDown) {
                        getFreeColClient().getCanvas().closeMenus();
                    }
                    getFreeColClient().getInGameController().setCurrentPlayer(currentPlayer);
                    getFreeColClient().getActionManager().update();
                }
            });
        } catch (InterruptedException e) {
            
        } catch (InvocationTargetException e) {
            
        }
        logger.finest("Succeeded in setting currentPlayer to " + currentPlayer.getName());

        new RefreshCanvasSwingTask(true).invokeLater();
        return null;
    }

    
    private Element newTurn(Element newTurnElement) {
        
        getGame().getTurn().increase();
        getFreeColClient().getMyPlayer().newTurn();
        new UpdateMenuBarSwingTask().invokeLater();
        
        
        Turn currTurn = getGame().getTurn(); 
        if(currTurn.getYear() == 1600 &&
        		Turn.getYear(currTurn.getNumber()-1) == 1599 ){
        	new ShowInformationMessageSwingTask("twoTurnsPerYear").invokeLater(); 
        }
        return null;
    }

    
    private Element setDead(Element element) {
        FreeColClient freeColClient = getFreeColClient();
        Player player = (Player) getGame().getFreeColGameObject(element.getAttribute("player"));

        if (player == freeColClient.getMyPlayer()) {
            if (freeColClient.isSingleplayer()) {
                if (!new ShowConfirmDialogSwingTask(null, "defeatedSingleplayer.text", "defeatedSingleplayer.yes",
                        "defeatedSingleplayer.no").confirm()) {
                    freeColClient.quit();
                } else {
                    freeColClient.getFreeColServer().enterRevengeMode(player.getName());
                }
            } else {
                if (!new ShowConfirmDialogSwingTask(null, "defeated.text", "defeated.yes", "defeated.no").confirm()) {
                    freeColClient.quit();
                }
            }
        }

        return null;
    }

    
    private Element gameEnded(Element element) {
        FreeColClient freeColClient = getFreeColClient();

        Player winner = (Player) getGame().getFreeColGameObject(element.getAttribute("winner"));
        if (winner == freeColClient.getMyPlayer()) {
            new ShowVictoryPanelSwingTask().invokeLater();
        } 

        return null;
    }

    
    private Element chat(Element element) {
        final ChatMessage chatMessage = new ChatMessage(getGame(), element);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Canvas canvas = getFreeColClient().getCanvas();
                canvas.displayChatMessage(chatMessage.getPlayer(),
                                          chatMessage.getMessage(),
                                          chatMessage.isPrivate());
            }
        });
        return null;
    }

    
    private Element error(Element element) {
        try {
            new ShowErrorMessageSwingTask(element.getAttribute("messageID"),
                                          element.getAttribute("message"))
                .invokeSpecial();
        } catch (Exception exception) {
            logger.warning("error() raised " + exception.toString());
        }
        return null;
    }

    
    private Element setAI(Element element) {

        Player p = (Player) getGame().getFreeColGameObject(element.getAttribute("player"));
        p.setAI(Boolean.valueOf(element.getAttribute("ai")).booleanValue());

        return null;
    }

    
    private Element chooseFoundingFather(Element element) {
        final List<FoundingFather> possibleFoundingFathers = new ArrayList<FoundingFather>();
        for (FoundingFatherType type : FoundingFatherType.values()) {
            String id = element.getAttribute(type.toString());
            if (id != null && !id.equals("")) {
                possibleFoundingFathers.add(FreeCol.getSpecification().getFoundingFather(id));
            }
        }

        FoundingFather foundingFather = new ShowSelectFoundingFatherSwingTask(possibleFoundingFathers).select();

        Element reply = Message.createNewRootElement("chosenFoundingFather");
        reply.setAttribute("foundingFather", foundingFather.getId());
        getFreeColClient().getMyPlayer().setCurrentFather(foundingFather);
        return reply;
    }

    
    private Element newConvert(Element element) {
        Tile tile = (Tile) getGame().getFreeColGameObject(element.getAttribute("colonyTile"));
        Colony colony = tile.getColony();
        Nation nation = Specification.getSpecification().getNation(element.getAttribute("nation"));
        
        Element unitElement = (Element) element.getFirstChild();
        Unit convert = new Unit(getGame(), unitElement);
        tile.add(convert);
        ModelMessage message = new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                                "model.colony.newConvert", convert)
            .add("%nation%", nation.getNameKey())
            .addName("%colony%", colony.getName());

        getFreeColClient().getMyPlayer().addModelMessage(message);
        return null;
    }

    
    private Element diplomacy(Element element) {
        Game game = getGame();
        Player player = getFreeColClient().getMyPlayer();
        DiplomacyMessage message = new DiplomacyMessage(game, element);
        Unit unit = message.getUnit(game);
        Settlement settlement = message.getSettlement(game);
        Player other = (unit.getOwner() == player) ? settlement.getOwner()
            : unit.getOwner();
        String nation = Messages.message(other.getNationName());
        DiplomaticTrade ourAgreement;
        DiplomaticTrade theirAgreement = message.getAgreement();

        switch (theirAgreement.getStatus()) {
        case ACCEPT_TRADE:
            new ShowInformationMessageSwingTask("negotiationDialog.offerAccepted",
                                                "%nation%", nation).show();
            break;
        case REJECT_TRADE:
            new ShowInformationMessageSwingTask("negotiationDialog.offerRejected",
                                                "%nation%", nation).show();
            break;
        case PROPOSE_TRADE:
            ourAgreement = new ShowNegotiationDialogSwingTask(unit, settlement,
                                                              theirAgreement)
                .select();
            if (ourAgreement == null) {
                ourAgreement = theirAgreement;
                ourAgreement.setStatus(TradeStatus.REJECT_TRADE);
            }
            return new DiplomacyMessage(unit, settlement,
                                        ourAgreement).toXMLElement();
        default:
            logger.warning("Bogus trade status");
            break;
        }
        return null;
    }

    
    private Element indianDemand(Element element) {
        Player player = getFreeColClient().getMyPlayer();
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Colony colony = (Colony) getGame().getFreeColGameObject(element.getAttribute("colony"));
        int gold = 0;
        Goods goods = null;
        boolean accepted;
        ModelMessage m = null;

        Element unitElement = Message.getChildElement(element, Unit.getXMLElementTagName());
        if (unitElement != null) {
            if (unit == null) {
                unit = new Unit(getGame(), unitElement);
            } else {
                unit.readFromXMLElement(unitElement);
            }
        }

        Element goodsElement = Message.getChildElement(element, Goods.getXMLElementTagName());
        if (goodsElement == null) {
            gold = Integer.parseInt(element.getAttribute("gold"));
            switch (getFreeColClient().getClientOptions().getInteger(ClientOptions.INDIAN_DEMAND_RESPONSE)) {
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ASK:
                accepted = new ShowConfirmDialogSwingTask(colony.getTile(),
                                                          "indianDemand.gold.text", "indianDemand.gold.yes",
                                                          "indianDemand.gold.no",
                                                          "%nation%", Messages.message(unit.getOwner().getNationName()),
                                                          "%colony%", colony.getName(),
                                                          "%amount%", String.valueOf(gold)).confirm();
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ACCEPT:
                m = new ModelMessage(ModelMessage.MessageType.ACCEPTED_DEMANDS,
                                     "indianDemand.gold.text", colony, unit)
                    .addStringTemplate("%nation%", unit.getOwner().getNationName())
                    .addName("%colony%", colony.getName())
                    .addName("%amount%", String.valueOf(gold));
                accepted = true;
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_REJECT:
                m = new ModelMessage(ModelMessage.MessageType.REJECTED_DEMANDS,
                                     "indianDemand.gold.text", colony, unit)
                    .addStringTemplate("%nation%", unit.getOwner().getNationName())
                    .addName("%colony%", colony.getName())
                    .addName("%amount%", String.valueOf(gold));
                accepted = false;
                break;
            default:
                throw new IllegalArgumentException("Impossible option value.");
            }
            if (accepted) {
                colony.getOwner().modifyGold(-gold);
            }
        } else {
            goods = new Goods(getGame(), goodsElement);

            switch (getFreeColClient().getClientOptions().getInteger(ClientOptions.INDIAN_DEMAND_RESPONSE)) {
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ASK:
                if (goods.getType().isFoodType()) {
                    accepted = new ShowConfirmDialogSwingTask(colony.getTile(),
                                                              "indianDemand.food.text", "indianDemand.food.yes",
                                                              "indianDemand.food.no",
                                                              "%nation%", Messages.message(unit.getOwner().getNationName()),
                                                              "%colony%", colony.getName(),
                                                              "%amount%", String.valueOf(goods.getAmount())).confirm();
                } else {
                    accepted =
                        new ShowConfirmDialogSwingTask(colony.getTile(),
                                                       Messages.message(StringTemplate.template("indianDemand.other.no")
                                                                        .addStringTemplate("%nation%", unit.getOwner().getNationName())
                                                                        .addName("%colony%", colony.getName())
                                                                        .addAmount("%amount%", goods.getAmount())
                                                                        .add("%goods%", goods.getNameKey())),
                                                       "indianDemand.other.text", "indianDemand.other.yes").confirm();
                }
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_ACCEPT:
                if (goods.getType().isFoodType()) {
                    m = new ModelMessage(ModelMessage.MessageType.ACCEPTED_DEMANDS,
                                         "indianDemand.food.text", colony, unit)
                        .addStringTemplate("%nation%", unit.getOwner().getNationName())
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", String.valueOf(goods.getAmount()));
                } else {
                    m = new ModelMessage(ModelMessage.MessageType.ACCEPTED_DEMANDS,
                                         "indianDemand.other.text", colony, unit)
                        .addStringTemplate("%nation%", unit.getOwner().getNationName())
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", String.valueOf(goods.getAmount()))
                        .add("%goods%", goods.getNameKey());
                }
                accepted = true;
                break;
            case ClientOptions.INDIAN_DEMAND_RESPONSE_REJECT:
                if (goods.getType().isFoodType()) {
                    m = new ModelMessage(ModelMessage.MessageType.REJECTED_DEMANDS,
                                         "indianDemand.food.text", colony, unit)
                        .addStringTemplate("%nation%", unit.getOwner().getNationName())
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", String.valueOf(goods.getAmount()));
                } else {
                    m = new ModelMessage(ModelMessage.MessageType.REJECTED_DEMANDS,
                                         "indianDemand.other.text", colony, unit)
                        .addStringTemplate("%nation%", unit.getOwner().getNationName())
                        .addName("%colony%", colony.getName())
                        .addName("%amount%", String.valueOf(goods.getAmount()))
                        .add("%goods%", goods.getNameKey());
                }
                accepted = false;
                break;
            default:
                throw new IllegalArgumentException("Impossible option value.");
            }
            if (accepted) {
                colony.getGoodsContainer().removeGoods(goods);
            }
        }
        if (m != null) {
            player.addModelMessage(m);
        }

        element.setAttribute("accepted", String.valueOf(accepted));

        return element;
    }

    
    private Element monarchAction(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Player player = freeColClient.getMyPlayer();
        Monarch monarch = player.getMonarch();
        final MonarchAction action = Enum.valueOf(MonarchAction.class, element.getAttribute("action"));
        Element reply;

        switch (action) {
        case RAISE_TAX:
            boolean force = Boolean.parseBoolean(element.getAttribute("force"));
            final int amount = new Integer(element.getAttribute("amount")).intValue();
            if (force) {
                freeColClient.getMyPlayer().setTax(amount);
                player.addModelMessage(new ModelMessage(ModelMessage.MessageType.WARNING,
                                                        "model.monarch.forceTaxRaise", player)
                                       .addName("%replace%", String.valueOf(amount)));
                reply = null;
            } else {
                reply = Message.createNewRootElement("acceptTax");
                if (new ShowMonarchPanelSwingTask(action,
                                                  "%replace%", element.getAttribute("amount"),
                                                  "%goods%", element.getAttribute("goods")).confirm()) {
                    freeColClient.getMyPlayer().setTax(amount);
                    reply.setAttribute("accepted", String.valueOf(true));
                    new UpdateMenuBarSwingTask().invokeLater();
                } else {
                    reply.setAttribute("accepted", String.valueOf(false));
                }
            }
            return reply;
        case LOWER_TAX:
	
            final int newTax = new Integer(element.getAttribute("amount")).intValue();
            final int difference = freeColClient.getMyPlayer().getTax() - newTax;
                    
            freeColClient.getMyPlayer().setTax(newTax);
            new ShowMonarchPanelSwingTask(action,
                                          "%difference%", String.valueOf(difference),
                                          "%newTax%", String.valueOf(newTax)).confirm();
            break;
        case ADD_TO_REF:
            Element additionElement = Message.getChildElement(element, "addition");
            NodeList childElements = additionElement.getChildNodes();
            ArrayList<AbstractUnit> units = new ArrayList<AbstractUnit>();
            ArrayList<String> unitNames = new ArrayList<String>();
            for (int index = 0; index < childElements.getLength(); index++) {
                AbstractUnit unit = new AbstractUnit();
                unit.readFromXMLElement((Element) childElements.item(index));
                units.add(unit);
                unitNames.add(unit.getNumber() + " "
                              + Messages.message(Messages.getLabel(unit.getUnitType(), unit.getRole())));
            }
            monarch.addToREF(units);
            new ShowMonarchPanelSwingTask(action,
                                          "%addition%", Utils.join(" " + Messages.message("and") + " ",
                                                                   unitNames)).confirm();
            break;
        case DECLARE_WAR:
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("enemy"));
            player.changeRelationWithPlayer(enemy, Stance.WAR);
            new ShowMonarchPanelSwingTask(action,
                                          "%nation%", Messages.message(enemy.getNationName()))
                .confirm();
            break;
        case SUPPORT_LAND:
        case SUPPORT_SEA:
        case ADD_UNITS:
            NodeList unitList = element.getChildNodes();
            for (int i = 0; i < unitList.getLength(); i++) {
                Element unitElement = (Element) unitList.item(i);
                Unit newUnit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
                if (newUnit == null) {
                    newUnit = new Unit(getGame(), unitElement);
                } else {
                    newUnit.readFromXMLElement(unitElement);
                }
                player.getEurope().add(newUnit);
            }
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Canvas canvas = getFreeColClient().getCanvas();
                        if (!canvas.isShowingSubPanel()
                            && (action == MonarchAction.ADD_UNITS ||
                                !canvas.showFreeColDialog(new MonarchPanel(canvas, action)))) {
                            canvas.showEuropePanel();
                        }
                    }
                });
            break;
        case OFFER_MERCENARIES:
            reply = Message.createNewRootElement("hireMercenaries");
            Element mercenaryElement = Message.getChildElement(element, "mercenaries");
            childElements = mercenaryElement.getChildNodes();
            ArrayList<String> mercenaries = new ArrayList<String>();
            for (int index = 0; index < childElements.getLength(); index++) {
                AbstractUnit unit = new AbstractUnit();
                unit.readFromXMLElement((Element) childElements.item(index));
                mercenaries.add(unit.getNumber() + " "
                                + Messages.message(Messages.getLabel(unit.getUnitType(), unit.getRole())));
            }
            if (new ShowMonarchPanelSwingTask(action,
                                              "%gold%", element.getAttribute("price"),
                                              "%mercenaries%", Utils.join(" " + Messages.message("and") + " ",
                                                                          mercenaries)).confirm()) {
                int price = new Integer(element.getAttribute("price")).intValue();
                freeColClient.getMyPlayer().modifyGold(-price);
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            freeColClient.getCanvas().updateGoldLabel();
                        }
                    });
                reply.setAttribute("accepted", String.valueOf(true));
            } else {
                reply.setAttribute("accepted", String.valueOf(false));
            }
            return reply;
        case NO_ACTION:
            
            break;
        }
        return null;
    }

    
    private Element setStance(Element element) {
        final FreeColClient freeColClient = getFreeColClient();
        Player player = freeColClient.getMyPlayer();
        Game game = getGame();
        Stance stance = Enum.valueOf(Stance.class, element.getAttribute("stance"));
        Player first = (Player) game.getFreeColGameObject(element.getAttribute("first"));
        Player second = (Player) game.getFreeColGameObject(element.getAttribute("second"));

        Stance oldStance = first.getStance(second);
        

        
        Player other = (player.equals(first)) ? second
            : (player.equals(second)) ? first
            : null;
        
        
        boolean firstContact = other != null && stance == Stance.PEACE
            && player.getStance(other) == Stance.UNCONTACTED;

        first.setStance(second, stance);
        if (second.getStance(first) != stance) second.setStance(first, stance);

        
        if (player.isAI()) return null;

        ModelMessage m = null;
        String sta = stance.toString().toLowerCase();
        if (other == null) {
            
            
            
            if (stance == Stance.WAR
                   || player.hasAbility("model.ability.betterForeignAffairsReport")
                   || player.hasContacted(first)
                   || player.hasContacted(second)) {
                m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     "model.diplomacy." + sta + ".others",
                                     first)
                    .addStringTemplate("%attacker%", first.getNationName())
                    .addStringTemplate("%defender%", second.getNationName());
            }
        } else {
            if (!firstContact) {
                m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                     "model.diplomacy." + sta + ".declared",
                                     first)
                    .addStringTemplate("%nation%", other.getNationName());
            }
        }
        if (m != null) player.addModelMessage(m);
        return null;
    }

    
    private Element addPlayer(Element element) {

        Element playerElement = (Element) element.getElementsByTagName(Player.getXMLElementTagName()).item(0);
        if (getGame().getFreeColGameObject(playerElement.getAttribute("ID")) == null) {
            Player newPlayer = new Player(getGame(), playerElement);
            getGame().addPlayer(newPlayer);
        } else {
            getGame().getFreeColGameObject(playerElement.getAttribute("ID")).readFromXMLElement(playerElement);
        }

        return null;
    }


    
    private Element removeGoods(Element element) {
        final FreeColClient freeColClient = getFreeColClient();

        NodeList nodeList = element.getChildNodes();
        Element goodsElement = (Element) nodeList.item(0);

        if (goodsElement == null) {
            
            new ShowMonarchPanelSwingTask(MonarchAction.WAIVE_TAX).confirm();
        } else {
            final Goods goods = new Goods(getGame(), goodsElement);
            final Colony colony = (Colony) goods.getLocation();
            colony.removeGoods(goods);

            
            freeColClient.getMyPlayer().setArrears(goods);

            String messageID = goods.getType().getId() + ".destroyed";
            if (!Messages.containsKey(messageID)) {
                if (colony.isLandLocked()) {
                    messageID = "model.monarch.colonyGoodsParty.landLocked";
                } else {
                    messageID = "model.monarch.colonyGoodsParty.harbour";
                }
            }
            colony.getFeatureContainer().addModifier(Modifier
               .createTeaPartyModifier(getGame().getTurn()));
            new ShowModelMessageSwingTask(new ModelMessage(ModelMessage.MessageType.WARNING,
                                                           messageID, colony)
                                          .addName("%colony%", colony.getName())
                                          .addName("%amount%", String.valueOf(goods.getAmount()))
                                          .add("%goods%", goods.getNameKey())).invokeLater();
        }

        return null;
    }

    
    private Element spanishSuccession(Element element) {
        final Player player = getFreeColClient().getMyPlayer();
        final Player loser = (Player) getGame().getFreeColGameObject(element.getAttribute("loser"));
        final Player winner = (Player) getGame().getFreeColGameObject(element.getAttribute("winner"));
        player.addModelMessage(new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                                "model.diplomacy.spanishSuccession", winner)
                               .addStringTemplate("%loserNation%", loser.getNationName())
                               .addStringTemplate("%nation%", winner.getNationName()));
        loser.setDead(true);
        update(element);
        player.getHistory().add(new HistoryEvent(player.getGame().getTurn().getNumber(),
                                                 HistoryEvent.EventType.SPANISH_SUCCESSION)
                                .addStringTemplate("%nation%", winner.getNationName())
                                .addStringTemplate("%loserNation%", loser.getNationName()));

        return null;
    }

    
    public Element disposeUnits(Element element) {
        Game game = getGame();
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            
            
            
            
            Element e = (Element) nodes.item(i);
            FreeColGameObject fcgo = game.getFreeColGameObjectSafely(e.getAttribute("ID"));

            if (fcgo instanceof Unit) {
                ((Unit) fcgo).dispose();
            } else {
                logger.warning("Object is not a unit: "
                               + ((fcgo == null) ? "null" : fcgo.getId()));
            }
        }
        return null;
    }

    
    public Element addMessages(Element element) {
        Game game = getGame();
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            ModelMessage m = new ModelMessage();
            Element e = (Element) nodes.item(i);
            m.readFromXMLElement(e);

            String owner = e.getAttribute("owner");
            FreeColGameObject fcgo = game.getFreeColGameObjectSafely(owner);
            if (fcgo instanceof Player) {
                ((Player) fcgo).addModelMessage(m);
            } else {
                logger.warning("addMessages with broken owner: "
                               + ((owner == null) ? "(null)" : owner));
            }
        }
        return null;
    }

    
    public Element addHistory(Element element) {
        Game game = getGame();
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            HistoryEvent h = new HistoryEvent();
            Element e = (Element) nodes.item(i);
            h.readFromXMLElement(e);

            
            String owner = e.getAttribute("owner");
            FreeColGameObject fcgo = game.getFreeColGameObjectSafely(owner);
            if (fcgo instanceof Player) {
                ((Player) fcgo).getHistory().add(h);
            } else {
                logger.warning("addHistory with broken owner: "
                               + ((owner == null) ? "(null)" : owner));
            }
        }
        return null;
    }

    
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        Element reply = null;

        for (int i = 0; i < nodes.getLength(); i++) {
            reply = handle(connection, (Element) nodes.item(i));
        }
        return reply;
    }


    


    
    abstract static class SwingTask implements Runnable {
        private static final Logger taskLogger = Logger.getLogger(SwingTask.class.getName());


        
        public Object invokeAndWait() throws InvocationTargetException {
            verifyNotStarted();
            markStarted(true);
            try {
                SwingUtilities.invokeAndWait(this);
            } catch (InterruptedException e) {
                throw new InvocationTargetException(e);
            }
            return _result;
        }

        
        public void invokeLater() {
            verifyNotStarted();
            markStarted(false);
            SwingUtilities.invokeLater(this);
        }

        
        public void invokeSpecial()
            throws InvocationTargetException {
            if (SwingUtilities.isEventDispatchThread()) {
                doWork();
            } else {
                this.invokeAndWait();
            }
        }

        
        private synchronized void markStarted(boolean synchronous) {
            _synchronous = synchronous;
            _started = true;
        }

        
        private synchronized void markDone() {
            _started = false;
        }

        
        private synchronized void verifyNotStarted() {
            if (_started) {
                throw new IllegalStateException("Swing task already started!");
            }
        }

        
        private synchronized boolean isSynchronous() {
            return _synchronous;
        }

        
        public final void run() {
            try {
                if (taskLogger.isLoggable(Level.FINEST)) {
                    taskLogger.log(Level.FINEST, "Running Swing task " + getClass().getName() + "...");
                }

                setResult(doWork());

                if (taskLogger.isLoggable(Level.FINEST)) {
                    taskLogger.log(Level.FINEST, "Swing task " + getClass().getName() + " returned " + _result);
                }
            } catch (RuntimeException e) {
                taskLogger.log(Level.WARNING, "Swing task " + getClass().getName() + " failed!", e);
                
                if (isSynchronous()) {
                    throw e;
                }
            } finally {
                markDone();
            }
        }

        
        public synchronized Object getResult() {
            return _result;
        }

        
        private synchronized void setResult(Object r) {
            _result = r;
        }

        
        protected abstract Object doWork();


        private Object _result;

        private boolean _synchronous;

        private boolean _started;
    }

    
    abstract class NoResultCanvasSwingTask extends SwingTask {

        protected Object doWork() {
            doWork(getFreeColClient().getCanvas());
            return null;
        }

        abstract void doWork(Canvas canvas);
    }

    
    class RefreshCanvasSwingTask extends NoResultCanvasSwingTask {
        
        public RefreshCanvasSwingTask() {
            this(false);
        }

        
        public RefreshCanvasSwingTask(boolean requestFocus) {
            _requestFocus = requestFocus;
        }

        protected void doWork(Canvas canvas) {
            canvas.refresh();
            
            if (_requestFocus && !canvas.isShowingSubPanel()) {
                canvas.requestFocusInWindow();
            }
        }


        private final boolean _requestFocus;
    }
    
    class RefreshTilesSwingTask extends NoResultCanvasSwingTask {
        
        public RefreshTilesSwingTask(Tile oldTile, Tile newTile) {
            super();
            _oldTile = oldTile;
            _newTile = newTile;
        }
        
        void doWork(Canvas canvas) {
            canvas.refreshTile(_oldTile);
            canvas.refreshTile(_newTile);
        }
        
        private final Tile _oldTile;
        private final Tile _newTile;
        
    }
    
    
    class UnitMoveAnimationCanvasSwingTask extends NoResultCanvasSwingTask {
                
        private final Unit unit;
        private final Tile destinationTile;
        private final Tile sourceTile;
        private boolean focus;

        
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile sourceTile,
                                                Tile destinationTile) {
            this(unit, sourceTile, destinationTile, true);
        }

        
        public UnitMoveAnimationCanvasSwingTask(Unit unit, Tile sourceTile,
                                                Tile destinationTile,
                                                boolean focus) {
            this.unit = unit;
            this.sourceTile = sourceTile;
            this.destinationTile = destinationTile;
            this.focus = focus;
        }

        protected void doWork(Canvas canvas) {
            GUI gui = canvas.getGUI();
            if (focus || !gui.onScreen(sourceTile.getPosition())) {
                gui.setFocusImmediately(sourceTile.getPosition());
            }
            Animations.unitMove(canvas, unit, sourceTile, destinationTile);
            canvas.refresh();
        }
    }

    
    class UnitAttackAnimationCanvasSwingTask extends NoResultCanvasSwingTask {

        private final Unit unit;
        private final Unit defender;
        private final CombatResultType result;
        private boolean focus;

        
        public UnitAttackAnimationCanvasSwingTask(Unit unit, Unit defender,
                                                  CombatResultType result) {
            this(unit, defender, result, true);
        }

        
        public UnitAttackAnimationCanvasSwingTask(Unit unit, Unit defender,
                                                  CombatResultType result,
                                                  boolean focus) {
            this.unit = unit;
            this.defender = defender;
            this.result = result;
            this.focus = focus;
        }

        protected void doWork(Canvas canvas) {
            GUI gui = canvas.getGUI();
            if (focus || !gui.onScreen(unit.getTile().getPosition())) {
                gui.setFocusImmediately(unit.getTile().getPosition());
            }
            Animations.unitAttack(canvas, unit, defender, result);
            canvas.refresh();
        }
    }
    
    
    class ReconnectSwingTask extends SwingTask {
        protected Object doWork() {
            getFreeColClient().getConnectController().reconnect();
            return null;
        }
    }

    
    class UpdateMenuBarSwingTask extends NoResultCanvasSwingTask {
        protected void doWork(Canvas canvas) {
            getFreeColClient().updateMenuBar();
        }
    }

    
    class ShowVictoryPanelSwingTask extends NoResultCanvasSwingTask {
        protected void doWork(Canvas canvas) {
            canvas.showPanel(new VictoryPanel(canvas));
        }
    }

    
    class ShowConfirmDialogSwingTask extends SwingTask {

        private Tile tile;
        private String text;
        private String okText;
        private String cancelText;
        private String[] replace;

        
        public ShowConfirmDialogSwingTask(Tile tile, String text,
                                          String okText, String cancelText,
                                          String... replace) {
            this.tile = tile;
            this.text = text;
            this.okText = okText;
            this.cancelText = cancelText;
            this.replace = replace;
        }

        
        public boolean confirm() {
            try {
                Object result = invokeAndWait();
                return ((Boolean) result).booleanValue();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            boolean choice = canvas.showConfirmDialog(tile, text,
                                                      okText, cancelText,
                                                      replace);
            return Boolean.valueOf(choice);
        }
    }

    
    abstract class ShowMessageSwingTask extends SwingTask {
        
        public void show() {
            try {
                invokeAndWait();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    
    class ShowModelMessageSwingTask extends ShowMessageSwingTask {

        
        public ShowModelMessageSwingTask(ModelMessage modelMessage) {
            _modelMessage = modelMessage;
        }

        protected Object doWork() {
            getFreeColClient().getCanvas().showModelMessages(_modelMessage);
            return null;
        }


        private ModelMessage _modelMessage;
    }

    
    class ShowInformationMessageSwingTask extends ShowMessageSwingTask {

        private String messageId;
        private String[] replace;

        
        public ShowInformationMessageSwingTask(String messageId, String... replace) {
            this.messageId = messageId;
            this.replace = replace;
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            canvas.showInformationMessage(messageId, null, replace);
            return null;
        }
    }

    
    class ShowErrorMessageSwingTask extends ShowMessageSwingTask {

        
        public ShowErrorMessageSwingTask(String messageId, String message) {
            _messageId = messageId;
            _message = message;
        }

        protected Object doWork() {
            getFreeColClient().getCanvas().errorMessage(_messageId, _message);
            return null;
        }


        private String _messageId;

        private String _message;
    }

    
    abstract class ShowSelectSwingTask extends SwingTask {
        
        public int select() {
            try {
                Object result = invokeAndWait();
                return ((Integer) result).intValue();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    
    class ShowSelectFoundingFatherSwingTask extends SwingTask {

        private List<FoundingFather> choices;

        
        public ShowSelectFoundingFatherSwingTask(List<FoundingFather> choices) {
            this.choices = choices;
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            return canvas.showFreeColDialog(new ChooseFoundingFatherDialog(canvas, choices));
        }

        
        public FoundingFather select() {
            try {
                Object result = invokeAndWait();
                return (FoundingFather) result;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    
    class ShowNegotiationDialogSwingTask extends SwingTask {

        
        public ShowNegotiationDialogSwingTask(Unit unit, Settlement settlement, DiplomaticTrade proposal) {
            this.unit = unit;
            this.settlement = settlement;
            this.proposal = proposal;
        }

        
        public DiplomaticTrade select() {
            try {
                Object result = invokeAndWait();
                return (DiplomaticTrade) result;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            return getFreeColClient().getCanvas().showNegotiationDialog(unit, settlement, proposal);
        }
        
        private Unit unit;
        private Settlement settlement;
        private DiplomaticTrade proposal;
    }

    
    class ShowMonarchPanelSwingTask extends SwingTask {

        
        public ShowMonarchPanelSwingTask(MonarchAction action, String... replace) {
            _action = action;
            _replace = replace;
        }

        
        public boolean confirm() {
            try {
                Object result = invokeAndWait();
                return ((Boolean) result).booleanValue();
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        protected Object doWork() {
            Canvas canvas = getFreeColClient().getCanvas();
            boolean choice = canvas.showFreeColDialog(new MonarchPanel(canvas, _action, _replace));
            return Boolean.valueOf(choice);
        }


        private MonarchAction  _action;

        private String[] _replace;
    }
}

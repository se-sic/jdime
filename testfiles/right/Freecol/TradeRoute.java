

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;

import org.w3c.dom.Element;


public class TradeRoute extends FreeColGameObject implements Cloneable, Ownable {

    public static final TradeRoute NO_TRADE_ROUTE = new TradeRoute();

    
    

    
    private String name;

    
    private int count;

    
    private boolean modified = false;

    
    private Player owner;

    
    private ArrayList<Stop> stops = new ArrayList<Stop>();

    
    private TradeRoute() {}

    
    public TradeRoute(Game game, String name, Player player) {
        super(game);
        this.name = name;
        this.owner = player;
        this.count = 0;
    }

    
    public TradeRoute(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    
    public TradeRoute(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    
    public synchronized void updateFrom(TradeRoute other) {
        setName(other.getName());
        stops.clear();
        for (Stop otherStop : other.getStops()) {
            addStop(new Stop(otherStop));
        }
    }

    
    public final boolean isModified() {
        return modified;
    }

    
    public final void setModified(final boolean newModified) {
        this.modified = newModified;
    }

    
    public final String getName() {
        return name;
    }

    
    public final void setName(final String newName) {
        this.name = newName;
    }

    
    public int getCount() {
        return count;
    }

    
    public void setCount(int newCount) {
        count = newCount;
    }

    
    public void addStop(Stop stop) {
        stops.add(stop);
    }

    
    public final Player getOwner() {
        return owner;
    }

    
    public final void setOwner(final Player newOwner) {
        this.owner = newOwner;
    }

    public String toString() {
        return getName();
    }

    
    public final ArrayList<Stop> getStops() {
        return stops;
    }

    
    public final void setStops(final ArrayList<Stop> newStops) {
        this.stops = newStops;
    }

    
    public void newTurn() {
    }

    
    public TradeRoute clone() {
        try {
            TradeRoute copy = (TradeRoute) super.clone();
            copy.replaceStops(getStops());
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone should be supported!", e);
        }
    }

    
    private void replaceStops(List<Stop> otherStops) {
        stops = new ArrayList<Stop>();
        for (Stop otherStop : otherStops) {
            addStop(new Stop(otherStop));
        }
    }

    public class Stop {

        private String locationId;

        private ArrayList<GoodsType> cargo = new ArrayList<GoodsType>();

        
        private boolean modified = false;


        public Stop(Location location) {
            this.locationId = location.getId();
        }

        
        private Stop(Stop other) {
            this.locationId = other.locationId;
            this.cargo = new ArrayList<GoodsType>(other.cargo);
        }

        private Stop(XMLStreamReader in) throws XMLStreamException {
            locationId = in.getAttributeValue(null, "location");
            List<GoodsType> goodsList = FreeCol.getSpecification().getGoodsTypeList();
            for (int cargoIndex : readFromArrayElement("cargo", in, new int[0])) {
                addCargo(goodsList.get(cargoIndex));
            }
            in.nextTag();
        }

        
        public final boolean isModified() {
            return modified;
        }

        
        public final void setModified(final boolean newModified) {
            this.modified = newModified;
        }

        
        public final Location getLocation() {
            Game g = getGame();
            return g != null ? (Location) g.getFreeColGameObject(locationId) : null;
        }

        
        @SuppressWarnings("unchecked")
        public final ArrayList<GoodsType> getCargo() {
            return (ArrayList<GoodsType>) cargo.clone();
        }

        
        public final void setCargo(ArrayList<GoodsType> cargo) {
            this.cargo.clear();
            this.cargo.addAll(cargo);
        }

        public void addCargo(GoodsType newCargo) {
            cargo.add(newCargo);
        }

        

        public String toString() {
            Location l = getLocation();
            return l != null ? l.getLocationName() : Messages.message("traderouteDialog.invalidStop");
        }

        public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
            out.writeStartElement(getStopXMLElementTagName());
            out.writeAttribute("location", this.locationId);
            int[] cargoIndexArray = new int[cargo.size()];
            for (int index = 0; index < cargoIndexArray.length; index++) {
                cargoIndexArray[index] = cargo.get(index).getIndex();
            }
            toArrayElement("cargo", cargoIndexArray, out);
            out.writeEndElement();
        }
    }


    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        out.writeAttribute("name", getName());
        out.writeAttribute("owner", getOwner().getId());
        for (Stop stop : stops) {
            stop.toXMLImpl(out);
        }

        out.writeEndElement();
    }

    
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        setName(in.getAttributeValue(null, "name"));
        String ownerID = in.getAttributeValue(null, "owner");
        
        Game game = getGame();
        if (game != null){
            if (ownerID.equals(Player.UNKNOWN_ENEMY)) {
                owner = game.getUnknownEnemy(); 
            } else {
                owner = (Player) getGame().getFreeColGameObject(ownerID);
                if (owner == null) {
                    owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
                }
            }
        }

        stops.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (getStopXMLElementTagName().equals(in.getLocalName())) {
                stops.add(new Stop(in));
            }
        }
    }
    
    public static boolean isStopValid(Unit unit, Stop stop){
    	return TradeRoute.isStopValid(unit.getOwner(), stop);
    }
    
    public static boolean isStopValid(Player player, Stop stop){
    	if(stop == null){
    		return false;
    	}
    	
    	Location location = stop.getLocation();
    	
    	if(location == null){
    		return false;
    	}
    	
    	if (((FreeColGameObject) location).isDisposed()) {
          return false;
    	}
    	   
    	return true;
    }

    
    public static String getXMLElementTagName() {
        return "tradeRoute";
    }

    
    public static String getStopXMLElementTagName() {
        return "tradeRouteStop";
    }
}

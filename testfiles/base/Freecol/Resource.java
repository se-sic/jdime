

package net.sf.freecol.common.model;

import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Element;


public class Resource extends TileItem {

    private static Logger logger = Logger.getLogger(Resource.class.getName());

    private ResourceType type;
    private int quantity;

    
    public Resource(Game game, Tile tile, ResourceType type) {
        super(game, tile);
        if (type == null) {
            throw new IllegalArgumentException("Parameter 'type' must not be 'null'.");
        }
        this.type = type;
        this.quantity = type.getRandomValue();
    }

    public Resource(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    public Resource(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    
    public String toString() {
        if (quantity > -1) {
            return Integer.toString(quantity) + " " + getName();
        } else {
            return getName();
        }
    }

    
    public String getName() {
        return getType().getName();
    }

    
    public ResourceType getType() {
        return type;
    }

    
    public String getOutputString() {
        return type.getOutputString();
    }

    
    public int getQuantity() {
        return quantity;
    }

    
    public void setQuantity(int newQuantity) {
        quantity = newQuantity;
    }

    
    public final int getZIndex() {
        return RESOURCE_ZINDEX;
    }

    
    public GoodsType getBestGoodsType() {
        return type.getBestGoodsType();
    }

    
    public int getBonus(GoodsType goodsType, UnitType unitType, int potential) {
        Set<Modifier> productionBonus = type.getProductionModifier(goodsType, unitType);
        int bonusAmount = (int) FeatureContainer.applyModifierSet(potential, null, productionBonus) - potential;
        if (quantity > -1 && bonusAmount > quantity) {
            return potential + quantity;
        } else {
            return potential + bonusAmount;
        }
    }

    
    public int useQuantity(GoodsType goodsType, UnitType unitType, int potential) {
        
        return useQuantity(getBonus(goodsType, unitType, potential) - potential);
    }

    
    public int useQuantity(int usedQuantity) {
        if (quantity >= usedQuantity) {
            quantity -= usedQuantity;
        } else if (quantity == -1) {
            logger.warning("useQuantity called for unlimited resource");
        } else {
            
            logger.severe("Insufficient quantity in " + this);
            quantity = 0;
        }
        return quantity;
    }

    
    @Override
        public void dispose() {
        super.dispose();
    }

    

    
    @Override
        protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        
        out.writeStartElement(getXMLElementTagName());

        
        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", getTile().getId());
        out.writeAttribute("type", getType().getId());
        out.writeAttribute("quantity", Integer.toString(quantity));

        
        out.writeEndElement();
    }

    
    @Override
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        tile = getFreeColGameObject(in, "tile", Tile.class);
        type = FreeCol.getSpecification().getResourceType(in.getAttributeValue(null, "type"));
        quantity = Integer.parseInt(in.getAttributeValue(null, "quantity"));

        in.nextTag();
    }

    
    public static String getXMLElementTagName() {
        return "resource";
    }

    public void setName(String newName) {
    }

}

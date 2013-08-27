

package edu.rice.cs.util.docnavigation;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import edu.rice.cs.util.swing.Utilities;




class JListNavigator<ItemT extends INavigatorItem> extends JList implements IDocumentNavigator<ItemT> {
  
  
  protected DefaultListModel _model;
  
  
  private ItemT _current = null;
  


  
  
  private CustomListCellRenderer _renderer;
  
  
  private final Vector<INavigationListener<? super ItemT>> navListeners = new Vector<INavigationListener<? super ItemT>>();
  
  
  public JListNavigator() { 
    super();
    init(new DefaultListModel());
  }
  
  private void init(DefaultListModel m) {
    _model = m;
    setModel(m);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    addListSelectionListener(new ListSelectionListener() {
      
      public void valueChanged(final ListSelectionEvent e) {
        Utilities.invokeLater( new Runnable() {
          public void run() {
            if (!e.getValueIsAdjusting() && !_model.isEmpty()) {
              @SuppressWarnings("unchecked") final ItemT newItem = (ItemT) getSelectedValue();

              if (_current != newItem) {
                final ItemT oldItem = _current;
                NodeData<ItemT> oldData = new NodeData<ItemT>() {
                  public <Ret> Ret execute(NodeDataVisitor<? super ItemT, Ret> v) { return v.itemCase(oldItem); }
                };
                NodeData<ItemT> newData = new NodeData<ItemT>() {
                  public <Ret> Ret execute(NodeDataVisitor<? super ItemT, Ret> v) { return v.itemCase(newItem); }
                };
                for(INavigationListener<? super ItemT> listener: navListeners) {
                  if (oldItem != null) listener.lostSelection(oldData);
                  if (newItem != null) listener.gainedSelection(newData);
                }
                _current = newItem;

              }
            }
          }
        });
      }
    });
    
    _renderer = new CustomListCellRenderer();
    _renderer.setOpaque(true);
    this.setCellRenderer(_renderer);
  }
  
  
  public void addDocument(ItemT doc) { synchronized(_model) { _model.addElement(doc); } }
  
  
  public void addDocument(ItemT doc, String path) { addDocument(doc); }
  
  
  protected ItemT getFromModel(int i) {
    @SuppressWarnings("unchecked") ItemT result = (ItemT) _model.get(i);
    return result;
  }
  
  
  public ItemT getNext(ItemT doc) { 
    synchronized (_model) {
      int i = _model.indexOf(doc);
      if (i == -1)
        throw new IllegalArgumentException("No such document " + doc.toString() + " found in collection of open documents");
      if ( i + 1 == _model.size()) return doc;
      
      return getFromModel(i + 1);
    }
  }
  
  
  public ItemT getPrevious(ItemT doc) {  
    synchronized (_model) {
      int i = _model.indexOf(doc);
      if ( i == -1 )
        throw new IllegalArgumentException("No such document " + doc.toString() + " found in collection of open documents");
      if ( i == 0) return doc;
      return getFromModel(i - 1);
    }
  }
  
  
  public ItemT getFirst() { 
    synchronized (_model) { return getFromModel(0); } 
  }
  
  
  public ItemT getLast() { 
    synchronized (_model) { return getFromModel(_model.size() - 1); } 
  }
  
  
  public ItemT getCurrent() { return _current; }
  
  
  public ItemT removeDocument(ItemT doc) {
    synchronized (_model) {
      
      int i = _model.indexOf(doc);
      if( i == -1 )
        throw new IllegalArgumentException("Document " + doc + " not found in Document Navigator");
      ItemT result = getFromModel(i);
      _model.remove(i);
      return result;
    }
  }

  
  public void refreshDocument(ItemT doc, String path) {
    synchronized (_model) {
      removeDocument(doc);
      addDocument(doc);
    }
  }
  
  
  public void setActiveDoc(ItemT doc) { 
    synchronized(_model) {
      if (_current == doc) return; 
      if (_model.contains(doc)) {
        setSelectedValue(doc, true);   

      }
    }
  }
    
  
  public boolean contains(ItemT doc) { 
    synchronized(_model) { return _model.contains(doc); }
  }
  
  
  public Enumeration<ItemT> getDocuments() { 
    synchronized (_model) {

      @SuppressWarnings("unchecked") Enumeration<ItemT> result = (Enumeration<ItemT>) _model.elements();
      return result;  
    }
  }
  
  
  public int getDocumentCount() { return _model.size(); }
  
  
  public boolean isEmpty() { return _model.isEmpty(); }
  
  
  public void addNavigationListener(INavigationListener<? super ItemT> listener) { 
    synchronized(_model) { navListeners.add(listener); }
  }
  
  
  public void removeNavigationListener(INavigationListener<? super ItemT> listener) { 
    synchronized (_model) { navListeners.remove(listener); }
  }
  
  
  public Collection<INavigationListener<? super ItemT>> getNavigatorListeners() { return navListeners; }
  
  
  public void clear() { synchronized(_model) { _model.clear(); } }
  
  
  public <InType, ReturnType> ReturnType execute(IDocumentNavigatorAlgo<ItemT, InType, ReturnType> algo, InType input) {
    return algo.forList(this, input);
  }
  
  
  public Container asContainer() { return this; }
  
  
  public boolean selectDocumentAt(final int x, final int y) {
    synchronized (_model) {
      final int idx = locationToIndex(new java.awt.Point(x,y));
      java.awt.Rectangle rect = getCellBounds(idx, idx);
      if (rect.contains(x, y)) {
        setActiveDoc(getFromModel(idx));
        return true;
      }
      return false;
    }
  }
    

  
  public Component getRenderer(){ return _renderer; }
  
  
  public boolean isGroupSelected() { return false; }
  
  
  public boolean isSelectedInGroup(ItemT i) { return false; }
  
  public void addTopLevelGroup(String name, INavigatorItemFilter<? super ItemT> f) {  }
  
  public boolean isTopLevelGroupSelected() { return false; }
  
  public String getNameOfSelectedTopLevelGroup() throws GroupNotSelectedException{
    throw new GroupNotSelectedException("A top level group is not selected");
  }
  
  
  public void requestSelectionUpdate(ItemT doc) {  }
  










  public String toString() { synchronized (_model) { return _model.toString(); } }
  
  
  private static class CustomListCellRenderer extends DefaultListCellRenderer {
    
    
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {

      super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
      setText(((INavigatorItem)value).getName());

      return this;
    }
  }
}

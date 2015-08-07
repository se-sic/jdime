
package genj.util.swing;

import genj.gedcom.GedcomException;
import genj.gedcom.MetaProperty;
import genj.gedcom.time.Calendar;
import genj.gedcom.time.PointInTime;
import genj.util.ChangeSupport;
import genj.util.WordBuffer;
import genj.window.WindowManager;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;


public class DateWidget extends JPanel {
  
  private final static 
   NestedBlockLayout LAYOUT = new NestedBlockLayout("<row><x/><x/><x/><x/></row>");
  
  
  private PopupWidget widgetCalendar; 
  private TextFieldWidget widgetDay,widgetYear;
  private ChoiceWidget widgetMonth;
  
  
  private Calendar calendar; 
  
  
  private ChangeSupport changeSupport = new ChangeSupport(this) {
    protected void fireChangeEvent(Object source) {
      
      updateStatus();
      
      super.fireChangeEvent(source);
    }
  };
    
  
  public DateWidget() {
    this(new PointInTime());
  }
  
  
  public DateWidget(PointInTime pit) {

    calendar = pit.getCalendar();
        
    
    ArrayList switches = new ArrayList(PointInTime.CALENDARS.length+1);
    for (int s=0;s<PointInTime.CALENDARS.length;s++)
      switches.add(new SwitchCalendar(PointInTime.CALENDARS[s]));
    
    
    widgetCalendar = new PopupWidget(); 
    widgetCalendar.setActions(switches);
    
    widgetYear  = new TextFieldWidget("",5+1);
    widgetYear.setSelectAllOnFocus(true);
    widgetYear.addChangeListener(changeSupport);
    
    widgetMonth = new ChoiceWidget(new Object[0], null);
    widgetMonth.setIgnoreCase(true);
    widgetMonth.setSelectAllOnFocus(true);
    widgetMonth.addChangeListener(changeSupport);

    widgetDay   = new TextFieldWidget("",2+1);
    widgetDay.setSelectAllOnFocus(true);
    widgetDay.addChangeListener(changeSupport);
    
    
    setLayout(LAYOUT.copy()); 
    
    add(widgetCalendar);
    
    String format;
    switch (new SimpleDateFormat().toPattern().charAt(0)) {
	    case 'm': case 'M':
        format = "mmm/dd/yyyy"; 
        add(widgetMonth); 
        add(widgetDay) ; 
        add(widgetYear); 
        break;
	    case 'd': case 'D':
	      format = "dd.mmm.yyyy"; 
        add(widgetDay) ; 
        add(widgetMonth); 
        add(widgetYear); 
	      break;
	    default: 
	      format = "yyyy-mmm-dd"; 
	      add(widgetYear); 
	      add(widgetMonth); 
	      add(widgetDay) ; 
	      break;
    }
    
    widgetDay.setToolTipText(format);
    widgetMonth.setToolTipText(format);
    widgetYear.setToolTipText(format);
    
    
    setValue(pit);
    updateStatus();

    
  }
  
  
  public void addChangeListener(ChangeListener l) {
    changeSupport.addChangeListener(l);
  }
  
  
  public void removeChangeListener(ChangeListener l) {
    changeSupport.removeChangeListener(l);
  }
  
  
  public void setValue(PointInTime pit) {

    
    calendar = pit.getCalendar();

    
    widgetCalendar.setToolTipText(calendar.getName());
    
    
    widgetYear.setText(calendar.getDisplayYear(pit.getYear ()));

    
    widgetDay.setText(calendar.getDay(pit.getDay()));

    
    String[] months = calendar.getMonths(true);
    widgetMonth.setValues(Arrays.asList(months));
    try {
      widgetMonth.setSelectedItem(null);
      widgetMonth.setSelectedItem(months[pit.getMonth()]);
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    
    
    updateStatus();
    
    
    getComponent(1).requestFocusInWindow();
    
    
  }
  
  
  public PointInTime getValue() {

    int 
      u = PointInTime.UNKNOWN,
      d = u,
      m = u,
      y = u;
      
    
    String day = widgetDay.getText().trim();
    if (day.length()>0) {
      try {
        d = Integer.parseInt(day) - 1;
      } catch (NumberFormatException e) {
        return null; 
      }
    }
    
    String year = widgetYear.getText().trim();
    if (year.length()>0) {
      try {
        y = calendar.getYear(year);
      } catch (GedcomException e) {
        return null; 
      }
    }
    
    String month = widgetMonth.getText();
    if (month.length()>0) {
      try {
        m = Integer.parseInt(month) - 1;
      } catch (NumberFormatException e) {
        String[] months = calendar.getMonths(true);
        for (m=0;m<months.length;m++)
          if (month.equalsIgnoreCase(months[m])) break;
        if (m==months.length) 
          return null;
      }
    }
    
    
    PointInTime result = new PointInTime(d, m, y, calendar);
    
    
    if ((d==u&&m==u&&y==u)||result.isValid())
      return result;
    
    
    return null;
  }

  
  private void updateStatus() {
    
    PointInTime value = getValue();
    if (value==null) {
      
      widgetCalendar.setEnabled(false);
      widgetCalendar.setIcon(MetaProperty.IMG_ERROR);
    } else {
      
      widgetCalendar.setEnabled(true);
      widgetCalendar.setIcon(calendar.getImage());
    }
  }

  
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
  }

  
  public void requestFocus() {
    getComponent(1).requestFocus();
  }
  
  
  public boolean requestFocusInWindow() {
    return getComponent(1).requestFocusInWindow();
  }
  
  
  private class SwitchCalendar extends Action2 {
    
    private Calendar newCalendar;
    
    private SwitchCalendar(Calendar cal) {
      newCalendar = cal;
      setImage(newCalendar.getImage());
    }
    
    public String getText() {
      WordBuffer result = new WordBuffer();
      result.append(newCalendar.getName());
      result.setFiller(" - ");
      try {
        PointInTime pit = DateWidget.this.getValue().getPointInTime(newCalendar); 
        result.append(pit.getDayOfWeek(true));
        result.append(pit);
      } catch (Throwable t) {
      }
      return result.toString();
    }
    
    protected void execute() {
      PointInTime pit = DateWidget.this.getValue();
      if (pit!=null) {
        try {
          pit.set(newCalendar);
        } catch (GedcomException e) {
          WindowManager wm = WindowManager.getInstance(DateWidget.this);
          if (wm==null) {
            Logger.getLogger("genj.util.swing").info(e.getMessage());
          } else {
            Action[] actions = { Action2.ok(),  new Action2(Calendar.TXT_CALENDAR_RESET) };
            int rc = wm.openDialog(null, Calendar.TXT_CALENDAR_SWITCH, WindowManager.ERROR_MESSAGE, e.getMessage(), actions, DateWidget.this);
            if (rc==0) 
              return;
          }
          pit = new PointInTime(newCalendar);
        }
        
        setValue(pit);
      }
      
      updateStatus();
    }
  } 
  
} 

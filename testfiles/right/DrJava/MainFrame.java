

package edu.rice.cs.drjava.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.dnd.*;
import java.beans.*;

import java.io.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.datatransfer.*;
import java.lang.ref.WeakReference;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.DrJavaRoot;
import edu.rice.cs.drjava.RemoteControlClient;
import edu.rice.cs.drjava.RemoteControlServer;
import edu.rice.cs.drjava.platform.*;
import edu.rice.cs.drjava.config.*;
import edu.rice.cs.drjava.model.*;
import edu.rice.cs.drjava.model.compiler.CompilerListener;
import edu.rice.cs.drjava.model.definitions.NoSuchDocumentException;
import edu.rice.cs.drjava.model.definitions.DefinitionsDocument;
import edu.rice.cs.drjava.model.definitions.DocumentUIListener;
import edu.rice.cs.drjava.model.definitions.ClassNameNotFoundException;
import edu.rice.cs.drjava.model.definitions.InvalidPackageException;
import edu.rice.cs.drjava.model.debug.*;
import edu.rice.cs.drjava.model.repl.*;
import edu.rice.cs.drjava.model.javadoc.JavadocModel;
import edu.rice.cs.drjava.ui.config.ConfigFrame;
import edu.rice.cs.drjava.ui.predictive.PredictiveInputFrame;
import edu.rice.cs.drjava.ui.predictive.PredictiveInputModel;
import edu.rice.cs.drjava.ui.ClipboardHistoryFrame;
import edu.rice.cs.drjava.ui.RegionsTreePanel;
import edu.rice.cs.drjava.project.*;

import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Runnable1;
import edu.rice.cs.plt.lambda.Thunk;

import edu.rice.cs.util.XMLConfig;
import edu.rice.cs.util.FileOpenSelector;
import edu.rice.cs.util.FileOps;
import edu.rice.cs.util.OperationCanceledException;
import edu.rice.cs.util.StringOps;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.classloader.ClassFileError;
import edu.rice.cs.util.docnavigation.*;
import edu.rice.cs.util.swing.Utilities;
import edu.rice.cs.util.swing.*;

import static edu.rice.cs.drjava.ui.RecentFileManager.*;
import static edu.rice.cs.drjava.ui.predictive.PredictiveInputModel.*;
import static edu.rice.cs.util.XMLConfig.XMLConfigException;
import static edu.rice.cs.plt.object.ObjectUtil.hash;


public class MainFrame extends SwingFrame implements ClipboardOwner, DropTargetListener {
  private final static edu.rice.cs.util.Log _log = new edu.rice.cs.util.Log("MainFrame.txt", false);
  
  private static final int INTERACTIONS_TAB = 0;
  private static final int CONSOLE_TAB = 1;
  private static final String ICON_PATH = "/edu/rice/cs/drjava/ui/icons/";
  private static final String DEBUGGER_OUT_OF_SYNC =
    " Current document is out of sync with the debugger and should be recompiled!";
  
  
  private static final int DEBUG_STEP_TIMER_VALUE = 2000;
  
  
  
  
  private volatile AbstractGlobalModel _model;
  
  
  private volatile ModelListener _mainListener; 
  
  
  private HashMap<OpenDefinitionsDocument, JScrollPane> _defScrollPanes;
  
  
  private volatile DefinitionsPane _currentDefPane;
  
  
  private volatile DefinitionsDocument _currentDefDoc;
  
  
  private volatile String _fileTitle = "";
  
  
  public final LinkedList<TabbedPanel>  _tabs = new LinkedList<TabbedPanel>();
  public final JTabbedPane _tabbedPane = new JTabbedPane();
  private volatile DetachedFrame _tabbedPanesFrame;
  public volatile Component _lastFocusOwner;
  private volatile CompilerErrorPanel _compilerErrorPanel;
  private volatile InteractionsPane _consolePane;
  private volatile JScrollPane _consoleScroll;  
  private volatile ConsoleController _consoleController;  
  private volatile InteractionsPane _interactionsPane;
  private volatile JPanel _interactionsContainer;  
  private volatile InteractionsController _interactionsController;  
  private volatile JUnitPanel _junitErrorPanel;
  private volatile JavadocErrorPanel _javadocErrorPanel;
  private volatile FindReplacePanel _findReplace;
  private volatile BreakpointsPanel _breakpointsPanel;
  volatile BookmarksPanel _bookmarksPanel;
  private final LinkedList<Pair<FindResultsPanel, Map<MovingDocumentRegion, HighlightManager.HighlightInfo>>> 
    _findResults = new LinkedList<Pair<FindResultsPanel, Map<MovingDocumentRegion, HighlightManager.HighlightInfo>>>();
  
  private volatile boolean _showDebugger;  
  
  private volatile InteractionsScriptController _interactionsScriptController;
  private volatile InteractionsScriptPane _interactionsScriptPane;
  private volatile DebugPanel _debugPanel;
  private volatile DetachedFrame _debugFrame;
  
  
  
  
  private final JPanel _statusBar = new JPanel(new BorderLayout()); 
  private final JLabel _statusField = new JLabel();
  private final JLabel _statusReport = new JLabel();  
  private final JLabel _currLocationField = new JLabel();
  private final PositionListener _posListener = new PositionListener();
  
  
  private volatile JSplitPane _docSplitPane;
  private volatile JSplitPane _debugSplitPane;
  JSplitPane _mainSplit;
  
  
  private volatile JButton _compileButton;
  private volatile JButton _closeButton;
  private volatile JButton _undoButton;
  private volatile JButton _redoButton;
  private volatile JButton _runButton;
  private volatile JButton _junitButton;
  private volatile JButton _errorsButton;
  
  private final JToolBar _toolBar = new JToolBar();
  private final JFileChooser _interactionsHistoryChooser = new JFileChooser();
  
  
  private final JMenuBar _menuBar = new MenuBar();
  private volatile JMenu _fileMenu;
  private volatile JMenu _editMenu;
  private volatile JMenu _toolsMenu;
  private volatile JMenu _projectMenu;
  private volatile JMenu _languageLevelMenu;
  private volatile JMenu _helpMenu;
  
  private volatile JMenu _debugMenu;
  private volatile JMenuItem _debuggerEnabledMenuItem;
  
  
  private JPopupMenu _interactionsPanePopupMenu;
  private JPopupMenu _consolePanePopupMenu;
  
  
  private volatile ConfigFrame _configFrame;
  private final HelpFrame _helpFrame = new HelpFrame();
  private final QuickStartFrame _quickStartFrame = new QuickStartFrame();
  private volatile AboutDialog _aboutDialog;
  private volatile RecentDocFrame _recentDocFrame;    
  

  
  
  private volatile RecentFileManager _recentFileManager;
  
  
  private volatile RecentFileManager _recentProjectManager;
  
  private volatile File _currentProjFile;
  
  
  private volatile Timer _debugStepTimer;
  
  
  private volatile Timer _automaticTraceTimer;
  
  
  private volatile HighlightManager.HighlightInfo _currentLocationHighlight = null;
  
  
  private final IdentityHashMap<Breakpoint, HighlightManager.HighlightInfo> _documentBreakpointHighlights =
    new IdentityHashMap<Breakpoint, HighlightManager.HighlightInfo>();
  
  
  private final IdentityHashMap<OrderedDocumentRegion, HighlightManager.HighlightInfo> _documentBookmarkHighlights =
    new IdentityHashMap<OrderedDocumentRegion, HighlightManager.HighlightInfo>();
  
  
  private volatile long _lastChangeTime = 0;
  
  
  private volatile boolean _promptBeforeQuit;
  
  
  volatile private ConfigOptionListeners.SlaveJVMXMXListener _slaveJvmXmxListener;
  
  
  volatile private ConfigOptionListeners.MasterJVMXMXListener _masterJvmXmxListener;
  
  
  protected java.util.HashMap<Window,WindowAdapter> _modalWindowAdapters 
    = new java.util.HashMap<Window,WindowAdapter>();
  
  
  protected volatile Window _modalWindowAdapterOwner = null;
  
  
  private volatile JFileChooser _openChooser;
  
  
  private volatile JFileChooser _openProjectChooser;
  
  
  private volatile JFileChooser _saveChooser;
  
  
  private final javax.swing.filechooser.FileFilter _javaSourceFilter = new JavaSourceFilter();
  
  
  private final javax.swing.filechooser.FileFilter _projectFilter = new javax.swing.filechooser.FileFilter() {
    public boolean accept(File f) {
      return f.isDirectory() || 
        f.getPath().endsWith(PROJECT_FILE_EXTENSION) ||
        f.getPath().endsWith(PROJECT_FILE_EXTENSION2) ||
        f.getPath().endsWith(OLD_PROJECT_FILE_EXTENSION);
    }
    public String getDescription() { 
      return "DrJava Project Files (*"+PROJECT_FILE_EXTENSION+", *"+PROJECT_FILE_EXTENSION2+", *"+OLD_PROJECT_FILE_EXTENSION+")";
    }
  };
  
  
  private final javax.swing.filechooser.FileFilter _anyFileFilter = new javax.swing.filechooser.FileFilter() {
    public boolean accept(File f) { return true; }
    public String getDescription() { return "All files (*.*)"; }
  };
  
    
  
  private ExecutorService _threadPool = Executors.newCachedThreadPool();
  
  
  
  
  private final FileOpenSelector _openSelector = new FileOpenSelector() {
    public File[] getFiles() throws OperationCanceledException {
      
      _openChooser.resetChoosableFileFilters();
      
      _openChooser.setFileFilter(_javaSourceFilter);
      return getOpenFiles(_openChooser);
    }
  };
  
  
  private final FileOpenSelector _openFileOrProjectSelector = new FileOpenSelector() {
    public File[] getFiles() throws OperationCanceledException {
      
      _openChooser.resetChoosableFileFilters();
      
      _openChooser.addChoosableFileFilter(_projectFilter);
      _openChooser.setFileFilter(_javaSourceFilter);
      return getOpenFiles(_openChooser);
    }
  };
  
  
  private final FileOpenSelector _openProjectSelector = new FileOpenSelector() {
    public File[] getFiles() throws OperationCanceledException {
      File[] retFiles = getOpenFiles(_openProjectChooser);
      return retFiles;
    }
  };
  
  
  private final FileOpenSelector _openAnyFileSelector = new FileOpenSelector() {
    public File[] getFiles() throws OperationCanceledException {
      _openChooser.resetChoosableFileFilters();
      _openChooser.setFileFilter(_anyFileFilter);
      return getOpenFiles(_openChooser);
    }
  };
  
  
  private final FileSaveSelector _saveSelector = new FileSaveSelector() {
    public File getFile() throws OperationCanceledException { return getSaveFile(_saveChooser); }
    public boolean warnFileOpen(File f) { return _warnFileOpen(f); }
    public boolean verifyOverwrite() { return _verifyOverwrite(); }
    public boolean shouldSaveAfterFileMoved(OpenDefinitionsDocument doc, File oldFile) {
      _model.setActiveDocument(doc);
      String text = "File " + oldFile.getAbsolutePath() +
        "\ncould not be found on disk!  It was probably moved\n" +
        "or deleted.  Would you like to save it in a new file?";
      int rc = JOptionPane.showConfirmDialog(MainFrame.this, text, "File Moved or Deleted", JOptionPane.YES_NO_OPTION);
      return (rc == JOptionPane.YES_OPTION);
    }
  };
  
  
  private final FileSaveSelector _saveAsSelector = new FileSaveSelector() {
    public File getFile() throws OperationCanceledException { return getSaveFile(_saveChooser); }
    public boolean warnFileOpen(File f) { return _warnFileOpen(f); }
    public boolean verifyOverwrite() { return _verifyOverwrite(); }
    public boolean shouldSaveAfterFileMoved(OpenDefinitionsDocument doc, File oldFile) { return true; }
  };
  
  
  private final JavadocDialog _javadocSelector = new JavadocDialog(this);
  
    
  private DirectoryChooser _folderChooser;
  private final JCheckBox _openRecursiveCheckBox = new JCheckBox("Open folders recursively");
  
  private final Action _moveToAuxiliaryAction = new AbstractAction("Include With Project") {
    { 
      String msg = 
      "<html>Open this document each time this project is opened.<br>"+
      "This file would then be compiled and tested with the<br>"+
      "rest of the project.</html>";
      putValue(Action.SHORT_DESCRIPTION, msg);
    }
    public void actionPerformed(ActionEvent ae) { _moveToAuxiliary(); }
  };
  private final Action _removeAuxiliaryAction = new AbstractAction("Do Not Include With Project") {
    { putValue(Action.SHORT_DESCRIPTION, "Do not open this document next time this project is opened."); }
    public void actionPerformed(ActionEvent ae) { _removeAuxiliary(); }
  };
  private final Action _moveAllToAuxiliaryAction = new AbstractAction("Include All With Project") {
    { 
      String msg = 
      "<html>Open these documents each time this project is opened.<br>"+
      "These files would then be compiled and tested with the<br>"+
      "rest of the project.</html>";
      putValue(Action.SHORT_DESCRIPTION, msg);
    }
    public void actionPerformed(ActionEvent ae) { _moveAllToAuxiliary(); }
  };
  
  private final Action _removeAllAuxiliaryAction = new AbstractAction("Do Not Include Any With Project") {
    { putValue(Action.SHORT_DESCRIPTION, "Do not open these documents next time this project is opened."); }
    public void actionPerformed(ActionEvent ae) { _removeAllAuxiliary(); }
  };
  
  
  private final Action _newAction = new AbstractAction("New") {
    public void actionPerformed(ActionEvent ae) {

      _new();
    }
  };
  
  private final Action _newProjectAction = new AbstractAction("New") {
    public void actionPerformed(ActionEvent ae) { _newProject(); }
  };
  
  private volatile AbstractAction _runProjectAction = new AbstractAction("Run Main Class of Project") {
    public void actionPerformed(ActionEvent ae) { _runProject(); }
  };
  
  
  private volatile JarOptionsDialog _jarOptionsDialog;
  
  
  private void initJarOptionsDialog() {
    if (DrJava.getConfig().getSetting(DIALOG_JAROPTIONS_STORE_POSITION).booleanValue())
      _jarOptionsDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_JAROPTIONS_STATE));  
  }
  
  
  public void resetJarOptionsDialogPosition() {
    _jarOptionsDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_JAROPTIONS_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_JAROPTIONS_STATE, "default");
    }
  }
  private final Action _jarProjectAction = new AbstractAction("Create Jar File from Project...") {
    public void actionPerformed(ActionEvent ae) { _jarOptionsDialog.setVisible(true); }
  };
  
  
  private void initTabbedPanesFrame() {
    if (DrJava.getConfig().getSetting(DIALOG_TABBEDPANES_STORE_POSITION).booleanValue()) {
      _tabbedPanesFrame.setFrameState(DrJava.getConfig().getSetting(DIALOG_TABBEDPANES_STATE));  
    }
  }
  
  
  public void resetTabbedPanesFrame() {
    _tabbedPanesFrame.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_TABBEDPANES_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_TABBEDPANES_STATE, "default");
    }
  }
  
  
  private final Action _detachTabbedPanesAction = new AbstractAction("Detach Tabbed Panes") {
    public void actionPerformed(ActionEvent ae) { 
      JMenuItem m = (JMenuItem)ae.getSource();
      boolean b = m.isSelected();
      DrJava.getConfig().setSetting(DETACH_TABBEDPANES, b);
      _tabbedPanesFrame.setDisplayInFrame(b);
    }
  };
  
  
  private JMenuItem _detachTabbedPanesMenuItem;
  
  
  private void initDebugFrame() {
    if (_debugFrame==null) return; 
    if (DrJava.getConfig().getSetting(DIALOG_DEBUGFRAME_STORE_POSITION).booleanValue()) {
      _debugFrame.setFrameState(DrJava.getConfig().getSetting(DIALOG_DEBUGFRAME_STATE));  
    }
  }
  
  
  public void resetDebugFrame() {
    if (_debugFrame==null) return; 
    _debugFrame.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_DEBUGFRAME_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_DEBUGFRAME_STATE, "default");
    }
  }
  
  
  private final Action _detachDebugFrameAction = new AbstractAction("Detach Debugger") {
    public void actionPerformed(ActionEvent ae) { 
      if (_debugFrame==null) return; 
      JMenuItem m = (JMenuItem)ae.getSource();
      boolean b = m.isSelected();
      DrJava.getConfig().setSetting(DETACH_DEBUGGER, b);
      _debugFrame.setDisplayInFrame(b);
    }
  };
  
  
  private JMenuItem _detachDebugFrameMenuItem;
  
  
  private final Action _newJUnitTestAction = new AbstractAction("New JUnit Test Case...") {
    public void actionPerformed(ActionEvent ae) {
      String testName = JOptionPane.showInputDialog(MainFrame.this,
                                                    "Please enter a name for the test class:",
                                                    "New JUnit Test Case",
                                                    JOptionPane.QUESTION_MESSAGE);
      if (testName != null) {
        String ext;
        for(int i=0; i < DrJavaRoot.LANGUAGE_LEVEL_EXTENSIONS.length; i++) {
          ext = "." + DrJavaRoot.LANGUAGE_LEVEL_EXTENSIONS[i];
          if (testName.endsWith(ext)) testName = testName.substring(0, testName.length() - ext.length());
        }
        
        _model.newTestCase(testName, false, false);
      }
    }
  };
  
  
  private final Action _openAction = new AbstractAction("Open...") {
    public void actionPerformed(ActionEvent ae) {
      _open();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _openFolderAction  = new AbstractAction("Open Folder...") {
    public void actionPerformed(ActionEvent ae) { 
      _openFolder();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _openFileOrProjectAction = new AbstractAction("Open...") {
    public void actionPerformed(ActionEvent ae) { 
      _openFileOrProject(); 
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _openProjectAction = new AbstractAction("Open...") {
    public void actionPerformed(ActionEvent ae) { _openProject(); }
  };
  
  private final Action _closeProjectAction = new AbstractAction("Close") {
    public void actionPerformed(ActionEvent ae) { 
      closeProject();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  
  private final Action _closeAction = new AbstractAction("Close") {
    public void actionPerformed(ActionEvent ae) { 
      _close();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _closeAllAction = new AbstractAction("Close All") {
    public void actionPerformed(ActionEvent ae) { 
      _closeAll();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _closeFolderAction = new AbstractAction("Close Folder") {
    public void actionPerformed(ActionEvent ae) { 
      _closeFolder();
      _findReplace.updateFirstDocInSearch();
      
      
      
      
      _model.getDocumentNavigator().selectDocument(_currentDefPane.getOpenDefDocument());
    }
  };
  
  
  private final Action _openAllFolderAction = new AbstractAction("Open All Files") {
    public void actionPerformed(ActionEvent ae) {
      
      List<File> l= _model.getDocumentNavigator().getSelectedFolders();
      for(File f: l) {
        File fAbs = new File(_model.getProjectRoot(), f.toString());
        _openFolder(fAbs, false);  
      }
      
      
      
      
      
      
      
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _openOneFolderAction = new AbstractAction("Open File in Folder") {
    public void actionPerformed(ActionEvent ae)  { 
      _open();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  public final Action _newFileFolderAction = new AbstractAction("Create New File in Folder") {
    public void actionPerformed(ActionEvent ae)  {
      
      _new();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private volatile AbstractAction _junitFolderAction = new AbstractAction("Test Folder") {
    public final void actionPerformed(ActionEvent ae) { _junitFolder(); }
  };
  
  
  private final Action _saveAction = new AbstractAction("Save") {
    public final void actionPerformed(ActionEvent ae) { _save(); }
  };
  
  
  public long getLastChangeTime() { return _lastChangeTime; }
  
  
  public void pack() {
    Utilities.invokeAndWait(new Runnable() { public void run() { packHelp(); } });
  }
  
  
  private void packHelp() { super.pack(); }
  
  
  public boolean saveEnabledHuh() { return _saveAction.isEnabled(); }
  
  
  private final Action _saveAsAction = new AbstractAction("Save As...") {
    public void actionPerformed(ActionEvent ae) { _saveAs(); }
  };
  
  
  private final Action _renameAction = new AbstractAction("Rename") {
    public void actionPerformed(ActionEvent ae) { _rename(); }
  };  
  
  private final Action _saveProjectAction = new AbstractAction("Save") {
    public void actionPerformed(ActionEvent ae) {
      _saveAll();  
    }
  };
  
  private final Action _saveProjectAsAction = new AbstractAction("Save As...") {
    public void actionPerformed(ActionEvent ae) {
      if (_saveProjectAs()) {  
        _saveAll();  
      }
    }
  };
  
  private final Action _exportProjectInOldFormatAction = 
    new AbstractAction("Export Project In Old \"" + OLD_PROJECT_FILE_EXTENSION + "\" Format") {
    public void actionPerformed(ActionEvent ae) {
      File cpf = _currentProjFile;
      _currentProjFile = FileOps.NULL_FILE;
      if (_saveProjectAs()) {  
        _saveAllOld();  
      }
      _currentProjFile = cpf;
      _model.setProjectFile(cpf);
      _recentProjectManager.updateOpenFiles(cpf);
    }
  };
  
  
  private final Action _revertAction = new AbstractAction("Revert to Saved") {
    public void actionPerformed(ActionEvent ae) {
      String title = "Revert to Saved?";
      
      
      int count = _model.getDocumentNavigator().getDocumentSelectedCount();
      String message;
      if (count==1) {
        message = "Are you sure you want to revert the current " +
          "file to the version on disk?";
      }
      else {
        message = "Are you sure you want to revert the " + count +
          " selected files to the versions on disk?";
      }
      
      int rc;
      Object[] options = {"Yes", "No"};  
      rc = JOptionPane.showOptionDialog(MainFrame.this, message, title, JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
      if (rc == JOptionPane.YES_OPTION) {
        _revert();
      }
    }
  };
  
  
  
  
  final Action _saveAllAction = new AbstractAction("Save All") {
    public void actionPerformed(ActionEvent ae) { _saveAll(); }
  };
  
  
  private final Action _printDefDocAction = new AbstractAction("Print...") {
    public void actionPerformed(ActionEvent ae) { _printDefDoc(); }
  };
  
  
  private final Action _printConsoleAction = new AbstractAction("Print Console...") {
    public void actionPerformed(ActionEvent ae) { _printConsole(); }
  };
  
  
  private final Action _printInteractionsAction = new AbstractAction("Print Interactions...") {
    public void actionPerformed(ActionEvent ae) { _printInteractions(); }
  };
  
  
  private final Action _printDefDocPreviewAction = new AbstractAction("Print Preview...") {
    public void actionPerformed(ActionEvent ae) { _printDefDocPreview(); }
  };
  
  
  private final Action _printConsolePreviewAction = new AbstractAction("Print Preview...") {
    public void actionPerformed(ActionEvent ae) { _printConsolePreview(); }
  };
  
  
  private final Action _printInteractionsPreviewAction = new AbstractAction("Print Preview...") {
    public void actionPerformed(ActionEvent ae) { _printInteractionsPreview(); }
  };
  
  
  private final Action _pageSetupAction = new AbstractAction("Page Setup...") {
    public void actionPerformed(ActionEvent ae) { _pageSetup(); }
  };
  




  
  
  private final Action _compileAction = new AbstractAction("Compile Current Document") {
    public void actionPerformed(ActionEvent ae) { 
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes(); 
      updateStatusField("Compiling " + _fileTitle);
      _compile();
      updateStatusField("Compilation of current document completed");
    }
  };
  
  
  private volatile AbstractAction _compileProjectAction = new AbstractAction("Compile Project") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes();
      updateStatusField("Compiling all source files in open project");
      _compileProject(); 
      _findReplace.updateFirstDocInSearch();
      updateStatusField("Compilation of open project completed");
    }
  };
  
  
  private volatile AbstractAction _compileFolderAction = new AbstractAction("Compile Folder") {
    public void actionPerformed(ActionEvent ae) { 
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes();
      updateStatusField("Compiling all sources in current folder");
      _compileFolder();
      _findReplace.updateFirstDocInSearch();
      updateStatusField("Compilation of folder completed");
    }
  };
  
  
  private volatile AbstractAction _compileAllAction = new AbstractAction("Compile All Documents") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes();
      _compileAll();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private volatile AbstractAction _cleanAction = new AbstractAction("Clean Build Directory") {
    public void actionPerformed(ActionEvent ae) { _clean(); }
  };
  
  
  private volatile AbstractAction _autoRefreshAction = new AbstractAction("Auto-Refresh Project") {
    public void actionPerformed(ActionEvent ae) { _model.autoRefreshProject(); }
  };
  
  
  private volatile AbstractAction _runAction = new AbstractAction("Run Document's Main Method") {
    public void actionPerformed(ActionEvent ae) { _runMain(); }
  };

  
  private volatile AbstractAction _runAppletAction = new AbstractAction("Run Document as Applet") {
    public void actionPerformed(ActionEvent ae) { _runApplet(); }
  };
  
  
  private volatile AbstractAction _junitAction = new AbstractAction("Test Current Document") {
    public void actionPerformed(ActionEvent ae) { 
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) _mainSplit.resetToPreferredSizes();
      _junit(); 
    }
  };
  
  
  private volatile AbstractAction _junitAllAction = new AbstractAction("Test All Documents") {
    public void actionPerformed(ActionEvent e) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) _mainSplit.resetToPreferredSizes();
      _junitAll();
      _findReplace.updateFirstDocInSearch();
    }
    
  };
  
  
  private volatile AbstractAction _junitProjectAction = new AbstractAction("Test Project") {
    public void actionPerformed(ActionEvent e) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) _mainSplit.resetToPreferredSizes();
      _junitProject();
      _findReplace.updateFirstDocInSearch();
    }
  };
  
  
  private final Action _javadocAllAction = new AbstractAction("Javadoc All Documents") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes();
      try {
        
        JavadocModel jm = _model.getJavadocModel();
        File suggestedDir = jm.suggestJavadocDestination(_model.getActiveDocument());
        _javadocSelector.setSuggestedDir(suggestedDir);
        jm.javadocAll(_javadocSelector, _saveSelector);
      }
      catch (IOException ioe) { _showIOError(ioe); }
      finally {
        
      }
    }
  };
  
  
  private final Action _javadocCurrentAction = new AbstractAction("Preview Javadoc for Current Document") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes();
      try { _model.getActiveDocument().generateJavadoc(_saveSelector); }
      catch (IOException ioe) { _showIOError(ioe); }
    }
  };
  
  
  final Action cutAction = new DefaultEditorKit.CutAction() {
    public void actionPerformed(ActionEvent e) {
      Component c = MainFrame.this.getFocusOwner();
      super.actionPerformed(e);
      if (_currentDefPane.hasFocus()) {
        String s = Utilities.getClipboardSelection(c);
        if (s != null && s.length() != 0) { ClipboardHistoryModel.singleton().put(s); }
      }
      if (c != null) c.requestFocusInWindow();
    }
  };
  
  
  final Action copyAction = new DefaultEditorKit.CopyAction() {
    public void actionPerformed(ActionEvent e) {
      Component c = MainFrame.this.getFocusOwner();
      super.actionPerformed(e);
      if (_currentDefPane.hasFocus() && _currentDefPane.getSelectedText() != null) {
        String s = Utilities.getClipboardSelection(c);
        if (s != null && s.length() != 0) { ClipboardHistoryModel.singleton().put(s); }
      }
      if (c != null) c.requestFocusInWindow();
    }
  };
  
  
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    
  }
  
  
  final Action pasteAction = new DefaultEditorKit.PasteAction() {
    public void actionPerformed(ActionEvent e) {
      
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable contents = clipboard.getContents(null);
      if ((contents != null) && (contents.isDataFlavorSupported(DataFlavor.stringFlavor))) {
        try {
          String result = (String)contents.getTransferData(DataFlavor.stringFlavor);
          StringBuilder sb = new StringBuilder();
          for(int i=0; i<result.length(); ++i) {
            char ch = result.charAt(i);
            if ((ch<32) && (ch!='\n')) sb.append(' ');
            else sb.append(ch);
          }
          StringSelection stringSelection = new StringSelection(sb.toString());
          clipboard.setContents(stringSelection, stringSelection);
        }
        catch (UnsupportedFlavorException ex) {  }
        catch (IOException ex) {  }
      }

      Component c = MainFrame.this.getFocusOwner();
      if (_currentDefPane.hasFocus()) {
        _currentDefPane.endCompoundEdit();


        super.actionPerformed(e);
        _currentDefPane.endCompoundEdit(); 

      }
      else super.actionPerformed(e);
      
      if (c != null) c.requestFocusInWindow();      
    }
  };
  
  
  public void resetClipboardHistoryDialogPosition() {
    if (DrJava.getConfig().getSetting(DIALOG_CLIPBOARD_HISTORY_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_CLIPBOARD_HISTORY_STATE, "default");
    }
  }
  
  
  private ClipboardHistoryFrame _clipboardHistoryDialog = null;
  
  
  private final Action _pasteHistoryAction = new AbstractAction("Paste from History...") {
    public void actionPerformed(final ActionEvent ae) {
      final ClipboardHistoryFrame.CloseAction cancelAction = new ClipboardHistoryFrame.CloseAction() {
        public Object value(String s) {
          
          if ((DrJava.getConfig().getSetting(DIALOG_CLIPBOARD_HISTORY_STORE_POSITION).booleanValue())
                && (_clipboardHistoryDialog != null) && (_clipboardHistoryDialog.getFrameState() != null)) {
            DrJava.getConfig().
              setSetting(DIALOG_CLIPBOARD_HISTORY_STATE, (_clipboardHistoryDialog.getFrameState().toString()));
          }
          else {
            
            DrJava.getConfig().setSetting(DIALOG_CLIPBOARD_HISTORY_STATE, DIALOG_CLIPBOARD_HISTORY_STATE.getDefault());
          }
          return null;
        }
      };
      ClipboardHistoryFrame.CloseAction okAction = new ClipboardHistoryFrame.CloseAction() {
        public Object value(String s) {
          cancelAction.value(null);
          
          StringSelection ssel = new StringSelection(s);
          Clipboard cb = MainFrame.this.getToolkit().getSystemClipboard();
          if (cb != null) {
            cb.setContents(ssel, MainFrame.this);
            pasteAction.actionPerformed(ae);
          }
          return null;
        }
      };
      
      _clipboardHistoryDialog = new ClipboardHistoryFrame(MainFrame.this, 
                                                          "Clipboard History", ClipboardHistoryModel.singleton(),
                                                          okAction, cancelAction);
      if (DrJava.getConfig().getSetting(DIALOG_CLIPBOARD_HISTORY_STORE_POSITION).booleanValue()) {
        _clipboardHistoryDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_CLIPBOARD_HISTORY_STATE));
      }
      _clipboardHistoryDialog.setVisible(true);
    }
  };
  
  
  private final Action _copyInteractionToDefinitionsAction =
    new AbstractAction("Lift Current Interaction to Definitions") {
    public void actionPerformed(ActionEvent a) {
      String text = _interactionsController.getDocument().getCurrentInput();
      if (! text.equals("")) {
        _putTextIntoDefinitions(text + "\n");
        return;
      }
      try { text = _interactionsController.getDocument().lastEntry(); }
      catch(Exception e) { return; } 
      
      
      _putTextIntoDefinitions(text + "\n");
      return;
    }
  };
  
  
  
  
  private final DelegatingAction _undoAction = new DelegatingAction() {
    public void actionPerformed(ActionEvent e) {
      _currentDefPane.endCompoundEdit();
      super.actionPerformed(e);
      _currentDefPane.requestFocusInWindow();
      OpenDefinitionsDocument doc = _model.getActiveDocument();

      _saveAction.setEnabled(doc.isModifiedSinceSave() || doc.isUntitled());

    }
  };
  
  
  private final DelegatingAction _redoAction = new DelegatingAction() {
    public void actionPerformed(ActionEvent e) {
      super.actionPerformed(e);
      _currentDefPane.requestFocusInWindow();
      OpenDefinitionsDocument doc = _model.getActiveDocument();
      _saveAction.setEnabled(doc.isModifiedSinceSave() || doc.isUntitled());
    }
  };
  
  
  private final Action _quitAction = new AbstractAction("Quit") {
    public void actionPerformed(ActionEvent ae) { quit(); }
  };
  
  
  
  private final Action _forceQuitAction = new AbstractAction("Force Quit") {
    public void actionPerformed(ActionEvent ae) { _forceQuit(); }
  };
  
  
  private final Action _selectAllAction = new AbstractAction("Select All") {
    public void actionPerformed(ActionEvent ae) { _selectAll(); }
  };
  
  
  private void _showFindReplaceTab(boolean showDetachedWindow) {
    if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
      _mainSplit.resetToPreferredSizes(); 
    final boolean wasDisplayed = isDisplayed(_findReplace);
    showTab(_findReplace, showDetachedWindow);
    if (!wasDisplayed) {
      _findReplace.beginListeningTo(_currentDefPane);
    }
    _findReplace.setVisible(true);
    _tabbedPane.setSelectedComponent(_findReplace);
  }
  
  
  private final Action _findReplaceAction = new AbstractAction("Find/Replace") {
    public void actionPerformed(ActionEvent ae) {
      _showFindReplaceTab(true);
      _findReplace.requestFocusInWindow();
      
      EventQueue.invokeLater(new Runnable() { public void run() { _findReplace.requestFocusInWindow(); } });
    }
  };
  
  
  private final Action _findNextAction = new AbstractAction("Find Next") {
    public void actionPerformed(ActionEvent ae) {
      _showFindReplaceTab(false);
      if (!DrJava.getConfig().getSetting(FIND_REPLACE_FOCUS_IN_DEFPANE).booleanValue()) {
        
        EventQueue.invokeLater(new Runnable() { public void run() { _findReplace.requestFocusInWindow(); } });
      }
      _findReplace.findNext();

      
    }
  };
  
  
  private final Action _findPrevAction = new AbstractAction("Find Previous") {
    public void actionPerformed(ActionEvent ae) {
      _showFindReplaceTab(false);
      if (!DrJava.getConfig().getSetting(FIND_REPLACE_FOCUS_IN_DEFPANE).booleanValue()) {
        
        EventQueue.invokeLater(new Runnable() { public void run() { _findReplace.requestFocusInWindow(); } });
      }
      _findReplace.findPrevious();
      _currentDefPane.requestFocusInWindow();
    }
  };
  
  
  private final Action _gotoLineAction = new AbstractAction("Go to Line...") {
    public void actionPerformed(ActionEvent ae) {
      int pos = _gotoLine();
      _currentDefPane.requestFocusInWindow();
      if (pos != -1) _currentDefPane.setCaretPosition(pos);  
      
    }
  };
  
  private static abstract class ClassNameAndPackageEntry implements Comparable<ClassNameAndPackageEntry> {
    
    public abstract String getClassName();
    
    public abstract String getFullPackage();
    
    public int compareTo(ClassNameAndPackageEntry other) {
      int res = getClassName().toLowerCase().compareTo(other.getClassName().toLowerCase());
      if (res != 0) { return res; }
      return getFullPackage().toLowerCase().compareTo(other.getFullPackage().toLowerCase());
    }
    
    public boolean equals(Object other) {
      if (other == null || ! (other instanceof ClassNameAndPackageEntry)) return false;  
      ClassNameAndPackageEntry o = (ClassNameAndPackageEntry) other;
      return (getClassName().equals(o.getClassName()) && getFullPackage().equals(o.getFullPackage()));
    }
    public int hashCode() { return hash(getClassName(), getFullPackage()); }
  }
  
  
  public static class GoToFileListEntry extends ClassNameAndPackageEntry {
    public final OpenDefinitionsDocument doc;
    protected String fullPackage = null;
    protected final String str;
    public GoToFileListEntry(OpenDefinitionsDocument d, String s) {
      doc = d;
      str = s;
    }
    public String getFullPackage() {
      if (fullPackage != null) { return fullPackage; }
      fullPackage = "";
      if (doc!=null) {
        try {
          fullPackage = doc.getPackageName();
          if (fullPackage.length() > 0) { fullPackage += '.'; }
        }
        catch(Exception e) { fullPackage = ""; }
      }
      return fullPackage;
    }
    public String getClassName() { return str; }
    public String toString() { return str; }
  }
  
  
  public void resetGotoFileDialogPosition() {
    initGotoFileDialog();
    _gotoFileDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_GOTOFILE_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_GOTOFILE_STATE, "default");
    }
  }
  
  
  void initGotoFileDialog() {
    if (_gotoFileDialog == null) {
      PredictiveInputFrame.InfoSupplier<GoToFileListEntry> info = 
        new PredictiveInputFrame.InfoSupplier<GoToFileListEntry>() {
        public String value(GoToFileListEntry entry) {
          final StringBuilder sb = new StringBuilder();
          
          if (entry.doc != null) {
            try {
              try { sb.append(FileOps.stringMakeRelativeTo(entry.doc.getRawFile(), entry.doc.getSourceRoot())); }
              catch(IOException e) { sb.append(entry.doc.getFile()); }
            }
            catch(edu.rice.cs.drjava.model.FileMovedException e) { sb.append(entry + " was moved"); }
            catch(java.lang.IllegalStateException e) { sb.append(entry); }
            catch(InvalidPackageException e) { sb.append(entry); }
          } 
          else sb.append(entry);
          return sb.toString();
        }
      };
      PredictiveInputFrame.CloseAction<GoToFileListEntry> okAction = 
        new PredictiveInputFrame.CloseAction<GoToFileListEntry>() {
        public String getName() { return "OK"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<GoToFileListEntry> p) {
          if (p.getItem() != null) {
            final OpenDefinitionsDocument newDoc = p.getItem().doc;
            final boolean docChanged = ! newDoc.equals(_model.getActiveDocument());
            final boolean docSwitch = _model.getActiveDocument() != newDoc;
            if (docSwitch) _model.setActiveDocument(newDoc);
            final int curLine = newDoc.getCurrentLine();
            final String t = p.getText();
            final int last = t.lastIndexOf(':');
            if (last >= 0) {
              try {
                String end = t.substring(last + 1);
                int val = Integer.parseInt(end);
                
                final int lineNum = Math.max(1, val);
                Runnable command = new Runnable() {
                  public void run() {
                    try { _jumpToLine(lineNum); }  
                    catch (RuntimeException e) { _jumpToLine(curLine); }
                  }
                };
                if (docSwitch) {
                  
                  EventQueue.invokeLater(command);
                }
                else command.run();
              }
              catch(RuntimeException e) {  }
            }
            else if (docChanged) {
              
              addToBrowserHistory();
            }
          }
          hourglassOff();
          return null;
        }
      };
      PredictiveInputFrame.CloseAction<GoToFileListEntry> cancelAction = 
        new PredictiveInputFrame.CloseAction<GoToFileListEntry>() {
        public String getName() { return "Cancel"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<GoToFileListEntry> p) {
          hourglassOff();
          return null;
        }
      };
      java.util.ArrayList<PredictiveInputModel.MatchingStrategy<GoToFileListEntry>> strategies =
        new java.util.ArrayList<PredictiveInputModel.MatchingStrategy<GoToFileListEntry>>();
      strategies.add(new PredictiveInputModel.FragmentLineNumStrategy<GoToFileListEntry>());
      strategies.add(new PredictiveInputModel.PrefixLineNumStrategy<GoToFileListEntry>());
      strategies.add(new PredictiveInputModel.RegExLineNumStrategy<GoToFileListEntry>());
      List<PredictiveInputFrame.CloseAction<GoToFileListEntry>> actions
        = new ArrayList<PredictiveInputFrame.CloseAction<GoToFileListEntry>>();
      actions.add(okAction);
      actions.add(cancelAction);
      _gotoFileDialog = 
        new PredictiveInputFrame<GoToFileListEntry>(MainFrame.this,
                                                    "Go to File",
                                                    true, 
                                                    true, 
                                                    info,
                                                    strategies,
                                                    actions, 1, 
                                                    new GoToFileListEntry(null, "dummyGoto")) {
        public void setOwnerEnabled(boolean b) {
          if (b) { hourglassOff(); } else { hourglassOn(); }
        }
      }; 
      
      
      if (DrJava.getConfig().getSetting(DIALOG_GOTOFILE_STORE_POSITION).booleanValue()) {
        _gotoFileDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_GOTOFILE_STATE));
      }      
    }
  }
  
  
  volatile PredictiveInputFrame<GoToFileListEntry> _gotoFileDialog = null;
  
  
  private final Action _gotoFileAction = new AbstractAction("Go to File...") {
    public void actionPerformed(ActionEvent ae) {
      initGotoFileDialog();
      List<OpenDefinitionsDocument> docs = _model.getOpenDefinitionsDocuments();
      if (docs == null || docs.size() == 0) {
        return; 
      }
      GoToFileListEntry currentEntry = null;
      ArrayList<GoToFileListEntry> list;
      if (DrJava.getConfig().getSetting(DIALOG_GOTOFILE_FULLY_QUALIFIED).booleanValue()) {
        list = new ArrayList<GoToFileListEntry>(2 * docs.size());
      }
      else {
        list = new ArrayList<GoToFileListEntry>(docs.size());
      }
      for(OpenDefinitionsDocument d: docs) {
        GoToFileListEntry entry = new GoToFileListEntry(d, d.toString());
        if (d.equals(_model.getActiveDocument())) currentEntry = entry;
        list.add(entry);
        if (DrJava.getConfig().getSetting(DIALOG_GOTOFILE_FULLY_QUALIFIED).booleanValue()) {
          try {
            try {
              String relative = FileOps.stringMakeRelativeTo(d.getFile(), d.getSourceRoot());
              if (!relative.equals(d.toString())) {
                list.add(new GoToFileListEntry(d, d.getPackageName() + "." + d.toString()));
              }
            }
            catch(IOException e) {  }
            catch(edu.rice.cs.drjava.model.definitions.InvalidPackageException e) {  }
          }
          catch(IllegalStateException e) {  }
        }
      }
      _gotoFileDialog.setItems(true, list); 
      if (currentEntry != null) _gotoFileDialog.setCurrentItem(currentEntry);
      hourglassOn();   
       
      _gotoFileDialog.setVisible(true);
    }
  };
  
  
  void _gotoFileUnderCursor() {

    OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
    String mask = "";
    int loc = getCurrentDefPane().getCaretPosition();
    String s = odd.getText();
    
    int start = loc;
    while(start>0) {
      if (! Character.isJavaIdentifierPart(s.charAt(start-1))) { break; }
      --start;
    }
    while((start<s.length()) && (!Character.isJavaIdentifierStart(s.charAt(start))) && (start<loc)) {
      ++start;
    }
    
    int end = loc-1;
    while(end<s.length()-1) {
      if (! Character.isJavaIdentifierPart(s.charAt(end+1))) { break; }
      ++end;
    }
    if ((start>=0) && (end<s.length())) {
      mask = s.substring(start, end + 1);
    }
    gotoFileMatchingMask(mask);
  }
  
  
  public void gotoFileMatchingMask(String mask) {        
    List<OpenDefinitionsDocument> docs = _model.getOpenDefinitionsDocuments();
    if ((docs == null) || (docs.size() == 0)) return; 
    
    GoToFileListEntry currentEntry = null;
    ArrayList<GoToFileListEntry> list;
    list = new ArrayList<GoToFileListEntry>(docs.size());
    for(OpenDefinitionsDocument d: docs) {
      GoToFileListEntry entry = new GoToFileListEntry(d, d.toString());
      if (d.equals(_model.getActiveDocument())) currentEntry = entry;
      list.add(entry);
    }
    
    PredictiveInputModel<GoToFileListEntry> pim =
      new PredictiveInputModel<GoToFileListEntry>(true, new PrefixStrategy<GoToFileListEntry>(), list);
    pim.setMask(mask);
    

    
    if (pim.getMatchingItems().size() == 1) {
      
      if (pim.getCurrentItem() != null) {
        boolean docChanged = ! pim.getCurrentItem().doc.equals(_model.getActiveDocument());

        _model.setActiveDocument(pim.getCurrentItem().doc);
        if (docChanged) { 
          addToBrowserHistory();
        }
      }
    }
    else {
      
      pim.extendMask(".java");
      if (pim.getMatchingItems().size() == 1) {
        
        if (pim.getCurrentItem() != null) {
          boolean docChanged = !pim.getCurrentItem().doc.equals(_model.getActiveDocument());

          _model.setActiveDocument(pim.getCurrentItem().doc);
          if (docChanged) { 
            addToBrowserHistory();
          }
        }
      }
      else {
        
        pim.setMask(mask);
        if (pim.getMatchingItems().size() == 0) {
          
          mask = pim.getMask();
          while (mask.length()>0) {
            mask = mask.substring(0, mask.length() - 1);
            pim.setMask(mask);
            if (pim.getMatchingItems().size()>0) { break; }
          }
        }       
        initGotoFileDialog();
        _gotoFileDialog.setModel(true, pim); 
        if (currentEntry != null) _gotoFileDialog.setCurrentItem(currentEntry);
        hourglassOn();
        
        if (MainFrame.this.isVisible()) _gotoFileDialog.setVisible(true);
      }
    }
  }
  
  
  final Action gotoFileUnderCursorAction = new AbstractAction("Go to File Under Cursor") {
    public void actionPerformed(ActionEvent ae) { _gotoFileUnderCursor(); }
  };
  
  
  
  public static class JavaAPIListEntry extends ClassNameAndPackageEntry {
    private final String str, fullStr;
    private final URL url;
    public JavaAPIListEntry(String s, String full, URL u) {
      str = s;
      fullStr = full;
      url = u;
    }
    public String toString() { return str; }
    public String getFullString() { return fullStr; }
    public URL getURL() { return url; }
    public String getClassName() { return str; }
    public String getFullPackage() {
      int pos = fullStr.lastIndexOf('.');
      if (pos>=0) { return fullStr.substring(0,pos+1); }
      return "";
    }
  }  
  
  
  public void resetOpenJavadocDialogPosition() {
    initOpenJavadocDialog();
    _openJavadocDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_OPENJAVADOC_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_OPENJAVADOC_STATE, "default");
    }
  }
  
  
  void initOpenJavadocDialog() {
    if (_openJavadocDialog == null) {
      PredictiveInputFrame.InfoSupplier<JavaAPIListEntry> info = 
        new PredictiveInputFrame.InfoSupplier<JavaAPIListEntry>() {
        public String value(JavaAPIListEntry entry) {
          return entry.getFullString();
        }
      };
      PredictiveInputFrame.CloseAction<JavaAPIListEntry> okAction = 
        new PredictiveInputFrame.CloseAction<JavaAPIListEntry>() {
        public String getName() { return "OK"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<JavaAPIListEntry> p) {
          if (p.getItem() != null) {
            PlatformFactory.ONLY.openURL(p.getItem().getURL());
          }
          hourglassOff();
          return null;
        }
      };
      PredictiveInputFrame.CloseAction<JavaAPIListEntry> cancelAction = 
        new PredictiveInputFrame.CloseAction<JavaAPIListEntry>() {
        public String getName() { return "Cancel"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<JavaAPIListEntry> p) {
          hourglassOff();
          return null;
        }
      };
      
      java.util.ArrayList<MatchingStrategy<JavaAPIListEntry>> strategies =
        new java.util.ArrayList<MatchingStrategy<JavaAPIListEntry>>();
      strategies.add(new FragmentStrategy<JavaAPIListEntry>());
      strategies.add(new PrefixStrategy<JavaAPIListEntry>());
      strategies.add(new RegExStrategy<JavaAPIListEntry>());
      List<PredictiveInputFrame.CloseAction<JavaAPIListEntry>> actions
        = new ArrayList<PredictiveInputFrame.CloseAction<JavaAPIListEntry>>();
      actions.add(okAction);
      actions.add(cancelAction);
      _openJavadocDialog = 
        new PredictiveInputFrame<JavaAPIListEntry>(MainFrame.this,
                                                   "Open Java API Javadoc Webpage",
                                                   true, 
                                                   true, 
                                                   info,
                                                   strategies,
                                                   actions, 1, 
                                                   new JavaAPIListEntry("dummyJavadoc", "dummyJavadoc", null)) {
        public void setOwnerEnabled(boolean b) {
          if (b) { hourglassOff(); } else { hourglassOn(); }
        }
      }; 
      
      
      if (DrJava.getConfig().getSetting(DIALOG_OPENJAVADOC_STORE_POSITION).booleanValue()) {
        _openJavadocDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_OPENJAVADOC_STATE));
      }
      generateJavaAPISet();
    }
  }

  
  
  public static Set<JavaAPIListEntry> _generateJavaAPISet(String base,
                                                          String stripPrefix,
                                                          String suffix) {
    
    URL url = MainFrame.class.getResource("/edu/rice/cs/drjava/docs/javaapi"+suffix);
    return _generateJavaAPISet(base, stripPrefix, url);
  }
  
  
  public static Set<JavaAPIListEntry> _generateJavaAPISet(String base,
                                                          String stripPrefix,
                                                          URL url) {
    
    Set<JavaAPIListEntry> s = new HashSet<JavaAPIListEntry>();
    try {
      InputStream urls = url.openStream();
      InputStreamReader is = null;
      BufferedReader br = null;
      try {
        is = new InputStreamReader(urls);
        br = new BufferedReader(is);
        String line = br.readLine();
        while(line != null) {
          final String aText = "<a href=\"";
          int aPos = line.toLowerCase().indexOf(aText);
          int aEndPos = line.toLowerCase().indexOf(".html\" ",aPos);
          if ((aPos>=0) && (aEndPos>=0)) {
            String link = line.substring(aPos+aText.length(), aEndPos);
            String fullClassName = link.substring(stripPrefix.length()).replace('/', '.');
            String simpleClassName = fullClassName;
            int lastDot = fullClassName.lastIndexOf('.');
            if (lastDot>=0) { simpleClassName = fullClassName.substring(lastDot + 1); }
            try {
              URL pageURL = new URL(base + link + ".html");
              s.add(new JavaAPIListEntry(simpleClassName, fullClassName, pageURL));
            }
            catch(MalformedURLException mue) {  }
          }
          line = br.readLine();
        }
      }
      finally {
        if (br!=null) { br.close(); }
        if (is!=null) { is.close(); }
        if (urls!=null) { urls.close(); }
      }
    }
    catch(IOException ioe) {  }
    return s;
  }
  
  
  public void generateJavaAPISet() {
    if (_javaAPISet == null) {
      
      String linkVersion = DrJava.getConfig().getSetting(JAVADOC_API_REF_VERSION);
      
      
      String base = "";
      
      
      String stripPrefix = "";
      
      
      String suffix = "";
      if (linkVersion.equals(JAVADOC_AUTO_TEXT)) {
        
        edu.rice.cs.plt.reflect.JavaVersion ver = _model.getCompilerModel().getActiveCompiler().version();
        if (ver==edu.rice.cs.plt.reflect.JavaVersion.JAVA_1_4) {
          linkVersion = JAVADOC_1_4_TEXT;
        }
        else if (ver==edu.rice.cs.plt.reflect.JavaVersion.JAVA_5) {
          linkVersion = JAVADOC_1_5_TEXT;
        }
        else if (ver==edu.rice.cs.plt.reflect.JavaVersion.JAVA_6) {
          linkVersion = JAVADOC_1_6_TEXT;
        }
        else {
          linkVersion = JAVADOC_1_3_TEXT;
        }
      }
      if (linkVersion.equals(JAVADOC_1_3_TEXT)) {
        base = DrJava.getConfig().getSetting(JAVADOC_1_3_LINK) + "/";
        stripPrefix = ""; 
        suffix = "/allclasses-1.3.html";
      }
      else if (linkVersion.equals(JAVADOC_1_4_TEXT)) {
        base = DrJava.getConfig().getSetting(JAVADOC_1_4_LINK) + "/";
        stripPrefix = ""; 
        suffix = "/allclasses-1.4.html";
      }
      else if (linkVersion.equals(JAVADOC_1_5_TEXT)) {
        base = DrJava.getConfig().getSetting(JAVADOC_1_5_LINK) + "/";
        stripPrefix = ""; 
        suffix = "/allclasses-1.5.html";
      }
      else if (linkVersion.equals(JAVADOC_1_6_TEXT)) {
        
        
        
        
        
        
        base = DrJava.getConfig().getSetting(JAVADOC_1_6_LINK) + "/";
        stripPrefix = ""; 
        suffix = "/allclasses-1.6.html";
      }
      else {
        
        return;
      }
      _javaAPISet = _generateJavaAPISet(base, stripPrefix, suffix);
      
      
      Set<JavaAPIListEntry> junit382APIList = _generateJavaAPISet(DrJava.getConfig().getSetting(JUNIT_3_8_2_LINK) + "/",
                                                                  "", 
                                                                  "/allclasses-junit3.8.2.html");
      _javaAPISet.addAll(junit382APIList);
      
      
      for(String url: DrJava.getConfig().getSetting(JAVADOC_ADDITIONAL_LINKS)) {
        try {
          Set<JavaAPIListEntry> additionalList = _generateJavaAPISet(url + "/",
                                                                     "", 
                                                                     new URL(url+"/allclasses-frame.html"));
          _javaAPISet.addAll(additionalList);
        }
        catch(MalformedURLException mue) {  }
      }
      
      if (_javaAPISet.size()==0) { _javaAPISet = null; }
    }
  }
  
  
  PredictiveInputFrame<JavaAPIListEntry> _openJavadocDialog = null;
  
  
  Set<JavaAPIListEntry> _javaAPISet = null;
  
  
  private Action _openJavadocAction = new AbstractAction("Open Java API Javadoc...") {
    public void actionPerformed(ActionEvent ae) {
      initOpenJavadocDialog();     
      _openJavadocDialog.setItems(true, _javaAPISet); 
      hourglassOn();
      _openJavadocDialog.setVisible(true);
    }
  };
  
  
  private void _openJavadocUnderCursor() {
    generateJavaAPISet();
    if (_javaAPISet == null) {

      return;
    }
    PredictiveInputModel<JavaAPIListEntry> pim =
      new PredictiveInputModel<JavaAPIListEntry>(true, new PrefixStrategy<JavaAPIListEntry>(), _javaAPISet);
    OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
    String mask = "";
    int loc = getCurrentDefPane().getCaretPosition();
    String s = odd.getText();
    
    int start = loc;
    while(start>0) {
      if (!Character.isJavaIdentifierPart(s.charAt(start-1))) { break; }
      --start;
    }
    while((start<s.length()) && (!Character.isJavaIdentifierStart(s.charAt(start))) && (start<loc)) {
      ++start;
    }
    
    int end = loc-1;
    while(end<s.length()-1) {
      if (!Character.isJavaIdentifierPart(s.charAt(end+1))) { break; }
      ++end;
    }
    if ((start>=0) && (end<s.length())) {
      mask = s.substring(start, end + 1);
      pim.setMask(mask);
    }
    

    
    if (pim.getMatchingItems().size() == 1) {
      
      if (pim.getCurrentItem() != null) {
        PlatformFactory.ONLY.openURL(pim.getCurrentItem().getURL());
      }
    }
    else {
      
      pim.extendMask(".java");
      if (pim.getMatchingItems().size() == 1) {
        
        if (pim.getCurrentItem() != null) {
          PlatformFactory.ONLY.openURL(pim.getCurrentItem().getURL());
        }
      }
      else {
        
        pim.setMask(mask);
        int found = 0;
        if (pim.getMatchingItems().size() == 0) {
          
          mask = pim.getMask();
          while(mask.length()>0) {
            mask = mask.substring(0, mask.length() - 1);
            pim.setMask(mask);
            if (pim.getMatchingItems().size() > 0) { break; }
          }
        }
        else {
          
          for(JavaAPIListEntry e: pim.getMatchingItems()) {
            if (e.toString().equalsIgnoreCase(mask)) {
              ++found;
            }
          }
        }
        if (found==1) {
          
          PlatformFactory.ONLY.openURL(pim.getCurrentItem().getURL());
        }
        else {
          initOpenJavadocDialog();
          _openJavadocDialog.setModel(true, pim); 
          hourglassOn();
          _openJavadocDialog.setVisible(true);
        }
      }
    }
  }
  
  
  final Action _openJavadocUnderCursorAction = new AbstractAction("Open Java API Javadoc for Word Under Cursor...") {
    public void actionPerformed(ActionEvent ae) {
      _openJavadocUnderCursor();
    }
  };
  
  
  final Action _closeSystemInAction = new AbstractAction("Close System.in") {
    public void actionPerformed(ActionEvent ae){
      _interactionsController.setEndOfStream(true);
      _interactionsController.interruptConsoleInput();
    }
  };
  
  
  public void resetCompleteWordDialogPosition() {
    initCompleteWordDialog();
    _completeWordDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_COMPLETE_WORD_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_COMPLETE_WORD_STATE, "default");
    }
  }
  
  
  void initCompleteWordDialog() {
    if (_completeWordDialog == null) {
      
      _completeJavaAPICheckbox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String curMask = _completeWordDialog.getMask();
          if (_completeJavaAPICheckbox.isSelected()) {
            DrJava.getConfig().setSetting(OptionConstants.DIALOG_COMPLETE_JAVAAPI, Boolean.TRUE);
            Set<ClassNameAndPackageEntry> s = new HashSet<ClassNameAndPackageEntry>(_completeWordDialog.getItems());
            addJavaAPIToSet(s);
            _completeWordDialog.setItems(true,s);
          }
          else {
            
            Set<ClassNameAndPackageEntry> s = new HashSet<ClassNameAndPackageEntry>(_completeWordDialog.getItems());
            generateJavaAPISet();
            if (_javaAPISet==null) {
              DrJava.getConfig().setSetting(OptionConstants.DIALOG_COMPLETE_JAVAAPI, Boolean.FALSE);
              _completeJavaAPICheckbox.setSelected(false);
              _completeJavaAPICheckbox.setEnabled(false);
              Set<ClassNameAndPackageEntry> n = new HashSet<ClassNameAndPackageEntry>();
              for(ClassNameAndPackageEntry entry: s) {
                if (!(entry instanceof JavaAPIListEntry)) { n.add(entry); }
              }
              _completeWordDialog.setItems(true,n);
            }
            else {
              for(JavaAPIListEntry entry: _javaAPISet) { s.remove(entry); }
              _completeWordDialog.setItems(true,s);
            }
          }
          _completeWordDialog.setMask(curMask);
          _completeWordDialog.resetFocus();
        }
      });
      PlatformFactory.ONLY.setMnemonic(_completeJavaAPICheckbox,'j');
      PredictiveInputFrame.InfoSupplier<ClassNameAndPackageEntry> info = 
        new PredictiveInputFrame.InfoSupplier<ClassNameAndPackageEntry>() {
        public String value(ClassNameAndPackageEntry entry) {
          
          StringBuilder sb = new StringBuilder();
          sb.append(entry.getFullPackage());
          sb.append(entry.getClassName());
          return sb.toString();
        }
      };
      PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry> okAction =
        new PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry>() {
        public String getName() { return "OK"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); }
        public String getToolTipText() { return "Complete the identifier"; }
        public Object value(PredictiveInputFrame<ClassNameAndPackageEntry> p) {
          if (p.getItem() != null) {
            OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
            try {
              int loc = getCurrentDefPane().getCaretPosition();
              String s = odd.getText(0, loc);
              
              
              if ((loc<s.length()) && (!Character.isWhitespace(s.charAt(loc))) &&
                  ("()[]{}<>.,:;/*+-!~&|%".indexOf(s.charAt(loc)) == -1)) return null;
              
              
              int start = loc;
              while(start>0) {
                if (!Character.isJavaIdentifierPart(s.charAt(start-1))) { break; }
                --start;
              }
              while((start<s.length()) && (!Character.isJavaIdentifierStart(s.charAt(start))) && (start < loc)) {
                ++start;
              }
              
              if (!s.substring(start, loc).equals(p.getItem().toString())) {
                odd.remove(start, loc-start);
                odd.insertString(start, p.getItem().getClassName(), null);
              }
            }
            catch(BadLocationException ble) {  }
          }
          hourglassOff();
          return null;
        }
      };
      PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry> fullAction =
        new PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry>() {
        public String getName() { return "Fully Qualified"; }
        public KeyStroke getKeyStroke() {
          return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, OptionConstants.MASK);
        }
        public String getToolTipText() { return "Complete the word using the fully-qualified class name"; }
        public Object value(PredictiveInputFrame<ClassNameAndPackageEntry> p) {
          if (p.getItem() != null) {
            OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
            try {
              int loc = getCurrentDefPane().getCaretPosition();
              String s = odd.getText(0, loc);
              
              
              if ((loc<s.length()) && (!Character.isWhitespace(s.charAt(loc))) &&
                  ("()[]{}<>.,:;/*+-!~&|%".indexOf(s.charAt(loc)) == -1)) return null;
              
              
              int start = loc;
              while(start>0) {
                if (!Character.isJavaIdentifierPart(s.charAt(start-1))) { break; }
                --start;
              }
              while((start<s.length()) && (!Character.isJavaIdentifierStart(s.charAt(start))) && (start < loc)) {
                ++start;
              }
              
              if (!s.substring(start, loc).equals(p.getItem().toString())) {
                odd.remove(start, loc-start);
                StringBuilder sb = new StringBuilder();
                sb.append(p.getItem().getFullPackage());
                sb.append(p.getItem().getClassName());
                odd.insertString(start, sb.toString(), null);
              }
            }
            catch(BadLocationException ble) {  }
          }
          hourglassOff();
          return null;
        }
      };
      PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry> cancelAction = 
        new PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry>() {
        public String getName() { return "Cancel"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<ClassNameAndPackageEntry> p) {
          hourglassOff();
          return null;
        }
      };
      
      java.util.ArrayList<MatchingStrategy<ClassNameAndPackageEntry>> strategies =
        new java.util.ArrayList<MatchingStrategy<ClassNameAndPackageEntry>>();
      strategies.add(new FragmentStrategy<ClassNameAndPackageEntry>());
      strategies.add(new PrefixStrategy<ClassNameAndPackageEntry>());
      strategies.add(new RegExStrategy<ClassNameAndPackageEntry>());
      List<PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry>> actions
        = new ArrayList<PredictiveInputFrame.CloseAction<ClassNameAndPackageEntry>>();
      actions.add(okAction);
      actions.add(fullAction);
      actions.add(cancelAction);
      GoToFileListEntry entry = new GoToFileListEntry(new DummyOpenDefDoc() {
        public String getPackageNameFromDocument() { return ""; }
      }, "dummyComplete");
      _completeWordDialog = 
        new PredictiveInputFrame<ClassNameAndPackageEntry>(MainFrame.this,
                                                           "Auto-Complete Word",
                                                           true, 
                                                           true, 
                                                           info,
                                                           strategies,
                                                           actions, 2, 
                                                           entry) {
        public void setOwnerEnabled(boolean b) {
          if (b) { hourglassOff(); } else { hourglassOn(); }
        }
        protected JComponent[] makeOptions() {
          return new JComponent[] { _completeJavaAPICheckbox };
        }
      }; 
      
      
      if (DrJava.getConfig().getSetting(DIALOG_COMPLETE_WORD_STORE_POSITION).booleanValue()) {
        _completeWordDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_COMPLETE_WORD_STATE));
      }      
    }
  }
  
  void addJavaAPIToSet(Set<ClassNameAndPackageEntry> s) {
    generateJavaAPISet();
    if (_javaAPISet==null) {
      DrJava.getConfig().setSetting(OptionConstants.DIALOG_COMPLETE_JAVAAPI, Boolean.FALSE);
      _completeJavaAPICheckbox.setSelected(false);
      _completeJavaAPICheckbox.setEnabled(false);
    }
    else {
      s.addAll(_javaAPISet);
    }
  }
  
  
  volatile PredictiveInputFrame<GoToFileListEntry> _completeFileDialog = null;
  
  volatile PredictiveInputFrame<ClassNameAndPackageEntry> _completeWordDialog = null;
  JCheckBox _completeJavaAPICheckbox = new JCheckBox("Java API");
  
  
  private void _completeWordUnderCursor() {
    List<OpenDefinitionsDocument> docs = _model.getOpenDefinitionsDocuments();
    if ((docs == null) || (docs.size() == 0)) return; 
    
    _completeJavaAPICheckbox.setSelected(DrJava.getConfig().getSetting(OptionConstants.DIALOG_COMPLETE_JAVAAPI));
    _completeJavaAPICheckbox.setEnabled(true);
    ClassNameAndPackageEntry currentEntry = null;
    HashSet<ClassNameAndPackageEntry> set;
    if ((DrJava.getConfig().getSetting(DIALOG_COMPLETE_SCAN_CLASS_FILES).booleanValue()) &&
        (_completeClassSet.size()>0)) {
      set = new HashSet<ClassNameAndPackageEntry>(_completeClassSet);
    }
    else {
      set = new HashSet<ClassNameAndPackageEntry>(docs.size());
      for(OpenDefinitionsDocument d: docs) {
        if (d.isUntitled()) continue;
        String str = d.toString();
        if (str.lastIndexOf('.')>=0) {
          str = str.substring(0, str.lastIndexOf('.'));
        }
        GoToFileListEntry entry = new GoToFileListEntry(d, str);
        if (d.equals(_model.getActiveDocument())) currentEntry = entry;
        set.add(entry);
      }
    }
    
    if (DrJava.getConfig().getSetting(OptionConstants.DIALOG_COMPLETE_JAVAAPI)) {
      addJavaAPIToSet(set);
    }
    
    
    PredictiveInputModel<ClassNameAndPackageEntry> pim = 
      new PredictiveInputModel<ClassNameAndPackageEntry>(true, new PrefixStrategy<ClassNameAndPackageEntry>(), set);
    OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
    try {
      String mask = "";
      int loc = getCurrentDefPane().getCaretPosition();
      String s = odd.getText(0, loc);
      
      
      if ((loc<s.length()) && (!Character.isWhitespace(s.charAt(loc))) &&
          ("()[]{}<>.,:;/*+-!~&|%".indexOf(s.charAt(loc)) == -1)) return;
      
      
      int start = loc;
      while(start > 0) {
        if (!Character.isJavaIdentifierPart(s.charAt(start-1))) { break; }
        --start;
      }
      while((start<s.length()) && (!Character.isJavaIdentifierStart(s.charAt(start))) && (start < loc)) {
        ++start;
      }
      
      int end = loc-1;
      
      if ((start>=0) && (end < s.length())) {
        mask = s.substring(start, end + 1);
        pim.setMask(mask);
      }
      
      if (pim.getMatchingItems().size() == 1) {
        if (pim.getCurrentItem() != null) {
          
          if (! s.substring(start, loc).equals(pim.getCurrentItem().toString())) {
            odd.remove(start, loc - start);
            odd.insertString(start, pim.getCurrentItem().toString(), null);
          }
          return;
        }
      }
      else {
        
        pim.setMask(mask);
        if (pim.getMatchingItems().size() == 0) {
          
          mask = pim.getMask();
          while(mask.length() > 0) {
            mask = mask.substring(0, mask.length() - 1);
            pim.setMask(mask);
            if (pim.getMatchingItems().size() > 0) { break; }
          }
        }       
        initCompleteWordDialog();
        _completeWordDialog.setModel(true, pim); 
        _completeWordDialog.selectStrategy();
        if (currentEntry != null) _completeWordDialog.setCurrentItem(currentEntry);
        hourglassOn();
        _completeWordDialog.setVisible(true);
      }
    }
    catch(BadLocationException ble) {  }
  }
  
  
  final Action completeWordUnderCursorAction = new AbstractAction("Auto-Complete Word Under Cursor") {
    public void actionPerformed(ActionEvent ae) {
      _completeWordUnderCursor();
    }
  };
  
  
  private final Action _indentLinesAction = new AbstractAction("Indent Line(s)") {
    public void actionPerformed(ActionEvent ae) {
      hourglassOn();
      try {
        _currentDefPane.endCompoundEdit();
        _currentDefPane.indent();
      } finally {
        hourglassOff();
      }
    }
  };
  
  
  private final Action _commentLinesAction = new AbstractAction("Comment Line(s)") {
    public void actionPerformed(ActionEvent ae) {
      hourglassOn();
      try{ commentLines(); }
      finally{ hourglassOff(); }
    }
  };
  
  
  private final Action _uncommentLinesAction = new AbstractAction("Uncomment Line(s)") {
    public void actionPerformed(ActionEvent ae){
      hourglassOn();
      try{ uncommentLines(); }
      finally{ hourglassOff(); }
    }
  };
  
  
  private final Action _clearConsoleAction = new AbstractAction("Clear Console") {
    public void actionPerformed(ActionEvent ae) { _model.resetConsole(); }
  };
  
  
  private final Action _showDebugConsoleAction = new AbstractAction("Show DrJava Debug Console") {
    public void actionPerformed(ActionEvent e) { DrJavaRoot.showDrJavaDebugConsole(MainFrame.this); }
  };
  
  
  public void enableResetInteractions() { _resetInteractionsAction.setEnabled(true); }
  
  
  private final Action _resetInteractionsAction = new AbstractAction("Reset Interactions") {
    public void actionPerformed(ActionEvent ae) {
      if (! DrJava.getConfig().getSetting(INTERACTIONS_RESET_PROMPT).booleanValue()) {
        _doResetInteractions();
        return;
      }
      
      String title = "Confirm Reset Interactions";
      String message = "Are you sure you want to reset the Interactions Pane?";
      ConfirmCheckBoxDialog dialog =
        new ConfirmCheckBoxDialog(MainFrame.this, title, message);
      int rc = dialog.show();
      if (rc == JOptionPane.YES_OPTION) {
        _doResetInteractions();
        
        if (dialog.getCheckBoxValue()) {
          DrJava.getConfig().setSetting(INTERACTIONS_RESET_PROMPT, Boolean.FALSE);
        }
      }
    }
  };
  
  private void _doResetInteractions() {
    _tabbedPane.setSelectedIndex(INTERACTIONS_TAB);
    updateStatusField("Resetting Interactions");
    
    new Thread(new Runnable() { 
      public void run() {
        _model.resetInteractions(_model.getWorkingDirectory(), true);
        _closeSystemInAction.setEnabled(true);
      }
    }).start();
  }
  
  
  private final Action _viewInteractionsClassPathAction = new AbstractAction("View Interactions Classpath...") {
    public void actionPerformed(ActionEvent e) { viewInteractionsClassPath(); }
  };
  
    
  public void viewInteractionsClassPath() {
    String cp = IterUtil.multilineToString(_model.getInteractionsClassPath());
    new DrJavaScrollableDialog(this, "Interactions Classpath", "Current Interpreter Classpath", cp).show();
  }
  
  
  private final Action _helpAction = new AbstractAction("Help") {
    public void actionPerformed(ActionEvent ae) {
      



      _helpFrame.setVisible(true);
    }
  };
  
  
  private final Action _quickStartAction = new AbstractAction("QuickStart") {
    public void actionPerformed(ActionEvent ae) {
      



      _quickStartFrame.setVisible(true);
    }
  };
  
  
  private final Action _aboutAction = new AbstractAction("About") {
    public void actionPerformed(ActionEvent ae) {
      


      _aboutDialog.setVisible(true);


      
    }
  };
  
  
  private final Action _checkNewVersionAction = new AbstractAction("Check for New Version") {
    public void actionPerformed(ActionEvent ae) {
      NewVersionPopup popup = new NewVersionPopup(MainFrame.this);
      popup.setVisible(true);
    }
  };
  
  
  private final Action _drjavaSurveyAction = new AbstractAction("Send System Information") {
    public void actionPerformed(ActionEvent ae) {
      DrJavaSurveyPopup popup = new DrJavaSurveyPopup(MainFrame.this);
      popup.setVisible(true);
    }
  };
  
  
  private final Action _errorsAction = new AbstractAction("DrJava Errors") {
    public void actionPerformed(ActionEvent ae) {
      setPopupLoc(DrJavaErrorWindow.singleton());
      DrJavaErrorWindow.singleton().setVisible(true);
    }
  };
  
  
  private final Action _switchToNextAction = new AbstractAction("Next Document") {
    public void actionPerformed(ActionEvent ae) {
      this.setEnabled(false);
      if (_docSplitPane.getDividerLocation() < _docSplitPane.getMinimumDividerLocation())
        _docSplitPane.setDividerLocation(DrJava.getConfig().getSetting(DOC_LIST_WIDTH).intValue());
      
      _model.setActiveNextDocument();
      _findReplace.updateFirstDocInSearch();
      this.setEnabled(true);
      
      addToBrowserHistory();
    }
  };
  
  
  private final Action _switchToPrevAction = new AbstractAction("Previous Document") {
    public void actionPerformed(ActionEvent ae) {
      this.setEnabled(false);
      if (_docSplitPane.getDividerLocation() < _docSplitPane.getMinimumDividerLocation())
        _docSplitPane.setDividerLocation(DrJava.getConfig().getSetting(DOC_LIST_WIDTH).intValue());
      _model.setActivePreviousDocument();
      _findReplace.updateFirstDocInSearch();
      this.setEnabled(true);
      
      addToBrowserHistory();
    }
  };
  
  
  private final Action _switchToNextPaneAction =  new AbstractAction("Next Pane") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes(); 
      this.setEnabled(false);
      _switchPaneFocus(true);
      this.setEnabled(true);
    }
  };
  
  
  private final Action _browseBackAction = new AbstractAction("Browse Back") {
    public void actionPerformed(ActionEvent ae) {
      updateStatusField("Browsing Back");
      this.setEnabled(false);
      if (_docSplitPane.getDividerLocation() < _docSplitPane.getMinimumDividerLocation())
        _docSplitPane.setDividerLocation(DrJava.getConfig().getSetting(DOC_LIST_WIDTH).intValue());
      
      
      
      BrowserHistoryManager rm = _model.getBrowserHistoryManager();      
      addToBrowserHistory();

      
      BrowserDocumentRegion r = rm.prevCurrentRegion(_model.getNotifier());
      if (r != null) scrollToDocumentAndOffset(r.getDocument(), r.getStartOffset(), false, false);
      _configureBrowsing();

    }
  };
  
  
  private final Action _browseForwardAction = new AbstractAction("Browse Forward") {
    public void actionPerformed(ActionEvent ae) {
      updateStatusField("Browsing Forward");
      this.setEnabled(false);
      if (_docSplitPane.getDividerLocation() < _docSplitPane.getMinimumDividerLocation())
        _docSplitPane.setDividerLocation(DrJava.getConfig().getSetting(DOC_LIST_WIDTH).intValue());
      
      
      
      BrowserHistoryManager rm = _model.getBrowserHistoryManager();      
      addToBrowserHistoryBefore();
      
      
      BrowserDocumentRegion r = rm.nextCurrentRegion(_model.getNotifier());
      if (r != null) scrollToDocumentAndOffset(r.getDocument(), r.getStartOffset(), false, false);
      _configureBrowsing();

    }
  };
  
  
  private final Action _switchToPreviousPaneAction =  new AbstractAction("Previous Pane") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes(); 
      this.setEnabled(false);
      _switchPaneFocus(false);
      this.setEnabled(true);
    }
  };
  
  
  private final Action _gotoClosingBraceAction =  new AbstractAction("Go to Closing Brace") {
    public void actionPerformed(ActionEvent ae) {
      OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
      try {
        int pos = odd.findNextEnclosingBrace(getCurrentDefPane().getCaretPosition(), '{', '}');
        if (pos != -1) { getCurrentDefPane().setCaretPosition(pos); }
      }
      catch(BadLocationException ble) {  }
    }
  };
  
  
  private final Action _gotoOpeningBraceAction =  new AbstractAction("Go to Opening Brace") {
    public void actionPerformed(ActionEvent ae) {
      OpenDefinitionsDocument odd = getCurrentDefPane().getOpenDefDocument();
      try {
        int pos = odd.findPrevEnclosingBrace(getCurrentDefPane().getCaretPosition(), '{', '}');
        if (pos != -1) { getCurrentDefPane().setCaretPosition(pos); }
      }
      catch(BadLocationException ble) {  }
    }
  };
  
  
  private void _switchToPane(Component c) {
    Component newC = c;


    showTab(newC, true);
  }
  
  
  private void _switchPaneFocus(boolean next) {
    int numTabs = _tabbedPane.getTabCount();
    
    
    if (next) _switchToPane(_tabbedPane.getComponentAt((numTabs + _tabbedPane.getSelectedIndex() +1 ) % numTabs));
    else _switchToPane(_tabbedPane.getComponentAt((numTabs + _tabbedPane.getSelectedIndex() - 1) % numTabs));
  }
  
  
  private final Action _editPreferencesAction = new AbstractAction("Preferences ...") {
    public void actionPerformed(ActionEvent ae) {
      
      _configFrame.setUp();
      setPopupLoc(_configFrame);
      _configFrame.setVisible(true);
      _configFrame.toFront();
    }
  };
  
  private volatile AbstractAction _projectPropertiesAction = new AbstractAction("Project Properties") {
    public void actionPerformed(ActionEvent ae) { _editProject(); }
  };
  
  
  private final Action _toggleDebuggerAction = new AbstractAction("Debug Mode") {
    public void actionPerformed(ActionEvent ae) { 
      setEnabled(false);
      debuggerToggle();
      setEnabled(true);
    }
  };
  
  
  private final Action _resumeDebugAction = new AbstractAction("Resume Debugger") {
    public void actionPerformed(ActionEvent ae) {
      try { debuggerResume(); }
      catch (DebugException de) { _showDebugError(de); }
    }
  };
  
  private JMenuItem _automaticTraceMenuItem;
  
  public void setAutomaticTraceMenuItemStatus() {
      if (_automaticTraceMenuItem!=null)
          _automaticTraceMenuItem.setSelected(_model.getDebugger().isAutomaticTraceEnabled());
  }
  
  
  private final Action _automaticTraceDebugAction = new AbstractAction("Automatic Trace") {
    public void actionPerformed(ActionEvent ae) { 
      debuggerAutomaticTrace(); 
    }
  };
  
  
  private final Action _stepIntoDebugAction = new AbstractAction("Step Into") {
    public void actionPerformed(ActionEvent ae) { debuggerStep(Debugger.StepType.STEP_INTO); }
  };
  
  
  private final Action _stepOverDebugAction = new AbstractAction("Step Over") {
    public void actionPerformed(ActionEvent ae) { debuggerStep(Debugger.StepType.STEP_OVER); }
  };
  
  
  private final Action _stepOutDebugAction = new AbstractAction("Step Out") {
    public void actionPerformed(ActionEvent ae) {
      debuggerStep(Debugger.StepType.STEP_OUT);
    }
  };
  
  
  
  
  
  final Action _toggleBreakpointAction = new AbstractAction("Toggle Breakpoint on Current Line") {
    public void actionPerformed(ActionEvent ae) { debuggerToggleBreakpoint(); }
  };
  
  
  private final Action _clearAllBreakpointsAction = new AbstractAction("Clear All Breakpoints") {
    public void actionPerformed(ActionEvent ae) { debuggerClearAllBreakpoints(); }
  };

  
  
  private final Action _breakpointsPanelAction = new AbstractAction("Breakpoints") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes(); 
      showTab(_breakpointsPanel, true);
      _breakpointsPanel.setVisible(true);
      _tabbedPane.setSelectedComponent(_breakpointsPanel);
      
      EventQueue.invokeLater(new Runnable() { public void run() { _breakpointsPanel.requestFocusInWindow(); } });
    }
  };
  
  
  private final Action _bookmarksPanelAction = new AbstractAction("Bookmarks") {
    public void actionPerformed(ActionEvent ae) {
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) 
        _mainSplit.resetToPreferredSizes(); 
      showTab(_bookmarksPanel, true);
      _tabbedPane.setSelectedComponent(_bookmarksPanel);
      
      EventQueue.invokeLater(new Runnable() { public void run() { _bookmarksPanel.requestFocusInWindow(); } });
    }
  };
  
  
  private final Action _toggleBookmarkAction = new AbstractAction("Toggle Bookmark") {
    public void actionPerformed(ActionEvent ae) { toggleBookmark(); }
  };
  
  
  public void toggleBookmark() {

    assert EventQueue.isDispatchThread();
    addToBrowserHistory();
    _model._toggleBookmark(_currentDefPane.getSelectionStart(), _currentDefPane.getSelectionEnd()); 
    showTab(_bookmarksPanel, true);
  }
  
  
  public void addToBrowserHistory() { _model.addToBrowserHistory(); }
  
  public void addToBrowserHistoryBefore() { _model.addToBrowserHistory(true); }
  
  
  public FindResultsPanel createFindResultsPanel(final RegionManager<MovingDocumentRegion> rm, MovingDocumentRegion region, String title,
                                                 String searchString, boolean searchAll, boolean searchSelectionOnly, 
                                                 boolean matchCase, boolean wholeWord, boolean noComments, 
                                                 boolean noTestCases, WeakReference<OpenDefinitionsDocument> doc,
                                                 FindReplacePanel findReplace) {
    
    final FindResultsPanel panel = new FindResultsPanel(this, rm, region, title, searchString, searchAll, searchSelectionOnly, matchCase,
                                                        wholeWord, noComments, noTestCases, doc, findReplace);
    
    final AbstractMap<MovingDocumentRegion, HighlightManager.HighlightInfo> highlights =
      new IdentityHashMap<MovingDocumentRegion, HighlightManager.HighlightInfo>();
    final Pair<FindResultsPanel, Map<MovingDocumentRegion, HighlightManager.HighlightInfo>> pair =
      new Pair<FindResultsPanel, Map<MovingDocumentRegion, HighlightManager.HighlightInfo>>(panel, highlights);
    _findResults.add(pair);
    
    
    rm.addListener(new RegionManagerListener<MovingDocumentRegion>() {     
      public void regionAdded(MovingDocumentRegion r) {
        DefinitionsPane pane = getDefPaneGivenODD(r.getDocument());

        highlights.put(r, pane.getHighlightManager().
                         addHighlight(r.getStartOffset(), r.getEndOffset(), panel.getSelectedPainter()));
      }
      public void regionChanged(MovingDocumentRegion r) { 
        regionRemoved(r);
        regionAdded(r);
      }
      public void regionRemoved(MovingDocumentRegion r) {

        HighlightManager.HighlightInfo highlight = highlights.get(r);

        if (highlight != null) highlight.remove();
        highlights.remove(r);
        
        if (rm.getDocuments().isEmpty()) {
          panel._close(); 
        }
      }
    });
    
    
    panel.addCloseListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) { _findResults.remove(pair); }
    });
    
    _tabs.addLast(panel);
    panel.getMainPanel().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = panel; }
    });
    
    return panel;
  }
  
  
  void disableFindAgainOnClose(List<OpenDefinitionsDocument> projDocs) {
    for(TabbedPanel t: _tabs) {
      if (t instanceof FindResultsPanel) {
        FindResultsPanel p = (FindResultsPanel) t;
        if (projDocs.contains(p.getDocument())) { p.disableFindAgain(); }
      }
    }
  }
  
  
  public void showFindResultsPanel(final FindResultsPanel panel) {
    assert EventQueue.isDispatchThread();
    if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) _mainSplit.resetToPreferredSizes(); 
    showTab(panel, true);
    panel.updatePanel();

    _tabbedPane.setSelectedComponent(panel);
    
    EventQueue.invokeLater(new Runnable() { public void run() { panel.requestFocusInWindow(); } });
  };
  
  
  protected final Action _cutLineAction = new AbstractAction("Cut Line") {
    public void actionPerformed(ActionEvent ae) {
      ActionMap actionMap = _currentDefPane.getActionMap();
      int oldCol = _model.getActiveDocument().getCurrentCol();
      actionMap.get(DefaultEditorKit.selectionEndLineAction).actionPerformed(ae);
      
      
      
      if (oldCol == _model.getActiveDocument().getCurrentCol()) {
        
        actionMap.get(DefaultEditorKit.selectionForwardAction).actionPerformed(ae);
        cutAction.actionPerformed(ae);
      }
      else cutAction.actionPerformed(ae);
    }
  };
  
  
  protected final Action _clearLineAction = new AbstractAction("Clear Line") {
    public void actionPerformed(ActionEvent ae) {
      ActionMap actionMap = _currentDefPane.getActionMap();
      actionMap.get(DefaultEditorKit.selectionEndLineAction).actionPerformed(ae);
      actionMap.get(DefaultEditorKit.deleteNextCharAction).actionPerformed(ae);
    }
  };
  
  
  private final Action _beginLineAction = new AbstractAction("Begin Line") {
    public void actionPerformed(ActionEvent ae) {
      int beginLinePos = _getBeginLinePos();
      _currentDefPane.setCaretPosition(beginLinePos);
    }
  };
  
  
  private final Action _selectionBeginLineAction = new AbstractAction("Select to Beginning of Line") {
    public void actionPerformed(ActionEvent ae) {
      int beginLinePos = _getBeginLinePos();
      _currentDefPane.moveCaretPosition(beginLinePos);
    }
  };
  
  
  private int _getBeginLinePos() {
    try {
      int currPos = _currentDefPane.getCaretPosition();
      OpenDefinitionsDocument openDoc = _model.getActiveDocument();
      openDoc.setCurrentLocation(currPos);
      return openDoc.getIntelligentBeginLinePos(currPos);
    }
    catch (BadLocationException ble) {
      
      throw new UnexpectedException(ble);
    }
  }
  
  private final FileOpenSelector _interactionsHistoryFileSelector = new FileOpenSelector() {
    public File[] getFiles() throws OperationCanceledException {
      return getOpenFiles(_interactionsHistoryChooser);
    }
  };
  
  
  private final Action _executeHistoryAction = new AbstractAction("Execute Interactions History...") {
    public void actionPerformed(ActionEvent ae) {
      
      _tabbedPane.setSelectedIndex(INTERACTIONS_TAB);
      
      _interactionsHistoryChooser.setDialogTitle("Execute Interactions History");
      try { _model.loadHistory(_interactionsHistoryFileSelector); }
      catch (FileNotFoundException fnf) { _showFileNotFoundError(fnf); }
      catch (IOException ioe) { _showIOError(ioe); }
      _interactionsPane.requestFocusInWindow();
    }
  };
  
  
  private void _closeInteractionsScript() {
    if (_interactionsScriptController != null) {
      _interactionsContainer.remove(_interactionsScriptPane);
      _interactionsScriptController = null;
      _interactionsScriptPane = null;
      _tabbedPane.invalidate();
      _tabbedPane.repaint();
    }
  }
  
  
  private final Action _loadHistoryScriptAction = new AbstractAction("Load Interactions History as Script...") {
    public void actionPerformed(ActionEvent e) {
      try {
        _interactionsHistoryChooser.setDialogTitle("Load Interactions History");
        InteractionsScriptModel ism = _model.loadHistoryAsScript(_interactionsHistoryFileSelector);
        _interactionsScriptController = new InteractionsScriptController(ism, new AbstractAction("Close") {
          public void actionPerformed(ActionEvent e) {
            _closeInteractionsScript();
            _interactionsPane.requestFocusInWindow();
          }
        }, _interactionsPane);
        _interactionsScriptPane = _interactionsScriptController.getPane();
        _interactionsContainer.add(_interactionsScriptPane, BorderLayout.EAST);
        _tabbedPane.invalidate();
        _tabbedPane.repaint();
      }
      catch (FileNotFoundException fnf) { _showFileNotFoundError(fnf); }
      catch (IOException ioe) { _showIOError(ioe); }
      catch (OperationCanceledException oce) {
      }
    }
  };
  
  
  private final Action _saveHistoryAction = new AbstractAction("Save Interactions History...") {
    public void actionPerformed(ActionEvent ae) {
      String[] options = {"Yes","No","Cancel"};
      int resp = JOptionPane.showOptionDialog(MainFrame.this,
                                              "Edit interactions history before saving?",
                                              "Edit History?",
                                              JOptionPane.YES_NO_CANCEL_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,options,
                                              options[1]);
      
      if (resp == 2 || resp == JOptionPane.CLOSED_OPTION) return;
      
      String history = _model.getHistoryAsStringWithSemicolons();
      
      
      if (resp == 0)
        history = (new HistorySaveDialog(MainFrame.this)).editHistory(history);
      if (history == null) return; 
      
      _interactionsHistoryChooser.setDialogTitle("Save Interactions History");
      FileSaveSelector selector = new FileSaveSelector() {
        public File getFile() throws OperationCanceledException {
          
          
          
          
          
          File selection = _interactionsHistoryChooser.getSelectedFile();
          if (selection != null) {
            _interactionsHistoryChooser.setSelectedFile(selection.getParentFile());
            _interactionsHistoryChooser.setSelectedFile(selection);
            _interactionsHistoryChooser.setSelectedFile(null);
          }

          _interactionsHistoryChooser.setMultiSelectionEnabled(false);
          int rc = _interactionsHistoryChooser.showSaveDialog(MainFrame.this);
          File c = getChosenFile(_interactionsHistoryChooser, rc);
          
          
          if ((c!=null) && (c.getName().indexOf('.') == -1)) {
            c = new File(c.getAbsolutePath() + "." + InteractionsHistoryFilter.HIST_EXTENSION);
          }
          _interactionsHistoryChooser.setSelectedFile(c);
          return c;
        }
        public boolean warnFileOpen(File f) { return true; }
        public boolean verifyOverwrite() { return _verifyOverwrite(); }
        public boolean shouldSaveAfterFileMoved(OpenDefinitionsDocument doc, File oldFile) {
          return true;
        }
      };
      
      try { _model.saveHistory(selector, history);}
      catch (IOException ioe) {
        _showIOError(new IOException("An error occured writing the history to a file"));
      }
      _interactionsPane.requestFocusInWindow();
    }
  };
  
  
  private final Action _clearHistoryAction = new AbstractAction("Clear Interactions History") {
    public void actionPerformed(ActionEvent ae) {
      _model.clearHistory();
      _interactionsPane.requestFocusInWindow();
    }
  };
  
  
  private final WindowListener _windowCloseListener = new WindowAdapter() {
    public void windowActivated(WindowEvent ev) { }
    public void windowClosed(WindowEvent ev) { }
    public void windowClosing(WindowEvent ev) { quit(); }
    public void windowDeactivated(WindowEvent ev) { }
    public void windowDeiconified(WindowEvent ev) {
      try { _model.getActiveDocument().revertIfModifiedOnDisk(); }
      catch (FileMovedException fme) { _showFileMovedError(fme); }
      catch (IOException e) { _showIOError(e);}
    }
    public void windowIconified(WindowEvent ev) { }
    public void windowOpened(WindowEvent ev) { _currentDefPane.requestFocusInWindow(); }
  };
  
  private final MouseListener _resetFindReplaceListener = new MouseListener() {
    public void mouseClicked (MouseEvent e) { }
    public void mousePressed (MouseEvent e) { }
    
    public void mouseReleased (MouseEvent e) {_findReplace.updateFirstDocInSearch();}
    public void mouseEntered (MouseEvent e) { }
    public void mouseExited (MouseEvent e) { }
  };
  
  
  
  private static final DJFileDisplayManager _djFileDisplayManager20;
  private static final DJFileDisplayManager _djFileDisplayManager30;
  private static final OddDisplayManager _oddDisplayManager20;
  private static final OddDisplayManager _oddDisplayManager30;
  private static final Icon _djProjectIcon;
  
  static {
    Icon java, dj0, dj1, dj2, other, star, jup, juf;
    
    java = MainFrame.getIcon("JavaIcon20.gif");
    dj0 = MainFrame.getIcon("ElementaryIcon20.gif");
    dj1 = MainFrame.getIcon("IntermediateIcon20.gif");
    dj2 = MainFrame.getIcon("AdvancedIcon20.gif");
    other = MainFrame.getIcon("OtherIcon20.gif");
    _djFileDisplayManager20 = new DJFileDisplayManager(java,dj0,dj1,dj2,other);
    
    java = MainFrame.getIcon("JavaIcon30.gif");
    dj0 = MainFrame.getIcon("ElementaryIcon30.gif");
    dj1 = MainFrame.getIcon("IntermediateIcon30.gif");
    dj2 = MainFrame.getIcon("AdvancedIcon30.gif");
    other = MainFrame.getIcon("OtherIcon30.gif");
    _djFileDisplayManager30 = new DJFileDisplayManager(java,dj0,dj1,dj2,other);
    
    star = MainFrame.getIcon("ModStar20.gif");
    jup = MainFrame.getIcon("JUnitPass20.gif");
    juf = MainFrame.getIcon("JUnitFail20.gif");
    _oddDisplayManager20 = new OddDisplayManager(_djFileDisplayManager20,star,jup,juf);
    
    star = MainFrame.getIcon("ModStar30.gif");
    jup = MainFrame.getIcon("JUnitPass30.gif");
    juf = MainFrame.getIcon("JUnitFail30.gif");
    _oddDisplayManager30 = new OddDisplayManager(_djFileDisplayManager30,star,jup,juf);
    
    _djProjectIcon = MainFrame.getIcon("ProjectIcon.gif");
  }
  
  
  
  private static class DJFileDisplayManager extends DefaultFileDisplayManager {
    private final Icon _java;
    private final Icon _dj0;
    private final Icon _dj1;
    private final Icon _dj2;
    private final Icon _other;
    
    public DJFileDisplayManager(Icon java, Icon dj0, Icon dj1, Icon dj2, Icon other) {
      _java = java;
      _dj0 = dj0;
      _dj1 = dj1;
      _dj2 = dj2;
      _other = other;
    }
    
    public Icon getIcon(File f) {
      if (f == null) return _other;
      Icon ret = null;
      if (! f.isDirectory()) {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".java")) ret = _java;
        if (name.endsWith(".dj0")) ret = _dj0;
        if (name.endsWith(".dj1")) ret = _dj1;
        if (name.endsWith(".dj2")) ret = _dj2;
      }
      if (ret == null) {
        ret = super.getIcon(f);
        if (ret.getIconHeight() < _java.getIconHeight()) {
          ret = new CenteredIcon(ret, _java.getIconWidth(), _java.getIconHeight());
        }
      }
      return ret;
    }
  }
  
  
  private static class OddDisplayManager implements DisplayManager<OpenDefinitionsDocument> {
    private final Icon _star;


    private final FileDisplayManager _default;
    
    
    public OddDisplayManager(FileDisplayManager fdm, Icon star, Icon junitPass, Icon junitFail) {
      _star = star;


      _default = fdm;
    }
    public Icon getIcon(OpenDefinitionsDocument odd) {
      File f = null;
      try { f = odd.getFile(); }
      catch(FileMovedException fme) {  }
      
      if (odd.isModifiedSinceSave()) return makeLayeredIcon(_default.getIcon(f), _star);
      return _default.getIcon(f);
    }
    public String getName(OpenDefinitionsDocument doc) { return doc.getFileName(); }
    private LayeredIcon makeLayeredIcon(Icon base, Icon star) {
      return new LayeredIcon(new Icon[]{base, star}, new int[]{0, 0}, 
                             new int[]{0, (base.getIconHeight() / 4)});
    }
  };
  
  
  private final DisplayManager<INavigatorItem> _navPaneDisplayManager = new DisplayManager<INavigatorItem>() {
    public Icon getIcon(INavigatorItem item) {
      OpenDefinitionsDocument odd = (OpenDefinitionsDocument) item;  
      return _oddDisplayManager20.getIcon(odd);
    }
    public String getName(INavigatorItem name) { return name.getName(); }
  };
  
  
  public KeyListener _historyListener = new KeyListener() {
    public void keyPressed(KeyEvent e) {
      int backQuote = java.awt.event.KeyEvent.VK_BACK_QUOTE;
      if (e.getKeyCode() == backQuote && e.isControlDown()) {
        if (e.isShiftDown()) prevRecentDoc();
        else nextRecentDoc();
      }
    }
    public void keyReleased(KeyEvent e) {
      if (e.getKeyCode() == java.awt.event.KeyEvent.VK_CONTROL) hideRecentDocFrame();
    }
    public void keyTyped(KeyEvent e) {  }
  };
  
  public FocusListener _focusListenerForRecentDocs = new FocusListener() {
    public void focusLost(FocusEvent e) { hideRecentDocFrame();  }
    public void focusGained(FocusEvent e) { }
  };
  
  public static DJFileDisplayManager getFileDisplayManager20() { return _djFileDisplayManager20; }
  public static DJFileDisplayManager getFileDisplayManager30() { return _djFileDisplayManager30; }
  public static OddDisplayManager getOddDisplayManager20() { return _oddDisplayManager20; }
  public static OddDisplayManager getOddDisplayManager30() { return _oddDisplayManager30; }
  public DisplayManager<INavigatorItem> getNavPaneDisplayManager() { return _navPaneDisplayManager; }
  
  
  
   
  public MainFrame() {
    Utilities.invokeAndWait(new Runnable() { public void run() {
      
      final Configuration config = DrJava.getConfig(); 
      
      
      assert _historyListener != null;
      
      
      _model = new DefaultGlobalModel();
      
      _showDebugger = _model.getDebugger().isAvailable();
      _findReplace = new FindReplacePanel(MainFrame.this, _model);
      
      
      
      Utilities.enableDisableWith(_findReplace._findNextAction, _findNextAction);
      Utilities.enableDisableWith(_findReplace._findPreviousAction, _findPrevAction);
      
      if (_showDebugger) {
        _debugPanel = new DebugPanel(MainFrame.this);
        _breakpointsPanel = new BreakpointsPanel(MainFrame.this, _model.getBreakpointManager());
      }
      else {
        _debugPanel = null;
        _breakpointsPanel = null; 
      }
      
      _compilerErrorPanel = new CompilerErrorPanel(_model, MainFrame.this);
      _consoleController = new ConsoleController(_model.getConsoleDocument(), _model.getSwingConsoleDocument());
      _consolePane = _consoleController.getPane();
      
      _consoleScroll = new BorderlessScrollPane(_consolePane) {
        public boolean requestFocusInWindow() { 
          super.requestFocusInWindow();
          return _consolePane.requestFocusInWindow(); 
        } 
      };
      
      _interactionsController =
        new InteractionsController(_model.getInteractionsModel(),
                                   _model.getSwingInteractionsDocument(),
                                   new Runnable() {
        public void run() {
          _closeSystemInAction.setEnabled(false);
        }
      });
      
      _interactionsPane = _interactionsController.getPane();
      
      _interactionsContainer = new JPanel(new BorderLayout());
      _lastFocusOwner = _interactionsContainer;
      
      _junitErrorPanel = new JUnitPanel(_model, MainFrame.this);
      _javadocErrorPanel = new JavadocErrorPanel(_model, MainFrame.this);
      
      _bookmarksPanel = new BookmarksPanel(MainFrame.this, _model.getBookmarkManager());
      
      
      _setUpStatusBar();
      
      
      
      
      
      
      DefinitionsPane.setEditorKit(_model.getEditorKit());
      
      _defScrollPanes = new HashMap<OpenDefinitionsDocument, JScrollPane>();
      
      
      _tabbedPane.setFocusable(false);
      
      _tabbedPane.addFocusListener(_focusListenerForRecentDocs);
      _tabbedPane.addKeyListener(_historyListener);    
      
      if (Utilities.isPlasticLaf()) {
        _tabbedPane.putClientProperty(com.jgoodies.looks.Options.EMBEDDED_TABS_KEY, Boolean.TRUE);
      }
      
      JScrollPane defScroll = _createDefScrollPane(_model.getActiveDocument());
      
      _docSplitPane = 
        new BorderlessSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                                new JScrollPane(_model.getDocumentNavigator().asContainer()), defScroll);
      _debugSplitPane = new BorderlessSplitPane(JSplitPane.VERTICAL_SPLIT, true);
      _mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, _docSplitPane, _tabbedPane);























      
      _tabbedPanesFrame = new DetachedFrame("Tabbed Panes", MainFrame.this, new Runnable1<DetachedFrame>() {
        public void run(DetachedFrame frame) {
          frame.getContentPane().add(_tabbedPane);
        }
      }, new Runnable1<DetachedFrame>() {
        public void run(DetachedFrame frame) {
          _mainSplit.setBottomComponent(_tabbedPane);
        }
      });
      _tabbedPanesFrame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          _detachTabbedPanesMenuItem.setSelected(false);
          DrJava.getConfig().setSetting(DETACH_TABBEDPANES, false);
        }
      });
      
      _model.getDocumentNavigator().asContainer().addKeyListener(_historyListener);
      _model.getDocumentNavigator().asContainer().addFocusListener(_focusListenerForRecentDocs);
      
      
      _model.getDocumentNavigator().asContainer().addMouseListener(_resetFindReplaceListener);
      
      if (_showDebugger) _model.getDebugger().addListener(new UIDebugListener()); 
      
      
      _debugStepTimer = new Timer(DEBUG_STEP_TIMER_VALUE, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          _model.printDebugMessage("Stepping...");
        }
      });
      _debugStepTimer.setRepeats(false);
      
      
      File workDir = _model.getMasterWorkingDirectory();
      
      
      _openChooser = new JFileChooser() {
        public void setCurrentDirectory(File dir) {
          
          super.setCurrentDirectory(dir);
          setDialogTitle("Open:  " + getCurrentDirectory());
        }
      };
      _openChooser.setPreferredSize(new Dimension(650, 410));
      _openChooser.setCurrentDirectory(workDir);
      _openChooser.setFileFilter(_javaSourceFilter);
      _openChooser.setMultiSelectionEnabled(true);
      
      _openRecursiveCheckBox.setSelected(config.getSetting(OptionConstants.OPEN_FOLDER_RECURSIVE).booleanValue());
      
      _folderChooser = makeFolderChooser(workDir);
      
      
      Vector<File> recentProjects = config.getSetting(RECENT_PROJECTS);
      _openProjectChooser = new JFileChooser();
      _openProjectChooser.setPreferredSize(new Dimension(650, 410));
      
      if (recentProjects.size() > 0 && recentProjects.elementAt(0).getParentFile() != null)
        _openProjectChooser.setCurrentDirectory(recentProjects.elementAt(0).getParentFile());
      else
        _openProjectChooser.setCurrentDirectory(workDir);
      
      _openProjectChooser.setFileFilter(_projectFilter);
      _openProjectChooser.setMultiSelectionEnabled(false);
      _saveChooser = new JFileChooser() {
        public void setCurrentDirectory(File dir) {
          
          super.setCurrentDirectory(dir);
          setDialogTitle("Save:  " + getCurrentDirectory());
        }
      };
      _saveChooser.setPreferredSize(new Dimension(650, 410));
      _saveChooser.setCurrentDirectory(workDir);
      _saveChooser.setFileFilter(_javaSourceFilter);
            
      _interactionsHistoryChooser.setPreferredSize(new Dimension(650, 410));
      _interactionsHistoryChooser.setCurrentDirectory(workDir);
      _interactionsHistoryChooser.setFileFilter(new InteractionsHistoryFilter());
      _interactionsHistoryChooser.setMultiSelectionEnabled(true);
      
      
      setGlassPane(new GlassPane());
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      
      
      addWindowListener(_windowCloseListener);
      
      
      _mainListener = new ModelListener();
      _model.addListener(_mainListener);
      
      
      _setUpTabs();
      
      
      _recentDocFrame = new RecentDocFrame(MainFrame.this);
      OpenDefinitionsDocument activeDoc = _model.getActiveDocument();
      _recentDocFrame.pokeDocument(activeDoc);
      _currentDefDoc = activeDoc.getDocument();
      _currentDefPane = (DefinitionsPane) defScroll.getViewport().getView();
      _currentDefPane.notifyActive();
      
      
      int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      
      
      KeyBindingManager.ONLY.setMainFrame(MainFrame.this);
      KeyBindingManager.ONLY.setActionMap(_currentDefPane.getActionMap());
      _setUpKeyBindingMaps();
      
      _posListener.updateLocation();
      
      
      
      _undoAction.setDelegatee(_currentDefPane.getUndoAction());
      _redoAction.setDelegatee(_currentDefPane.getRedoAction());
      
      _compilerErrorPanel.reset();
      _junitErrorPanel.reset();
      _javadocErrorPanel.reset();
      
      
      _fileMenu = _setUpFileMenu(mask);
      _editMenu = _setUpEditMenu(mask);
      _toolsMenu = _setUpToolsMenu(mask);
      _projectMenu = _setUpProjectMenu(mask);
      _debugMenu = null;
      if (_showDebugger) _debugMenu = _setUpDebugMenu(mask);
      _languageLevelMenu = _setUpLanguageLevelMenu(mask);
      _helpMenu = _setUpHelpMenu(mask);
      
      
      _setUpActions();
      _setUpMenuBar();
      
      
      _setUpContextMenus();
      
      
      _undoButton = _createManualToolbarButton(_undoAction);
      _redoButton = _createManualToolbarButton(_redoAction);
      
      
      
      _setUpToolBar();
      
      
      if (_debugPanel!=null) { 
        _debugFrame = new DetachedFrame("Debugger", MainFrame.this, new Runnable1<DetachedFrame>() {
          public void run(DetachedFrame frame) {
            frame.getContentPane().add(_debugPanel);
          }
        }, new Runnable1<DetachedFrame>() {
          public void run(DetachedFrame frame) {
            _debugSplitPane.setTopComponent(_docSplitPane);
            _debugSplitPane.setBottomComponent(_debugPanel);
            _mainSplit.setTopComponent(_debugSplitPane);
          }
        });
        _debugFrame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent we) {
            if (_debugFrame==null) return; 
            _detachDebugFrameMenuItem.setSelected(false);
            DrJava.getConfig().setSetting(DETACH_DEBUGGER, false);
          }
        });
      }
      else { 
        _debugFrame = null;
      }
      
      
      RecentFileAction fileAct = new RecentFileManager.RecentFileAction() { 
        public void actionPerformed(FileOpenSelector selector) { open(selector); }
      }; 
      _recentFileManager = new RecentFileManager(_fileMenu.getItemCount() - 2, _fileMenu,
                                                 fileAct, OptionConstants.RECENT_FILES);
      
      RecentFileAction projAct = new RecentFileManager.RecentFileAction() { 
        public void actionPerformed(FileOpenSelector selector) { openProject(selector); } 
      };
      _recentProjectManager = new RecentFileManager(_projectMenu.getItemCount() - 2, _projectMenu,
                                                    projAct, OptionConstants.RECENT_PROJECTS);
      
      
      setIconImage(getIcon("drjava64.png").getImage());
      
      
      int x = config.getSetting(WINDOW_X).intValue();
      int y = config.getSetting(WINDOW_Y).intValue();
      int width = config.getSetting(WINDOW_WIDTH).intValue();
      int height = config.getSetting(WINDOW_HEIGHT).intValue();
      int state = config.getSetting(WINDOW_STATE).intValue();
      
      
      
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      
      final int menubarHeight = 24;
      if (height > screenSize.height - menubarHeight)  height = screenSize.height - menubarHeight; 
      
      if (width > screenSize.width)  width = screenSize.width; 
      
      
      
      Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().
        getDefaultConfiguration().getBounds();
      
      if (x == Integer.MAX_VALUE)  x = (bounds.width - width + bounds.x) / 2;    
      if (y == Integer.MAX_VALUE)  y = (bounds.height - height + bounds.y) / 2;  
      if (x < bounds.x)  x = bounds.x;                                           
      if (y < bounds.y)  y = bounds.y;                                           
      if ((x + width) > (bounds.x + bounds.width))  x = bounds.width - width + bounds.x; 
      
      if ((y + height) > (bounds.y + bounds.height))  y = bounds.height - height + bounds.y; 
      
      
      
      state &= ~Frame.ICONIFIED;
      
      if (!Toolkit.getDefaultToolkit().isFrameStateSupported(state)) {
        
        state = WINDOW_STATE.getDefault();
      }
      
      
      setBounds(x, y, width, height);
      
      
      
      final int stateCopy = state;
      addWindowListener(new WindowAdapter() {
        public void windowOpened(WindowEvent e) {
          setExtendedState(stateCopy);
          
          removeWindowListener(this);
        }
      });
      
      _setUpPanes();
      updateStatusField();
      
      _promptBeforeQuit = config.getSetting(QUIT_PROMPT).booleanValue();
      
      
      _setMainFont();
      Font doclistFont = config.getSetting(FONT_DOCLIST);
      _model.getDocCollectionWidget().setFont(doclistFont);
      
      
      _updateNormalColor();
      _updateBackgroundColor();
      
      
      config.addOptionListener(DEFINITIONS_NORMAL_COLOR, new NormalColorOptionListener());
      config.addOptionListener(DEFINITIONS_BACKGROUND_COLOR, new BackgroundColorOptionListener());
      
      
      config.addOptionListener(FONT_MAIN, new MainFontOptionListener());
      config.addOptionListener(FONT_LINE_NUMBERS, new LineNumbersFontOptionListener());
      config.addOptionListener(FONT_DOCLIST, new DoclistFontOptionListener());
      config.addOptionListener(FONT_TOOLBAR, new ToolbarFontOptionListener());
      config.addOptionListener(TOOLBAR_ICONS_ENABLED, new ToolbarOptionListener());
      config.addOptionListener(TOOLBAR_TEXT_ENABLED, new ToolbarOptionListener());
      config.addOptionListener(TOOLBAR_ENABLED, new ToolbarOptionListener());
      config.addOptionListener(LINEENUM_ENABLED, new LineEnumOptionListener());
      config.addOptionListener(DEFINITIONS_LINE_NUMBER_COLOR, new LineEnumColorOptionListener());
      config.addOptionListener(DEFINITIONS_LINE_NUMBER_BACKGROUND_COLOR, new LineEnumColorOptionListener());
      config.addOptionListener(QUIT_PROMPT, new QuitPromptOptionListener());
      config.addOptionListener(RECENT_FILES_MAX_SIZE, new RecentFilesOptionListener());
      
      config.addOptionListener(FORCE_TEST_SUFFIX, new OptionListener<Boolean>() {
        public void optionChanged(OptionEvent<Boolean> oce) {
          _model.getJUnitModel().setForceTestSuffix(oce.value.booleanValue());
        }
      });
      
      
      OptionListener<String> choiceOptionListener = new OptionListener<String>() {
        public void optionChanged(OptionEvent<String> oce) {
          _javaAPISet = null;
        }
      };
      DrJava.getConfig().addOptionListener(JAVADOC_API_REF_VERSION, choiceOptionListener);
      
      
      OptionListener<String> link13OptionListener = new OptionListener<String>() {
        public void optionChanged(OptionEvent<String> oce) {
          String linkVersion = DrJava.getConfig().getSetting(JAVADOC_API_REF_VERSION);
          if (linkVersion.equals(JAVADOC_1_3_TEXT)) {
            _javaAPISet = null;
          }
        }
      };
      DrJava.getConfig().addOptionListener(JAVADOC_1_3_LINK, link13OptionListener);
      OptionListener<String> link14OptionListener = new OptionListener<String>() {
        public void optionChanged(OptionEvent<String> oce) {
          String linkVersion = DrJava.getConfig().getSetting(JAVADOC_API_REF_VERSION);
          if (linkVersion.equals(JAVADOC_1_4_TEXT)) {
            _javaAPISet = null;
          }
        }
      };
      DrJava.getConfig().addOptionListener(JAVADOC_1_4_LINK, link14OptionListener);
      OptionListener<String> link15OptionListener = new OptionListener<String>() {
        public void optionChanged(OptionEvent<String> oce) {
          String linkVersion = DrJava.getConfig().getSetting(JAVADOC_API_REF_VERSION);
          if (linkVersion.equals(JAVADOC_1_5_TEXT)) {
            _javaAPISet = null;
          }
        }
      };
      DrJava.getConfig().addOptionListener(JAVADOC_1_5_LINK, link15OptionListener);
      OptionListener<String> link16OptionListener = new OptionListener<String>() {
        public void optionChanged(OptionEvent<String> oce) {
          String linkVersion = DrJava.getConfig().getSetting(JAVADOC_API_REF_VERSION);
          if (linkVersion.equals(JAVADOC_1_6_TEXT)) {
            _javaAPISet = null;
          }
        }
      };
      DrJava.getConfig().addOptionListener(JAVADOC_1_6_LINK, link16OptionListener);
      OptionListener<String> link382OptionListener = new OptionListener<String>() {
        public void optionChanged(OptionEvent<String> oce) {
          _javaAPISet = null;
        }
      };
      DrJava.getConfig().addOptionListener(JUNIT_3_8_2_LINK, link382OptionListener);
      OptionListener<Vector<String>> additionalLinkOptionListener = new OptionListener<Vector<String>>() {
        public void optionChanged(OptionEvent<Vector<String>> oce) {
          _javaAPISet = null;
        }
      };
      DrJava.getConfig().addOptionListener(JAVADOC_ADDITIONAL_LINKS, additionalLinkOptionListener);
      
      
      
      
      
      
      _configFrame = new ConfigFrame(MainFrame.this);
      
      _aboutDialog = new AboutDialog(MainFrame.this);
      
      _interactionsScriptController = null;
      _executeExternalDialog = new ExecuteExternalDialog(MainFrame.this);
      _editExternalDialog = new EditExternalDialog(MainFrame.this);
      _jarOptionsDialog = new JarOptionsDialog(MainFrame.this);
      
      initTabbedPanesFrame();
      initDebugFrame();
      initJarOptionsDialog();
      initExecuteExternalProcessDialog();

      
      config.addOptionListener(DISPLAY_ALL_COMPILER_VERSIONS, new ConfigOptionListeners.DisplayAllCompilerVersionsListener(_configFrame));
      config.addOptionListener(LOOK_AND_FEEL, new ConfigOptionListeners.LookAndFeelListener(_configFrame));
      config.addOptionListener(PLASTIC_THEMES, new ConfigOptionListeners.PlasticThemeListener(_configFrame));
      OptionListener<String> slaveJVMArgsListener = new ConfigOptionListeners.SlaveJVMArgsListener(_configFrame);
      config.addOptionListener(SLAVE_JVM_ARGS, slaveJVMArgsListener);
      _slaveJvmXmxListener = new ConfigOptionListeners.SlaveJVMXMXListener(_configFrame);
      config.addOptionListener(SLAVE_JVM_XMX, _slaveJvmXmxListener);
      OptionListener<String> masterJVMArgsListener = new ConfigOptionListeners.MasterJVMArgsListener(_configFrame);
      config.addOptionListener(MASTER_JVM_ARGS, masterJVMArgsListener);
      _masterJvmXmxListener = new ConfigOptionListeners.MasterJVMXMXListener(_configFrame);
      config.addOptionListener(MASTER_JVM_XMX, _masterJvmXmxListener);
      config.addOptionListener(JAVADOC_CUSTOM_PARAMS, 
                               new ConfigOptionListeners.JavadocCustomParamsListener(_configFrame));
      ConfigOptionListeners.sanitizeSlaveJVMArgs(MainFrame.this, config.getSetting(SLAVE_JVM_ARGS), slaveJVMArgsListener);
      ConfigOptionListeners.sanitizeSlaveJVMXMX(MainFrame.this, config.getSetting(SLAVE_JVM_XMX));
      ConfigOptionListeners.sanitizeMasterJVMArgs(MainFrame.this, config.getSetting(MASTER_JVM_ARGS), masterJVMArgsListener);
      ConfigOptionListeners.sanitizeMasterJVMXMX(MainFrame.this, config.getSetting(MASTER_JVM_XMX));
      ConfigOptionListeners.sanitizeJavadocCustomParams(MainFrame.this, config.getSetting(JAVADOC_CUSTOM_PARAMS));
      
      
      _showConfigException();
      
      KeyBindingManager.ONLY.setShouldCheckConflict(false);
      
      
      PlatformFactory.ONLY.afterUISetup(_aboutAction, _editPreferencesAction, _quitAction);
      setUpKeys();    
      
      
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
        public boolean dispatchKeyEvent(KeyEvent e) {
          boolean discardEvent = false;
          
          if ((e.getID() == KeyEvent.KEY_TYPED) &&
              (e.getKeyChar()=='`') &&
              (((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) ||
               ((e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK))
                  == (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK))) &&
              (e.getComponent().getClass().equals(DefinitionsPane.class))) {

            discardEvent = true;
          }
          return discardEvent;
        }
      });
      
      if (DrJava.getConfig().getSetting(edu.rice.cs.drjava.config.OptionConstants.REMOTE_CONTROL_ENABLED)) {
        
        try {
          if (! RemoteControlClient.isServerRunning()) {
            new RemoteControlServer(MainFrame.this);
          }
        }
        catch(IOException ioe) {
          try { RemoteControlClient.openFile(null); }
          catch(IOException ignored) {  }
          if (!Utilities.TEST_MODE && !System.getProperty("user.name").equals(RemoteControlClient.getServerUser())) {
            Object[] options = {"Disable","Ignore"};
            String msg = "<html>Could not start DrJava's remote control server";
            if (RemoteControlClient.getServerUser()!=null) {
              msg += "<br>because user "+RemoteControlClient.getServerUser()+" is already using the same port";
            }
            msg += ".<br>Please select an unused port in the Preferences dialog.<br>"+
              "In the meantime, do you want to disable the remote control feature?";
            int n = JOptionPane.showOptionDialog(MainFrame.this,
                                                 msg,
                                                 "Could Not Start Remote Control Server",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE,
                                                 null,
                                                 options,
                                                 options[1]);
            if (n==JOptionPane.YES_OPTION) {
              DrJava.getConfig().setSetting(edu.rice.cs.drjava.config.OptionConstants.REMOTE_CONTROL_ENABLED, false);
            }
          }
        }
      }
      
      setUpDrJavaProperties();  
      
      DrJavaErrorHandler.setButton(_errorsButton);
      
      
      boolean alreadyShowedDialog = false;
      if (PlatformFactory.ONLY.canRegisterFileExtensions()) {
        
        if (DrJava.getConfig().getSetting(OptionConstants.FILE_EXT_REGISTRATION)
              .equals(OptionConstants.FILE_EXT_REGISTRATION_CHOICES.get(2))) { 
          
          PlatformFactory.ONLY.registerDrJavaFileExtensions();
          PlatformFactory.ONLY.registerJavaFileExtension();
        }
        else if (DrJava.getConfig().getSetting(OptionConstants.FILE_EXT_REGISTRATION)
                   .equals(OptionConstants.FILE_EXT_REGISTRATION_CHOICES.get(1)) && 
                 !edu.rice.cs.util.swing.Utilities.TEST_MODE &&
                 ((!PlatformFactory.ONLY.areDrJavaFileExtensionsRegistered()) ||
                  (!PlatformFactory.ONLY.isJavaFileExtensionRegistered()))) {
          alreadyShowedDialog = true;
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              int rc;
              Object[] options = {"Yes", "No", "Always", "Never"};
              String text = "Do you want to associate .java, .drjava and .djapp files with DrJava?\n" + 
                "Double-clicking on those files will open them in DrJava.\n\n" +
                "Select 'Always' to let DrJava do this automatically.\n"+
                "Select 'Never' if you don't want to be asked again.\n\n"+
                "You can change this setting in the Preferences dialog under\n"+
                "Miscellaneous/File Types.";
              
              rc = JOptionPane.showOptionDialog(MainFrame.this, text, "Set File Associations?", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
              if ((rc==0) || (rc==2)) { 
                PlatformFactory.ONLY.registerDrJavaFileExtensions();
                PlatformFactory.ONLY.registerJavaFileExtension();
              }
              if (rc==2) { 
                DrJava.getConfig().setSetting(OptionConstants.FILE_EXT_REGISTRATION, OptionConstants.FILE_EXT_REGISTRATION_CHOICES.get(2));
              }
              if (rc==3) { 
                DrJava.getConfig().setSetting(OptionConstants.FILE_EXT_REGISTRATION, OptionConstants.FILE_EXT_REGISTRATION_CHOICES.get(0));
              }
            }
          });
        }
      }
      
      if (!alreadyShowedDialog) {
        
        
        
        if (!DrJava.getConfig().getSetting(OptionConstants.NEW_VERSION_NOTIFICATION)
              .equals(OptionConstants.NEW_VERSION_NOTIFICATION_CHOICES.get(3)) &&
            !edu.rice.cs.util.swing.Utilities.TEST_MODE) {
          int days = DrJava.getConfig().getSetting(NEW_VERSION_NOTIFICATION_DAYS);
          java.util.Date nextCheck = 
            new java.util.Date(DrJava.getConfig().getSetting(OptionConstants.LAST_NEW_VERSION_NOTIFICATION)
                                 + days * 24L * 60 * 60 * 1000); 
          if (new java.util.Date().after(nextCheck)) {
            alreadyShowedDialog = true;
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                NewVersionPopup popup = new NewVersionPopup(MainFrame.this);
                if (popup.checkNewVersion()) { popup.setVisible(true); }
              }
            });
          }
        }
      }
      if (!alreadyShowedDialog) {
        
        
        
        if (DrJava.getConfig().getSetting(DIALOG_DRJAVA_SURVEY_ENABLED) && !edu.rice.cs.util.swing.Utilities.TEST_MODE) {
          if (DrJavaSurveyPopup.maySubmitSurvey()) {
            
            alreadyShowedDialog = true;
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                DrJavaSurveyPopup popup = new DrJavaSurveyPopup(MainFrame.this);
                popup.setVisible(true);
              }
            });
          }
        }
      }
      
      initDone();  
      
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          _tabbedPanesFrame.setDisplayInFrame(DrJava.getConfig().getSetting(DETACH_TABBEDPANES));
        }
      });
    } });
  }   
  
  public void setVisible(boolean b) { 
    _updateToolBarVisible();
    super.setVisible(b); 
  }
  
  
  public void setUpDrJavaProperties() {
    final String DEF_DIR = "${drjava.working.dir}";
    
    DrJavaPropertySetup.setup(); 
    
    
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileProperty("drjava.current.file", new Thunk<File>() {
      public File value() { return _model.getActiveDocument().getRawFile(); }
    }, 
                                   "Returns the current document in DrJava.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which the output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    PropertyMaps.TEMPLATE.setProperty("DrJava", 
                                      new DrJavaProperty("drjava.current.line", 
                                                         "Returns the current line in the Definitions Pane.") {
      public void update(PropertyMaps pm) { _value = String.valueOf(_posListener.lastLine()); }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", new DrJavaProperty("drjava.current.col",
                                               "Returns the current column in the Definitions Pane.") {
      public void update(PropertyMaps pm) {



        _value = String.valueOf(_posListener.lastCol());
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileProperty("drjava.working.dir", new Thunk<File>() {
      public File value() { return _model.getInteractionsModel().getWorkingDirectory(); }
    },
                                   "Returns the current working directory of DrJava.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileProperty("drjava.master.working.dir", new Thunk<File>() {
      public File value() { return _model.getMasterWorkingDirectory(); }
    },
                                   "Returns the working directory of the DrJava master JVM.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    
    
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileListProperty("drjava.all.files", File.pathSeparator, DEF_DIR,
                                       "Returns a list of all files open in DrJava.\n"+
                                       "Optional attributes:\n"+
                                       "\trel=\"<dir to which output should be relative\"\n"+
                                       "\tsep=\"<separator between files>\"\n"+
                                       "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                       "\tdquote=\"<true to enclose file in double quotes>\"") {
      protected List<File> getList(PropertyMaps pm) {
        ArrayList<File> l = new ArrayList<File>();
        for(OpenDefinitionsDocument odd: _model.getOpenDefinitionsDocuments()) {
          l.add(odd.getRawFile());
        }
        return l;
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileListProperty("drjava.project.files", File.pathSeparator, DEF_DIR,
                                       "Returns a list of all files open in DrJava that belong " +
                                       "to a project and are underneath the project root.\n" +
                                       "Optional attributes:\n" +
                                       "\trel=\"<dir to which output should be relative\"\n" +
                                       "\tsep=\"<separator between files>\"\n"+
                                       "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                       "\tdquote=\"<true to enclose file in double quotes>\"") {
      protected List<File> getList(PropertyMaps pm) {
        ArrayList<File> l = new ArrayList<File>();
        for(OpenDefinitionsDocument odd: _model.getProjectDocuments()) {
          l.add(odd.getRawFile());
        }
        return l;
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    }).listenToInvalidatesOf(PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files"));
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileListProperty("drjava.included.files", File.pathSeparator, DEF_DIR,
                                       "Returns a list of all files open in DrJava that are " +
                                       "not underneath the project root but are included in " +
                                       "the project.\n" +
                                       "Optional attributes:\n" +
                                       "\trel=\"<dir to which output should be relative\"\n" +
                                       "\tsep=\"<separator between files>\"\n"+
                                       "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                       "\tdquote=\"<true to enclose file in double quotes>\"") {
      protected List<File> getList(PropertyMaps pm) {
        ArrayList<File> l = new ArrayList<File>();
        for(OpenDefinitionsDocument odd: _model.getAuxiliaryDocuments()) {
          l.add(odd.getRawFile());
        }
        return l;
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    }).listenToInvalidatesOf(PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files"));
    PropertyMaps.TEMPLATE.
      setProperty("DrJava", 
                  new FileListProperty("drjava.external.files", File.pathSeparator, DEF_DIR,
                                       "Returns a list of all files open in DrJava that are "+
                                       "not underneath the project root and are not included in "+
                                       "the project.\n"+
                                       "Optional attributes:\n"+
                                       "\trel=\"<dir to which output should be relative\"\n"+
                                       "\tsep=\"<separator between files>\"\n"+
                                       "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                       "\tdquote=\"<true to enclose file in double quotes>\"") {
      protected List<File> getList(PropertyMaps pm) {
        ArrayList<File> l = new ArrayList<File>();
        for(OpenDefinitionsDocument odd: _model.getNonProjectDocuments()) {
          l.add(odd.getRawFile());
        }
        return l;
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    }).listenToInvalidatesOf(PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files"));    
    
    PropertyMaps.TEMPLATE.
      setProperty("Misc", 
                  new DrJavaProperty("input", "(User Input...)",
                                     "Get an input string from the user.\n"+
                                     "Optional attributes:\n"+
                                     "\tprompt=\"<prompt to display>\"\n"+
                                     "\tdefault=\"<suggestion to the user>\"") {
      public String toString() {
        return "(User Input...)";
      }
      public void update(PropertyMaps pm) {
        String msg = _attributes.get("prompt");
        if (msg == null) msg = "Please enter text for the external process.";
        String input = _attributes.get("default");
        if (input == null) input = "";
        input = JOptionPane.showInputDialog(MainFrame.this, msg, input);
        if (input == null) input = _attributes.get("default");
        if (input == null) input = "";
        _value = input;
      }
      public String getCurrent(PropertyMaps pm) {
        invalidate();
        return super.getCurrent(pm);
      }
      public void resetAttributes() {
        _attributes.clear();
        _attributes.put("prompt", null);
        _attributes.put("default", null);
      }
      public boolean isCurrent() { return false; }
    });
    
    
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new DrJavaProperty("project.mode",
                                     "Evaluates to true if a project is loaded.") {
      public void update(PropertyMaps pm) {
        Boolean b = _model.isProjectActive();
        String f = _attributes.get("fmt").toLowerCase();
        if (f.equals("int")) _value = b ? "1" : "0";
        else if (f.equals("yes")) _value = b ? "yes" : "no";
        else _value = b.toString();
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public void resetAttributes() {
        _attributes.clear();
        _attributes.put("fmt", "boolean");
      }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new DrJavaProperty("project.changed",
                                     "Evaluates to true if the project has been "+
                                     "changed since the last save.") {  
      public void update(PropertyMaps pm) {

        String f = _attributes.get("fmt").toLowerCase();
        Boolean b = _model.isProjectChanged();
        if (f.equals("int")) _value = b ? "1" : "0";
        else if (f.equals("yes")) _value = b ? "yes" : "no";
        else  _value = b.toString();
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public void resetAttributes() {
        _attributes.clear();
        _attributes.put("fmt", "boolean");
      }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new FileProperty("project.file", 
                                   new Thunk<File>() {
      public File value() { return _model.getProjectFile(); }
    },
                                   "Returns the current project file in DrJava.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which the output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new FileProperty("project.main.class", 
                                   new Thunk<File>() {
      public File value() { return new File(_model.getMainClass()); }
    },
                                   "Returns the current project file in DrJava.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which the output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new FileProperty("project.root", 
                                   new Thunk<File>() {
      public File value() { return _model.getProjectRoot(); }
    },
                                   "Returns the current project root in DrJava.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which the output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new FileProperty("project.build.dir", 
                                   new Thunk<File>() {
      public File value() { return _model.getBuildDirectory(); }
    },
                                   "Returns the current build directory in DrJava.\n"+
                                   "Optional attributes:\n"+
                                   "\trel=\"<dir to which the output should be relative\"\n"+
                                   "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                   "\tdquote=\"<true to enclose file in double quotes>\"") {
                                     public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
                                   });
    RecursiveFileListProperty classFilesProperty = 
      new RecursiveFileListProperty("project.class.files", File.pathSeparator, DEF_DIR,
                                    _model.getBuildDirectory().getAbsolutePath(),
                                    "Returns the class files currently in the build directory.\n"+
                                    "\trel=\"<dir to which the output should be relative\"\n"+
                                    "\tsep=\"<string to separate files in the list>\"\n"+
                                    "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                    "\tdquote=\"<true to enclose file in double quotes>\"") {
      
      public void resetAttributes() {
        _attributes.clear();
        _attributes.put("sep", _sep);
        _attributes.put("rel", _dir);
        _attributes.put("dir", _model.getBuildDirectory().getAbsolutePath());
        _attributes.put("filter", "*.class");
        _attributes.put("dirfilter", "*");
      }
    };
    PropertyMaps.TEMPLATE.setProperty("Project", classFilesProperty);
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new DrJavaProperty("project.auto.refresh",
                                     "Evaluates to true if project auto-refresh is enabled.") {
      public void update(PropertyMaps pm) {
        Boolean b = _model.getAutoRefreshStatus();
        String f = _attributes.get("fmt").toLowerCase();
        if (f.equals("int")) _value = b ? "1" : "0";
        else if (f.equals("yes")) _value = b ? "yes" : "no";
        else _value = b.toString();
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public void resetAttributes() {
        _attributes.clear();
        _attributes.put("fmt", "boolean");
      }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new FileListProperty("project.excluded.files", File.pathSeparator, DEF_DIR,
                                       "Returns a list of files that are excluded from DrJava's "+
                                       "project auto-refresh.\n"+
                                       "Optional attributes:\n"+
                                       "\trel=\"<dir to which output should be relative\"\n"+
                                       "\tsep=\"<separator between files>\"\n"+
                                       "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                       "\tdquote=\"<true to enclose file in double quotes>\"") {
      protected List<File> getList(PropertyMaps pm) {
        ArrayList<File> l = new ArrayList<File>();
        for(File f: _model.getExclFiles()) { l.add(f); }
        return l;
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Project", 
                  new FileListProperty("project.extra.class.path", File.pathSeparator, DEF_DIR,
                                       "Returns a list of files in the project's extra "+
                                       "class path.\n"+
                                       "Optional attributes:\n"+
                                       "\trel=\"<dir to which output should be relative\"\n"+
                                       "\tsep=\"<separator between files>\"\n"+
                                       "\tsquote=\"<true to enclose file in single quotes>\"\n"+
                                       "\tdquote=\"<true to enclose file in double quotes>\"") {
      protected List<File> getList(PropertyMaps pm) {
        ArrayList<File> l = new ArrayList<File>();
        for(File f: _model.getExtraClassPath()) { l.add(f); }
        return l;
      }
      public String getLazy(PropertyMaps pm) { return getCurrent(pm); }
      public boolean isCurrent() { return false; }
    });
    
    
    PropertyMaps.TEMPLATE.setProperty("Action", new DrJavaActionProperty("action.save.all", "(Save All...)",
                                                                         "Execute a \"Save All\" action.") {
      public void update(PropertyMaps pm) { _saveAll(); }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Action", new DrJavaActionProperty("action.compile.all", "(Compile All...)",
                                                     "Execute a \"Compile All\" action.") {
      public void update(PropertyMaps pm) { _compileAll(); }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Action", 
                  new DrJavaActionProperty("action.clean", "(Clean Build Directory...)",
                                           "Execute a \"Clean Build Directory\" action.") {
      public void update(PropertyMaps pm) {
        
        
        edu.rice.cs.plt.io.IOUtil.deleteRecursively(_model.getBuildDirectory());
      }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.setProperty("Action", new DrJavaActionProperty("action.open.file", "(Open File...)",
                                                                         "Execute an \"Open File\" action.\n"+
                                                                         "Required attributes:\n"+
                                                                         "\tfile=\"<file to open>\"\n"+
                                                                         "Optional attributes:\n"+
                                                                         "\tline=\"<line number to display>") {
      public void update(PropertyMaps pm) {
        if (_attributes.get("file") != null) {
          final String dir = StringOps.
            unescapeFileName(StringOps.replaceVariables(DEF_DIR, pm, PropertyMaps.GET_CURRENT));
          final String fil = StringOps.
            unescapeFileName(StringOps.replaceVariables(_attributes.get("file"), pm, PropertyMaps.GET_CURRENT));
          FileOpenSelector fs = new FileOpenSelector() {
            public File[] getFiles() {
              if (fil.startsWith("/")) { return new File[] { new File(fil) }; }
              else { return new File[] { new File(dir, fil) }; }
            }
          };
          open(fs);
          int lineNo = -1;
          if (_attributes.get("line")!=null) {
            try { lineNo = Integer.valueOf(_attributes.get("line")); }
            catch(NumberFormatException nfe) { lineNo = -1; }
          }
          if (lineNo >= 0) {
            final int l = lineNo;
            Utilities.invokeLater(new Runnable() { public void run() { _jumpToLine(l); } });
          }
        }
      }      
      
      public void resetAttributes() {
        _attributes.clear();
        _attributes.put("file", null);
        _attributes.put("line", null);
      }
      public boolean isCurrent() { return false; }
    });
    PropertyMaps.TEMPLATE.
      setProperty("Action", 
                  new DrJavaActionProperty("action.auto.refresh", "(Auto-Refresh...)",
                                           "Execute an \"Auto-Refresh Project\" action.") {
      public void update(PropertyMaps pm) {
        _model.autoRefreshProject();
      }
      public boolean isCurrent() { return false; }
    });
  }
  
  
  void refreshBreakpointHighlightPainter() {
    for(Map.Entry<Breakpoint,HighlightManager.HighlightInfo> pair: _documentBreakpointHighlights.entrySet()) {
      if (pair.getKey().isEnabled()) pair.getValue().refresh(DefinitionsPane.BREAKPOINT_PAINTER);
      else pair.getValue().refresh(DefinitionsPane.DISABLED_BREAKPOINT_PAINTER);
    }
  }
  
  
  void refreshBookmarkHighlightPainter() {
    for(HighlightManager.HighlightInfo hi: _documentBookmarkHighlights.values()) {
      hi.refresh(DefinitionsPane.BOOKMARK_PAINTER);
    }
  }
  
  
  void refreshFindResultsHighlightPainter(FindResultsPanel panel, LayeredHighlighter.LayerPainter painter) {
    for(Pair<FindResultsPanel, Map<MovingDocumentRegion, HighlightManager.HighlightInfo>> pair: _findResults) {
      if (pair.first() == panel) {
        Map<MovingDocumentRegion, HighlightManager.HighlightInfo> highlights = pair.second();
        for(HighlightManager.HighlightInfo hi: highlights.values()) { hi.refresh(painter); }
      }
    }
  }
  
  
  private DirectoryChooser makeFolderChooser(final File workDir) {
    assert duringInit() || EventQueue.isDispatchThread();
    final DirectoryChooser dc = new DirectoryChooser(this);
    
    dc.setSelectedFile(workDir);
    dc.setApproveButtonText("Select");
    dc.setDialogTitle("Open Folder");
    dc.setAccessory(_openRecursiveCheckBox);
    return dc;
  }






























  














  
  
  
  private void setUpKeys() { setFocusTraversalKeysEnabled(false); }
  
  
  public void dispose() {
    _model.dispose();
    super.dispose();
  }
  
  
  public SingleDisplayModel getModel() { return _model; }
  
  
  InteractionsPane getInteractionsPane() { return _interactionsPane; }
  
  
  InteractionsController getInteractionsController() { return _interactionsController; }
  
  
  JButton getCloseButton() { return _closeButton; }
  
  
  JButton getCompileAllButton() { return _compileButton; }
  
  private volatile int _hourglassNestLevel = 0;
  
    
  public void hourglassOn() {
    assert EventQueue.isDispatchThread();
    _hourglassNestLevel++;
    if (_hourglassNestLevel == 1) {
      getGlassPane().setVisible(true);
      _currentDefPane.setEditable(false);
      setAllowKeyEvents(false); 
    }
  }
  
   
  public void hourglassOff() { 
    assert EventQueue.isDispatchThread();
    _hourglassNestLevel--;
    if (_hourglassNestLevel == 0) {
      getGlassPane().setVisible(false);
      _currentDefPane.setEditable(true);
      setAllowKeyEvents(true);
    }
  }
  
  private volatile boolean _allowKeyEvents = true;
  
  public void setAllowKeyEvents(boolean a) { _allowKeyEvents = a; }
  
  public boolean getAllowKeyEvents() { return _allowKeyEvents; }
  
  
  public void debuggerToggle() {
    assert EventQueue.isDispatchThread();
    
    Debugger debugger = _model.getDebugger();
    if (! debugger.isAvailable()) return;
    
    updateStatusField("Toggling Debugger Mode");
    try { 
      if (isDebuggerReady()) {
        debugger.shutdown();
      }
      else {
        
        hourglassOn();
        try {
          debugger.startUp();  

          _model.refreshActiveDocument();
          _updateDebugStatus();
        }
        finally { hourglassOff(); }
      }
    }
    catch (DebugException de) { _showError(de, "Debugger Error", "Could not start the debugger."); }
    catch (NoClassDefFoundError err) {
      _showError(err, "Debugger Error",
                 "Unable to find the JPDA package for the debugger.\n" +
                 "Please make sure either tools.jar or jpda.jar is\n" +
                 "in your classpath when you start DrJava.");
      _setDebugMenuItemsEnabled(false);
    }
  }
  
  
  public void showDebugger() {
    assert EventQueue.isDispatchThread();
    _setDebugMenuItemsEnabled(true);
    _showDebuggerPanel();
  }
  
  
  public void hideDebugger() {
    _setDebugMenuItemsEnabled(false);
    _hideDebuggerPanel();
  }
  
  private void _showDebuggerPanel() {
    if (_detachDebugFrameMenuItem.isSelected()) {
      _debugFrame.setDisplayInFrame(true);
    }
    else {
      _debugSplitPane.setTopComponent(_docSplitPane);
      _mainSplit.setTopComponent(_debugSplitPane);
    }
    _debugPanel.updateData();
    _lastFocusOwner.requestFocusInWindow();
  }
  
  private void _hideDebuggerPanel() {
    if (_detachDebugFrameMenuItem.isSelected()) {
      _debugFrame.setVisible(false);
    }
    else {
      _mainSplit.setTopComponent(_docSplitPane);
    }
    _lastFocusOwner.requestFocusInWindow();
  }
  
  
  public void updateStatusField(String text) {
    assert EventQueue.isDispatchThread();
    _statusField.setText(text);
    _statusField.paint(getGraphics());  
  }
  
  
  public void updateStatusField() {
    OpenDefinitionsDocument doc = _model.getActiveDocument();
    String fileName = doc.getCompletePath();
    if (! fileName.equals(_fileTitle)) {
      _fileTitle = fileName;
      setTitle(fileName);
      _model.getDocCollectionWidget().repaint();
    }
    String path = doc.getCompletePath();
    
    String text = "Editing " + path;
    




    

    
    if (! _statusField.getText().equals(text)) { 
      _statusField.setText(text); 
      _statusField.paint(getGraphics());  
    }
  }
  
  
  public File[] getOpenFiles(JFileChooser jfc) throws OperationCanceledException {
    int rc = jfc.showOpenDialog(this);
    return getChosenFiles(jfc, rc);
  }
  
  
  public File getSaveFile(JFileChooser jfc) throws OperationCanceledException {
    






    
    OpenDefinitionsDocument active = _model.getActiveDocument();
    
    
    
    try {
      String className = active.getFirstTopLevelClassName();
      if (!className.equals("")) {
        jfc.setSelectedFile(new File(jfc.getCurrentDirectory(), className));
      }
    }
    catch (ClassNameNotFoundException e) {
      
    }
    
    
    _saveChooser.removeChoosableFileFilter(_projectFilter);
    _saveChooser.removeChoosableFileFilter(_javaSourceFilter);
    _saveChooser.setFileFilter(_javaSourceFilter);
    jfc.setMultiSelectionEnabled(false);
    int rc = jfc.showSaveDialog(this);
    return getChosenFile(jfc, rc);
  }
  
  
  public DefinitionsPane getCurrentDefPane() { return _currentDefPane; }
  
  
  public ErrorPanel getSelectedErrorPanel() {
    Component c = _tabbedPane.getSelectedComponent();
    if (c instanceof ErrorPanel) return (ErrorPanel) c;
    return null;
  }
  
  
  public boolean isCompilerTabSelected() {
    return _tabbedPane.getSelectedComponent() == _compilerErrorPanel;
  }
  
  
  public boolean isTestTabSelected() {
    return _tabbedPane.getSelectedComponent() == _junitErrorPanel;
  }
  
  
  public boolean isJavadocTabSelected() {
    return _tabbedPane.getSelectedComponent() == _javadocErrorPanel;
  }
  
  
  private void _installNewDocumentListener(final OpenDefinitionsDocument d) {
    d.addDocumentListener(new DocumentUIListener() {
      public void changedUpdate(DocumentEvent e) {  }
      public void insertUpdate(DocumentEvent e) {
        _saveAction.setEnabled(true);
        if (isDebuggerEnabled() && _debugPanel.getStatusText().equals(""))
          _debugPanel.setStatusText(DEBUGGER_OUT_OF_SYNC);
      }
      public void removeUpdate(DocumentEvent e) {
        _saveAction.setEnabled(true);
        if (isDebuggerEnabled() && _debugPanel.getStatusText().equals(""))
          _debugPanel.setStatusText(DEBUGGER_OUT_OF_SYNC);
      }
    });
  }
  
  
  public void setStatusMessage(String msg) { _statusReport.setText(msg); }
  
  
  public void clearStatusMessage() { _statusReport.setText(""); }
  
  
  public void setStatusMessageFont(Font f) { _statusReport.setFont(f); }
  
  
  public void setStatusMessageColor(Color c) { _statusReport.setForeground(c); }
  
  
  private void _processDocs(Collection<OpenDefinitionsDocument> docs, Runnable1<OpenDefinitionsDocument> op) {
    for (OpenDefinitionsDocument doc: docs) {
      if (doc != null && ! doc.isUntitled()) {
        op.run(doc);
        try {
          String path = _model.fixPathForNavigator(doc.getFile().getCanonicalPath());
          _model.getDocumentNavigator().refreshDocument(doc, path);
        }
        catch(IOException e) {  }
      }
    }
    PropertyMaps.TEMPLATE.getProperty("DrJava","drjava.project.files").invalidate();
    PropertyMaps.TEMPLATE.getProperty("DrJava","drjava.included.files").invalidate();
    PropertyMaps.TEMPLATE.getProperty("DrJava","drjava.external.files").invalidate();
  }
  
  
  void _moveToAuxiliary() {
    Runnable1<OpenDefinitionsDocument> op =  new Runnable1<OpenDefinitionsDocument>() { 
      public void run(OpenDefinitionsDocument d) { _model.addAuxiliaryFile(d); }
    };
    _processDocs(_model.getDocumentNavigator().getSelectedDocuments(), op);
  }
  
         
  private void _removeAuxiliary() {
    Runnable1<OpenDefinitionsDocument> op =  new Runnable1<OpenDefinitionsDocument>() { 
      public void run(OpenDefinitionsDocument d) { _model.removeAuxiliaryFile(d); }
    };
    _processDocs(_model.getDocumentNavigator().getSelectedDocuments(), op);
  }
  
  
  void _moveAllToAuxiliary() {
    assert EventQueue.isDispatchThread();
    Runnable1<OpenDefinitionsDocument> op =  new Runnable1<OpenDefinitionsDocument>() { 
      public void run(OpenDefinitionsDocument d) { _model.addAuxiliaryFile(d); }
    };
    _processDocs(_model.getDocumentNavigator().getDocumentsInBin(_model.getExternalBinTitle()), op);
  }
  
  
  private void _removeAllAuxiliary() {
    assert EventQueue.isDispatchThread();
    Runnable1<OpenDefinitionsDocument> op =  new Runnable1<OpenDefinitionsDocument>() { 
      public void run(OpenDefinitionsDocument d) { _model.removeAuxiliaryFile(d); }
    };
    _processDocs(_model.getDocumentNavigator().getDocumentsInBin(_model.getAuxiliaryBinTitle()), op);
  }
  
  private void _new() { 
    updateStatusField("Creating a new Untitled Document");
    _model.newFile(); 
  }
  
  private void _open() {
    updateStatusField("Opening File");
    open(_openSelector); 
  }
  
  private void _openFolder() { 
    openFolder(_folderChooser); 
  }
  
  private void _openFileOrProject() {
    try {
      final File[] fileList = _openFileOrProjectSelector.getFiles();
      
      FileOpenSelector fos = new FileOpenSelector() { public File[] getFiles() { return fileList; } };
      
      if (_openChooser.getFileFilter().equals(_projectFilter)) openProject(fos);
      else open(fos);
    }
    catch(OperationCanceledException oce) {  }
  }
  
  
  private void _putTextIntoDefinitions(String text) {
    int caretPos = _currentDefPane.getCaretPosition();
    
    try { _model.getActiveDocument().insertString(caretPos, text, null); }
    catch (BadLocationException ble) { throw new UnexpectedException(ble); }
  }
  
  
  private void _resetNavigatorPane() {
    if (_model.getDocumentNavigator() instanceof JTreeSortNavigator<?>) {
      JTreeSortNavigator<?> nav = (JTreeSortNavigator<?>)_model.getDocumentNavigator();
      nav.setDisplayManager(getNavPaneDisplayManager());
      nav.setRootIcon(_djProjectIcon);
    }
    _docSplitPane.remove(_docSplitPane.getLeftComponent());
    _docSplitPane.setLeftComponent(new JScrollPane(_model.getDocumentNavigator().asContainer()));
    Font doclistFont = DrJava.getConfig().getSetting(FONT_DOCLIST);
    _model.getDocCollectionWidget().setFont(doclistFont);
    _updateNormalColor();
    _updateBackgroundColor();
  }
  
  
  private void _openProject() { openProject(_openProjectSelector); }
  
  public void openProject(FileOpenSelector projectSelector) {
    
    try { 
      final File[] files = projectSelector.getFiles();
      if (files.length < 1)
        throw new IllegalStateException("Open project file selection not canceled but no project file was selected.");
      final File file = files[0];
      
      updateStatusField("Opening project " + file);
      
      try {
        hourglassOn();
        
        if (! _model.isProjectActive() || (_model.isProjectActive() && _closeProject())) _openProjectHelper(file);
      }
      catch(Exception e) { e.printStackTrace(System.out); }
      finally { hourglassOff(); } 
    }
    catch(OperationCanceledException oce) {  }
    
  }  
  
  
  private void _openProjectHelper(File projectFile) {
    _currentProjFile = projectFile;
    try {
      _mainListener.resetFNFCount();
      _model.openProject(projectFile);
      _setUpProjectButtons(projectFile);
      _openProjectUpdate();
      
      if (_mainListener.someFilesNotFound()) _model.setProjectChanged(true);
      _completeClassSet = new HashSet<GoToFileListEntry>(); 
      addToBrowserHistory();
    }
    catch(MalformedProjectFileException e) {
      _showProjectFileParseError(e); 
      return;
    }
    catch(FileNotFoundException e) {
      _showFileNotFoundError(e); 
      return;
    }
    catch(IOException e) {
      _showIOError(e); 
      return;
    }
  }
  
  private void _setUpProjectButtons(File projectFile) {
    _compileButton = _updateToolbarButton(_compileButton, _compileProjectAction);
    _junitButton = _updateToolbarButton(_junitButton, _junitProjectAction);
    _recentProjectManager.updateOpenFiles(projectFile);
  }
  
  private void _openProjectUpdate() {
    if (_model.isProjectActive()) {
      _closeProjectAction.setEnabled(true);
      _saveProjectAction.setEnabled(true);
      _saveProjectAsAction.setEnabled(true);
      _exportProjectInOldFormatAction.setEnabled(true);
      _projectPropertiesAction.setEnabled(true);

      _junitProjectAction.setEnabled(true);

      _compileProjectAction.setEnabled(true);
      _jarProjectAction.setEnabled(true);
      if (_model.getBuildDirectory() != null) _cleanAction.setEnabled(true);
      _autoRefreshAction.setEnabled(true);
      _model.getDocumentNavigator().asContainer().addKeyListener(_historyListener);
      _model.getDocumentNavigator().asContainer().addFocusListener(_focusListenerForRecentDocs);
      _model.getDocumentNavigator().asContainer().addMouseListener(_resetFindReplaceListener);
      _resetNavigatorPane();
      _model.refreshActiveDocument();


    }
  }
  
  
  boolean closeProject() {
    updateStatusField("Closing current project");
    return _closeProject();
  }
  
  boolean _closeProject() { return _closeProject(false); }
  
  
  boolean _closeProject(boolean quitting) {
    
    
    _completeClassSet = new HashSet<GoToFileListEntry>(); 
    _autoImportClassSet = new HashSet<JavaAPIListEntry>(); 
    
    if (_checkProjectClose()) {
      List<OpenDefinitionsDocument> projDocs = _model.getProjectDocuments();

      _cleanUpDebugger();
      boolean couldClose = _model.closeFiles(projDocs);
      if (! couldClose) return false;
      
      disableFindAgainOnClose(projDocs); 
      
      
      if (quitting) return true;
      _model.closeProject(quitting);
      
      Component renderer = _model.getDocumentNavigator().getRenderer();
      new ForegroundColorListener(renderer);
      new BackgroundColorListener(renderer);
      _resetNavigatorPane();
      if (_model.getDocumentCount() == 1) _model.setActiveFirstDocument();
      _closeProjectAction.setEnabled(false);
      _saveProjectAction.setEnabled(false);
      _saveProjectAsAction.setEnabled(false);
      _exportProjectInOldFormatAction.setEnabled(false);
      _projectPropertiesAction.setEnabled(false);
      _jarProjectAction.setEnabled(false);
      _junitProjectAction.setEnabled(false);

      _compileProjectAction.setEnabled(false);
      _setUpContextMenus();
      _currentProjFile = FileOps.NULL_FILE;

      return true;
    }
    else return false;  
  }
  
  private void _configureBrowsing() {
    BrowserHistoryManager bm = _model.getBrowserHistoryManager();
    _browseBackAction.setEnabled(!bm.isCurrentRegionFirst());
    _browseForwardAction.setEnabled(!bm.isCurrentRegionLast());
  }
  
  private boolean _checkProjectClose() {
    _log.log("is changed? "+_model.isProjectChanged()+" based on "+_model);
    
    if (_model.isProjectChanged()) {
      String fname = _model.getProjectFile().getName();
      String text = fname + " has been modified. Would you like to save it?";
      int rc = 
        JOptionPane.showConfirmDialog(MainFrame.this, text, "Save " + fname + "?", JOptionPane.YES_NO_CANCEL_OPTION);
      switch (rc) {
        case JOptionPane.YES_OPTION:
          _saveProject();
          return true;
        case JOptionPane.NO_OPTION:
          return true;
        case JOptionPane.CLOSED_OPTION:
        case JOptionPane.CANCEL_OPTION:
          return false;
        default:
          throw new RuntimeException("Invalid rc: " + rc);        
      }
    } 
    return true;
  }
  
  public File getCurrentProject() { return _currentProjFile;  }
  
  
  public void open(FileOpenSelector openSelector) {
    try {
      hourglassOn();
      _model.openFiles(openSelector);
    }
    catch (AlreadyOpenException aoe) {
      OpenDefinitionsDocument[] openDocs = aoe.getOpenDocuments();
      for(OpenDefinitionsDocument openDoc : openDocs) {
        try {
          File f = openDoc.getFile();
          if (! _model.inProject(f)) _recentFileManager.updateOpenFiles(f);
        }
        catch (IllegalStateException ise) {
          
          throw new UnexpectedException(ise);
        }
        catch (FileMovedException fme) {
          File f = fme.getFile();
          
          if (! _model.inProject(f))
            _recentFileManager.updateOpenFiles(f);
        }
      }
    }  
    catch (OperationCanceledException oce) {  }
    catch (FileNotFoundException fnf) { 
      _showFileNotFoundError(fnf); 
    }
    catch (IOException ioe) { _showIOError(ioe); }
    finally { hourglassOff(); }
  }
  
  
  public void openFolder(DirectoryChooser chooser) {
    String type = "'." + DrJavaRoot.LANGUAGE_LEVEL_EXTENSIONS[DrJava.getConfig().getSetting(LANGUAGE_LEVEL)] + "' ";
    chooser.setDialogTitle("Open All " + type + "Files in ...");
    
    File openDir = FileOps.NULL_FILE;
    try { 
      File activeFile = _model.getActiveDocument().getFile();
      if (activeFile != null) openDir = activeFile.getParentFile();
      else openDir = _model.getProjectRoot();
    }
    catch(FileMovedException e) {  }
    
    int result = chooser.showDialog(openDir);
    if (result != DirectoryChooser.APPROVE_OPTION)  return; 
    
    File dir = chooser.getSelectedDirectory();
    boolean rec = _openRecursiveCheckBox.isSelected();
    DrJava.getConfig().setSetting(OptionConstants.OPEN_FOLDER_RECURSIVE, Boolean.valueOf(rec));
    updateStatusField("Opening folder " + dir);
    _openFolder(dir, rec);
  }
  
  
  private void _openFolder(File dir, boolean rec) {
    hourglassOn();
    try { _model.openFolder(dir, rec); }
    catch(AlreadyOpenException e) {  }
    catch(IOException e) { _showIOError(e); }
    catch(OperationCanceledException oce) {  }
    finally { hourglassOff(); }
  }
  
  
  private void _close() {
    
    
    
    
    
    List<OpenDefinitionsDocument> l = _model.getDocumentNavigator().getSelectedDocuments();    
    boolean queryNecessary = false; 
    for (OpenDefinitionsDocument doc: l) {
      if ((_model.isProjectActive() && doc.inProjectPath()) || doc.isAuxiliaryFile()) {
        queryNecessary = true;
        break;
      }
    }
    if (queryNecessary) {
      int rc;
      String fileName = null;
      Object[] options = {"Yes", "No"};
      if (l.size()==1) {
        OpenDefinitionsDocument doc = l.get(0);
        try {
          if (doc.isUntitled()) fileName = "File";
          else fileName = _model.getActiveDocument().getFile().getName();
        }
        catch(FileMovedException e) { fileName = e.getFile().getName(); }
        String text = "Closing this file will permanently remove it from the current project." + 
          "\nAre you sure that you want to close this file?";
        
        rc = JOptionPane.showOptionDialog(MainFrame.this, text,"Close " + fileName + "?", JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
      }
      else {
        fileName = l.size()+" files";
        String text = "Closing these "+fileName+" will permanently remove them from the current project." + 
          "\nAre you sure that you want to close these files?";
        
        rc = JOptionPane.showOptionDialog(MainFrame.this, text, "Close "+l.size()+" files?", JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
      }
      if (rc != JOptionPane.YES_OPTION) return;
      
      updateStatusField("Closing " + fileName);
      _model.setProjectChanged(true);
    }
    
    disableFindAgainOnClose(l); 
    
    
    for(OpenDefinitionsDocument doc: l) {
      _model.closeFile(doc);
    }
  }
  
  private void _closeFolder() {
    ArrayList<OpenDefinitionsDocument> docs = _model.getDocumentNavigator().getDocuments();
    final LinkedList<OpenDefinitionsDocument> l = new LinkedList<OpenDefinitionsDocument>();
    
    if (_model.getDocumentNavigator().isGroupSelected()) {
      for (OpenDefinitionsDocument doc: docs) {
        if (_model.getDocumentNavigator().isSelectedInGroup(doc)) { l.add(doc); }
      }
      disableFindAgainOnClose(l); 
      _model.closeFiles(l);
      if (! l.isEmpty()) _model.setProjectChanged(true);
    }
  }
  
  private void _printDefDoc() {
    try {
      _model.getActiveDocument().print();
    }
    catch (FileMovedException fme) {
      _showFileMovedError(fme);
    }
    catch (PrinterException e) {
      _showError(e, "Print Error", "An error occured while printing.");
    }
    catch (BadLocationException e) {
      _showError(e, "Print Error", "An error occured while printing.");
    }
  }
  
  private void _printConsole() {
    try {
      _model.getConsoleDocument().print();
    }
    catch (PrinterException e) {
      _showError(e, "Print Error", "An error occured while printing.");
    }
  }
  
  private void _printInteractions() {
    try {
      _model.getInteractionsDocument().print();
    }
    catch (PrinterException e) {
      _showError(e, "Print Error", "An error occured while printing.");
    }
  }
  
  
  private void _printDefDocPreview() {
    try {
      _model.getActiveDocument().preparePrintJob();
      new PreviewDefDocFrame(_model, this);
    }
    catch (FileMovedException fme) {
      _showFileMovedError(fme);
    }
    catch (BadLocationException e) {
      _showError(e, "Print Error", "An error occured while preparing the print preview.");
    }
    catch (IllegalStateException e) {
      _showError(e, "Print Error", "An error occured while preparing the print preview.");
    }
  }
  
  private void _printConsolePreview() {
    try {
      _model.getConsoleDocument().preparePrintJob();
      new PreviewConsoleFrame(_model, this, false);
    }
    catch (IllegalStateException e) {
      _showError(e, "Print Error", "An error occured while preparing the print preview.");
    }
  }
  
  private void _printInteractionsPreview() {
    try {
      _model.getInteractionsDocument().preparePrintJob();
      new PreviewConsoleFrame(_model, this, true);
    }
    catch (IllegalStateException e) {
      _showError(e, "Print Error", "An error occured while preparing the print preview.");
    }
  }
  
  private void _pageSetup() {
    PrinterJob job = PrinterJob.getPrinterJob();
    _model.setPageFormat(job.pageDialog(_model.getPageFormat()));
  }
  
  
  void closeAll() { _closeAll(); }
  
  private void _closeAll() {
    updateStatusField("Closing All Files");
    if (!_model.isProjectActive() || _model.isProjectActive() && _closeProject())  _model.closeAllFiles();
  }
  
  private boolean _save() {
    updateStatusField("Saving File");
    try {
      
      List<OpenDefinitionsDocument> l = _model.getDocumentNavigator().getSelectedDocuments();
      boolean success = false;
      for(OpenDefinitionsDocument doc: l) {
        if (doc.saveFile(_saveSelector)) {
          getDefPaneGivenODD(doc).hasWarnedAboutModified(false);
          success = true;
        }
      }
      
      
      _model.refreshActiveDocument();
      return success;
    }
    catch (IOException ioe) { 
      _showIOError(ioe);
      return false;
    }
  }
  
  private boolean _saveAs() {
    updateStatusField("Saving File Under New Name");
    try {
      boolean toReturn = _model.getActiveDocument().saveFileAs(_saveAsSelector);
      _model.refreshActiveDocument();  
      return toReturn;
    }
    catch (IOException ioe) {
      _showIOError(ioe);
      return false;
    }
  }
  
  private boolean _rename() {
    try {
      if (!_model.getActiveDocument().fileExists()) return _saveAs();
      else {
        File fileToDelete;
        try { fileToDelete = _model.getActiveDocument().getFile(); } 
        catch (FileMovedException fme) { return _saveAs(); }
        boolean toReturn = _model.getActiveDocument().saveFileAs(_saveAsSelector);
        
        
        if (toReturn && ! _model.getActiveDocument().getFile().equals(fileToDelete)) fileToDelete.delete();
        
        _model.refreshActiveDocument();
        return toReturn;
      }
    }
    catch (IOException ioe) {
      _showIOError(ioe);
      return false;
    }
  }  
  
  
  void _saveAll() {
    hourglassOn();
    try {
      if (_model.isProjectActive()) _saveProject();
      _model.saveAllFiles(_saveSelector);
    }
    catch (IOException ioe) { _showIOError(ioe); }
    finally { hourglassOff(); }
  }
  
  void _saveAllOld() {
    hourglassOn();
    File file = _currentProjFile;
    try {
      if (_model.isProjectActive()) {
        if (file.getName().indexOf(".") == -1) file = new File (file.getAbsolutePath() + OLD_PROJECT_FILE_EXTENSION);
        _model.exportOldProject(file, gatherProjectDocInfo());
      }
      _model.saveAllFiles(_saveSelector);
    }
    catch (IOException ioe) { _showIOError(ioe); }
    finally { hourglassOff(); }
  }
  
  
  void saveProject() { _saveProject(); }
  
  private void _saveProject() {
    
    _saveProjectHelper(_currentProjFile);
  }
  
    
  private void _editProject() {
    ProjectPropertiesFrame ppf = new ProjectPropertiesFrame(this);
    ppf.setVisible(true);
    ppf.reset();
    ppf.toFront();  
  }
  
  
  private void _newProject() {
    
    _closeProject(true);  
    _saveChooser.setFileFilter(_projectFilter);
    _saveChooser.setMultiSelectionEnabled(false);
    int rc = _saveChooser.showSaveDialog(this);
    if (rc == JFileChooser.APPROVE_OPTION) {      
      File projectFile = _saveChooser.getSelectedFile();
      
      if (projectFile == null || projectFile.getParentFile() == null) { return; }
      String fileName = projectFile.getName();
      
      if (! fileName.endsWith(OptionConstants.PROJECT_FILE_EXTENSION)) {
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex == -1) projectFile = new File (projectFile.getAbsolutePath() + OptionConstants.PROJECT_FILE_EXTENSION);
        else projectFile = new File(projectFile.getParentFile(), fileName.substring(0, lastIndex) + OptionConstants.PROJECT_FILE_EXTENSION);
      }
      if (projectFile == null ||
          projectFile.getParentFile() == null ||
          (projectFile.exists() && ! _verifyOverwrite())) { return; }
      
      _model.createNewProject(projectFile); 


      _editProject();  
      try { _model.configNewProject(); }  
      catch(IOException e) { throw new UnexpectedException(e); }
      _setUpProjectButtons(projectFile);
      _currentProjFile = projectFile;
    }
  }
  
  
  private boolean _saveProjectAs() {
    

    _saveChooser.removeChoosableFileFilter(_projectFilter);
    _saveChooser.removeChoosableFileFilter(_javaSourceFilter);
    _saveChooser.setFileFilter(_projectFilter);






    
    if (_currentProjFile != FileOps.NULL_FILE) _saveChooser.setSelectedFile(_currentProjFile);
    _saveChooser.setMultiSelectionEnabled(false);
    int rc = _saveChooser.showSaveDialog(this);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File file = _saveChooser.getSelectedFile();
      if ((file!=null) && (! file.exists() || _verifyOverwrite())) { 
        _model.setProjectFile(file);
        _currentProjFile = file;
      }
    }
    
    return (rc == JFileChooser.APPROVE_OPTION);
  }
  
  void _saveProjectHelper(File file) {
    try {
      String fileName = file.getAbsolutePath();
      if (!fileName.endsWith(PROJECT_FILE_EXTENSION) &&
          !fileName.endsWith(PROJECT_FILE_EXTENSION2) &&
          !fileName.endsWith(OLD_PROJECT_FILE_EXTENSION)) {
        
        String text = "The file name does not end with a DrJava project file "+
          "extension ("+PROJECT_FILE_EXTENSION+" or "+PROJECT_FILE_EXTENSION2+" or "+OLD_PROJECT_FILE_EXTENSION+"):\n"+
          file.getName()+"\n"+
          "Do you want to append "+PROJECT_FILE_EXTENSION+" at the end?";
        
        Object[] options = {"Append "+PROJECT_FILE_EXTENSION, "Don't Change File Name"};  
        int rc = 0;
        if (!Utilities.TEST_MODE) {
          rc = JOptionPane.showOptionDialog(MainFrame.this, text, "Append Extension?", JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        }
        if (rc==0) {
          int lastDot = fileName.lastIndexOf('.');
          if (lastDot == -1) {
            file = new File(fileName + PROJECT_FILE_EXTENSION);
          }
          else {
            file = new File(fileName.substring(0,lastDot) + PROJECT_FILE_EXTENSION);
          }
        }
      }
      fileName = file.getCanonicalPath();
      if (fileName.endsWith(OLD_PROJECT_FILE_EXTENSION)) {
        String text = "The project will be saved in XML format." + 
          "\nDo you want to change the project file's extension to "+PROJECT_FILE_EXTENSION+"?";
        
        Object[] options = {"Change to "+PROJECT_FILE_EXTENSION+"", "Keep \"" + 
          fileName.substring(fileName.lastIndexOf('.'))+"\""};  
        int rc = 1;
        if (!Utilities.TEST_MODE) {
          rc = JOptionPane.showOptionDialog(MainFrame.this, text, "Change Extension?", JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        }
        if (rc == 0) {
          fileName = fileName.substring(0,fileName.length() - OLD_PROJECT_FILE_EXTENSION.length()) + 
            PROJECT_FILE_EXTENSION;
          file = new File(fileName);
          if (! file.exists() || _verifyOverwrite()) { 
            _model.setProjectFile(file);
            _currentProjFile = file;
          }
        }
      }
      _model.saveProject(file, gatherProjectDocInfo());



    }
    catch(IOException ioe) { _showIOError(ioe); }
    _recentProjectManager.updateOpenFiles(file);
    _model.setProjectChanged(false);
  }
  
  public HashMap<OpenDefinitionsDocument,DocumentInfoGetter> gatherProjectDocInfo() {
    HashMap<OpenDefinitionsDocument,DocumentInfoGetter> map =
      new HashMap<OpenDefinitionsDocument,DocumentInfoGetter>();
    List<OpenDefinitionsDocument> docs = _model.getProjectDocuments();
    for(OpenDefinitionsDocument doc: docs) {
      map.put(doc, _makeInfoGetter(doc));
    }
    return map;
  }
  
  private DocumentInfoGetter _makeInfoGetter(final OpenDefinitionsDocument doc) {
    JScrollPane s = _defScrollPanes.get(doc);
    if (s == null) s = _createDefScrollPane(doc);
    
    final DefinitionsPane pane = _currentDefPane; 
    return new DocumentInfoGetter() {
      public Pair<Integer,Integer> getSelection() {
        Integer selStart = Integer.valueOf(pane.getSelectionStart());
        Integer selEnd = Integer.valueOf(pane.getSelectionEnd());
        if ( selStart == 0 && selEnd == 0) 
          return new Pair<Integer,Integer>(pane.getCaretPosition(),pane.getCaretPosition());
        if (pane.getCaretPosition() == selStart) return new Pair<Integer,Integer>(selEnd,selStart);
        return new Pair<Integer,Integer>(selStart,selEnd);
      }
      public Pair<Integer,Integer> getScroll() {
        Integer scrollv = Integer.valueOf(pane.getVerticalScroll());
        Integer scrollh = Integer.valueOf(pane.getHorizontalScroll());
        return new Pair<Integer,Integer>(scrollv,scrollh); 
      }
      public File getFile() { return doc.getRawFile(); }
      public String getPackage() { return doc.getPackageName(); }
      public boolean isActive() { return _model.getActiveDocument() == doc; }
      public boolean isUntitled() { return doc.isUntitled(); }
    };
  }
  
  private void _revert() {
    
    List<OpenDefinitionsDocument> l = _model.getDocumentNavigator().getSelectedDocuments();
    for(OpenDefinitionsDocument d: l) { _revert(d); }
  }
  
  private void _revert(OpenDefinitionsDocument doc) {
    try {
      doc.revertFile();
    }
    catch (FileMovedException fme) {
      _showFileMovedError(fme);
    }
    catch (IOException ioe) {
      _showIOError(ioe);
    }
  }
  
  
  
  void quit() {

    if (_promptBeforeQuit) {
      String title = "Quit DrJava?";
      String message = "Are you sure you want to quit DrJava?";
      ConfirmCheckBoxDialog dialog = new ConfirmCheckBoxDialog(MainFrame.this, title, message);
      int rc = dialog.show();
      if (rc != JOptionPane.YES_OPTION) return;
      else {
        
        if (dialog.getCheckBoxValue() == true) {
          DrJava.getConfig().setSetting(QUIT_PROMPT, Boolean.FALSE);
        }
      }
    }
    _executeExternalDialog.setVisible(false);
    
    
    
    if (! _closeProject(true)) { return;  }
    
    _recentFileManager.saveRecentFiles();
    _recentProjectManager.saveRecentFiles();
    if (! _model.closeAllFilesOnQuit()) { return;  }
    _storePositionInfo();
    
    
    
    if (! DrJava.getConfig().hadStartupException()) {
      try { DrJava.getConfig().saveConfiguration(); }
      catch (IOException ioe) { _showIOError(ioe); }
    }
    
    dispose();    
    _model.quit();
  }
  
  private void _forceQuit() { _model.forceQuit(); }
  
  
  private void _storePositionInfo() {
    assert EventQueue.isDispatchThread();
    Configuration config = DrJava.getConfig();
    
    
    if (config.getSetting(WINDOW_STORE_POSITION).booleanValue()) {
      Rectangle bounds = getBounds();
      config.setSetting(WINDOW_HEIGHT, Integer.valueOf(bounds.height));
      config.setSetting(WINDOW_WIDTH, Integer.valueOf(bounds.width));
      config.setSetting(WINDOW_X, Integer.valueOf(bounds.x));
      config.setSetting(WINDOW_Y, Integer.valueOf(bounds.y));
      config.setSetting(WINDOW_STATE, Integer.valueOf(getExtendedState()));
    }
    else {
      
      config.setSetting(WINDOW_HEIGHT, WINDOW_HEIGHT.getDefault());
      config.setSetting(WINDOW_WIDTH, WINDOW_WIDTH.getDefault());
      config.setSetting(WINDOW_X, WINDOW_X.getDefault());
      config.setSetting(WINDOW_Y, WINDOW_Y.getDefault());
      config.setSetting(WINDOW_STATE, WINDOW_STATE.getDefault());
    }
    
    
    if ((DrJava.getConfig().getSetting(DIALOG_GOTOFILE_STORE_POSITION).booleanValue())
          && (_gotoFileDialog != null) && (_gotoFileDialog.getFrameState() != null)) {
      config.setSetting(DIALOG_GOTOFILE_STATE, (_gotoFileDialog.getFrameState().toString()));
    }
    else {
      
      config.setSetting(DIALOG_GOTOFILE_STATE, DIALOG_GOTOFILE_STATE.getDefault());
    }
    
    
    if ((DrJava.getConfig().getSetting(DIALOG_OPENJAVADOC_STORE_POSITION).booleanValue())
          && (_openJavadocDialog != null) && (_openJavadocDialog.getFrameState() != null)) {
      config.setSetting(DIALOG_OPENJAVADOC_STATE, (_openJavadocDialog.getFrameState().toString()));
    }
    else {
      
      config.setSetting(DIALOG_OPENJAVADOC_STATE, DIALOG_OPENJAVADOC_STATE.getDefault());
    }    
    
    
    if ((DrJava.getConfig().getSetting(DIALOG_COMPLETE_WORD_STORE_POSITION).booleanValue())
          && (_completeWordDialog != null) && (_completeWordDialog.getFrameState() != null)) {
      config.setSetting(DIALOG_COMPLETE_WORD_STATE, (_completeWordDialog.getFrameState().toString()));
    }
    else {
      
      config.setSetting(DIALOG_COMPLETE_WORD_STATE, DIALOG_COMPLETE_WORD_STATE.getDefault());
    }
    
    
    if ((DrJava.getConfig().getSetting(DIALOG_JAROPTIONS_STORE_POSITION).booleanValue())
          && (_jarOptionsDialog != null) && (_jarOptionsDialog.getFrameState() != null)) {
      config.setSetting(DIALOG_JAROPTIONS_STATE, (_jarOptionsDialog.getFrameState().toString()));
    }
    else {
      
      config.setSetting(DIALOG_JAROPTIONS_STATE, DIALOG_JAROPTIONS_STATE.getDefault());
    }
    
    
    if ((DrJava.getConfig().getSetting(DIALOG_TABBEDPANES_STORE_POSITION).booleanValue())
          && (_tabbedPanesFrame != null) && (_tabbedPanesFrame.getFrameState() != null)) {
      config.setSetting(DIALOG_TABBEDPANES_STATE, (_tabbedPanesFrame.getFrameState().toString()));
    }
    else {
      
      config.setSetting(DIALOG_TABBEDPANES_STATE, DIALOG_TABBEDPANES_STATE.getDefault());
    }
    
    
    if ((DrJava.getConfig().getSetting(DIALOG_DEBUGFRAME_STORE_POSITION).booleanValue())
          && (_debugFrame != null) && (_debugFrame.getFrameState() != null)) {
      config.setSetting(DIALOG_DEBUGFRAME_STATE, (_debugFrame.getFrameState().toString()));
    }
    else {
      
      config.setSetting(DIALOG_DEBUGFRAME_STATE, DIALOG_DEBUGFRAME_STATE.getDefault());
    }
    
    
    if (_showDebugger) config.setSetting(DEBUG_PANEL_HEIGHT, Integer.valueOf(_debugPanel.getHeight()));
    
    
    config.setSetting(DOC_LIST_WIDTH, Integer.valueOf(_docSplitPane.getDividerLocation()));
  }
  
  private void _cleanUpDebugger() { if (isDebuggerReady()) _model.getDebugger().shutdown(); }
  
  private void _compile() {
    
    _cleanUpDebugger();
    hourglassOn();
    try {

      try { _model.getCompilerModel().compile(_model.getDocumentNavigator().getSelectedDocuments()); }
      catch (FileMovedException fme) { _showFileMovedError(fme); }
      catch (IOException ioe) { _showIOError(ioe); }
    }
    finally { hourglassOff();}

  }
  
  private void _compileFolder() {
    _cleanUpDebugger();
    hourglassOn();
    try {
      ArrayList<OpenDefinitionsDocument> docs = _model.getDocumentNavigator().getDocuments();
      final LinkedList<OpenDefinitionsDocument> l = new LinkedList<OpenDefinitionsDocument>();
      if (_model.getDocumentNavigator().isGroupSelected()) {
        for (OpenDefinitionsDocument doc: docs) {
          if (_model.getDocumentNavigator().isSelectedInGroup(doc)) l.add(doc);
        }
        


        try { _model.getCompilerModel().compile(l); }
        catch (FileMovedException fme) { _showFileMovedError(fme); }
        catch (IOException ioe) { _showIOError(ioe); }


      }
    }
    finally { hourglassOff(); }

  }
  
  private void _compileProject() { 
    _cleanUpDebugger();


    hourglassOn();
    try { _model.getCompilerModel().compileProject(); }
    catch (FileMovedException fme) { _showFileMovedError(fme); }
    catch (IOException ioe) { _showIOError(ioe); }
    finally { hourglassOff(); }



  }
  
  private void _compileAll() {
    _cleanUpDebugger();
    hourglassOn();
    try { _model.getCompilerModel().compileAll(); }
    catch (FileMovedException fme) { _showFileMovedError(fme); }
    catch (IOException ioe) { _showIOError(ioe); }
    finally{ hourglassOff(); }
  }
  
  private boolean showCleanWarning() {
    if (DrJava.getConfig().getSetting(PROMPT_BEFORE_CLEAN).booleanValue()) {
      String buildDirTxt = "";
      try { buildDirTxt = _model.getBuildDirectory().getCanonicalPath(); }
      catch (Exception e) { buildDirTxt = _model.getBuildDirectory().getPath(); }
      ConfirmCheckBoxDialog dialog =
        new ConfirmCheckBoxDialog(MainFrame.this,
                                  "Clean Build Directory?",
                                  "Cleaning your build directory will delete all\n" + 
                                  "class files and empty folders within that directory.\n" + 
                                  "Are you sure you want to clean\n" + 
                                  buildDirTxt + "?",
                                  "Do not show this message again");
      int rc = dialog.show();
      switch (rc) {
        case JOptionPane.YES_OPTION:
          _saveAll();
          
          if (dialog.getCheckBoxValue()) DrJava.getConfig().setSetting(PROMPT_BEFORE_CLEAN, Boolean.FALSE);
          return true;
        case JOptionPane.NO_OPTION:      return false;
        case JOptionPane.CANCEL_OPTION:  return false;
        case JOptionPane.CLOSED_OPTION:  return false;
        default:  throw new RuntimeException("Invalid rc from showConfirmDialog: " + rc);
      }
    }
    return true;
  }
  
  private void _clean() { _model.cleanBuildDirectory(); }  
  
  
  HashSet<GoToFileListEntry> _completeClassSet = new HashSet<GoToFileListEntry>();
  
  
  HashSet<JavaAPIListEntry> _autoImportClassSet = new HashSet<JavaAPIListEntry>();
  
  
  private void _scanClassFiles() {
    Thread t = new Thread(new Runnable() {
      public void run() {
        File buildDir = _model.getBuildDirectory();
        HashSet<GoToFileListEntry> hs = new HashSet<GoToFileListEntry>();
        HashSet<JavaAPIListEntry> hs2 = new HashSet<JavaAPIListEntry>();
        if (buildDir!=null) {
          List<File> classFiles = _model.getClassFiles();
          DummyOpenDefDoc dummyDoc = new DummyOpenDefDoc();
          for(File f: classFiles) {
            String s = f.toString();
            if (s.lastIndexOf(File.separatorChar) >= 0) {
              s = s.substring(s.lastIndexOf(File.separatorChar)+1);
            }
            s = s.substring(0, s.lastIndexOf(".class"));
            s = s.replace('$', '.');
            int pos = 0;
            boolean ok = true;
            while ((pos=s.indexOf('.', pos)) >= 0) {
              if (s.length() <= pos + 1 || Character.isDigit(s.charAt(pos + 1))) {
                ok = false;
                break;
              }
              ++pos;
            }
            if (ok) {
              if (s.lastIndexOf('.') >= 0) {
                s = s.substring(s.lastIndexOf('.') + 1);
              }
              GoToFileListEntry entry = new GoToFileListEntry(dummyDoc, s);
              hs.add(entry);
              try {
                String rel = FileOps.stringMakeRelativeTo(f, buildDir);
                String full = rel.replace(File.separatorChar, '.');
                full = full.substring(0, full.lastIndexOf(".class"));
                if (full.indexOf('$')<0) {
                  
                  
                  
                  hs2.add(new JavaAPIListEntry(s, full, null));
                }
              }
              catch(IOException ioe) {  }
              catch(SecurityException se) {  }
            }
          }
        }
        _completeClassSet = new HashSet<GoToFileListEntry>(hs);
        _autoImportClassSet = new HashSet<JavaAPIListEntry>(hs2);
      }
    });
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }
  
  private void _runProject() {
    if (_model.isProjectActive()) {
      try {
        final File f = _model.getMainClassContainingFile();
        if (f != null) {
          updateStatusField("Running Open Project");
          OpenDefinitionsDocument doc = _model.getDocumentForFile(f);
          doc.runMain(_model.getMainClass());
        }
      }
      catch (ClassNameNotFoundException e) {
        
        String msg =
          "DrJava could not find the top level class name in the\n" +
          "current document, so it could not run the class.  Please\n" +
          "make sure that the class is properly defined first.";
        
        JOptionPane.showMessageDialog(MainFrame.this, msg, "No Class Found", JOptionPane.ERROR_MESSAGE);
      }
      catch (FileMovedException fme) { _showFileMovedError(fme); }
      catch (IOException ioe) { _showIOError(ioe); }
    }
    else _runMain();
  }
  
  
  private void _runMain() {
    updateStatusField("Running main Method of Current Document");
    
    try { _model.getActiveDocument().runMain(null); }
    
    catch (ClassNameNotFoundException e) {
      
      String msg =
        "DrJava could not find the top level class name in the\n" +
        "current document, so it could not run the class.  Please\n" +
        "make sure that the class is properly defined first.";
      
      JOptionPane.showMessageDialog(MainFrame.this, msg, "No Class Found", JOptionPane.ERROR_MESSAGE);
    }
    catch (FileMovedException fme) { _showFileMovedError(fme); }
    catch (IOException ioe) { _showIOError(ioe); }
  }
  
  
  private void _runApplet() {
    updateStatusField("Running Current Document as Applet");
    
    try { _model.getActiveDocument().runApplet(null); }
    
    catch (ClassNameNotFoundException e) {
      
      String msg =
        "DrJava could not find the top level class name in the\n" +
        "current document, so it could not run the class.  Please\n" +
        "make sure that the class is properly defined first.";
      
      JOptionPane.showMessageDialog(MainFrame.this, msg, "No Class Found", JOptionPane.ERROR_MESSAGE);
    }
    catch (FileMovedException fme) { _showFileMovedError(fme); }
    catch (IOException ioe) { _showIOError(ioe); }
  }
  
  private void _junit() {
    hourglassOn(); 
    new Thread("Run JUnit on Current Document") {
      public void run() {
        _disableJUnitActions();
        

        try { _model.getJUnitModel().junitDocs(_model.getDocumentNavigator().getSelectedDocuments()); }
        catch(UnexpectedException e) { _junitInterrupted(e); }
        catch(Exception e) { _junitInterrupted(new UnexpectedException(e)); }
      }
    }.start();
  }
  
  private void _junitFolder() {
    updateStatusField("Running Unit Tests in Current Folder");
    hourglassOn();  
    new Thread("Run JUnit on specified folder") {
      public void run() { 
        _disableJUnitActions();

        if (_model.getDocumentNavigator().isGroupSelected()) {
          ArrayList<OpenDefinitionsDocument> docs = _model.getDocumentNavigator().getDocuments();
          final LinkedList<OpenDefinitionsDocument> l = new LinkedList<OpenDefinitionsDocument>();
          for (OpenDefinitionsDocument doc: docs) {
            if (_model.getDocumentNavigator().isSelectedInGroup(doc)) l.add(doc);
          }
          try { _model.getJUnitModel().junitDocs(l); }  
          catch(UnexpectedException e) { _junitInterrupted(e); }
          catch(Exception e) { _junitInterrupted(new UnexpectedException(e)); }
        }
      }
    }.start();
  }
  
  
  private void _junitProject() {
    updateStatusField("Running JUnit Tests in Project");
    hourglassOn();  
    _disableJUnitActions();
    try { _model.getJUnitModel().junitProject(); } 
    catch(UnexpectedException e) { _junitInterrupted(e); }
    catch(Exception e) { _junitInterrupted(new UnexpectedException(e)); }
  }
  
  
  private void _junitAll() {
    updateStatusField("Running All Open Unit Tests");
    hourglassOn();  
    _disableJUnitActions();
    try { _model.getJUnitModel().junitAll(); } 
    catch(UnexpectedException e) { _junitInterrupted(e); }
    catch(Exception e) { _junitInterrupted(new UnexpectedException(e)); }
  }
  
  
  private volatile DecoratedAction _junit_compileProjectDecoratedAction;
  private volatile DecoratedAction _junit_compileAllDecoratedAction;
  private volatile DecoratedAction _junit_compileFolderDecoratedAction;
  private volatile DecoratedAction _junit_junitFolderDecoratedAction;
  private volatile DecoratedAction _junit_junitAllDecoratedAction;
  private volatile DecoratedAction _junit_junitDecoratedAction;
  private volatile DecoratedAction _junit_junitOpenProjectFilesDecoratedAction;
  private volatile DecoratedAction _junit_cleanDecoratedAction;
  private volatile DecoratedAction _junit_autoRefreshDecoratedAction;
  private volatile DecoratedAction _junit_projectPropertiesDecoratedAction;
  private volatile DecoratedAction _junit_runProjectDecoratedAction;
  private volatile DecoratedAction _junit_runDecoratedAction;
  private volatile DecoratedAction _junit_runAppletDecoratedAction;

  
  private static class DecoratedAction extends AbstractAction {
    
    AbstractAction _decoree;
    
    boolean _shallowEnabled;
    
    public DecoratedAction(AbstractAction a, boolean b) {
      super((String)a.getValue("Name"));
      _decoree = a;
      _shallowEnabled = _decoree.isEnabled();
      _decoree.setEnabled(b);
    }
    public void actionPerformed(ActionEvent ae) { _decoree.actionPerformed(ae); }
    
    public void setEnabled(boolean b) { _shallowEnabled = b; }
    
    public AbstractAction getUpdatedDecoree() { _decoree.setEnabled(_shallowEnabled); return _decoree; }
  }
  
  
  private void _disableJUnitActions() {
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    _compileProjectAction = _junit_compileProjectDecoratedAction = new DecoratedAction(_compileProjectAction, false);
    _compileAllAction = _junit_compileAllDecoratedAction = new DecoratedAction(_compileAllAction, false);
    _compileFolderAction = _junit_compileFolderDecoratedAction = new DecoratedAction(_compileFolderAction, false);
    _junitFolderAction = _junit_junitFolderDecoratedAction = new DecoratedAction(_junitFolderAction, false);
    _junitAllAction = _junit_junitAllDecoratedAction = new DecoratedAction(_junitAllAction, false);
    _junitAction = _junit_junitDecoratedAction = new DecoratedAction(_junitAction, false);
    _junitProjectAction = _junit_junitOpenProjectFilesDecoratedAction = new DecoratedAction(_junitProjectAction, false);  
    _cleanAction = _junit_cleanDecoratedAction = new DecoratedAction(_cleanAction, false);
    _autoRefreshAction = _junit_autoRefreshDecoratedAction = new DecoratedAction(_autoRefreshAction, false);
    _projectPropertiesAction = _junit_projectPropertiesDecoratedAction = 
      new DecoratedAction(_projectPropertiesAction, false);
    _runProjectAction = _junit_runProjectDecoratedAction = new DecoratedAction(_runProjectAction, false);
    _runAction = _junit_runDecoratedAction = new DecoratedAction(_runAction, false);
    _runAppletAction = _junit_runAppletDecoratedAction = new DecoratedAction(_runAppletAction, false);
  }
  private void _restoreJUnitActionsEnabled() {












    
    _compileProjectAction = _junit_compileProjectDecoratedAction.getUpdatedDecoree();
    _compileAllAction = _junit_compileAllDecoratedAction.getUpdatedDecoree();
    _compileFolderAction = _junit_compileFolderDecoratedAction.getUpdatedDecoree();
    _junitFolderAction = _junit_junitFolderDecoratedAction.getUpdatedDecoree();
    _junitAllAction = _junit_junitAllDecoratedAction.getUpdatedDecoree();
    _junitAction = _junit_junitDecoratedAction.getUpdatedDecoree();
    _junitProjectAction = _junit_junitOpenProjectFilesDecoratedAction.getUpdatedDecoree();
    _cleanAction = _junit_cleanDecoratedAction.getUpdatedDecoree();
    _autoRefreshAction = _junit_autoRefreshDecoratedAction.getUpdatedDecoree();
    _projectPropertiesAction = _junit_projectPropertiesDecoratedAction.getUpdatedDecoree();
    _runProjectAction = _junit_runProjectDecoratedAction.getUpdatedDecoree();
    _runAction = _junit_runDecoratedAction.getUpdatedDecoree();
    _runAppletAction = _junit_runAppletDecoratedAction.getUpdatedDecoree();
  }
  







  
  
  void debuggerResume() throws DebugException {
    if (isDebuggerReady()) {
      _model.getDebugger().resume();
      removeCurrentLocationHighlight();
    }
  }
  
  
  void debuggerAutomaticTrace() {
    if(isDebuggerReady())  {
      if(!_model.getDebugger().isAutomaticTraceEnabled()) {
        try {
          int rate = DrJava.getConfig().getSetting(OptionConstants.AUTO_STEP_RATE);
          
          _automaticTraceTimer = new Timer(rate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              if (_model.getDebugger().isAutomaticTraceEnabled()) {
                
                debuggerStep(Debugger.StepType.STEP_INTO);
              }
            }
          });
          _automaticTraceTimer.setRepeats(false);
          _model.getDebugger().setAutomaticTraceEnabled(true);
          _debugPanel.setAutomaticTraceButtonText();
          debuggerStep(Debugger.StepType.STEP_INTO);
        }
        catch (IllegalStateException ise) {
          
          
          
          
          
          
        }        
      }
      else {
        _model.getDebugger().setAutomaticTraceEnabled(false);
        _debugPanel.setAutomaticTraceButtonText();
        if (_automaticTraceTimer!=null) _automaticTraceTimer.stop();
      }
    }    
  }
  
  
  void debuggerStep(Debugger.StepType type) {
    if (isDebuggerReady()) {
      try { _model.getDebugger().step(type); }
      catch (IllegalStateException ise) {
        
        
        
        
        
        
      }
      catch (DebugException de) {
        _showError(de, "Debugger Error",
                   "Could not create a step request.");
      }
    }
  }
  
  
  void debuggerToggleBreakpoint() {
    addToBrowserHistory();
    OpenDefinitionsDocument doc = _model.getActiveDocument();
    
    boolean isUntitled = doc.isUntitled();
    if (isUntitled) {
      JOptionPane.showMessageDialog(this,
                                    "You must save and compile this document before you can\n" +
                                    "set a breakpoint in it.",
                                    "Must Save and Compile",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    
    boolean isModified = doc.isModifiedSinceSave();
    if (isDebuggerReady() && isModified  && !_currentDefPane.hasWarnedAboutModified() &&
        DrJava.getConfig().getSetting(WARN_BREAKPOINT_OUT_OF_SYNC).booleanValue()) {
      String message =
        "This document has been modified and may be out of sync\n" +
        "with the debugger.  It is recommended that you first\n" +
        "save and recompile before continuing to use the debugger,\n" +
        "to avoid any unexpected errors.  Would you still like to\n" +
        "toggle the breakpoint on the specified line?";
      String title = "Toggle breakpoint on modified file?";
      
      ConfirmCheckBoxDialog dialog = new ConfirmCheckBoxDialog(this, title, message);
      int rc = dialog.show();
      switch (rc) {
        case JOptionPane.YES_OPTION:
          _currentDefPane.hasWarnedAboutModified(true);
          if (dialog.getCheckBoxValue()) {
            DrJava.getConfig().setSetting(WARN_BREAKPOINT_OUT_OF_SYNC, Boolean.FALSE);
          }
          break;
          
        case JOptionPane.NO_OPTION:
          if (dialog.getCheckBoxValue())  DrJava.getConfig().setSetting(WARN_BREAKPOINT_OUT_OF_SYNC, Boolean.FALSE);
          return;
          
        case JOptionPane.CANCEL_OPTION:
        case JOptionPane.CLOSED_OPTION:
          
          return;
          
        default:
          throw new RuntimeException("Invalid rc from showConfirmDialog: " + rc);
      }
    }
    
    try {
      Debugger debugger = _model.getDebugger();
      boolean breakpointSet = 
        debugger.toggleBreakpoint(doc, _currentDefPane.getCaretPosition(), true);
      if (breakpointSet) showBreakpoints();
    }
    catch (DebugException de) {
      _showError(de, "Debugger Error", "Could not set a breakpoint at the current line.");
    }
  }
  
  

  






















  



  
  
  void debuggerClearAllBreakpoints() {
    _model.getBreakpointManager().clearRegions();
  }
  
  void _showFileMovedError(FileMovedException fme) {
    try {
      File f = fme.getFile();
      OpenDefinitionsDocument doc = _model.getDocumentForFile(f);
      if (doc != null && _saveSelector.shouldSaveAfterFileMoved(doc, f)) _saveAs();
    }
    catch (IOException ioe) {  }
  }
  
  void _showProjectFileParseError(MalformedProjectFileException mpfe) {
    _showError(mpfe, "Invalid Project File", "DrJava could not read the given project file.");
  }
  
  void _showFileNotFoundError(FileNotFoundException fnf) {
    _showError(fnf, "File Not Found", "The specified file was not found on disk.");
  }
  
  void _showIOError(IOException ioe) {
    _showError(ioe, "Input/output error", "An I/O exception occurred during the last operation.");
  }
  
  void _showClassNotFoundError(ClassNotFoundException cnfe) {
    _showError(cnfe, "Class Not Found",
               "A ClassNotFound exception occurred during the last operation.\n" +
               "Please check that your classpath includes all relevant directories.\n\n");
  }
  
  void _showNoClassDefError(NoClassDefFoundError ncde) {
    _showError(ncde, "No Class Def",
               "A NoClassDefFoundError occurred during the last operation.\n" +
               "Please check that your classpath includes all relevant paths.\n\n");
  }
  
  void _showDebugError(DebugException de) {
    _showError(de, "Debug Error", "A Debugger error occurred in the last operation.\n\n");
  }

  void _showJUnitInterrupted(UnexpectedException e) {
    _showWarning(e.getCause(), "JUnit Testing Interrupted", 
                 "The slave JVM has thrown a RemoteException probably indicating that it has been reset.\n\n");
  }
  
  void _showJUnitInterrupted(String message) {
    JOptionPane.showMessageDialog(this, message, "JUnit Testing Interrupted", JOptionPane.WARNING_MESSAGE);
  }
  
  private void _showError(Throwable e, String title, String message) {
    JOptionPane.showMessageDialog(this, message + "\n" + e, title, JOptionPane.ERROR_MESSAGE);
  }
  
  private void _showWarning(Throwable e, String title, String message) {
    JOptionPane.showMessageDialog(this, message + "\n" + e, title, JOptionPane.WARNING_MESSAGE);
  }
  
  
  private void _showConfigException() {
    if (DrJava.getConfig().hadStartupException()) {
      try {
        DrJava.getConfig().saveConfiguration();
      }
      catch(IOException ioe) {  }
      Exception e = DrJava.getConfig().getStartupException();
      _showError(e, "Error in Config File",
                 "Could not read the '.drjava' configuration file\n" +
                 "in your home directory.  Starting with default\n" +
                 "values instead.\n\n" + "The problem was:\n");
    }
  }
  
  
  private File getChosenFile(JFileChooser fc, int choice) throws OperationCanceledException {
    switch (choice) {
      case JFileChooser.CANCEL_OPTION:
      case JFileChooser.ERROR_OPTION:
        throw new OperationCanceledException();
      case JFileChooser.APPROVE_OPTION:
        File chosen = fc.getSelectedFile();
        if (chosen != null) {
          
          if (fc.getFileFilter() instanceof JavaSourceFilter) {
            if (chosen.getName().indexOf(".") == -1)
              return new File(chosen.getAbsolutePath() + "." + 
                              DrJavaRoot.LANGUAGE_LEVEL_EXTENSIONS[DrJava.getConfig().getSetting(LANGUAGE_LEVEL)]);
          }
          return chosen;
        }
        else
          throw new RuntimeException("Filechooser returned null file");
      default:                  
        throw  new RuntimeException("Filechooser returned bad rc " + choice);
    }
  }
  
  private File[] getChosenFiles(JFileChooser fc, int choice) throws OperationCanceledException {
    switch (choice) {
      case JFileChooser.CANCEL_OPTION:
      case JFileChooser.ERROR_OPTION:
        throw new OperationCanceledException();
      case JFileChooser.APPROVE_OPTION:
        File[] chosen = fc.getSelectedFiles();
        if (chosen == null)
          throw new UnexpectedException(new OperationCanceledException(), "filechooser returned null file");
        
        
        
        
        if (chosen.length == 0) {
          if (!fc.isMultiSelectionEnabled()) {
            return new File[] { fc.getSelectedFile() };
          }
          else {
            
            throw new OperationCanceledException();
          }
        }
        else {
          return chosen;
        }
        
      default:                  
        throw new UnexpectedException(new OperationCanceledException(), "filechooser returned bad rc " + choice);
    }
  }
  
  private void _selectAll() { _currentDefPane.selectAll(); }
  
  
  public int _jumpToLine(int lineNum) {   
    int pos = _model.getActiveDocument().gotoLine(lineNum);
    addToBrowserHistory();
    _currentDefPane.setCaretPosition(pos);
    _currentDefPane.centerViewOnOffset(pos);
    return pos;
  }
  
  
  private int _gotoLine() {
    final String msg = "What line would you like to go to?";
    final String title = "Go to Line";
    String lineStr = JOptionPane.showInputDialog(this, msg, title, JOptionPane.QUESTION_MESSAGE);
    try {
      if (lineStr != null) {
        int lineNum = Integer.parseInt(lineStr);
        return _jumpToLine(lineNum);      }
    }
    catch (NumberFormatException nfe) {
      
      Toolkit.getDefaultToolkit().beep();
      
    }
    
    return -1;
  }
  
  
  private void _removeErrorListener(OpenDefinitionsDocument doc) {
    JScrollPane scroll = _defScrollPanes.get(doc);
    if (scroll != null) {
      DefinitionsPane pane = (DefinitionsPane) scroll.getViewport().getView();
      pane.removeCaretListener(pane.getErrorCaretListener());
    }
  }
  
  
  private void _setUpActions() {
    _setUpAction(_newAction, "New", "Create a new document");
    _setUpAction(_newJUnitTestAction, "New", "Create a new JUnit test case class");
    _setUpAction(_newProjectAction, "New", "Make a new project");
    _setUpAction(_openAction, "Open", "Open an existing file");
    _setUpAction(_openFolderAction, "Open Folder", "OpenAll", "Open all files within a directory");
    _setUpAction(_openFileOrProjectAction, "Open", "Open an existing file or project");
    _setUpAction(_openProjectAction, "Open", "Open an existing project");
    _setUpAction(_saveAction, "Save", "Save the current document");
    _setUpAction(_saveAsAction, "Save As", "SaveAs", "Save the current document with a new name");
    _setUpAction(_renameAction, "Rename", "Rename", "Rename the current document");
    _setUpAction(_saveProjectAction, "Save", "Save", "Save the current project");
    _saveProjectAction.setEnabled(false);
    _setUpAction(_saveProjectAsAction, "Save As", "SaveAs", "Save current project to new project file");
    _saveProjectAsAction.setEnabled(false);
    _setUpAction(_exportProjectInOldFormatAction, "Export Project In Old \"" + OLD_PROJECT_FILE_EXTENSION +
                 "\" Format", "SaveAs", "Export Project In Old \"" + OLD_PROJECT_FILE_EXTENSION + "\" Format");
    _exportProjectInOldFormatAction.setEnabled(false);
    _setUpAction(_revertAction, "Revert", "Revert the current document to the saved version");
    


    
    _setUpAction(_closeAction, "Close", "Close the current document");
    _setUpAction(_closeAllAction, "Close All", "CloseAll", "Close all documents");
    _setUpAction(_closeProjectAction, "Close", "CloseAll", "Close the current project");
    _closeProjectAction.setEnabled(false);
    
    _setUpAction(_projectPropertiesAction, "Project Properties", "Preferences", "Edit Project Properties");
    _projectPropertiesAction.setEnabled(false);    
    


    _setUpAction(_junitProjectAction, "Test Project", "Test the documents in the project source tree");
    _junitProjectAction.setEnabled(false);
    

    _setUpAction(_compileProjectAction, "Compile Project", "Compile the documents in the project source tree");

    _compileProjectAction.setEnabled(false);
    
    _setUpAction(_runProjectAction, "Run Project", "Run the project's main method");
    _runProjectAction.setEnabled(false);
    
    _setUpAction(_jarProjectAction, "Jar", "Create a jar archive from this project");
    _jarProjectAction.setEnabled(false);
    
    _setUpAction(_saveAllAction, "Save All", "SaveAll", "Save all open documents");
    
    _setUpAction(_cleanAction, "Clean", "Clean Build directory");
    _cleanAction.setEnabled(false);
    _setUpAction(_autoRefreshAction, "Auto-Refresh", "Auto-refresh project");
    _autoRefreshAction.setEnabled(false);
    _setUpAction(_compileAction, "Compile Current Document", "Compile the current document");
    _setUpAction(_compileAllAction, "Compile", "Compile all open documents");
    _setUpAction(_printDefDocAction, "Print", "Print the current document");
    _setUpAction(_printConsoleAction, "Print", "Print the Console pane");
    _setUpAction(_printInteractionsAction, "Print", "Print the Interactions pane");
    _setUpAction(_pageSetupAction, "Page Setup", "PageSetup", "Change the printer settings");
    _setUpAction(_printDefDocPreviewAction, "Print Preview", "PrintPreview", 
                 "Preview how the document will be printed");
    _setUpAction(_printConsolePreviewAction, "Print Preview", "PrintPreview", 
                 "Preview how the console document will be printed");
    _setUpAction(_printInteractionsPreviewAction, "Print Preview", "PrintPreview", 
                 "Preview how the interactions document will be printed");    
    
    _setUpAction(_quitAction, "Quit", "Quit", "Quit DrJava");
    
    _setUpAction(_undoAction, "Undo", "Undo previous command");
    _setUpAction(_redoAction, "Redo", "Redo last undo");
    _undoAction.putValue(Action.NAME, "Undo Previous Command");
    _redoAction.putValue(Action.NAME, "Redo Last Undo");
    
    _setUpAction(cutAction, "Cut", "Cut selected text to the clipboard");
    _setUpAction(copyAction, "Copy", "Copy selected text to the clipboard");
    _setUpAction(pasteAction, "Paste", "Paste text from the clipboard");
    _setUpAction(_pasteHistoryAction, "Paste from History", "Paste text from the clipboard history");
    _setUpAction(_selectAllAction, "Select All", "Select all text");
    
    cutAction.putValue(Action.NAME, "Cut");
    copyAction.putValue(Action.NAME, "Copy");
    pasteAction.putValue(Action.NAME, "Paste");
    _pasteHistoryAction.putValue(Action.NAME, "Paste from History");
    
    _setUpAction(_indentLinesAction, "Indent Lines", "Indent all selected lines");
    _setUpAction(_commentLinesAction, "Comment Lines", "Comment out all selected lines");
    _setUpAction(_uncommentLinesAction, "Uncomment Lines", "Uncomment all selected lines");
    
    _setUpAction(completeWordUnderCursorAction, "Auto-Complete Word Under Cursor",
                 "Auto-complete the word the cursor is currently located on");
    _setUpAction(_bookmarksPanelAction, "Bookmarks", "Display the bookmarks panel");
    _setUpAction(_toggleBookmarkAction, "Toggle Bookmark", "Toggle the bookmark at the current cursor location");
    _setUpAction(_followFileAction, "Follow File", "Follow a file's updates");
    _setUpAction(_executeExternalProcessAction, "Execute External", "Execute external process");
    _setUpAction(_editExternalProcessesAction, "Preferences", "Edit saved external processes");
    
    _setUpAction(_findReplaceAction, "Find", "Find or replace text in the document");
    _setUpAction(_findNextAction, "Find Next", "Repeats the last find");
    _setUpAction(_findPrevAction, "Find Previous", "Repeats the last find in the opposite direction");
    _setUpAction(_gotoLineAction, "Go to line", "Go to a line number in the document");
    _setUpAction(_gotoFileAction, "Go to File", "Go to a file specified by its name");
    _setUpAction(gotoFileUnderCursorAction, "Go to File Under Cursor",
                 "Go to the file specified by the word the cursor is located on");
    
    _setUpAction(_switchToPrevAction, "Previous Document", "Up", "Switch to the previous document");
    _setUpAction(_switchToNextAction, "Next Document", "Down", "Switch to the next document");
    
    _setUpAction(_browseBackAction, "Back", "Back", "Move back in the browser history");
    _setUpAction(_browseForwardAction, "Forward", "Forward", "Move forward in the browser history");    
    
    _setUpAction(_switchToPreviousPaneAction, "Previous Pane", "Switch focus to the previous pane");
    _setUpAction(_switchToNextPaneAction, "Next Pane", "Switch focus to the next pane");
    _setUpAction(_gotoOpeningBraceAction, "Go to Opening Brace", 
                 "Go th the opening brace of the block enclosing the cursor");
    _setUpAction(_gotoClosingBraceAction, "Go to Closing Brace", 
                 "Go th the closing brace of the block enclosing the cursor");
    
    _setUpAction(_editPreferencesAction, "Preferences", "Edit configurable settings in DrJava");
    
    _setUpAction(_junitAction, "Test Current", "Run JUnit over the current document");
    _setUpAction(_junitAllAction, "Test", "Run JUnit over all open JUnit tests");
    _setUpAction(_javadocAllAction, "Javadoc", "Create and save Javadoc for the packages of all open documents");
    _setUpAction(_javadocCurrentAction, "Preview Javadoc Current", "Preview the Javadoc for the current document");
    _setUpAction(_runAction, "Run", "Run the main method of the current document");
    _setUpAction(_runAppletAction, "Run", "Run the current document as applet");
    
    _setUpAction(_openJavadocAction, "Open Java API Javadoc...", "Open the Java API Javadoc Web page for a class");
    _setUpAction(_openJavadocUnderCursorAction, "Open Java API Javadoc for Word Under Cursor...", "Open the Java API " +
                 "Javadoc Web page for the word under the cursor");
    
    _setUpAction(_executeHistoryAction, "Execute History", "Load and execute a history of interactions from a file");
    _setUpAction(_loadHistoryScriptAction, "Load History as Script", 
                 "Load a history from a file as a series of interactions");
    _setUpAction(_saveHistoryAction, "Save History", "Save the history of interactions to a file");
    _setUpAction(_clearHistoryAction, "Clear History", "Clear the current history of interactions");
    
    
    _setUpAction(_resetInteractionsAction, "Reset", "Reset the Interactions Pane");
    _resetInteractionsAction.setEnabled(true);
    _setUpAction(_closeSystemInAction, "Close System.in", "Close System.in Stream in Interactions Pane"); 

    _setUpAction(_viewInteractionsClassPathAction, "View Interactions Classpath", 
                 "Display the classpath in use by the Interactions Pane");
    _setUpAction(_copyInteractionToDefinitionsAction, "Lift Current Interaction", 
                 "Copy the current interaction into the Definitions Pane");
    
    _setUpAction(_clearConsoleAction, "Clear Console", "Clear all text in the Console Pane");
    _setUpAction(_showDebugConsoleAction, "Show DrJava Debug Console", "<html>Show a console for debugging DrJava<br>" +
                 "(with \"mainFrame\", \"model\", and \"config\" variables defined)</html>");
    
    if (_model.getDebugger().isAvailable()) {
      _setUpAction(_toggleDebuggerAction, "Debug Mode", "Enable or disable DrJava's debugger");
      _setUpAction(_toggleBreakpointAction, "Toggle Breakpoint", "Set or clear a breakpoint on the current line");
      _setUpAction(_clearAllBreakpointsAction, "Clear Breakpoints", "Clear all breakpoints in all classes");
      _setUpAction(_resumeDebugAction, "Resume", "Resume the current suspended thread");
      _setUpAction(_automaticTraceDebugAction, "Automatic Trace", "Automatically trace through entire program");
      _setUpAction(_stepIntoDebugAction, "Step Into", "Step into the current line or method call");
      _setUpAction(_stepOverDebugAction, "Step Over", "Step over the current line or method call");
      _setUpAction(_stepOutDebugAction, "Step Out", "Step out of the current method");
      _setUpAction(_breakpointsPanelAction, "Breakpoints", "Display the breakpoints panel");
    }
    
    _setUpAction(_helpAction, "Help", "Show documentation on how to use DrJava");
    _setUpAction(_quickStartAction, "Help", "View Quick Start Guide for DrJava");
    _setUpAction(_aboutAction, "About", "About DrJava");
    _setUpAction(_checkNewVersionAction, "Check for New Version", "Find", "Check for New Version");
    _setUpAction(_drjavaSurveyAction, "Send System Information", "About", 
                 "Send anonymous system information to DrJava developers");
    _setUpAction(_errorsAction, "DrJava Errors", "drjavaerror", "Show a window with internal DrJava errors");
    _setUpAction(_forceQuitAction, "Force Quit", "Stop", "Force DrJava to quit without cleaning up");
  }
  
  private void _setUpAction(Action a, String name, String icon, String shortDesc) {
    a.putValue(Action.SMALL_ICON, _getIcon(icon + "16.gif"));
    a.putValue(Action.DEFAULT, name);
    a.putValue(Action.SHORT_DESCRIPTION, shortDesc);
  }
  
  private void _setUpAction(Action a, String icon, String shortDesc) { _setUpAction(a, icon, icon, shortDesc); }
  
  
  
  private ImageIcon _getIcon(String name) { return getIcon(name); }
  
  public static ImageIcon getIcon(String name) {
    URL url = MainFrame.class.getResource(ICON_PATH + name);
    if (url != null)  return new ImageIcon(url);
    
    return null;
  }
  
  
  
  private class MenuBar extends JMenuBar {
    public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
      if (MainFrame.this.getAllowKeyEvents()) return super.processKeyBinding(ks, e, condition, pressed);
      return false;
    }
  }
  
  
  private void _setUpMenuBar() {
    _menuBar.add(_fileMenu);
    _menuBar.add(_editMenu);
    _menuBar.add(_toolsMenu);
    _menuBar.add(_projectMenu);
    if (_showDebugger) _menuBar.add(_debugMenu);
    _menuBar.add(_languageLevelMenu);
    _menuBar.add(_helpMenu);
    
    if(Utilities.isPlasticLaf()) {
      _menuBar.putClientProperty(com.jgoodies.looks.Options.HEADER_STYLE_KEY, com.jgoodies.looks.HeaderStyle.BOTH);
    }
    setJMenuBar(_menuBar);
  }

  
  private void _addMenuItem(JMenu menu, Action a, VectorOption<KeyStroke> opt) {
    JMenuItem item;
    item = menu.add(a);
    _setMenuShortcut(item, a, opt);
  }
  
  
  private void _setMenuShortcut(JMenuItem item, Action a, VectorOption<KeyStroke> opt) {
    Vector<KeyStroke> keys = DrJava.getConfig().getSetting(opt);
    
    
    
    
    
    KeyBindingManager.ONLY.put(opt, a, item, item.getText());
    if ((keys.size()>0) && KeyBindingManager.ONLY.get(keys.get(0)) == a) {
      item.setAccelerator(keys.get(0));
    }
  }
  
  
  private JMenu _setUpFileMenu(int mask) {
    JMenu fileMenu = new JMenu("File");
    PlatformFactory.ONLY.setMnemonic(fileMenu,KeyEvent.VK_F);
    
    _addMenuItem(fileMenu, _newAction, KEY_NEW_FILE);
    _addMenuItem(fileMenu, _newJUnitTestAction, KEY_NEW_TEST);
    _addMenuItem(fileMenu, _openAction, KEY_OPEN_FILE);
    _addMenuItem(fileMenu, _openFolderAction, KEY_OPEN_FOLDER);
    
    
    fileMenu.addSeparator();
    
    _addMenuItem(fileMenu, _saveAction, KEY_SAVE_FILE);
    _saveAction.setEnabled(true);
    _addMenuItem(fileMenu, _saveAsAction, KEY_SAVE_FILE_AS);
    _addMenuItem(fileMenu, _saveAllAction, KEY_SAVE_ALL_FILES);
    _addMenuItem(fileMenu, _renameAction, KEY_RENAME_FILE);
    _renameAction.setEnabled(false);

    
    _addMenuItem(fileMenu, _revertAction, KEY_REVERT_FILE);
    _revertAction.setEnabled(false);
    
    
    
    fileMenu.addSeparator();
    _addMenuItem(fileMenu, _closeAction, KEY_CLOSE_FILE);
    _addMenuItem(fileMenu, _closeAllAction, KEY_CLOSE_ALL_FILES);
    
    
    
    fileMenu.addSeparator();
    _addMenuItem(fileMenu, _pageSetupAction, KEY_PAGE_SETUP);
    _addMenuItem(fileMenu, _printDefDocPreviewAction, KEY_PRINT_PREVIEW);
    _addMenuItem(fileMenu, _printDefDocAction, KEY_PRINT);
    
    
    fileMenu.addSeparator();
    _addMenuItem(fileMenu, _quitAction, KEY_QUIT);
    
    return fileMenu;
  }
  
  
  private JMenu _setUpEditMenu(int mask) {
    JMenu editMenu = new JMenu("Edit");
    PlatformFactory.ONLY.setMnemonic(editMenu,KeyEvent.VK_E);
    
    _addMenuItem(editMenu, _undoAction, KEY_UNDO);
    _addMenuItem(editMenu, _redoAction, KEY_REDO);
    
    
    editMenu.addSeparator();
    _addMenuItem(editMenu, cutAction, KEY_CUT);
    _addMenuItem(editMenu, copyAction, KEY_COPY);
    _addMenuItem(editMenu, pasteAction, KEY_PASTE);
    _addMenuItem(editMenu, _pasteHistoryAction, KEY_PASTE_FROM_HISTORY);
    _addMenuItem(editMenu, _selectAllAction, KEY_SELECT_ALL);
    
    
    editMenu.addSeparator();
    
    JMenuItem editItem = editMenu.add(_indentLinesAction);
    editItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
    _addMenuItem(editMenu, _commentLinesAction, KEY_COMMENT_LINES);
    _addMenuItem(editMenu, _uncommentLinesAction, KEY_UNCOMMENT_LINES);
    _addMenuItem(editMenu, completeWordUnderCursorAction, KEY_COMPLETE_FILE);
    
    
    editMenu.addSeparator();
    _addMenuItem(editMenu, _findReplaceAction, KEY_FIND_REPLACE);
    _addMenuItem(editMenu, _findNextAction, KEY_FIND_NEXT);
    _addMenuItem(editMenu, _findPrevAction, KEY_FIND_PREV);
    
    
    editMenu.addSeparator();
    _addMenuItem(editMenu, _switchToPrevAction, KEY_PREVIOUS_DOCUMENT);
    _addMenuItem(editMenu, _switchToNextAction, KEY_NEXT_DOCUMENT);
    _addMenuItem(editMenu, _browseBackAction, KEY_BROWSE_BACK);
    _addMenuItem(editMenu, _browseForwardAction, KEY_BROWSE_FORWARD);
    editMenu.addSeparator();
    
    
    final JMenu goToMenu = new JMenu("Go To");
    _addMenuItem(goToMenu, _gotoLineAction, KEY_GOTO_LINE);
    _addMenuItem(goToMenu, _gotoFileAction, KEY_GOTO_FILE);
    _addMenuItem(goToMenu, gotoFileUnderCursorAction, KEY_GOTO_FILE_UNDER_CURSOR);
    _addMenuItem(goToMenu, _gotoOpeningBraceAction, KEY_OPENING_BRACE);
    _addMenuItem(goToMenu, _gotoClosingBraceAction, KEY_CLOSING_BRACE);
    editMenu.add(goToMenu);

    
    final JMenu panesMenu = new JMenu("Tabbed Panes");
    _addMenuItem(panesMenu, _switchToPreviousPaneAction, KEY_PREVIOUS_PANE);
    _addMenuItem(panesMenu, _switchToNextPaneAction, KEY_NEXT_PANE);
    _detachTabbedPanesMenuItem = _newCheckBoxMenuItem(_detachTabbedPanesAction);
    _detachTabbedPanesMenuItem.setSelected(DrJava.getConfig().getSetting(DETACH_TABBEDPANES));
    _setMenuShortcut(_detachTabbedPanesMenuItem, _detachTabbedPanesAction, KEY_DETACH_TABBEDPANES);
    panesMenu.add(_detachTabbedPanesMenuItem);
    editMenu.add(panesMenu);
    
    
    editMenu.addSeparator();
    _addMenuItem(editMenu, _editPreferencesAction, KEY_PREFERENCES);
    
    
    return editMenu;
  }
  
  
  private JMenu _setUpToolsMenu(int mask) {
    JMenu toolsMenu = new JMenu("Tools");
    PlatformFactory.ONLY.setMnemonic(toolsMenu,KeyEvent.VK_T);
    
    
    _addMenuItem(toolsMenu, _compileAllAction, KEY_COMPILE_ALL);
    _addMenuItem(toolsMenu, _compileAction, KEY_COMPILE);
    _addMenuItem(toolsMenu, _junitAllAction, KEY_TEST_ALL);
    _addMenuItem(toolsMenu, _junitAction, KEY_TEST);
    toolsMenu.addSeparator();
    
    
    _addMenuItem(toolsMenu, _runAction, KEY_RUN);
    _addMenuItem(toolsMenu, _runAppletAction, KEY_RUN_APPLET);
    _addMenuItem(toolsMenu, _resetInteractionsAction, KEY_RESET_INTERACTIONS);
    toolsMenu.addSeparator();
    
    
    final JMenu javadocMenu = new JMenu("Javadoc");
    _addMenuItem(javadocMenu, _javadocAllAction, KEY_JAVADOC_ALL);
    _addMenuItem(javadocMenu, _javadocCurrentAction, KEY_JAVADOC_CURRENT);
    javadocMenu.addSeparator();
    _addMenuItem(javadocMenu, _openJavadocAction, KEY_OPEN_JAVADOC);
    _addMenuItem(javadocMenu, _openJavadocUnderCursorAction, KEY_OPEN_JAVADOC_UNDER_CURSOR);    
    toolsMenu.add(javadocMenu);
    
    final JMenu historyMenu = new JMenu("History");
    _addMenuItem(historyMenu, _executeHistoryAction, KEY_EXECUTE_HISTORY);
    _addMenuItem(historyMenu, _loadHistoryScriptAction, KEY_LOAD_HISTORY_SCRIPT);
    _addMenuItem(historyMenu, _saveHistoryAction, KEY_SAVE_HISTORY);
    _addMenuItem(historyMenu, _clearHistoryAction, KEY_CLEAR_HISTORY);
    toolsMenu.add(historyMenu);
    
    
    
    final JMenu interMenu = new JMenu("Interactions & Console");    
    _addMenuItem(interMenu, _viewInteractionsClassPathAction, KEY_VIEW_INTERACTIONS_CLASSPATH);
    _addMenuItem(interMenu, _copyInteractionToDefinitionsAction, KEY_LIFT_CURRENT_INTERACTION);
    _addMenuItem(interMenu, _printInteractionsAction, KEY_PRINT_INTERACTIONS);
    interMenu.addSeparator();
    _addMenuItem(interMenu, _clearConsoleAction, KEY_CLEAR_CONSOLE);
    _addMenuItem(interMenu, _printConsoleAction, KEY_PRINT_CONSOLE);
    _addMenuItem(interMenu, _closeSystemInAction, KEY_CLOSE_SYSTEM_IN);
    if (DrJava.getConfig().getSetting(SHOW_DEBUG_CONSOLE).booleanValue()) {
      toolsMenu.add(_showDebugConsoleAction);
    }
    toolsMenu.add(interMenu);
    
    final JMenu extMenu = new JMenu("External Processes");
    _addMenuItem(extMenu, _executeExternalProcessAction, KEY_EXEC_PROCESS);
    final JMenuItem execItem = extMenu.getItem(0);
    extMenu.addSeparator();
    extMenu.add(_editExternalProcessesAction);
    toolsMenu.add(extMenu);
    
    final int savedCount = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_COUNT);
    final int namesCount = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_NAMES).size();
    final int cmdlinesCount = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_CMDLINES).size();
    final int workdirsCount = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_WORKDIRS).size();
    final int enclosingFileCount = 
      DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_ENCLOSING_DJAPP_FILES).size();
    if ((savedCount!=namesCount) ||
        (savedCount!=cmdlinesCount) ||
        (savedCount!=workdirsCount) ||
        (savedCount!=enclosingFileCount)) {
      DrJava.getConfig().setSetting(OptionConstants.EXTERNAL_SAVED_COUNT, 0);
      DrJava.getConfig().setSetting(OptionConstants.EXTERNAL_SAVED_NAMES, new Vector<String>());
      DrJava.getConfig().setSetting(OptionConstants.EXTERNAL_SAVED_CMDLINES, new Vector<String>());
      DrJava.getConfig().setSetting(OptionConstants.EXTERNAL_SAVED_WORKDIRS, new Vector<String>());
      DrJava.getConfig().setSetting(OptionConstants.EXTERNAL_SAVED_ENCLOSING_DJAPP_FILES, new Vector<String>());
    }
    
    OptionListener<Integer> externalSavedCountListener =
      new OptionListener<Integer>() {
      public void optionChanged(final OptionEvent<Integer> oce) {
        extMenu.removeAll();
        extMenu.add(execItem);
        extMenu.addSeparator();
        for (int count=0; count<oce.value; ++count) {
          final int i = count;
          final Vector<String> names = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_NAMES);
          final Vector<String> cmdlines = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_CMDLINES);
          final Vector<String> workdirs = DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_WORKDIRS);
          final Vector<String> enclosingfiles = 
            DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_ENCLOSING_DJAPP_FILES);
          
          extMenu.insert(new AbstractAction(names.get(i)) {
            public void actionPerformed(ActionEvent ae) {
              try {
                PropertyMaps pm = PropertyMaps.TEMPLATE.clone();
                String s = enclosingfiles.get(i).trim();
                ((MutableFileProperty) pm.getProperty("enclosing.djapp.file")).
                  setFile(s.length() > 0 ? new File(s) : null);
                _executeExternalDialog.
                  runCommand(names.get(i),cmdlines.get(i),workdirs.get(i),enclosingfiles.get(i),pm);
              }
              catch(CloneNotSupportedException e) { throw new edu.rice.cs.util.UnexpectedException(e); }
            }
          },i+2);
        }
        if (oce.value>0) { extMenu.addSeparator(); }
        extMenu.add(_editExternalProcessesAction);
        _editExternalProcessesAction.setEnabled(true); 
      }
    };
    DrJava.getConfig().addOptionListener(OptionConstants.EXTERNAL_SAVED_COUNT, externalSavedCountListener);
    externalSavedCountListener.
      optionChanged(new OptionEvent<Integer>(OptionConstants.EXTERNAL_SAVED_COUNT,
                                             DrJava.getConfig().getSetting(OptionConstants.EXTERNAL_SAVED_COUNT)));
    toolsMenu.addSeparator();
    
    _addMenuItem(toolsMenu, _bookmarksPanelAction, KEY_BOOKMARKS_PANEL);
    _addMenuItem(toolsMenu, _toggleBookmarkAction, KEY_BOOKMARKS_TOGGLE);
    
    toolsMenu.addSeparator();
    _addMenuItem(toolsMenu, _followFileAction, KEY_FOLLOW_FILE);
    
    
    return toolsMenu;
  }
  
  
  private JMenu _setUpProjectMenu(int mask) {
    JMenu projectMenu = new JMenu("Project");
    PlatformFactory.ONLY.setMnemonic(projectMenu,KeyEvent.VK_P);
    
    projectMenu.add(_newProjectAction);
    _addMenuItem(projectMenu, _openProjectAction, KEY_OPEN_PROJECT);
    
    
    projectMenu.add(_saveProjectAction);
    
    projectMenu.add(_saveProjectAsAction);
    
    
    _addMenuItem(projectMenu, _closeProjectAction, KEY_CLOSE_PROJECT);
    
    projectMenu.addSeparator();
    

    projectMenu.add(_compileProjectAction);
    projectMenu.add(_junitProjectAction);
    projectMenu.add(_runProjectAction);

    projectMenu.add(_cleanAction);
    projectMenu.add(_autoRefreshAction);
    projectMenu.add(_jarProjectAction);
    
    projectMenu.addSeparator();
    
    projectMenu.add(_projectPropertiesAction);
    
    return projectMenu;
  }
  
  
  private JMenu _setUpDebugMenu(int mask) {
    JMenu debugMenu = new JMenu("Debugger");
    PlatformFactory.ONLY.setMnemonic(debugMenu,KeyEvent.VK_D);
    
    _debuggerEnabledMenuItem = _newCheckBoxMenuItem(_toggleDebuggerAction);
    _debuggerEnabledMenuItem.setSelected(false);
    _setMenuShortcut(_debuggerEnabledMenuItem, _toggleDebuggerAction, KEY_DEBUG_MODE_TOGGLE);
    debugMenu.add(_debuggerEnabledMenuItem);
    debugMenu.addSeparator();
    
    _addMenuItem(debugMenu, _toggleBreakpointAction, KEY_DEBUG_BREAKPOINT_TOGGLE);
    
    
    _addMenuItem(debugMenu, _clearAllBreakpointsAction, KEY_DEBUG_CLEAR_ALL_BREAKPOINTS);
    _addMenuItem(debugMenu, _breakpointsPanelAction, KEY_DEBUG_BREAKPOINT_PANEL);
    debugMenu.addSeparator();
    
    
    _addMenuItem(debugMenu, _resumeDebugAction, KEY_DEBUG_RESUME);
    _addMenuItem(debugMenu, _stepIntoDebugAction, KEY_DEBUG_STEP_INTO);
    _addMenuItem(debugMenu, _stepOverDebugAction, KEY_DEBUG_STEP_OVER);
    _addMenuItem(debugMenu, _stepOutDebugAction, KEY_DEBUG_STEP_OUT);
    _automaticTraceMenuItem = _newCheckBoxMenuItem(_automaticTraceDebugAction);
    _setMenuShortcut(_automaticTraceMenuItem, _automaticTraceDebugAction, KEY_DEBUG_AUTOMATIC_TRACE);
    debugMenu.add(_automaticTraceMenuItem);
    
    debugMenu.addSeparator();
    _detachDebugFrameMenuItem = _newCheckBoxMenuItem(_detachDebugFrameAction);
    _detachDebugFrameMenuItem.setSelected(DrJava.getConfig().getSetting(DETACH_DEBUGGER));
    _setMenuShortcut(_detachDebugFrameMenuItem, _detachDebugFrameAction, KEY_DETACH_DEBUGGER);
    debugMenu.add(_detachDebugFrameMenuItem);
    
    
    _setDebugMenuItemsEnabled(false);
    
    
    return debugMenu;
  }
  
  
  private void _setDebugMenuItemsEnabled(boolean isEnabled) {
    
    _debuggerEnabledMenuItem.setSelected(isEnabled);
    
    _resumeDebugAction.setEnabled(false);
    _automaticTraceDebugAction.setEnabled(false);
    _stepIntoDebugAction.setEnabled(false);
    _stepOverDebugAction.setEnabled(false);
    _stepOutDebugAction.setEnabled(false);
    _detachDebugFrameAction.setEnabled(isEnabled);
    
    if (_showDebugger) _debugPanel.disableButtons();
  }
  
  
  private void _setThreadDependentDebugMenuItems(boolean isSuspended) {
    
    _resumeDebugAction.setEnabled(isSuspended);
    _automaticTraceDebugAction.setEnabled(isSuspended);
    _stepIntoDebugAction.setEnabled(isSuspended);
    _stepOverDebugAction.setEnabled(isSuspended);
    _stepOutDebugAction.setEnabled(isSuspended);
    _debugPanel.setThreadDependentButtons(isSuspended);
  }
  
  
  private JMenu _setUpLanguageLevelMenu(int mask) {
    JMenu languageLevelMenu = new JMenu("Language Level");
    PlatformFactory.ONLY.setMnemonic(languageLevelMenu,KeyEvent.VK_L);
    ButtonGroup group = new ButtonGroup();
    
    final Configuration config = DrJava.getConfig();
    int currentLanguageLevel = config.getSetting(LANGUAGE_LEVEL);
    JRadioButtonMenuItem rbMenuItem;
    rbMenuItem = new JRadioButtonMenuItem("Full Java");
    rbMenuItem.setToolTipText("Use full Java syntax");
    if (currentLanguageLevel == DrJavaRoot.FULL_JAVA) { rbMenuItem.setSelected(true); }
    rbMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        config.setSetting(LANGUAGE_LEVEL, DrJavaRoot.FULL_JAVA);
      }});
    group.add(rbMenuItem);
    languageLevelMenu.add(rbMenuItem);
    languageLevelMenu.addSeparator();
    
    rbMenuItem = new JRadioButtonMenuItem("Elementary");
    rbMenuItem.setToolTipText("Use Elementary language-level features");
    if (currentLanguageLevel == DrJavaRoot.ELEMENTARY_LEVEL) { rbMenuItem.setSelected(true); }
    rbMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        config.setSetting(LANGUAGE_LEVEL, DrJavaRoot.ELEMENTARY_LEVEL);
      }});
    group.add(rbMenuItem);
    languageLevelMenu.add(rbMenuItem);
    
    rbMenuItem = new JRadioButtonMenuItem("Intermediate");
    rbMenuItem.setToolTipText("Use Intermediate language-level features");
    if (currentLanguageLevel == DrJavaRoot.INTERMEDIATE_LEVEL) { rbMenuItem.setSelected(true); }
    rbMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        config.setSetting(LANGUAGE_LEVEL, DrJavaRoot.INTERMEDIATE_LEVEL);
      }});
    group.add(rbMenuItem);
    languageLevelMenu.add(rbMenuItem);
    
    rbMenuItem = new JRadioButtonMenuItem("Advanced");
    rbMenuItem.setToolTipText("Use Advanced language-level features");
    if (currentLanguageLevel == DrJavaRoot.ADVANCED_LEVEL) { rbMenuItem.setSelected(true); }
    rbMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        config.setSetting(LANGUAGE_LEVEL, DrJavaRoot.ADVANCED_LEVEL);
      }});
    group.add(rbMenuItem);
    languageLevelMenu.add(rbMenuItem);
    return languageLevelMenu;
  }
  
  
  private JMenu _setUpHelpMenu(int mask) {
    JMenu helpMenu = new JMenu("Help");
    PlatformFactory.ONLY.setMnemonic(helpMenu,KeyEvent.VK_H);
    _addMenuItem(helpMenu, _helpAction, KEY_HELP);
    _addMenuItem(helpMenu, _quickStartAction, KEY_QUICKSTART);
    helpMenu.addSeparator();
    _addMenuItem(helpMenu, _aboutAction, KEY_ABOUT);
    _addMenuItem(helpMenu, _drjavaSurveyAction, KEY_DRJAVA_SURVEY);
    _addMenuItem(helpMenu, _checkNewVersionAction, KEY_CHECK_NEW_VERSION);
    _addMenuItem(helpMenu, _errorsAction, KEY_DRJAVA_ERRORS);
    helpMenu.addSeparator();
    _addMenuItem(helpMenu, _forceQuitAction, KEY_FORCE_QUIT);
    _addMenuItem(helpMenu, _exportProjectInOldFormatAction, KEY_EXPORT_OLD);
    return helpMenu;
  }
  
  
  JButton _createManualToolbarButton(Action a) {
    final JButton ret;
    Font buttonFont = DrJava.getConfig().getSetting(FONT_TOOLBAR);
    
    
    boolean useIcon = DrJava.getConfig().getSetting(TOOLBAR_ICONS_ENABLED).booleanValue();
    boolean useText = DrJava.getConfig().getSetting(TOOLBAR_TEXT_ENABLED).booleanValue();
    final Icon icon = (useIcon) ? (Icon) a.getValue(Action.SMALL_ICON) : null;
    if (icon == null) {
      ret = new UnfocusableButton((String) a.getValue(Action.DEFAULT));
    }
    else {
      ret = new UnfocusableButton(icon);
      if (useText) ret.setText((String) a.getValue(Action.DEFAULT));
    }
    ret.setEnabled(false);
    ret.addActionListener(a);
    ret.setToolTipText( (String) a.getValue(Action.SHORT_DESCRIPTION));
    ret.setFont(buttonFont);

    a.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
          Boolean val = (Boolean) evt.getNewValue();
          ret.setEnabled(val.booleanValue());
        }
      }
    });
    
    return ret;
  }
  
  
  public JButton _createToolbarButton(Action a) {
    boolean useText = DrJava.getConfig().getSetting(TOOLBAR_TEXT_ENABLED).booleanValue();
    boolean useIcons = DrJava.getConfig().getSetting(TOOLBAR_ICONS_ENABLED).booleanValue();
    Font buttonFont = DrJava.getConfig().getSetting(FONT_TOOLBAR);
    
    final JButton result = new UnfocusableButton(a);
    result.setText((String) a.getValue(Action.DEFAULT));
    result.setFont(buttonFont);
    if (! useIcons) result.setIcon(null);
    if (! useText && (result.getIcon() != null)) result.setText("");
    return result;
  }
  
  
  public JButton _updateToolbarButton(JButton b, Action a) {
    final JButton result = _createToolbarButton(a);
    
    int index = _toolBar.getComponentIndex(b);
    _toolBar.remove(b);
    _toolBar.add(result, index);
    
    _fixToolbarHeights();
    
    return result;
  }
  
  
  private void _setUpToolBar() {
    
    _toolBar.setFloatable(false);
    

    
    
    _toolBar.add(_createToolbarButton(_newAction));
    _toolBar.add(_createToolbarButton(_openFileOrProjectAction));
    _toolBar.add(_createToolbarButton(_saveAction));
    _closeButton = _createToolbarButton(_closeAction);
    _toolBar.add(_closeButton);
    
    
    _toolBar.addSeparator();
    _toolBar.add(_createToolbarButton(cutAction));
    _toolBar.add(_createToolbarButton(copyAction));
    _toolBar.add(_createToolbarButton(pasteAction));
    
    
    
    
    
    
    
    
    _toolBar.add(_undoButton);
    _toolBar.add(_redoButton);
    
    
    _toolBar.addSeparator();
    _toolBar.add(_createToolbarButton(_findReplaceAction));
    
    
    _toolBar.addSeparator();
    _toolBar.add(_compileButton = _createToolbarButton(_compileAllAction));
    _toolBar.add(_createToolbarButton(_resetInteractionsAction));
    
    
    
    _toolBar.addSeparator();
    
    _toolBar.add(_runButton = _createToolbarButton(_runAction));
    _toolBar.add(_junitButton = _createToolbarButton(_junitAllAction));
    _toolBar.add(_createToolbarButton(_javadocAllAction));
    
    
    _toolBar.addSeparator();
    _errorsButton = _createToolbarButton(_errorsAction);
    _errorsButton.setVisible(false);
    _errorsButton.setBackground(DrJava.getConfig().getSetting(DRJAVA_ERRORS_BUTTON_COLOR));
    _toolBar.add(_errorsButton);
    
    OptionListener<Color> errBtnColorOptionListener = new OptionListener<Color>() {
      public void optionChanged(OptionEvent<Color> oce) {
        _errorsButton.setBackground(oce.value);
      }
    };
    DrJava.getConfig().addOptionListener(DRJAVA_ERRORS_BUTTON_COLOR, errBtnColorOptionListener);
    
    
    _fixToolbarHeights();
    
    
    if(Utilities.isPlasticLaf()) {
      _toolBar.putClientProperty("JToolBar.isRollover", Boolean.FALSE);
      _toolBar.putClientProperty(com.jgoodies.looks.Options.HEADER_STYLE_KEY,
                                 com.jgoodies.looks.HeaderStyle.BOTH);
    }
    
    getContentPane().add(_toolBar, BorderLayout.NORTH);

  }
  
  
  private void _updateToolBarVisible() {
    _toolBar.setVisible(DrJava.getConfig().getSetting(TOOLBAR_ENABLED));
  }  
  
  
  private void _updateToolbarButtons() {
    _updateToolBarVisible();
    Component[] buttons = _toolBar.getComponents();
    
    Font toolbarFont = DrJava.getConfig().getSetting(FONT_TOOLBAR);
    boolean iconsEnabled = DrJava.getConfig().getSetting(TOOLBAR_ICONS_ENABLED).booleanValue();
    boolean textEnabled = DrJava.getConfig().getSetting(TOOLBAR_TEXT_ENABLED).booleanValue();
    
    for (int i = 0; i< buttons.length; i++) {
      
      if (buttons[i] instanceof JButton) {
        
        JButton b = (JButton) buttons[i];
        Action a = b.getAction();
        
        
        
        
        b.setFont(toolbarFont);
        
        if (a == null) {
          if (b == _undoButton) a = _undoAction;
          else if (b == _redoButton) a = _redoAction;
          else continue;
        }
        
        if (b.getIcon() == null) {
          if (iconsEnabled) b.setIcon( (Icon) a.getValue(Action.SMALL_ICON));
        }
        else if (!iconsEnabled && b.getText().equals(""))  b.setIcon(null);
        
        if (b.getText().equals("")) {
          if (textEnabled) b.setText( (String) a.getValue(Action.DEFAULT));
        }
        else if (!textEnabled && b.getIcon() != null) b.setText("");
        
      }
    }
    
    
    _fixToolbarHeights();
  }
  
  
  private void _fixToolbarHeights() {
    Component[] buttons = _toolBar.getComponents();
    
    
    int max = 0;
    for (int i = 0; i< buttons.length; i++) {
      
      if (buttons[i] instanceof JButton) {
        JButton b = (JButton) buttons[i];
        
        
        b.setPreferredSize(null);
        
        
        Dimension d = b.getPreferredSize();
        int cur = (int) d.getHeight();
        if (cur > max) {
          max = cur;
        }
      }
    }
    
    
    for (int i = 0; i< buttons.length; i++) {
      
      if (buttons[i] instanceof JButton) {
        JButton b = (JButton) buttons[i];
        Dimension d = new Dimension((int) b.getPreferredSize().getWidth(), max);
        
        
        
        b.setPreferredSize(d);
        b.setMaximumSize(d);
      }
    }
    
    
  }
  
  
  private void _setUpStatusBar() {
    
    
    
    _statusField.setFont(_statusField.getFont().deriveFont(Font.PLAIN));
    _statusReport.setHorizontalAlignment(SwingConstants.RIGHT);
    
    JPanel fileNameAndMessagePanel = new JPanel(new BorderLayout());
    fileNameAndMessagePanel.add(_statusField, BorderLayout.CENTER);
    fileNameAndMessagePanel.add(_statusReport, BorderLayout.EAST);
    
    _currLocationField.setFont(_currLocationField.getFont().deriveFont(Font.PLAIN));
    _currLocationField.setHorizontalAlignment(SwingConstants.RIGHT);
    _currLocationField.setPreferredSize(new Dimension(165,12));

    
    

    _statusBar.add( fileNameAndMessagePanel, BorderLayout.CENTER );

    _statusBar.add( _currLocationField, BorderLayout.EAST );
    _statusBar.
      setBorder(new CompoundBorder(new EmptyBorder(2,2,2,2),
                                   new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(2,2,2,2))));
    getContentPane().add(_statusBar, BorderLayout.SOUTH);
    















  }
  
  
  private class PositionListener implements CaretListener {
    
    
    private int _offset;
    private int _line;
    private int _col;
    
    
    public void caretUpdate(final CaretEvent ce) {
      
      Utilities.invokeLater(new Runnable() { 
        public void run() {

          int offset = ce.getDot();
          try { 
            if (offset == _offset + 1 && _currentDefDoc.getText(_offset, 1).charAt(0) != '\n') {
              _col += 1;
              _offset += 1;
            }
            else {
              Element root = _currentDefDoc.getDefaultRootElement();
              int line = root.getElementIndex(offset); 
              _line = line + 1;     
              _col = offset - root.getElement(line).getStartOffset();
            }
          }
          catch(BadLocationException e) {  }
          finally { 
            _offset = offset;
            updateLocation(_line, _col);
          }
        }
      });
    }
    
    
    public void updateLocation() {

      _line = _currentDefDoc.getCurrentLine();
      _col = _currentDefDoc.getCurrentCol(); 
      updateLocation(_line, _col);
    }
    
    private void updateLocation(int line, int col) { 
      _currLocationField.setText(line + ":" + col +" \t");  


    }
    
    public int lastLine() { return _line; }
    public int lastCol() { return _col; }
  }
  
  
  private void _setUpTabs() {
    
    
    _interactionsController.setPrevPaneAction(_switchToPreviousPaneAction);
    _interactionsController.setNextPaneAction(_switchToNextPaneAction);
    
    JScrollPane interactionsScroll = 
      new BorderlessScrollPane(_interactionsPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    _interactionsContainer.add(interactionsScroll, BorderLayout.CENTER);
    
    if (_showDebugger) {
      
      _model.getBreakpointManager().addListener(new RegionManagerListener<Breakpoint>() {
        
        public void regionAdded(final Breakpoint bp) {
          DefinitionsPane bpPane = getDefPaneGivenODD(bp.getDocument());
          _documentBreakpointHighlights.
            put(bp, bpPane.getHighlightManager().
                  addHighlight(bp.getStartOffset(), bp.getEndOffset(), 
                               bp.isEnabled() ? DefinitionsPane.BREAKPOINT_PAINTER
                                 : DefinitionsPane.DISABLED_BREAKPOINT_PAINTER));
          _updateDebugStatus();
        }
        
        
        public void regionChanged(Breakpoint bp) { 
          regionRemoved(bp);
          regionAdded(bp);
        }
        
        
        public void regionRemoved(final Breakpoint bp) {      
          HighlightManager.HighlightInfo highlight = _documentBreakpointHighlights.get(bp);
          if (highlight != null) highlight.remove();
          _documentBreakpointHighlights.remove(bp);
        }
      });
    }
    
    
    _model.getBookmarkManager().addListener(new RegionManagerListener<MovingDocumentRegion>() { 
      
      public void regionAdded(MovingDocumentRegion r) {
        DefinitionsPane bpPane = getDefPaneGivenODD(r.getDocument());
        _documentBookmarkHighlights.
          put(r, bpPane.getHighlightManager().
                addHighlight(r.getStartOffset(), r.getEndOffset(), DefinitionsPane.BOOKMARK_PAINTER));
      }
      public void regionChanged(MovingDocumentRegion r) { 
        regionRemoved(r);
        regionAdded(r);
      }
      public void regionRemoved(MovingDocumentRegion r) {
        HighlightManager.HighlightInfo highlight = _documentBookmarkHighlights.get(r);
        if (highlight != null) highlight.remove();
        _documentBookmarkHighlights.remove(r);
      }
    });
    
    _tabbedPane.addChangeListener(new ChangeListener () {
      
      public void stateChanged(ChangeEvent e) {

        clearStatusMessage();
        
        if (_tabbedPane.getSelectedIndex() == INTERACTIONS_TAB) {
          

          _interactionsContainer.setVisible(true);  
          EventQueue.invokeLater(new Runnable() {  public void run() { _interactionsContainer.requestFocusInWindow(); }  });
        }
        else if (_tabbedPane.getSelectedIndex() == CONSOLE_TAB) {
          

          EventQueue.invokeLater(new Runnable() { public void run() { _consoleScroll.requestFocusInWindow(); } });
        }
        
        if (_currentDefPane != null) {
          int pos = _currentDefPane.getCaretPosition();
          _currentDefPane.removeErrorHighlight(); 
          _currentDefPane.getErrorCaretListener().updateHighlight(pos);
        }
      }
    });
    
    _tabbedPane.add("Interactions", _interactionsContainer);
    _tabbedPane.add("Console", _consoleScroll);
    
    _interactionsPane.addKeyListener(_historyListener);
    _interactionsPane.addFocusListener(_focusListenerForRecentDocs);
    
    _consoleScroll.addKeyListener(_historyListener);
    _consoleScroll.addFocusListener(_focusListenerForRecentDocs);
    
    
    _tabs.addLast(_compilerErrorPanel);
    _tabs.addLast(_junitErrorPanel);
    _tabs.addLast(_javadocErrorPanel);
    _tabs.addLast(_findReplace);
    if (_showDebugger) { _tabs.addLast(_breakpointsPanel); }
    _tabs.addLast(_bookmarksPanel);
    
    _interactionsContainer.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { 
        EventQueue.invokeLater(new Runnable() { 
          public void run() {

            _interactionsPane.requestFocusInWindow(); 
          }
        });
      }
    });
    
    _interactionsPane.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = _interactionsContainer; }
    });
    _consolePane.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = _consoleScroll; }
    });
    _compilerErrorPanel.getMainPanel().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = _compilerErrorPanel; }
    });
    _junitErrorPanel.getMainPanel().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = _junitErrorPanel; }
    });
    _javadocErrorPanel.getMainPanel().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = _javadocErrorPanel; }
    });
    _findReplace.getFindField().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) { _lastFocusOwner = _findReplace; }
    });
    if (_showDebugger) {
      _breakpointsPanel.getMainPanel().addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) { _lastFocusOwner = _breakpointsPanel; }
      });
    }
    _bookmarksPanel.getMainPanel().addFocusListener(new FocusAdapter() { 
      public void focusGained(FocusEvent e) { _lastFocusOwner = _bookmarksPanel; }
    });
  }
  
  
  public void start() {
    
    
    EventQueue.invokeLater(new Runnable() { 
      public void run() { 
        setVisible(true);
        _compilerErrorPanel.setVisible(true);
        showTab(_compilerErrorPanel, true); 
        
        _tabbedPane.invalidate();
        _tabbedPane.repaint();

        try {
            
            _model.getInteractionsModel().performDefaultImports();
        }
        catch(Throwable t) {
            DrJavaErrorHandler.record(t);
        }
      }
    });
  }
  
  
  private void _setUpContextMenus() {      
    

    






    









    

















    

















    
    















    
    _model.getDocCollectionWidget().addMouseListener(new RightClickMouseAdapter() {
      protected void _popupAction(MouseEvent e) {
        boolean showContextMenu = true;
        if (!_model.getDocumentNavigator().isSelectedAt(e.getX(), e.getY())) {
          
          showContextMenu = _model.getDocumentNavigator().selectDocumentAt(e.getX(), e.getY());
        }
        if (showContextMenu) {
          boolean rootSelected = _model.getDocumentNavigator().isRootSelected();
          boolean folderSelected = false;
          boolean docSelected = false;
          boolean externalSelected = false;
          boolean auxiliarySelected = false;
          boolean externalBinSelected = false;
          boolean auxiliaryBinSelected = false;
          
          final int docSelectedCount = _model.getDocumentNavigator().getDocumentSelectedCount();          
          final int groupSelectedCount = _model.getDocumentNavigator().getGroupSelectedCount();
          try {
            java.util.Set<String> groupNames = _model.getDocumentNavigator().getNamesOfSelectedTopLevelGroup();
            
            if (docSelectedCount>0) {
              
              rootSelected = false;
              if (groupNames.contains(_model.getSourceBinTitle())) {
                
                docSelected = true;
              }
              if (groupNames.contains(_model.getExternalBinTitle())) {
                
                externalSelected = true;
              }
              if (groupNames.contains(_model.getAuxiliaryBinTitle())) {
                
                auxiliarySelected = true;
              }
            }
            else {
              
              if (groupSelectedCount>0) {
                
                if (!_model.getDocumentNavigator().isTopLevelGroupSelected()) {
                  
                  folderSelected = true;
                }
                else {
                  
                  if (groupNames.contains(_model.getSourceBinTitle())) {
                    
                    folderSelected = true;
                  }
                  if (groupNames.contains(_model.getExternalBinTitle())) {
                    
                    externalBinSelected = true;
                  }
                  if (groupNames.contains(_model.getAuxiliaryBinTitle())) {
                    
                    auxiliaryBinSelected = true;
                  }
                }
              }
            }
          }
          catch(GroupNotSelectedException ex) {
            
            if (_model.isProjectActive()) {
              
              rootSelected = true;
            }
            else {
              
              docSelected = true;
              rootSelected = false;
              folderSelected = false;
              externalSelected = false;
              auxiliarySelected = false;
              externalBinSelected = false;
              auxiliaryBinSelected = false;
            }
          }
          
          if (!rootSelected && !folderSelected && !docSelected && !externalSelected &&
              !auxiliarySelected && !externalBinSelected && !auxiliaryBinSelected) {
            
            return;
          }
          
          final JPopupMenu m = new JPopupMenu();
          if (docSelectedCount==0) { docSelected = externalSelected = auxiliarySelected = false; }
          if (groupSelectedCount==0) { folderSelected = false; }
          
          if (rootSelected) {
            
            m.add(Utilities.createDelegateAction("Save Project", _saveProjectAction));
            m.add(Utilities.createDelegateAction("Close Project", _closeProjectAction));
            m.add(_compileProjectAction);
            m.add(_runProjectAction);
            m.add(_junitProjectAction);
            m.add(_projectPropertiesAction);
          }
          if (folderSelected) {
            
            if (m.getComponentCount()>0) { m.addSeparator(); }
            if (groupSelectedCount==1) {
              
              
              m.add(_newFileFolderAction);
              m.add(_openOneFolderAction);
              
              
              m.add(Utilities.createDelegateAction("Open All Files in Folder", _openAllFolderAction));
              m.add(_closeFolderAction);
              m.add(_compileFolderAction);
              m.add(_junitFolderAction);
            }
            else if (groupSelectedCount>1) {
              if (!externalBinSelected && !auxiliaryBinSelected) {
                
                
                m.add(Utilities.createDelegateAction("Open All Files in All Folders (" + groupSelectedCount + ")",
                                                     _openAllFolderAction));
              }
              m.add(Utilities.
                      createDelegateAction("Close All Folders ("+groupSelectedCount+")", _closeFolderAction));
              m.add(Utilities.
                      createDelegateAction("Compile All Folders ("+groupSelectedCount+")", _compileFolderAction));
              m.add(Utilities.
                      createDelegateAction("Test All Folders ("+groupSelectedCount+")", _junitFolderAction));
              
            }
          }
          if (docSelected || externalSelected || auxiliarySelected) {
            
            if (m.getComponentCount()>0) { m.addSeparator(); }
            if (docSelectedCount==1) {
              m.add(Utilities.createDelegateAction("Save File", _saveAction));
              m.add(Utilities.createDelegateAction("Save File As...", _saveAsAction));
              m.add(Utilities.createDelegateAction("Rename File", _renameAction));
              m.add(Utilities.createDelegateAction("Revert File to Saved", _revertAction));
              m.add(Utilities.createDelegateAction("Close File", _closeAction));
              m.add(Utilities.createDelegateAction("Print File...", _printDefDocAction));
              m.add(Utilities.createDelegateAction("Print File Preview...", _printDefDocPreviewAction));
              m.add(Utilities.createDelegateAction("Compile File", _compileAction));
              m.add(Utilities.createDelegateAction("Test File", _junitAction));
              m.add(Utilities.createDelegateAction("Preview Javadoc for File", _javadocCurrentAction));
              m.add(Utilities.createDelegateAction("Run File's Main Method", _runAction));
              m.add(Utilities.createDelegateAction("Run File as Applet", _runAppletAction));
            }
            else if (docSelectedCount>1) {
              m.add(Utilities.createDelegateAction("Save All Files ("+docSelectedCount+")", _saveAction));
              m.add(Utilities.createDelegateAction("Revert All Files to Saved ("+docSelectedCount+")", _revertAction));
              m.add(Utilities.createDelegateAction("Close All Files  ("+docSelectedCount+")", _closeAction));
              m.add(Utilities.createDelegateAction("Compile All Files ("+docSelectedCount+")", _compileAction));
              m.add(Utilities.createDelegateAction("Test All Files ("+docSelectedCount+")", _junitAction));
            }
          }
          if (externalSelected && !docSelected && !auxiliarySelected) {
            
            if (m.getComponentCount()>0) { m.addSeparator(); }
            if (docSelectedCount==1) {
              m.add(Utilities.createDelegateAction("Include File With Project",
                                                   _moveToAuxiliaryAction));
            }
            else if (docSelectedCount>1) {
              m.add(Utilities.createDelegateAction("Include All Files With Project ("+docSelectedCount+")",
                                                   _moveToAuxiliaryAction));
            }
          }
          if (auxiliarySelected && !docSelected && !externalSelected) {
            
            if (m.getComponentCount()>0) { m.addSeparator(); }
            if (docSelectedCount==1) {
              m.add(Utilities.createDelegateAction("Do Not Include File With Project",
                                                   _removeAuxiliaryAction));
            }
            else if (docSelectedCount>1) {
              m.add(Utilities.createDelegateAction("Do Not Include Any Files With Project ("+docSelectedCount+")",
                                                   _removeAuxiliaryAction));
            }
          }
          if (!folderSelected && (externalBinSelected || auxiliaryBinSelected)) {
            
            if (m.getComponentCount()>0) { m.addSeparator(); }
            m.add(Utilities.createDelegateAction("Close All Files", _closeFolderAction));
            m.add(Utilities.createDelegateAction("Compile All Files", _compileFolderAction));
            m.add(Utilities.createDelegateAction("Test All Files", _junitFolderAction));
          }
          if (externalBinSelected && !auxiliaryBinSelected) {
            
            m.add(Utilities.createDelegateAction("Include All Files With Project",
                                                 _moveAllToAuxiliaryAction));
          }
          if (auxiliaryBinSelected && !externalBinSelected) {
            
            m.add(Utilities.createDelegateAction("Do Not Include Any Files With Project",
                                                 _removeAllAuxiliaryAction));
          }
          
          m.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    });
































    
    
    _interactionsPanePopupMenu = new JPopupMenu();
    _interactionsPanePopupMenu.add(cutAction);
    _interactionsPanePopupMenu.add(copyAction);
    _interactionsPanePopupMenu.add(pasteAction);
    _interactionsPanePopupMenu.addSeparator();
    _interactionsPanePopupMenu.add(_printInteractionsAction);
    _interactionsPanePopupMenu.add(_printInteractionsPreviewAction);
    _interactionsPanePopupMenu.addSeparator();
    _interactionsPanePopupMenu.add(_executeHistoryAction);
    _interactionsPanePopupMenu.add(_loadHistoryScriptAction);
    _interactionsPanePopupMenu.add(_saveHistoryAction);
    _interactionsPanePopupMenu.add(_clearHistoryAction);
    _interactionsPanePopupMenu.addSeparator();
    _interactionsPanePopupMenu.add(_resetInteractionsAction);
    _interactionsPanePopupMenu.add(_viewInteractionsClassPathAction);
    _interactionsPanePopupMenu.add(_copyInteractionToDefinitionsAction);
    _interactionsPane.addMouseListener(new RightClickMouseAdapter() {
      protected void _popupAction(MouseEvent e) {
        _interactionsPane.requestFocusInWindow();
        _interactionsPanePopupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    });
    






    _consolePanePopupMenu = new JPopupMenu();
    _consolePanePopupMenu.add(_clearConsoleAction);
    _consolePanePopupMenu.addSeparator();
    _consolePanePopupMenu.add(_printConsoleAction);
    _consolePanePopupMenu.add(_printConsolePreviewAction);
    _consolePane.addMouseListener(new RightClickMouseAdapter() {
      protected void _popupAction(MouseEvent e) {
        _consolePane.requestFocusInWindow();
        _consolePanePopupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    });
  }
  
  private void nextRecentDoc() {

    if (_recentDocFrame.isVisible()) _recentDocFrame.next();
    else _recentDocFrame.setVisible(true);
  }
  
  private void prevRecentDoc() {

    if (_recentDocFrame.isVisible()) _recentDocFrame.prev();
    else _recentDocFrame.setVisible(true);
  }
  
  private void hideRecentDocFrame() {
    if (_recentDocFrame.isVisible()) {
      _recentDocFrame.setVisible(false);
      OpenDefinitionsDocument doc = _recentDocFrame.getDocument();
      if (doc != null) {
        addToBrowserHistory();
        _model.setActiveDocument(doc);

      }
    }
  }
  
  private volatile Object _updateLock = new Object();
  private volatile boolean _tabUpdatePending = false;
  private volatile boolean _waitAgain = false;
  private volatile Runnable _pendingUpdate = null;
  private volatile OpenDefinitionsDocument _pendingDocument = null;
  private volatile OrderedDocumentRegion _firstRegion = null;
  private volatile OrderedDocumentRegion _lastRegion = null;

  public static long UPDATE_DELAY = 500L;  
  public static int UPDATER_PRIORITY = 2;   
  




















  
  private static boolean isDisplayed(TabbedPanel p) { return p != null && p.isDisplayed(); }
  
  
  JScrollPane _createDefScrollPane(OpenDefinitionsDocument doc) {
    DefinitionsPane pane = new DefinitionsPane(this, doc);
    
    pane.addKeyListener(_historyListener);
    pane.addFocusListener(_focusListenerForRecentDocs);
    
    
    _installNewDocumentListener(doc);
    ErrorCaretListener caretListener = new ErrorCaretListener(doc, pane, this);
    pane.addErrorCaretListener(caretListener);
    
    doc.addDocumentListener(new DocumentUIListener() {
      
      private void updateUI(OpenDefinitionsDocument doc, int offset) {

        
        Component c = _tabbedPane.getSelectedComponent();
        if (c instanceof RegionsTreePanel<?>) {
           reloadPanel((RegionsTreePanel<?>) c, doc, offset);
        }
        

      }
      
      
      private <R extends OrderedDocumentRegion> void reloadPanel(final RegionsTreePanel<R> p,
                                                                 final OpenDefinitionsDocument doc,
                                                                 int offset) {
        
        final RegionManager<R> rm = p._regionManager;
        SortedSet<R> regions = rm.getRegions(doc);
        if (regions == null || regions.size() == 0) return;
        
        
        final int numLinesChangedAfter = doc.getDocument().getAndResetNumLinesChangedAfter();
        
        
        Pair<R, R> lineNumInterval = null;
        
        if (numLinesChangedAfter >= 0)  {  
          
          
          
          
          
          
          
          @SuppressWarnings("unchecked") R start =
            (R) new DocumentRegion(doc, numLinesChangedAfter, numLinesChangedAfter);
          int len = doc.getLength();
          @SuppressWarnings("unchecked") R end = (R) new DocumentRegion(doc, len, len);
          lineNumInterval = Pair.make(start, end); 
        }

        Pair<R, R> interval = rm.getRegionInterval(doc, offset);
        if (interval == null && lineNumInterval == null) return;
        
        interval = maxInterval(lineNumInterval, interval);
    
        final R first = interval.first();
        final R last = interval.second();
            
        synchronized(_updateLock) {
          if (_tabUpdatePending && _pendingDocument == doc) {  
            _firstRegion = _firstRegion.compareTo(first) <= 0 ? _firstRegion : first;
            _lastRegion = _lastRegion.compareTo(last) >= 0 ? _lastRegion : last;
            _waitAgain = true;
            return;
          }
          else {  
            _firstRegion = first;
            _lastRegion = last;
            _pendingDocument = doc;
            _tabUpdatePending = true;
            _pendingUpdate = new Runnable() { 
              public void run() {
                
                
                
                @SuppressWarnings("unchecked") R first = (R) _firstRegion;
                @SuppressWarnings("unchecked") R last = (R) _lastRegion;
                rm.updateLines(first, last); 
                p.reload(first, last);  
                p.repaint();
              }
            };  
          }
        }
        
        
        
        _threadPool.submit(new Runnable() {
          public void run() {
            Thread.currentThread().setPriority(UPDATER_PRIORITY);
            synchronized (_updateLock) {
              try { 
                do { 
                  _waitAgain = false;
                  _updateLock.wait(UPDATE_DELAY); 
                } 
                while (_waitAgain);
              }
              catch(InterruptedException e) {  }
              _tabUpdatePending = false;
            } 
            Utilities.invokeLater(_pendingUpdate);
          }
        });
      }
      
      public void changedUpdate(DocumentEvent e) { }
      public void insertUpdate(DocumentEvent e) {
        updateUI(((DefinitionsDocument) e.getDocument()).getOpenDefDoc(), e.getOffset()); 
      }
      public void removeUpdate(DocumentEvent e) {
        updateUI(((DefinitionsDocument) e.getDocument()).getOpenDefDoc(), e.getOffset());
      }
    });
    
    
    pane.addCaretListener(_posListener);
    
    
    pane.addFocusListener(new LastFocusListener());
    
    
    final JScrollPane scroll = 
      new BorderlessScrollPane(pane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pane.setScrollPane(scroll);
    
    
    if (DrJava.getConfig().getSetting(LINEENUM_ENABLED).booleanValue()) {
      scroll.setRowHeaderView(new LineEnumRule(pane));
    }
    
    _defScrollPanes.put(doc, scroll);
    
    return scroll;
  }
  
  private static <R extends OrderedDocumentRegion> Pair<R, R> maxInterval(Pair<R, R> i, Pair<R, R> j) {
    if (i == null) return j;
    if (j == null) return i;
    R i1 = i.first();
    R i2 = i.second();
    R j1 = j.first();
    R j2 = j.second();
             
    return Pair.make(i1.compareTo(j1) <= 0 ? i1 : j1, i2.compareTo(j2) >= 0 ? i2 : j2);
  }
  
  private void _setUpPanes() {
    

    
    
    if (_showDebugger) {
      try {
        
        int debugHeight = DrJava.getConfig().getSetting(DEBUG_PANEL_HEIGHT).intValue();
        Dimension debugMinSize = _debugPanel.getMinimumSize();
        
        
        if ((debugHeight > debugMinSize.height)) debugMinSize.height = debugHeight;
        _debugPanel.setPreferredSize(debugMinSize);
      }
      catch(NoClassDefFoundError e) {
        
        _showDebugger = false;
      }
    } 
    
    _debugSplitPane.setBottomComponent(_debugPanel);
    _mainSplit.setResizeWeight(1.0);
    _debugSplitPane.setResizeWeight(1.0);
    getContentPane().add(_mainSplit, BorderLayout.CENTER);
    
    
    
    
    

    
    
    _mainSplit.setDividerLocation(_mainSplit.getHeight() - 132);

    _mainSplit.setOneTouchExpandable(true);
    _debugSplitPane.setOneTouchExpandable(true);
    
    int docListWidth = DrJava.getConfig().getSetting(DOC_LIST_WIDTH).intValue();
    
    
    _docSplitPane.setDividerLocation(docListWidth);
    _docSplitPane.setOneTouchExpandable(true);
  }
  
  
  void _switchDefScrollPane() {
    assert EventQueue.isDispatchThread();
    
    
    
    
    _currentDefPane.notifyInactive();
    


    OpenDefinitionsDocument activeDoc = _model.getActiveDocument();
    _currentDefDoc = activeDoc.getDocument();
    JScrollPane scroll = _defScrollPanes.get(activeDoc);
    
    if (scroll == null) scroll = _createDefScrollPane(activeDoc);
    
    
    _reenableScrollBar();
    
    int oldLocation = _docSplitPane.getDividerLocation();
    _docSplitPane.setRightComponent(scroll); 
    _docSplitPane.setDividerLocation(oldLocation);
    
    
    
    
    if (_currentDefPane.isEditable()) {
      _currentDefPane = (DefinitionsPane) scroll.getViewport().getView();
      _currentDefPane.notifyActive();
    }
    else {
      try { _currentDefPane.setEditable(true); }
      catch(NoSuchDocumentException e) {  }
      
      _currentDefPane = (DefinitionsPane) scroll.getViewport().getView();
      _currentDefPane.notifyActive();
      _currentDefPane.setEditable(false);
    }
    
    resetUndo();
    _updateDebugStatus();
  }
  
  
  private void _refreshDefScrollPane() {
    
    
    _currentDefPane.notifyInactive();
    


    OpenDefinitionsDocument doc = _model.getActiveDocument();
    JScrollPane scroll = _defScrollPanes.get(doc);
    

    
    
    _reenableScrollBar();
    
    int oldLocation = _docSplitPane.getDividerLocation();
    _docSplitPane.setRightComponent(scroll); 
    _docSplitPane.setDividerLocation(oldLocation);
    





    _currentDefPane.notifyActive();










    resetUndo();
    _updateDebugStatus();
  }
  
  public void resetUndo() {
    _undoAction.setDelegatee(_currentDefPane.getUndoAction());
    _redoAction.setDelegatee(_currentDefPane.getRedoAction());
  }
  
  public DefinitionsPane getDefPaneGivenODD(OpenDefinitionsDocument doc) {
    JScrollPane scroll = _defScrollPanes.get(doc);
    if (scroll == null) { 
      if (_model.getOpenDefinitionsDocuments().contains(doc)) scroll = _createDefScrollPane(doc);
      else throw new UnexpectedException(new Exception("Attempted to get DefinitionsPane for a closed document")); 
    }
    
    DefinitionsPane pane = (DefinitionsPane) scroll.getViewport().getView();
    return pane;
  }
  
  
  private void _reenableScrollBar() {
    JScrollPane scroll = _defScrollPanes.get(_model.getActiveDocument());
    if (scroll == null)
      throw new UnexpectedException(new Exception("Current definitions scroll pane not found."));
    
    JScrollBar oldbar = scroll.getVerticalScrollBar();
    JScrollBar newbar = scroll.createVerticalScrollBar();
    newbar.setMinimum(oldbar.getMinimum());
    newbar.setMaximum(oldbar.getMaximum());
    newbar.setValue(oldbar.getValue());
    newbar.setVisibleAmount(oldbar.getVisibleAmount());
    newbar.setEnabled(true);
    newbar.revalidate();
    scroll.setVerticalScrollBar(newbar);
    
    
    oldbar = scroll.getHorizontalScrollBar();
    newbar = scroll.createHorizontalScrollBar();
    newbar.setMinimum(oldbar.getMinimum());
    newbar.setMaximum(oldbar.getMaximum());
    newbar.setValue(oldbar.getValue());
    newbar.setVisibleAmount(oldbar.getVisibleAmount());
    newbar.setEnabled(true);
    newbar.revalidate();
    scroll.setHorizontalScrollBar(newbar);
    scroll.revalidate();
  }
  
  
  private JMenuItem _newCheckBoxMenuItem(Action action) {
    String RADIO_ICON_KEY = "RadioButtonMenuItem.checkIcon";
    String CHECK_ICON_KEY = "CheckBoxMenuItem.checkIcon";
    
    
    Object radioIcon = UIManager.get(RADIO_ICON_KEY);
    
    
    
    UIManager.put(RADIO_ICON_KEY, UIManager.get(CHECK_ICON_KEY));
    JRadioButtonMenuItem pseudoCheckBox = new JRadioButtonMenuItem(action);
    
    
    UIManager.put(RADIO_ICON_KEY, radioIcon);
    
    return pseudoCheckBox;
  }
  
  
  private File _getFullFile(File f) throws IOException {
    if (PlatformFactory.ONLY.isWindowsPlatform() &&
        ((f.getAbsolutePath().indexOf("..") != -1) || (f.getAbsolutePath().indexOf("./") != -1) ||
         (f.getAbsolutePath().indexOf(".\\") != -1))) {
      return f.getCanonicalFile();
    }
    return f.getAbsoluteFile();
  }
  
  
  private void _setCurrentDirectory(File file) {
    
    try {
      file = _getFullFile(file);
      _openChooser.setCurrentDirectory(file);
      _saveChooser.setCurrentDirectory(file);
      DrJava.getConfig().setSetting(LAST_DIRECTORY, file);
    }
    catch (IOException ioe) {
      
    }
  }
  
  
  private void _setCurrentDirectory(OpenDefinitionsDocument doc) {
    try {
      File file = doc.getFile();
      if (file != null) _setCurrentDirectory(file); 
    }
    catch (FileMovedException fme) {
      
      _setCurrentDirectory(fme.getFile());
    }
  }
  
  
  private void _setMainFont() {
    
    Font f = DrJava.getConfig().getSetting(FONT_MAIN);
    
    for (JScrollPane scroll: _defScrollPanes.values()) {
      if (scroll != null) {
        DefinitionsPane pane = (DefinitionsPane) scroll.getViewport().getView();
        pane.setFont(f);
        
        if (DrJava.getConfig().getSetting(LINEENUM_ENABLED).booleanValue()) {
          scroll.setRowHeaderView(new LineEnumRule(pane));
        }
      }
    }
    
    
    _interactionsPane.setFont(f);
    _interactionsController.setDefaultFont(f);
    
    
    _consolePane.setFont(f);
    _consoleController.setDefaultFont(f);
    
    _findReplace.setFieldFont(f);
    _compilerErrorPanel.setListFont(f);
    _junitErrorPanel.setListFont(f);
    _javadocErrorPanel.setListFont(f);
  }
  
  
  private void _updateNormalColor() {
    
    Color norm = DrJava.getConfig().getSetting(DEFINITIONS_NORMAL_COLOR);
    
    
    _model.getDocCollectionWidget().setForeground(norm);
    
    
    _repaintLineNums();
  }
  
  
  private void _updateBackgroundColor() {
    
    Color back = DrJava.getConfig().getSetting(DEFINITIONS_BACKGROUND_COLOR);
    
    
    _model.getDocCollectionWidget().setBackground(back);
    
    
    _repaintLineNums();
  }
  
  
  private void _updateLineNums() {
    if (DrJava.getConfig().getSetting(LINEENUM_ENABLED).booleanValue()) {
      
      
      for (JScrollPane spane: _defScrollPanes.values()) { 
        
        LineEnumRule ler = (LineEnumRule) spane.getRowHeader().getView();
        ler.updateFont();
        ler.revalidate();
      }
      
      
      _repaintLineNums();
    }
  }
  
  
  private void _repaintLineNums() {
    JScrollPane front = _defScrollPanes.get(_model.getActiveDocument());
    if (front != null) {
      JViewport rhvport = front.getRowHeader();
      
      if (rhvport != null) {
        Component view = rhvport.getView();
        if (view != null) view.repaint();
      }
    }
  }
  











  
  
  private void _updateDefScrollRowHeader() {
    boolean ruleEnabled = DrJava.getConfig().getSetting(LINEENUM_ENABLED).booleanValue();
    
    for (JScrollPane scroll: _defScrollPanes.values()) {
      if (scroll != null) {
        DefinitionsPane pane = (DefinitionsPane) scroll.getViewport().getView();
        if (scroll.getRowHeader() == null || scroll.getRowHeader().getView() == null) {
          if (ruleEnabled) scroll.setRowHeaderView(new LineEnumRule(pane));
        }
        else if (! ruleEnabled) scroll.setRowHeaderView(null);
      }
    }
  }
  
  
  public void removeCurrentLocationHighlight() {
    if (_currentLocationHighlight != null) {
      _currentLocationHighlight.remove();
      _currentLocationHighlight = null;
    }
  }
  
  
  private void _disableStepTimer() {
    synchronized(_debugStepTimer) { if (_debugStepTimer.isRunning()) _debugStepTimer.stop(); }
  }
  
  
  private void _updateDebugStatus() {
    if (! isDebuggerReady()) return;
    
    
    if (_model.getActiveDocument().isUntitled() || _model.getActiveDocument().getClassFileInSync()) {
      
      if (_debugPanel.getStatusText().equals(DEBUGGER_OUT_OF_SYNC)) _debugPanel.setStatusText("");
    } 
    else {
      
      if (_debugPanel.getStatusText().equals("")) {
        _debugPanel.setStatusText(DEBUGGER_OUT_OF_SYNC);
      }
    }
    _debugPanel.repaint();  
  }
  
  
  protected void _disableInteractionsPane() {
    assert EventQueue.isDispatchThread();
    _interactionsPane.setEditable(false);
    _interactionsPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    
    if (_interactionsScriptController != null) _interactionsScriptController.setActionsDisabled();
  }
  
  
  protected void _enableInteractionsPane() {
    assert EventQueue.isDispatchThread();
    _interactionsPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    _interactionsPane.setEditable(true);
    _interactionsController.moveToEnd();
    if (_interactionsPane.hasFocus()) _interactionsPane.getCaret().setVisible(true);
    if (_interactionsScriptController != null) _interactionsScriptController.setActionsEnabled();
  }
  
  
  public void commentLines() {
    assert EventQueue.isDispatchThread();
    
    
    OpenDefinitionsDocument openDoc = _model.getActiveDocument();
    int caretPos = _currentDefPane.getCaretPosition();
    openDoc.setCurrentLocation(caretPos);
    int start = _currentDefPane.getSelectionStart();
    int end = _currentDefPane.getSelectionEnd();


    int newEnd = openDoc.commentLines(start, end);

    _currentDefPane.setCaretPosition(start+2);
    if (start != end) _currentDefPane.moveCaretPosition(newEnd);
  }
  
  
  public void uncommentLines() {
    assert EventQueue.isDispatchThread();
    
    
    OpenDefinitionsDocument openDoc = _model.getActiveDocument();
    int caretPos = _currentDefPane.getCaretPosition();
    openDoc.setCurrentLocation(caretPos);
    int start = _currentDefPane.getSelectionStart();
    int end = _currentDefPane.getSelectionEnd();
    _currentDefPane.endCompoundEdit();
    
    

    openDoc.setCurrentLocation(start);
    Position startPos;
    try { startPos = openDoc.createUnwrappedPosition(start); }
    catch (BadLocationException e) { throw new UnexpectedException(e); }
    
    int startOffset = startPos.getOffset();        
    final int newEnd = openDoc.uncommentLines(start, end);

    if (startOffset != startPos.getOffset()) start -= 2;
    final int f_start = start;
    final boolean moveSelection = start != end;
    _currentDefPane.setCaretPosition(f_start);
    if (moveSelection) _currentDefPane.moveCaretPosition(newEnd);
  }
  
  
  private static class GlassPane extends JComponent {
    
    
    public GlassPane() {
      addKeyListener(new KeyAdapter() { });
      addMouseListener(new MouseAdapter() { });
      super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
  }
  
  
  public void scrollToDocumentAndOffset(final OpenDefinitionsDocument doc, final int offset, 
                                        final boolean shouldHighlight) {
    scrollToDocumentAndOffset(doc, offset, shouldHighlight, true);
  }
  
  public void goToRegionAndHighlight(final IDocumentRegion r) {
    assert EventQueue.isDispatchThread();
    addToBrowserHistory();
    final OpenDefinitionsDocument doc = r.getDocument();
    boolean toSameDoc = doc == _model.getActiveDocument();
    Runnable command = new Runnable() {
      public void run() {
        int startOffset = r.getStartOffset();
        int endOffset = r.getEndOffset();
        doc.setCurrentLocation(startOffset);
        _currentLocationHighlight = _currentDefPane.getHighlightManager().
          addHighlight(startOffset, endOffset, DefinitionsPane.THREAD_PAINTER);
        _currentDefPane.centerViewOnOffset(startOffset);
        _currentDefPane.select(startOffset, endOffset);
        _currentDefPane.requestFocusInWindow();
      }
    };
    
    if (! toSameDoc) {
      _model.setActiveDocument(doc);    
      _findReplace.updateFirstDocInSearch();
      EventQueue.invokeLater(command);  
    }
    else {
      _model.refreshActiveDocument();
      command.run();
    }
    EventQueue.invokeLater(new Runnable() { public void run() { addToBrowserHistory(); } });  
  }
  
  
  public void scrollToDocumentAndOffset(final OpenDefinitionsDocument doc, final int offset, 
                                        final boolean shouldHighlight, final boolean shouldAddToHistory) {
    
    assert duringInit() || EventQueue.isDispatchThread();
    
    if (shouldAddToHistory) addToBrowserHistory();
    OpenDefinitionsDocument activeDoc =  _model.getActiveDocument();
    final boolean toSameDoc = (activeDoc == doc);
    
    Runnable command = new Runnable() {
      public void run() {

        if (shouldHighlight) {
          removeCurrentLocationHighlight();
          int startOffset = doc._getLineStartPos(offset);
          if (startOffset >= 0) {
            int endOffset = doc._getLineEndPos(offset);
            if (endOffset >= 0) {
              _currentLocationHighlight = _currentDefPane.getHighlightManager().
                addHighlight(startOffset, endOffset, DefinitionsPane.THREAD_PAINTER);
            }
          }
        }
        
        if (_currentDefPane.getSize().getWidth() > 0 && _currentDefPane.getSize().getHeight() > 0) {
          EventQueue.invokeLater(new Runnable() { 
            public void run() {
              _currentDefPane.centerViewOnOffset(offset);
              _currentDefPane.requestFocusInWindow();
            }
          });
        }
        
        if (_showDebugger) {
          
          _interactionsPane.requestFocusInWindow();


          _updateDebugStatus();
        }
      }
    };
    
    if (! toSameDoc) {
      _model.setActiveDocument(doc);    
      _findReplace.updateFirstDocInSearch();
      EventQueue.invokeLater(command);  
    }
    else {
      _model.refreshActiveDocument();
      command.run();
    }
  }
  
  
  private class UIDebugListener implements DebugListener {
    
    public void debuggerStarted() { EventQueue.invokeLater(new Runnable() { public void run() { showDebugger(); } }); }
    
    
    public void debuggerShutdown() {
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          _disableStepTimer();
          hideDebugger();
          removeCurrentLocationHighlight();
        }
      } );
    }                        
    
    
    public void stepRequested() {
      
      synchronized(_debugStepTimer) { if (! _debugStepTimer.isRunning()) _debugStepTimer.start(); }
    }
    
    public void currThreadSuspended() {
      assert EventQueue.isDispatchThread();
      _disableStepTimer();
      _setThreadDependentDebugMenuItems(true);
      _model.getInteractionsModel().autoImport();               
      if(_model.getDebugger().isAutomaticTraceEnabled()) {
        
        if((_automaticTraceTimer!=null) && (!_automaticTraceTimer.isRunning()))
          _automaticTraceTimer.start();
      }
    }
    
    
    public void currThreadResumed() {
      _setThreadDependentDebugMenuItems(false);
      removeCurrentLocationHighlight();
    }    
    
    
    public void threadLocationUpdated(OpenDefinitionsDocument doc, int lineNumber, boolean shouldHighlight) {
      scrollToDocumentAndOffset(doc, doc._getOffset(lineNumber), shouldHighlight); 
    }
    
    
    public void currThreadDied() {
      assert EventQueue.isDispatchThread();
      _disableStepTimer();
      _model.getDebugger().setAutomaticTraceEnabled(false);
      if (_automaticTraceTimer!=null) _automaticTraceTimer.stop();
      if (isDebuggerReady()) {
        try {        
          if (!_model.getDebugger().hasSuspendedThreads()) {
            
            
            _setThreadDependentDebugMenuItems(false);
            removeCurrentLocationHighlight();
            
            
            _interactionsController.moveToPrompt(); 
          }
        }
        catch (DebugException de) {
          _showError(de, "Debugger Error", "Error with a thread in the debugger.");
        }
      }
    }
    
    public void currThreadSet(DebugThreadData dtd) { }
    public void regionAdded(final Breakpoint bp) { }
    public void breakpointReached(Breakpoint bp) {
      showTab(_interactionsContainer, true);
    }
    public void regionChanged(Breakpoint bp) {  }
    public void regionRemoved(final Breakpoint bp) { }    
    public void watchSet(final DebugWatchData w) { }
    public void watchRemoved(final DebugWatchData w) { }
    public void threadStarted() { }
    public void nonCurrThreadDied() { }
  }
  
  
  private class DJAsyncTaskLauncher extends AsyncTaskLauncher {
    
    protected boolean shouldSetEnabled() { return true; }
    
    protected void setParentContainerEnabled(boolean enabled) {
      if (enabled) hourglassOff(); 
      else hourglassOn();
    }
    
    protected IAsyncProgress createProgressMonitor(final String description, final int min, final int max) {
      return new IAsyncProgress() {
        private ProgressMonitor _monitor = new ProgressMonitor(MainFrame.this, description, "", min, max);
        
        public void close() { _monitor.close(); }
        public int  getMaximum() { return _monitor.getMaximum() ; }
        public int  getMillisToDecideToPopup() { return _monitor.getMillisToDecideToPopup(); }
        public int  getMillisToPopup() { return  _monitor.getMillisToPopup(); }
        public int  getMinimum() { return _monitor.getMinimum(); }
        public String  getNote() { return _monitor.getNote(); }
        public boolean  isCanceled() { return _monitor.isCanceled(); }
        public void  setMaximum(int m) { _monitor.setMaximum(m); }
        public void  setMinimum(int m) { _monitor.setMinimum(m); }
        public void  setNote(String note) { _monitor.setNote(note); }
        public void  setProgress(int nv) { _monitor.setProgress(nv); }
      };
    }
  }
  
  
  void askToIncreaseSlaveMaxHeap() {
    String value = "set to "+DrJava.getConfig().getSetting(SLAVE_JVM_XMX)+" MB";
    if ((!("".equals(DrJava.getConfig().getSetting(SLAVE_JVM_XMX)))) &&
        ((OptionConstants.heapSizeChoices.get(0).equals(DrJava.getConfig().getSetting(SLAVE_JVM_XMX))))) { 
      value = "not set, implying the system's default";
    }
    
    String res = (String)JOptionPane.
      showInputDialog(MainFrame.this,
                      "Your program ran out of memory. You may try to enter a larger\n" +
                      "maximum heap size for the Interactions JVM. The maximum heap size is\n" +
                      "currently "+value+".\n"+
                      "A restart is required after changing this setting.",
                      "Increase Maximum Heap Size?",
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      OptionConstants.heapSizeChoices.toArray(),
                      DrJava.getConfig().getSetting(SLAVE_JVM_XMX));
    
    if (res != null) {
      
      DrJava.getConfig().removeOptionListener(SLAVE_JVM_XMX, _slaveJvmXmxListener);
      final ConfigOptionListeners.SlaveJVMXMXListener l = new ConfigOptionListeners.SlaveJVMXMXListener(MainFrame.this);
      DrJava.getConfig().addOptionListener(SLAVE_JVM_XMX, l);
      
      DrJava.getConfig().setSetting(SLAVE_JVM_XMX,res.trim());
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          
          DrJava.getConfig().removeOptionListener(SLAVE_JVM_XMX, l);
          DrJava.getConfig().addOptionListener(SLAVE_JVM_XMX, _slaveJvmXmxListener);
        }
      });
    }
    _model.getInteractionsModel().resetLastErrors();
  }
  
  
  void askToIncreaseMasterMaxHeap() {
    String value = "set to "+DrJava.getConfig().getSetting(MASTER_JVM_XMX)+" MB";
    if ((!("".equals(DrJava.getConfig().getSetting(MASTER_JVM_XMX)))) &&
        ((OptionConstants.heapSizeChoices.get(0).equals(DrJava.getConfig().getSetting(MASTER_JVM_XMX))))) { 
      value = "not set, implying the system's default";
    }
    
    String res = (String)JOptionPane.showInputDialog(MainFrame.this,
                                                     "DrJava ran out of memory. You may try to enter a larger\n" +
                                                     "maximum heap size for the main JVM. The maximum heap size is\n" +
                                                     "currently " + value + ".\n" +
                                                     "A restart is required after changing this setting.",
                                                     "Increase Maximum Heap Size?",
                                                     JOptionPane.QUESTION_MESSAGE,
                                                     null,
                                                     OptionConstants.heapSizeChoices.toArray(),
                                                     DrJava.getConfig().getSetting(MASTER_JVM_XMX));
    
    if (res != null) {
      
      DrJava.getConfig().removeOptionListener(MASTER_JVM_XMX, _masterJvmXmxListener);
      final ConfigOptionListeners.MasterJVMXMXListener l = 
        new ConfigOptionListeners.MasterJVMXMXListener(MainFrame.this);
      DrJava.getConfig().addOptionListener(MASTER_JVM_XMX, l);
      
      DrJava.getConfig().setSetting(MASTER_JVM_XMX,res.trim());
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          
          DrJava.getConfig().removeOptionListener(MASTER_JVM_XMX, l);
          DrJava.getConfig().addOptionListener(MASTER_JVM_XMX, _masterJvmXmxListener);
        }
      });
    }
    _model.getInteractionsModel().resetLastErrors();
  }
  
  
  private class ModelListener implements GlobalModelListener {
    
    public <P,R> void executeAsyncTask(AsyncTask<P,R> task, P param, boolean showProgress, boolean lockUI) {
      new DJAsyncTaskLauncher().executeTask(task, param, showProgress, lockUI);
    }
    public void handleAlreadyOpenDocument(OpenDefinitionsDocument doc) {


      
      
      _model.setActiveDocument(doc);
      


      
      
      if (doc.isModifiedSinceSave()) {
        String title = "Revert to Saved?";
        String message = doc.getFileName() + " is already open and modified.\n" +
          "Would you like to revert to the version on disk?\n";
        int choice = JOptionPane.showConfirmDialog(MainFrame.this, message, title, JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
          _revert(doc);
        }
      }
    }
    
    public void newFileCreated(final OpenDefinitionsDocument doc) {
      _createDefScrollPane(doc);
      PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate();
    }
    
    private volatile int _fnfCount = 0;
    
    private boolean resetFNFCount() { return _fnfCount == 0; }
    
    private boolean someFilesNotFound() {
      PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate();
      return _fnfCount > 0;
    }
    
    public void filesNotFound(File... files) {
      if (files.length == 0) return;
      _fnfCount += files.length;
      
      if (files.length == 1) {
        JOptionPane.showMessageDialog(MainFrame.this,
                                      "The following file could not be found and has been removed from the project.\n"
                                        + files[0].getPath(),
                                      "File Not Found",
                                      JOptionPane.ERROR_MESSAGE);
      }
      else {
        final List<String> filePaths = new ArrayList<String>();
        for (File f : files) { filePaths.add(f.getPath()); }
        
        ScrollableListDialog<String> dialog = new ScrollableListDialog.Builder<String>()
          .setOwner(MainFrame.this)
          .setTitle("Files Not Found")
          .setText("The following files could not be found and have been removed from the project.")
          .setItems(filePaths)
          .setMessageType(JOptionPane.ERROR_MESSAGE)
          .build();
        
        setPopupLoc(dialog);
        dialog.showDialog();
        PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate();
      }
    }
    
    public File[] filesReadOnly(File... files) {
      if (files.length == 0) return new File[0];
      _fnfCount += files.length;
      
      final ArrayList<String> choices = new java.util.ArrayList<String>();
      choices.add("Yes");
      choices.add("No");
      final List<String> filePaths = new ArrayList<String>();
      for (File f : files) { filePaths.add(f.getPath()); }
      ScrollableListDialog<String> dialog = new ScrollableListDialog.Builder<String>()
        .setOwner(MainFrame.this)
        .setTitle("Files are Read-Only")
        .setText("The following files could not be saved because they are read-only.\n"+
                 "Do you want to overwrite them anyway?")
        .setItems(filePaths)
        .setSelectedItems(filePaths)
        .setMessageType(JOptionPane.QUESTION_MESSAGE)
        .clearButtons()
        .addButton(new JButton("Yes"))
        .addButton(new JButton("No"))
        .setSelectable(true)
        .build();
      
      boolean overwrite = false;
      
      if (files.length == 1) {
        int res = JOptionPane.showConfirmDialog(MainFrame.this,
                                                "The following file could not be saved because it is read-only.\n" +
                                                "Do you want to overwrite it anyway?\n" + files[0].getPath(),
                                                "File is Read-Only",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
        overwrite = (res == 0);
      }
      else {
        setPopupLoc(dialog);
        dialog.showDialog();
        overwrite = (dialog.getButtonPressed() == 0);
      }
      
      if (overwrite) {
        if (files.length == 1) return files;
        else {
          File[] overwriteFiles = new File[dialog.getSelectedItems().size()];
          int i = 0;
          for(String s: dialog.getSelectedItems()) { overwriteFiles[i++] = new File(s); }
          return overwriteFiles;
        }
      }
      else return new File[0];
    }
    
    public void fileSaved(final OpenDefinitionsDocument doc) {
      doc.documentSaved();  
      _saveAction.setEnabled(false);
      _renameAction.setEnabled(true);
      _revertAction.setEnabled(true);
      updateStatusField();
      _currentDefPane.requestFocusInWindow();
      PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate();
      try {
        File f = doc.getFile();
        if (! _model.inProject(f)) _recentFileManager.updateOpenFiles(f);
      }
      catch (FileMovedException fme) {
        File f = fme.getFile();
        
        if (! _model.inProject(f)) _recentFileManager.updateOpenFiles(f);
      }
      
      _updateDebugStatus();
    }
    
    public void fileOpened(final OpenDefinitionsDocument doc) { 
      _fileOpened(doc);
      PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate(); 
    }
    
    private void _fileOpened(final OpenDefinitionsDocument doc) {
      try {
        File f = doc.getFile();
        if (! _model.inProject(f)) {
          _recentFileManager.updateOpenFiles(f);
          PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate();
        }
      }
      catch (FileMovedException fme) {
        File f = fme.getFile();
        
        if (! _model.inProject(f)) _recentFileManager.updateOpenFiles(f);
      }
    }
    
    public void fileClosed(final OpenDefinitionsDocument doc) { _fileClosed(doc); }
    
    
    private void _fileClosed(OpenDefinitionsDocument doc) {

      _recentDocFrame.closeDocument(doc);
      _removeErrorListener(doc);
      JScrollPane jsp = _defScrollPanes.get(doc);
      if (jsp != null) {
        ((DefinitionsPane)jsp.getViewport().getView()).close();
        _defScrollPanes.remove(doc);
      }
      PropertyMaps.TEMPLATE.getProperty("DrJava", "drjava.all.files").invalidate();
    }
    
    public void fileReverted(OpenDefinitionsDocument doc) {
      updateStatusField();
      _saveAction.setEnabled(false);
      _currentDefPane.resetUndo();
      _currentDefPane.hasWarnedAboutModified(false);
      _currentDefPane.setPositionAndScroll(0);
      _updateDebugStatus();
    }
    
    public void undoableEditHappened() {    
      assert EventQueue.isDispatchThread();
      _currentDefPane.getUndoAction().updateUndoState();
      _currentDefPane.getRedoAction().updateRedoState();
    }
    
    public void activeDocumentRefreshed(final OpenDefinitionsDocument active) {
      assert EventQueue.isDispatchThread();

      _recentDocFrame.pokeDocument(active);
      _refreshDefScrollPane();
      
      
      int pos = _currentDefPane.getCaretPosition();
      _currentDefPane.getErrorCaretListener().updateHighlight(pos);
      focusOnLastFocusOwner();
    }
    
    public void activeDocumentChanged(final OpenDefinitionsDocument active) {
      assert EventQueue.isDispatchThread();

      
      _recentDocFrame.pokeDocument(active);
      _switchDefScrollPane();  
      
      boolean isModified = active.isModifiedSinceSave();
      boolean canCompile = (! isModified && ! active.isUntitled());
      boolean hasName = ! active.isUntitled();
      _saveAction.setEnabled(! canCompile);
      _renameAction.setEnabled(hasName);
      _revertAction.setEnabled(hasName);
      
      
      int pos = _currentDefPane.getCaretPosition();
      _currentDefPane.getErrorCaretListener().updateHighlight(pos);
      
      
      _setCurrentDirectory(active);
      
      
      updateStatusField();
      _posListener.updateLocation();
      
      
      if (isModified) _model.getDocumentNavigator().repaint();
      
      try { active.revertIfModifiedOnDisk(); }
      catch (FileMovedException fme) { _showFileMovedError(fme); }
      catch (IOException e) { _showIOError(e); }
      
      
      if (isDisplayed(_findReplace)) {
        _findReplace.stopListening();
        _findReplace.beginListeningTo(_currentDefPane);
        
        
      }

      EventQueue.invokeLater(new Runnable() { 
        public void run() { 
          _lastFocusOwner = _currentDefPane;

          _currentDefPane.requestFocusInWindow(); 
          PropertyMaps.TEMPLATE.getProperty("DrJava","drjava.current.file").invalidate();
        } 
      });
    }
    
    public void focusOnLastFocusOwner() {

      _lastFocusOwner.requestFocusInWindow();
    }
    
    
    public void focusOnDefinitionsPane() {
      _currentDefPane.requestFocusInWindow();
    }
    
    public void interactionStarted() {
      _disableInteractionsPane();
      _runAction.setEnabled(false);
      _runAppletAction.setEnabled(false);
      _runProjectAction.setEnabled(false);
    }
    
    public void interactionEnded() {
      assert EventQueue.isDispatchThread();
      final InteractionsModel im = _model.getInteractionsModel();
      final String lastError = im.getLastError();
      final edu.rice.cs.drjava.config.FileConfiguration config = DrJava.getConfig();
      if (config != null && config.getSetting(edu.rice.cs.drjava.config.OptionConstants.DIALOG_AUTOIMPORT_ENABLED)) {
        if (lastError != null) {
          
          
          final String secondToLastError = im.getSecondToLastError();
          if (secondToLastError != null || ! lastError.equals(secondToLastError)) {
            
            if (lastError.startsWith("Static Error: Undefined class '") && lastError.endsWith("'")) {
              
              
              String undefinedClassName = lastError.substring(lastError.indexOf('\'') + 1, lastError.lastIndexOf('\''));
              _showAutoImportDialog(undefinedClassName);
            }
            else if (lastError.startsWith("java.lang.OutOfMemoryError")) {
              askToIncreaseSlaveMaxHeap();
            }
          }
        }
      }
      else im.resetLastErrors(); 
      
      _enableInteractionsPane();
      _runAction.setEnabled(true);
      _runAppletAction.setEnabled(true);
      _runProjectAction.setEnabled(_model.isProjectActive());
    }
    
    public void interactionErrorOccurred(final int offset, final int length) {
      _interactionsPane.highlightError(offset, length); 
    }
    
    
    public void interpreterChanged(final boolean inProgress) {
      _runAction.setEnabled(! inProgress);
      _runAppletAction.setEnabled(! inProgress);
      _runProjectAction.setEnabled(! inProgress);
      if (inProgress) _disableInteractionsPane();
      else _enableInteractionsPane();
    }
    
    public void compileStarted() {
      assert EventQueue.isDispatchThread();
      showTab(_compilerErrorPanel, true);
      _compilerErrorPanel.setCompilationInProgress();
      _saveAction.setEnabled(false);
    }    
    
    public void compileEnded(File workDir, final List<? extends File> excludedFiles) {
      assert EventQueue.isDispatchThread();    
      
      _compilerErrorPanel.reset(excludedFiles.toArray(new File[0]));
      if (isDebuggerReady()) {

        
        _updateDebugStatus();
      }
      if ((DrJava.getConfig().getSetting(DIALOG_COMPLETE_SCAN_CLASS_FILES).booleanValue()) && 
          (_model.getBuildDirectory() != null)) {
        _scanClassFiles();
      }
      if (_junitErrorPanel.isDisplayed()) _resetJUnit();
      _model.refreshActiveDocument();
    }
    
    
    public void compileAborted(Exception e) {  }
    
    
    public void activeCompilerChanged() {
      String linkVersion = DrJava.getConfig().getSetting(JAVADOC_API_REF_VERSION);
      if (linkVersion.equals(JAVADOC_AUTO_TEXT)) {
        
        _javaAPISet = null;
        generateJavaAPISet();
      }
    }
    
    public void prepareForRun(final OpenDefinitionsDocument doc) {
      
      assert EventQueue.isDispatchThread();
      
      
      showTab(_interactionsContainer, true);
      _lastFocusOwner = _interactionsContainer;
    }
    
    
    public void junitStarted() {
      assert EventQueue.isDispatchThread();
      
      
      try { showTab(_junitErrorPanel, true);
        _junitErrorPanel.setJUnitInProgress();
        
        
      }
      finally { 

        hourglassOff();
      }  
    }
    
    
    public void junitClassesStarted() {
      assert EventQueue.isDispatchThread();
      
      
      showTab(_junitErrorPanel, true);
      _junitErrorPanel.setJUnitInProgress();
      
      
    }
    
    
    
    public void junitSuiteStarted(final int numTests) {
      assert EventQueue.isDispatchThread();
      _junitErrorPanel.progressReset(numTests);
    }
    
    public void junitTestStarted(final String name) {
      assert EventQueue.isDispatchThread();
      _junitErrorPanel.getErrorListPane().testStarted(name);          
    }
    
    public void junitTestEnded(final String name, final boolean succeeded, final boolean causedError) {
      assert EventQueue.isDispatchThread();


      _junitErrorPanel.getErrorListPane().testEnded(name, succeeded, causedError);  
      _junitErrorPanel.progressStep(succeeded);
      _model.refreshActiveDocument();
    }
    
    public void junitEnded() {
      assert EventQueue.isDispatchThread();

      _restoreJUnitActionsEnabled();
      
      EventQueue.invokeLater(new Runnable() { public void run() { _junitErrorPanel.reset(); } });
      _model.refreshActiveDocument();
    }
    
    
    public void javadocStarted() {
      
      assert EventQueue.isDispatchThread();
      
      hourglassOn();
      
      showTab(_javadocErrorPanel, true);
      _javadocErrorPanel.setJavadocInProgress();
      _javadocAllAction.setEnabled(false);
      _javadocCurrentAction.setEnabled(false);
    }
    
    public void javadocEnded(final boolean success, final File destDir, final boolean allDocs) {
      
      assert EventQueue.isDispatchThread();
      try {
        showTab(_javadocErrorPanel, true);
        _javadocAllAction.setEnabled(true);
        _javadocCurrentAction.setEnabled(true);
        _javadocErrorPanel.reset();
        _model.refreshActiveDocument();
      }
      finally { hourglassOff(); }
      
      
      if (success) {
        String className;
        try {
          className = _model.getActiveDocument().getQualifiedClassName();
          className = className.replace('.', File.separatorChar);
        }
        catch (ClassNameNotFoundException cnf) {
          
          className = "";
        }
        try {
          String fileName = (allDocs || className.equals("")) ? "index.html" : (className + ".html");
          File index = new File(destDir, fileName);
          URL address = FileOps.toURL(index.getAbsoluteFile());
          
          if (! PlatformFactory.ONLY.openURL(address)) {
            JavadocFrame _javadocFrame = new JavadocFrame(destDir, className, allDocs);
            _javadocFrame.setVisible(true);
          }
        }
        catch (MalformedURLException me) { throw new UnexpectedException(me); }
        catch (IllegalStateException ise) {
          
          
          String msg =
            "Javadoc completed successfully, but did not produce any HTML files.\n" +
            "Please ensure that your access level in Preferences is appropriate.";
          JOptionPane.showMessageDialog(MainFrame.this, msg,
                                        "No output to display.",
                                        JOptionPane.INFORMATION_MESSAGE);
        }
      }
    }
    
    public void interpreterExited(final int status) {
      
      if (DrJava.getConfig().getSetting(INTERACTIONS_EXIT_PROMPT).booleanValue() && ! Utilities.TEST_MODE && 
          MainFrame.this.isVisible()) {
        
        String msg = "The interactions window was terminated by a call " +
          "to System.exit(" + status + ").\n" +
          "The interactions window will now be restarted.";
        
        String title = "Interactions terminated by System.exit(" + status + ")";
        
        ConfirmCheckBoxDialog dialog =
          new ConfirmCheckBoxDialog(MainFrame.this, title, msg,
                                    "Do not show this message again",
                                    JOptionPane.INFORMATION_MESSAGE,
                                    JOptionPane.DEFAULT_OPTION);
        if (dialog.show() == JOptionPane.OK_OPTION && dialog.getCheckBoxValue()) {
          DrJava.getConfig().setSetting(INTERACTIONS_EXIT_PROMPT, Boolean.FALSE);
        }
      }
    }
    
    public void interpreterResetFailed(Throwable t) { interpreterReady(FileOps.NULL_FILE); }
    
    public void interpreterResetting() {
      assert duringInit() || EventQueue.isDispatchThread();
      _junitAction.setEnabled(false);
      _junitAllAction.setEnabled(false);
      _junitProjectAction.setEnabled(false);
      _runAction.setEnabled(false);
      _runAppletAction.setEnabled(false);
      _runProjectAction.setEnabled(false);
      _closeInteractionsScript();
      _interactionsPane.setEditable(false);
      _interactionsPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      if (_showDebugger) _toggleDebuggerAction.setEnabled(false);
    }
    
    public void interpreterReady(File wd) {
      assert duringInit() || EventQueue.isDispatchThread();
      
      interactionEnded();
      _runAction.setEnabled(true);
      _runAppletAction.setEnabled(true);
      _runProjectAction.setEnabled(_model.isProjectActive());
      _junitAction.setEnabled(true);
      _junitAllAction.setEnabled(true);
      _junitProjectAction.setEnabled(_model.isProjectActive());
      

      if (_showDebugger) _toggleDebuggerAction.setEnabled(true);
      
      
      _interactionsController.interruptConsoleInput();
    }
    
    public void consoleReset() { }
    
    public void saveBeforeCompile() {
      assert EventQueue.isDispatchThread();
      
      
      _saveAllBeforeProceeding
        ("To compile, you must first save ALL modified files.\n" + "Would you like to save and then compile?",
         ALWAYS_SAVE_BEFORE_COMPILE,
         "Always save before compiling");
    }
    
    
    public void compileBeforeJUnit(final CompilerListener testAfterCompile, List<OpenDefinitionsDocument> outOfSync) {

      if (DrJava.getConfig().getSetting(ALWAYS_COMPILE_BEFORE_JUNIT).booleanValue() || Utilities.TEST_MODE) {
        
        _model.getCompilerModel().addListener(testAfterCompile);  
        _compileAll();
      }
      else { 
        final JButton yesButton = new JButton(new AbstractAction("Yes") {
          public void actionPerformed(ActionEvent e) {
            
            _model.getCompilerModel().addListener(testAfterCompile);  
            _compileAll();
          }
        });
        final JButton noButton = new JButton(new AbstractAction("No") {
          public void actionPerformed(ActionEvent e) {
            
            
            _junitInterrupted("Unit testing cancelled by user.");
          }
        });
        ScrollableListDialog<OpenDefinitionsDocument> dialog = new ScrollableListDialog.Builder<OpenDefinitionsDocument>()
          .setOwner(MainFrame.this)
          .setTitle("Must Compile All Source Files to Run Unit Tests")
          .setText("Before you can run unit tests, you must first compile all out of sync source files.\n"+
                   "The files below are out of sync. Would you like to compile all files and\n"+
                   "run the specified test(s)?")
          .setItems(outOfSync)
          .setMessageType(JOptionPane.QUESTION_MESSAGE)
          .setFitToScreen(true)
          .clearButtons()
          .addButton(yesButton)
          .addButton(noButton)
          .build();
        
        dialog.showDialog();
      }
    }
    
    public void saveBeforeJavadoc() {
      _saveAllBeforeProceeding
        ("To run Javadoc, you must first save ALL modified files.\n" +
         "Would you like to save and then run Javadoc?", ALWAYS_SAVE_BEFORE_JAVADOC,
         "Always save before running Javadoc");
    }
    
    
    private void _saveAllBeforeProceeding(String message, BooleanOption option, String checkMsg) {

      if (_model.hasModifiedDocuments()) {
        if (! DrJava.getConfig().getSetting(option).booleanValue() && ! Utilities.TEST_MODE) {
          ConfirmCheckBoxDialog dialog =
            new ConfirmCheckBoxDialog(MainFrame.this, "Must Save All Files to Continue", message, checkMsg);
          int rc = dialog.show();
          
          switch (rc) {
            case JOptionPane.YES_OPTION:
              _saveAll();
              
              if (dialog.getCheckBoxValue())  DrJava.getConfig().setSetting(option, Boolean.TRUE);
              break;
            case JOptionPane.NO_OPTION:
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
              
              break;
            default:
              throw new RuntimeException("Invalid rc from showConfirmDialog: " + rc);
          }
        }
        else _saveAll();
      }
    }
    
    
    public void saveUntitled() { _saveAs(); }
    
    public void filePathContainsPound() {
      if (DrJava.getConfig().getSetting(WARN_PATH_CONTAINS_POUND).booleanValue()) {
        String msg =
          "Files whose paths contain the '#' symbol cannot be used in the\n" +
          "Interactions Pane due to a bug in Java's file to URL conversion.\n" +
          "It is suggested that you change the name of the directory\n" +
          "containing the '#' symbol.";
        
        String title = "Path Contains Pound Sign";
        
        ConfirmCheckBoxDialog dialog =
          new ConfirmCheckBoxDialog(MainFrame.this, title, msg,
                                    "Do not show this message again",
                                    JOptionPane.WARNING_MESSAGE,
                                    JOptionPane.DEFAULT_OPTION);
        if (dialog.show() == JOptionPane.OK_OPTION && dialog.getCheckBoxValue()) {
          DrJava.getConfig().setSetting(WARN_PATH_CONTAINS_POUND, Boolean.FALSE);
        }
      }
    }
    
     
    public void nonTestCase(boolean isTestAll, boolean didCompileFail) {
      assert EventQueue.isDispatchThread();
      

      String message;
      String title = "Cannot Run JUnit Test Cases";
      if (didCompileFail) {
        message = "Compile failed. Cannot run JUnit TestCases.\n" +
          "Please examine the Compiler Output.";
      }
      else {        
        if (isTestAll) {
          message = "There are no compiled JUnit TestCases available for execution.\n" +
            "Perhaps you have not yet saved and compiled your test files.";
        }
        else {
          message = "The current document is not a valid JUnit test case.\n" +
            "Please make sure that:\n" +
            "- it has been compiled and\n" +
            "- it is a subclass of junit.framework.TestCase.\n";
        }
      }
      JOptionPane.showMessageDialog(MainFrame.this, message,
                                    title,
                                    JOptionPane.ERROR_MESSAGE);
      
      try {
        if (!didCompileFail) showTab(_junitErrorPanel, true);
        _resetJUnit();
      }
      finally { 
        hourglassOff();
        _restoreJUnitActionsEnabled();
      }
    }
    
     
    public void classFileError(ClassFileError e) {
      
      assert EventQueue.isDispatchThread();
      
      final String message = 
        "The class file for class " + e.getClassName() + " in source file " + e.getCanonicalPath() + 
        " cannot be loaded.\n "
        + "When DrJava tries to load it, the following error is generated:\n" +  e.getError();
      
      JOptionPane.showMessageDialog(MainFrame.this, message,
                                    "Testing works only on valid class files",
                                    JOptionPane.ERROR_MESSAGE);
      
      showTab(_junitErrorPanel, true);
      _junitAction.setEnabled(true);
      _junitAllAction.setEnabled(true);
      _junitProjectAction.setEnabled(_model.isProjectActive());
      _junitErrorPanel.reset();
    }
    
    
    public void currentDirectoryChanged(final File dir) { _setCurrentDirectory(dir); }
    
    
    public boolean canAbandonFile(OpenDefinitionsDocument doc) {
      return _fileSaveHelper(doc, JOptionPane.YES_NO_CANCEL_OPTION);
    }
    
    private boolean _fileSaveHelper(OpenDefinitionsDocument doc, int paneOption) {
      String text,fname;
      OpenDefinitionsDocument lastActive = _model.getActiveDocument();
      if (lastActive != doc) _model.setActiveDocument(doc);
      
      boolean notFound = false;
      try {
        File file = doc.getFile();
        if (file == null) {
          fname = "Untitled file";
          text = "Untitled file has been modified. Would you like to save it?";
        }
        else {
          fname = file.getName();
          text = fname + " has been modified. Would you like to save it?";
        }
      }
      catch (FileMovedException fme) {
        
        fname = fme.getFile().getName();
        text = fname + " not found on disk. Would you like to save to another file?";
        notFound = true;
      }
      
      int rc = JOptionPane.showConfirmDialog(MainFrame.this, text, "Save " + fname + "?", paneOption);
      switch (rc) {
        case JOptionPane.YES_OPTION:
          boolean saved = false;
          if (notFound) saved = _saveAs(); 
          else saved = _save();
          if (doc != lastActive) {
            _model.setActiveDocument(lastActive);  
          }
          return saved;
        case JOptionPane.NO_OPTION:
          if (doc != lastActive) {
          _model.setActiveDocument(lastActive);  
        }
          return true;
        case JOptionPane.CLOSED_OPTION:
        case JOptionPane.CANCEL_OPTION:
          return false;
        default:                         
          throw new RuntimeException("Invalid option: " + rc);
      }
    }
    
    
    public boolean quitFile(OpenDefinitionsDocument doc) { 
      return _fileSaveHelper(doc, JOptionPane.YES_NO_CANCEL_OPTION); 
    }
    
    
    public boolean shouldRevertFile(OpenDefinitionsDocument doc) {
      String fname;
      if (! _model.getActiveDocument().equals(doc)) {
        _model.setActiveDocument(doc);
      }
      try {
        File file = doc.getFile();
        if (file == null) fname = "Untitled file";
        else fname = file.getName();
      }
      catch (FileMovedException fme) { fname = fme.getFile().getName(); } 
      
      String text = fname + " has changed on disk.\n" + 
        "Would you like to reload it and discard any changes you have made?";
      String[] options = { "Reload from disk", "Keep my changes" };
      int rc = JOptionPane.showOptionDialog(MainFrame.this, text, fname + " Modified on Disk", 
                                            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                                            null, options, options[0]);
      switch (rc) {
        case 0:                         return true;
        case 1:                         return false;
        case JOptionPane.CLOSED_OPTION:
        case JOptionPane.CANCEL_OPTION: return false;
        default:                        throw new RuntimeException("Invalid rc: " + rc);
      }
    }
    
    public void interactionIncomplete() { }
    
    
    
    public void projectBuildDirChanged() {
      if (_model.getBuildDirectory() != null) {
        _cleanAction.setEnabled(true);
      }
      else _cleanAction.setEnabled(false);
    }
    
    public void projectWorkDirChanged() { }
    
    public void projectModified() {

    }
    
    public void projectClosed() {
      _model.getDocumentNavigator().asContainer().addKeyListener(_historyListener);
      _model.getDocumentNavigator().asContainer().addFocusListener(_focusListenerForRecentDocs);
      _model.getDocumentNavigator().asContainer().addMouseListener(_resetFindReplaceListener);

      removeTab(_junitErrorPanel);
      _runButton = _updateToolbarButton(_runButton, _runAction);
      _compileButton = _updateToolbarButton(_compileButton, _compileAllAction);
      _junitButton = _updateToolbarButton(_junitButton, _junitAllAction);
      projectRunnableChanged();
    }
    
    
    public void openProject(File projectFile, FileOpenSelector files) {
      _setUpContextMenus();
      projectRunnableChanged();
      _setUpProjectButtons(projectFile);
      open(files);
      _openProjectUpdate();
    }
    
    public void projectRunnableChanged() {
      if (_model.getMainClass() != null && _model.getMainClassContainingFile() != null && _model.getMainClassContainingFile().exists()) {
        _runProjectAction.setEnabled(_model.isProjectActive());
        _runButton = _updateToolbarButton(_runButton, _runProjectAction);
      }
      else {
        _runProjectAction.setEnabled(false);
        _runButton = _updateToolbarButton(_runButton, _runAction);
      }
    }
    
    public void documentNotFound(OpenDefinitionsDocument d, File f) {
      
      _model.setProjectChanged(true);
      
      String text = "File " + f.getAbsolutePath() +
        "\ncould not be found on disk!  It was probably moved\n" +
        "or deleted.  Would you like to try to find it?";
      int rc = JOptionPane.showConfirmDialog(MainFrame.this, text, "File Moved or Deleted", JOptionPane.YES_NO_OPTION);
      if (rc == JOptionPane.NO_OPTION) return;
      if (rc == JOptionPane.YES_OPTION) {
        try {
          File[] opened = _openSelector.getFiles(); 
          d.setFile(opened[0]);
        } 
        catch(OperationCanceledException oce) {
          
        }
      }


    }
    
    public void browserChanged() { _configureBrowsing(); }
    
    public void updateCurrentLocationInDoc() {

      if (_currentDefPane!=null) { _currentDefPane.updateCurrentLocationInDoc(); }
    }
  } 
  

  
  public JViewport getDefViewport() {
    OpenDefinitionsDocument doc = _model.getActiveDocument();

    JScrollPane defScroll = _defScrollPanes.get(doc);
    return defScroll.getViewport();
  }
  
  public void removeTab(final Component c) {
    
    if (_tabbedPane.getTabCount() > 1) {


      
      _tabbedPane.remove(c);
      ((TabbedPanel)c).setDisplayed(false);
    }
    _currentDefPane.requestFocusInWindow();
  }
  
  
  public void showBookmarks() { showTab(_bookmarksPanel, true); }
  
  
  public void showBreakpoints() { showTab(_breakpointsPanel, true); }
  
  private void _createTab(TabbedPanel panel) {
    int numVisible = 0;
    for (TabbedPanel t: _tabs) {
      if (t == panel) {
        Icon icon = (panel instanceof FindResultsPanel) ? FIND_ICON : null;
        _tabbedPane.insertTab(panel.getName(), icon, panel, null, numVisible + 2);  
        panel.setVisible(true);
        panel.setDisplayed(true);
        panel.repaint();
        break;
      }
      else if (isDisplayed(t)) numVisible++;
    }
  }
  
  public static final Icon FIND_ICON = getIcon("Find16.gif");
  
  
  public void showTab(final Component c, boolean showDetachedWindow) {
    
    
    assert EventQueue.isDispatchThread();
    try {
      if (c instanceof TabbedPanel) _createTab((TabbedPanel) c);
      if (c instanceof RegionsTreePanel<?>) {
        RegionsTreePanel<?> p = (RegionsTreePanel<?>) c;
        DefaultTreeModel model = p._regTreeModel;
        
        model.reload(); 
        p.expandTree();
        p.repaint();
      }
      
      _tabbedPane.setSelectedComponent(c);
      c.requestFocusInWindow();
      
      if (_mainSplit.getDividerLocation() > _mainSplit.getMaximumDividerLocation()) _mainSplit.resetToPreferredSizes();
    }
    finally {
      if (showDetachedWindow && (_tabbedPanesFrame != null) && (_tabbedPanesFrame.isVisible())) { 
        _tabbedPanesFrame.toFront(); 
      }
    }
  }
  
  
  private boolean _warnFileOpen(File f) {
    OpenDefinitionsDocument d = null;
    try { d = _model.getDocumentForFile(f); }
    catch(IOException ioe) {  }
    Object[] options = {"Yes","No"};
    if (d == null) return false;
    boolean dMod = d.isModifiedSinceSave();
    String msg = "This file is already open in DrJava" + (dMod ? " and has been modified" : "") + 
      ".  Do you wish to overwrite it?";
    int choice = JOptionPane.showOptionDialog(MainFrame.this, msg, "File Open Warning", JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
    if (choice == JOptionPane.YES_OPTION) return _model.closeFileWithoutPrompt(d);
    return false;
  }
  
  
  private boolean _verifyOverwrite() {
    Object[] options = {"Yes","No"};
    int n = JOptionPane.showOptionDialog(MainFrame.this,
                                         "This file already exists.  Do you wish to overwrite the file?",
                                         "Confirm Overwrite",
                                         JOptionPane.YES_NO_OPTION,
                                         JOptionPane.QUESTION_MESSAGE,
                                         null,
                                         options,
                                         options[1]);
    return (n == JOptionPane.YES_OPTION);
  }
  
  private void _resetJUnit() {
    _junitAction.setEnabled(true);
    _junitAllAction.setEnabled(true);
    _junitProjectAction.setEnabled(_model.isProjectActive());
    _junitErrorPanel.reset();
  }
  
  
  private void _junitInterrupted(final UnexpectedException e) {
    try {
      _showJUnitInterrupted(e);
      removeTab(_junitErrorPanel);
      _resetJUnit(); 
      _restoreJUnitActionsEnabled();
      _model.refreshActiveDocument();
    }
    finally { hourglassOff(); }
  }

  
  private void _junitInterrupted(String message) {
    try {
      _showJUnitInterrupted(message);
      removeTab(_junitErrorPanel);
      _resetJUnit(); 
      _restoreJUnitActionsEnabled();
      _model.refreshActiveDocument();
    }
    finally { hourglassOff(); }
  }
  
  boolean isDebuggerReady() { return _showDebugger &&  _model.getDebugger().isReady(); }
  
  boolean isDebuggerEnabled() { return _showDebugger; }
  
  
  FindReplacePanel getFindReplaceDialog() { return _findReplace; }
  
  
  private void _setUpKeyBindingMaps() {
    final ActionMap actionMap = _currentDefPane.getActionMap();
    final KeyBindingManager kbm = KeyBindingManager.ONLY;
    
    kbm.put(KEY_BACKWARD, actionMap.get(DefaultEditorKit.backwardAction), null, "Cursor Backward");
    kbm.put(KEY_BACKWARD_SELECT, actionMap.get(DefaultEditorKit.selectionBackwardAction), null, "Cursor Backward (Select)");
    
    kbm.put(KEY_BEGIN_DOCUMENT, actionMap.get(DefaultEditorKit.beginAction), null, "Cursor Begin Document");
    kbm.put(KEY_BEGIN_DOCUMENT_SELECT, actionMap.get(DefaultEditorKit.selectionBeginAction), null, "Cursor Begin Document (Select)");
    
    kbm.put(KEY_BEGIN_LINE, _beginLineAction, null, "Cursor Begin Line");
    kbm.put(KEY_BEGIN_LINE_SELECT, _selectionBeginLineAction, null, "Cursor Begin Line (Select)");
    
    kbm.put(KEY_PREVIOUS_WORD, actionMap.get(_currentDefDoc.getEditor().previousWordAction), null, "Cursor Previous Word");
    kbm.put(KEY_PREVIOUS_WORD_SELECT, actionMap.get(_currentDefDoc.getEditor().selectionPreviousWordAction), null, "Cursor Previous Word (Select)");
    
    kbm.put(KEY_DOWN, actionMap.get(DefaultEditorKit.downAction), null, "Cursor Down");
    kbm.put(KEY_DOWN_SELECT, actionMap.get(DefaultEditorKit.selectionDownAction), null, "Cursor Down (Select)");
    
    kbm.put(KEY_END_DOCUMENT, actionMap.get(DefaultEditorKit.endAction), null, "Cursor End Document");
    kbm.put(KEY_END_DOCUMENT_SELECT, actionMap.get(DefaultEditorKit.selectionEndAction), null, "Cursor End Document (Select)");
    
    kbm.put(KEY_END_LINE, actionMap.get(DefaultEditorKit.endLineAction), null, "Cursor End Line");
    kbm.put(KEY_END_LINE_SELECT, actionMap.get(DefaultEditorKit.selectionEndLineAction), null, "Cursor End Line (Select)");
    
    kbm.put(KEY_NEXT_WORD, actionMap.get(_currentDefDoc.getEditor().nextWordAction), null, "Cursor Next Word");
    kbm.put(KEY_NEXT_WORD_SELECT, actionMap.get(_currentDefDoc.getEditor().selectionNextWordAction), null, "Cursor Next Word (Select)");
    
    kbm.put(KEY_FORWARD, actionMap.get(DefaultEditorKit.forwardAction), null, "Cursor Forward");
    kbm.put(KEY_FORWARD_SELECT, actionMap.get(DefaultEditorKit.selectionForwardAction), null, "Cursor Forward (Select)");
    
    kbm.put(KEY_UP, actionMap.get(DefaultEditorKit.upAction), null, "Cursor Up");
    kbm.put(KEY_UP_SELECT, actionMap.get(DefaultEditorKit.selectionUpAction), null, "Cursor Up (Select)");
    


    
    
    kbm.put(KEY_PAGE_DOWN, actionMap.get(DefaultEditorKit.pageDownAction), null, "Cursor Page Down");
    kbm.put(KEY_PAGE_UP, actionMap.get(DefaultEditorKit.pageUpAction), null, "Cursor Page Up");
    kbm.put(KEY_CUT_LINE, _cutLineAction, null, "Cut Line");
    kbm.put(KEY_CLEAR_LINE, _clearLineAction, null, "Clear Line");
    kbm.put(KEY_SHIFT_DELETE_PREVIOUS, actionMap.get(DefaultEditorKit.deletePrevCharAction), null, "Delete Previous");
    kbm.put(KEY_SHIFT_DELETE_NEXT, actionMap.get(DefaultEditorKit.deleteNextCharAction), null, "Delete Next");
  }
  
  
  public void addComponentListenerToOpenDocumentsList(ComponentListener listener) {
    _docSplitPane.getLeftComponent().addComponentListener(listener);
  }
  
  
  public String getFileNameField() { return _statusField.getText(); }
  
  
  public JMenu getEditMenu() { return _editMenu; }
  
  
  private class MainFontOptionListener implements OptionListener<Font> {
    public void optionChanged(OptionEvent<Font> oce) { _setMainFont(); }
  }
  
  
  private class LineNumbersFontOptionListener implements OptionListener<Font> {
    public void optionChanged(OptionEvent<Font> oce) { _updateLineNums(); }
  }
  
  
  private class DoclistFontOptionListener implements OptionListener<Font> {
    public void optionChanged(OptionEvent<Font> oce) {
      Font doclistFont = DrJava.getConfig().getSetting(FONT_DOCLIST);
      _model.getDocCollectionWidget().setFont(doclistFont);
    }
  }
  
  
  private class ToolbarFontOptionListener implements OptionListener<Font> {
    public void optionChanged(OptionEvent<Font> oce) { _updateToolbarButtons(); }
  }
  
  
  private class NormalColorOptionListener implements OptionListener<Color> {
    public void optionChanged(OptionEvent<Color> oce) { _updateNormalColor(); }
  }
  
  
  private class BackgroundColorOptionListener implements OptionListener<Color> {
    public void optionChanged(OptionEvent<Color> oce) { _updateBackgroundColor(); }
  }
  
  
  private class ToolbarOptionListener implements OptionListener<Boolean> {
    public void optionChanged(OptionEvent<Boolean> oce) { _updateToolbarButtons(); }
  }
  
  
  private class LineEnumOptionListener implements OptionListener<Boolean> {
    public void optionChanged(OptionEvent<Boolean> oce) { _updateDefScrollRowHeader(); }
  }
  
  
  private class LineEnumColorOptionListener implements OptionListener<Color> {
    public void optionChanged(OptionEvent<Color> oce) { _updateLineNums(); }
  }

  
  private class QuitPromptOptionListener implements OptionListener<Boolean> {
    public void optionChanged(OptionEvent<Boolean> oce) { _promptBeforeQuit = oce.value.booleanValue(); }
  }
  
  
  private class RecentFilesOptionListener implements OptionListener<Integer> {
    public void optionChanged(OptionEvent<Integer> oce) {
      _recentFileManager.updateMax(oce.value.intValue());
      _recentFileManager.numberItems();
      _recentProjectManager.updateMax(oce.value.intValue());
      _recentProjectManager.numberItems();
    }
  }
  
  private class LastFocusListener extends FocusAdapter {
    public void focusGained(FocusEvent e) { 
      _lastFocusOwner = e.getComponent(); 

    }
  };
  
  
  
  public void setPopupLoc(Window popup) {
    Utilities.setPopupLoc(popup, (popup.getOwner() != null) ? popup.getOwner() : this);
  }
  
  
  DropTarget dropTarget = new DropTarget(this, this);
  
  
  private static DataFlavor uriListFlavor;
  static {
    try { uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String"); }
    catch(ClassNotFoundException cnfe) { uriListFlavor = null; }
  }
  
  
  public void dragEnter(DropTargetDragEvent dropTargetDragEvent)
  {
    dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
  }
  
  public void dragExit(DropTargetEvent dropTargetEvent) {}
  public void dragOver(DropTargetDragEvent dropTargetDragEvent) {}
  public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent){}
  
  
  public  void drop(DropTargetDropEvent dropTargetDropEvent) {
    assert EventQueue.isDispatchThread();
    try {
      Transferable tr = dropTargetDropEvent.getTransferable();
      if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
          ((uriListFlavor!=null) && (tr.isDataFlavorSupported(uriListFlavor)))) {
        dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        List<File> fileList;
        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          @SuppressWarnings("unchecked")
          List<File> data = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
          fileList = data;
        }
        else {
          
          String data = (String) tr.getTransferData(uriListFlavor);
          fileList = textURIListToFileList(data);
        }
        java.util.Iterator<File> iterator = fileList.iterator();
        List<File> filteredFileList = new java.util.ArrayList<File>();
        while (iterator.hasNext()) {
          File file = iterator.next();
          if (file.isFile() && (file.getName().endsWith(".java") || file.getName().endsWith(".dj0") || 
                                file.getName().endsWith(".dj1") || file.getName().endsWith(".dj2") || 
                                file.getName().endsWith(".dj0") || file.getName().endsWith(".txt"))) {
            filteredFileList.add(file);
          }
          else if (file.isFile() && file.getName().endsWith(OptionConstants.EXTPROCESS_FILE_EXTENSION)) {
            openExtProcessFile(file);
          }
        }
        final File[] fileArray = filteredFileList.toArray(new File[filteredFileList.size()]);
        FileOpenSelector fs = new FileOpenSelector() {
          public File[] getFiles() { return fileArray; }
        };
        open(fs);
        dropTargetDropEvent.getDropTargetContext().dropComplete(true);
      }
      else {
        dropTargetDropEvent.rejectDrop();
      }
    }
    catch(IOException ioe) {
      ioe.printStackTrace();
      dropTargetDropEvent.rejectDrop();
    }
    catch (UnsupportedFlavorException ufe) {
      ufe.printStackTrace();
      dropTargetDropEvent.rejectDrop();
    }    
  }
  
  
  public static void openExtProcessFile(File file) {
    try {
      XMLConfig xc = new XMLConfig(file);
      String name = xc.get("drjava/extprocess/name");
      ExecuteExternalDialog.addToMenu(name, xc.get("drjava/extprocess/cmdline"),
                                      xc.get("drjava/extprocess/workdir"), "");
      JOptionPane.showMessageDialog(null, "The installation was successful for:\n"+name,
                                    "Installation Successful", JOptionPane.INFORMATION_MESSAGE);
      
      
    }
    catch(XMLConfigException xce) {
      
      openExtProcessJarFile(file);
    }
  }
  
  
  public static void openExtProcessJarFile(File file) {
    try {
      JarFile jf = new JarFile(file);
      JarEntry je = jf.getJarEntry(EXTPROCESS_FILE_NAME_INSIDE_JAR);
      InputStream is = jf.getInputStream(je);
      XMLConfig xc = new XMLConfig(is);
      String name = xc.get("drjava/extprocess/name");
      ExecuteExternalDialog.addToMenu(name, xc.get("drjava/extprocess/cmdline"),
                                      xc.get("drjava/extprocess/workdir"), file.getAbsolutePath());
      JOptionPane.showMessageDialog(null, "The installation was successful for:\n"+name,
                                    "Installation Successful", JOptionPane.INFORMATION_MESSAGE);
      
      
      is.close();
      jf.close();
    }
    catch(IOException ioe) {  }
    catch(XMLConfigException xce) {  }
  }
  
  
  private static List<File> textURIListToFileList(String data) {
    List<File> list = new java.util.ArrayList<File>();
    java.util.StringTokenizer st = new java.util.StringTokenizer(data, "\r\n");
    while(st.hasMoreTokens()) {
      String s = st.nextToken();
      if (s.startsWith("#")) continue; 
      try {
        java.net.URI uri = new java.net.URI(s);
        File file = new File(uri);
        list.add(file);
      }
      catch (java.net.URISyntaxException e) {  }
      catch (IllegalArgumentException e) {  }
    }
    return list;
  }
  
  
  public void handleRemoteOpenFile(final File f, final int lineNo) {
    if (f.getName().endsWith(OptionConstants.EXTPROCESS_FILE_EXTENSION)) {
      openExtProcessFile(f);
    }
    else {
      FileOpenSelector openSelector = new FileOpenSelector() {
        public File[] getFiles() throws OperationCanceledException {
          return new File[] { f };
        }
      };
      String currFileName = f.getName();
      if (currFileName.endsWith(OptionConstants.PROJECT_FILE_EXTENSION) ||
          currFileName.endsWith(OptionConstants.PROJECT_FILE_EXTENSION2) ||
          currFileName.endsWith(OptionConstants.OLD_PROJECT_FILE_EXTENSION)) {
        openProject(openSelector);
      }
      else {
        open(openSelector);
        if (lineNo>=0) {
          final int l = lineNo;
          Utilities.invokeLater(new Runnable() { 
            public void run() { _jumpToLine(l); }
          });
        }
      }
    }
  }
  
  
  public void resetAutoImportDialogPosition() {
    _initAutoImportDialog();
    _autoImportDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_AUTOIMPORT_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_AUTOIMPORT_STATE, "default");
    }
  }
  
  
  private void _initAutoImportDialog() {
    if (_autoImportDialog == null) {
      _autoImportPackageCheckbox = new JCheckBox("Import Package");
      _autoImportPackageCheckbox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { _autoImportDialog.resetFocus(); }
      });
      PlatformFactory.ONLY.setMnemonic(_autoImportPackageCheckbox,'p');
      PredictiveInputFrame.InfoSupplier<JavaAPIListEntry> info = 
        new PredictiveInputFrame.InfoSupplier<JavaAPIListEntry>() {
        public String value(JavaAPIListEntry entry) { 
          return entry.getFullString();
        }
      };
      PredictiveInputFrame.CloseAction<JavaAPIListEntry> okAction = 
        new PredictiveInputFrame.CloseAction<JavaAPIListEntry>() {
        public String getName() { return "OK"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<JavaAPIListEntry> p) {
          String text;
          if (p.getItem() != null) { 
            text = p.getItem().getFullString();
          }
          else { 
            text = p.getText();
          }
          if (_autoImportPackageCheckbox.isSelected()) {
            int lastDot = text.lastIndexOf('.');
            if (lastDot > 0) text = text.substring(0, lastDot + 1) + "*";
          }
          final InteractionsModel im = _model.getInteractionsModel();
          
          String lastLine = im.removeLastFromHistory();
          
          String importLine = "import " + text + "; // auto-import";
          
          final String code = importLine + ((lastLine != null)  ?  ("\n" + lastLine)  : "");
          EventQueue.invokeLater(new Runnable() { 
            public void run() { 
              try {
                im.append(code, InteractionsDocument.DEFAULT_STYLE);
                im.interpretCurrentInteraction();
              }
              finally { hourglassOff(); }
            }
          });
          return null;
        }
      };
      PredictiveInputFrame.CloseAction<JavaAPIListEntry> cancelAction = 
        new PredictiveInputFrame.CloseAction<JavaAPIListEntry>() {
        public String getName() { return "Cancel"; }
        public KeyStroke getKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); }
        public String getToolTipText() { return null; }
        public Object value(PredictiveInputFrame<JavaAPIListEntry> p) {
          
          _model.getInteractionsModel().resetLastErrors();
          hourglassOff();
          return null;
        }
      };
      
      ArrayList<MatchingStrategy<JavaAPIListEntry>> strategies =
        new ArrayList<MatchingStrategy<JavaAPIListEntry>>();
      strategies.add(new FragmentStrategy<JavaAPIListEntry>());
      strategies.add(new PrefixStrategy<JavaAPIListEntry>());
      strategies.add(new RegExStrategy<JavaAPIListEntry>());
      List<PredictiveInputFrame.CloseAction<JavaAPIListEntry>> actions
        = new ArrayList<PredictiveInputFrame.CloseAction<JavaAPIListEntry>>();
      actions.add(okAction);
      actions.add(cancelAction);
      _autoImportDialog = 
        new PredictiveInputFrame<JavaAPIListEntry>(MainFrame.this, "Auto Import Class", false, true, info, strategies,
                                                   actions, 1, new JavaAPIListEntry("dummyImport", "dummyImport", null)) 
      {
        public void setOwnerEnabled(boolean b) { if (b) hourglassOff(); else hourglassOn(); }
        protected JComponent[] makeOptions() { return new JComponent[] { _autoImportPackageCheckbox }; }
      }; 
      
      if (DrJava.getConfig().getSetting(DIALOG_AUTOIMPORT_STORE_POSITION).booleanValue()) {
        _autoImportDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_AUTOIMPORT_STATE));
      }
      generateJavaAPISet();
    }
  }
  
  
  PredictiveInputFrame<JavaAPIListEntry> _autoImportDialog = null;
  JCheckBox _autoImportPackageCheckbox;
  
  
  private void _showAutoImportDialog(String s) {
    generateJavaAPISet();
    if (_javaAPISet == null) return;
    
    List<JavaAPIListEntry> autoImportList = new ArrayList<JavaAPIListEntry>(_javaAPISet);
    if (DrJava.getConfig().getSetting(DIALOG_COMPLETE_SCAN_CLASS_FILES).booleanValue() &&
        _autoImportClassSet.size() > 0) {
      autoImportList.addAll(_autoImportClassSet);
    }
    else {
      File projectRoot = _model.getProjectRoot();
      List<OpenDefinitionsDocument> docs = _model.getOpenDefinitionsDocuments();
      if (docs != null) {
        for (OpenDefinitionsDocument d: docs) {
          if (d.isUntitled()) continue;
          try {
            String rel = FileOps.stringMakeRelativeTo(d.getRawFile(), projectRoot);
            String full = rel.replace(File.separatorChar, '.');
            for (String ext: edu.rice.cs.drjava.model.compiler.CompilerModel.EXTENSIONS) {
              if (full.endsWith(ext)) {
                full = full.substring(0, full.lastIndexOf(ext));
                break;
              }
            }
            String simple = full;
            if (simple.lastIndexOf('.') >= 0) simple = simple.substring(simple.lastIndexOf('.') + 1);
            
            JavaAPIListEntry entry = new JavaAPIListEntry(simple, full, null);
            if (! autoImportList.contains(entry)) { autoImportList.add(entry); }
          }
          catch(IOException ioe) {  }
          catch(SecurityException se) {  }
        }
      }
    }
    PredictiveInputModel<JavaAPIListEntry> pim =
      new PredictiveInputModel<JavaAPIListEntry>(true, new PrefixStrategy<JavaAPIListEntry>(), autoImportList);
    pim.setMask(s);
    _initAutoImportDialog();
    _autoImportDialog.setModel(true, pim); 
    hourglassOn();
    _autoImportPackageCheckbox.setSelected(false);
    _autoImportDialog.setVisible(true);
  }
  
  
  private final Action _followFileAction = new AbstractAction("Follow File...") {
    public void actionPerformed(ActionEvent ae) { _followFile(); }
  };
  
  
  private void _followFile() {
    updateStatusField("Opening File for Following");
    try {      
      final File[] files = _openAnyFileSelector.getFiles();
      if (files == null) { return; }
      for (final File f: files) {
        if (f == null) continue;
        String end = f.getName();
        int lastIndex = end.lastIndexOf(File.separatorChar);
        if (lastIndex >= 0) end = end.substring(lastIndex+1);
        final LessPanel panel = new LessPanel(this, "Follow: "+end, f);
        _tabs.addLast(panel);
        panel.getMainPanel().addFocusListener(new FocusAdapter() {
          public void focusGained(FocusEvent e) { _lastFocusOwner = panel; }
        });
        panel.setVisible(true);
        showTab(panel, true);
        _tabbedPane.setSelectedComponent(panel);
        
        EventQueue.invokeLater(new Runnable() { public void run() { panel.requestFocusInWindow(); } });
      }
    }
    catch(OperationCanceledException oce) {  }
  }
  
  
  private final Action _executeExternalProcessAction = new AbstractAction("New External Process...") {
    public void actionPerformed(ActionEvent ae) { _executeExternalProcess(); }
  };
  
  
  private void _executeExternalProcess() { _executeExternalDialog.setVisible(true); }
  
  
  private volatile ExecuteExternalDialog _executeExternalDialog;
  
  
  private void initExecuteExternalProcessDialog() {
    if (DrJava.getConfig().getSetting(DIALOG_EXTERNALPROCESS_STORE_POSITION).booleanValue()) {
      _executeExternalDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_EXTERNALPROCESS_STATE));
    }
  }
  
  
  public void resetExecuteExternalProcessPosition() {
    _executeExternalDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_EXTERNALPROCESS_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_EXTERNALPROCESS_STATE, "default");
    }
  }
  
  
  private volatile EditExternalDialog _editExternalDialog;
  
  
  private void initEditExternalProcessDialog() {
    if (DrJava.getConfig().getSetting(DIALOG_EDITEXTERNALPROCESS_STORE_POSITION).booleanValue()) {
      _editExternalDialog.setFrameState(DrJava.getConfig().getSetting(DIALOG_EDITEXTERNALPROCESS_STATE));
    }
  }
  
  
  public void resetEditExternalProcessPosition() {
    _editExternalDialog.setFrameState("default");
    if (DrJava.getConfig().getSetting(DIALOG_EDITEXTERNALPROCESS_STORE_POSITION).booleanValue()) {
      DrJava.getConfig().setSetting(DIALOG_EDITEXTERNALPROCESS_STATE, "default");
    }
  }
  
  
  private final Action _editExternalProcessesAction = new AbstractAction("Edit...") {
    public void actionPerformed(ActionEvent ae) { _editExternalDialog.setVisible(true); }
  };
  
  
  public void installModalWindowAdapter(final Window w, final Runnable1<? super WindowEvent> toFrontAction,
                                        final Runnable1<? super WindowEvent> closeAction) {
    assert EventQueue.isDispatchThread();
    
    if (_modalWindowAdapters.containsKey(w)) { 
      return;
    }
    
    WindowAdapter wa;
    if (_modalWindowAdapterOwner == null) {
      
      _modalWindowAdapterOwner = w;
      
      wa = new WindowAdapter() {
        final HashSet<Window> trumpedBy = new HashSet<Window>(); 
        
        final WindowAdapter regainFront = new WindowAdapter() {
          public void windowClosed(WindowEvent we) {
            
            w.toFront();
            w.requestFocus();
            toFrontAction.run(we);
            
            Window o = we.getOppositeWindow();
            if (o!=null) {
              trumpedBy.remove(o);
              
              o.removeWindowListener(this);
            }
          }
        };
        final WindowAdapter regainFrontAfterNative = new WindowAdapter() {
          public void windowActivated(WindowEvent we) {
            
            MainFrame.this.removeWindowListener(this);
            _tabbedPanesFrame.removeWindowListener(this);
            _debugFrame.removeWindowListener(this);
            
            
            if (_modalWindowAdapterOwner==w) {
              w.toFront();
              w.requestFocus();
              toFrontAction.run(we);
            }
          }
        };
        public void toFront(WindowEvent we) {
          Window opposite = we.getOppositeWindow();
          if (opposite==null) {
            
            
            
            
            
            
            
            
            
            MainFrame.this.addWindowListener(regainFrontAfterNative);
            _tabbedPanesFrame.addWindowListener(regainFrontAfterNative);
            _debugFrame.addWindowListener(regainFrontAfterNative);
            return;
          }
          if (opposite instanceof Dialog) {
            Dialog d = (Dialog)opposite;
            if (d.isModal()) {
              
              if (!trumpedBy.contains(d)) {
                
                d.addWindowListener(regainFront);
                
                trumpedBy.add(d);
              }
              return; 
            }
          }
          we.getWindow().toFront();
          we.getWindow().requestFocus();
          toFrontAction.run(we);
        }
        public void windowDeactivated(WindowEvent we) { toFront(we); }
        public void windowIconified(WindowEvent we) { toFront(we); }
        public void windowLostFocus(WindowEvent we) { toFront(we); }
        public void windowClosing(WindowEvent we) { closeAction.run(we); }
      };
    }
    else {
      
      wa = new WindowAdapter() {
        public void windowDeactivated(WindowEvent we) { }
        public void windowIconified(WindowEvent we) { }
        public void windowLostFocus(WindowEvent we) { }
        public void windowClosing(WindowEvent we) { closeAction.run(we); }
      };
    }
    
    _modalWindowAdapters.put(w, wa);
    w.addWindowListener(wa);
    w.addWindowFocusListener(wa);
  }
  
  
  public  void removeModalWindowAdapter(Window w) {
    assert EventQueue.isDispatchThread();
    if (! _modalWindowAdapters.containsKey(w)) { 
      return;
    }
    w.removeWindowListener(_modalWindowAdapters.get(w));
    w.removeWindowFocusListener(_modalWindowAdapters.get(w));
    _modalWindowAdapterOwner = null;
    _modalWindowAdapters.remove(w);
  }
}

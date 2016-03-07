/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v7
 * (C) Copyright IBM Corp. 2014, 2014. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javax.swing.plaf.basic;

import java.awt.event.ActionEvent;
import java.awt.KeyboardFocusManager;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;                                                 //IBM-accessibility
import java.awt.KeyEventPostProcessor;                                          //IBM-accessibility
import java.awt.Window;                                                         //IBM-accessibility
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.plaf.*;
import sun.swing.DefaultLookup;
import sun.swing.UIAction;

/**
 * Basic implementation of RootPaneUI, there is one shared between all
 * JRootPane instances.
 *
 * @author Scott Violet
 * @since 1.3
 */
public class BasicRootPaneUI extends RootPaneUI implements
                  PropertyChangeListener {
    private static RootPaneUI rootPaneUI = new BasicRootPaneUI();

    private static final AltProcessor altProcessor = new AltProcessor();        //IBM-accessibility
                                                                                //IBM-accessibility
    public static ComponentUI createUI(JComponent c) {
        return rootPaneUI;
    }

    public void installUI(JComponent c) {
        installDefaults((JRootPane)c);
        installComponents((JRootPane)c);
        installListeners((JRootPane)c);
        installKeyboardActions((JRootPane)c);
    }


    public void uninstallUI(JComponent c) {
        uninstallDefaults((JRootPane)c);
        uninstallComponents((JRootPane)c);
        uninstallListeners((JRootPane)c);
        uninstallKeyboardActions((JRootPane)c);
    }

    protected void installDefaults(JRootPane c){
        LookAndFeel.installProperty(c, "opaque", Boolean.FALSE);
    }

    protected void installComponents(JRootPane root) {
    }

    protected void installListeners(JRootPane root) {
        root.addPropertyChangeListener(this);
        if (UIManager.get("altProcessor")==null){                               //IBM-accessibility
            KeyboardFocusManager.getCurrentKeyboardFocusManager().              //IBM-accessibility
                addKeyEventPostProcessor(altProcessor);                         //IBM-accessibility
            UIManager.put("altProcessor", altProcessor);                        //IBM-accessibility
        }                                                                       //IBM-accessibility
    }

    protected void installKeyboardActions(JRootPane root) {
        InputMap km = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, root);
        SwingUtilities.replaceUIInputMap(root,
                JComponent.WHEN_IN_FOCUSED_WINDOW, km);
        km = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                root);
        SwingUtilities.replaceUIInputMap(root,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, km);

        LazyActionMap.installLazyActionMap(root, BasicRootPaneUI.class,
                "RootPane.actionMap");
        updateDefaultButtonBindings(root);
    }

    protected void uninstallDefaults(JRootPane root) {
    }

    protected void uninstallComponents(JRootPane root) {
    }

    protected void uninstallListeners(JRootPane root) {
        root.removePropertyChangeListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().                  //IBM-accessibility
            removeKeyEventPostProcessor(altProcessor);                          //IBM-accessibility
        if(UIManager.get("altProcessor")==altProcessor){                        //IBM-accessibility
            UIManager.put("altProcessor", null);                                //IBM-accessibility
        }                                                                       //IBM-accessibility
    }

    protected void uninstallKeyboardActions(JRootPane root) {
        SwingUtilities.replaceUIInputMap(root, JComponent.
                                       WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(root, null);
    }

    InputMap getInputMap(int condition, JComponent c) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap)DefaultLookup.get(c, this,
                                       "RootPane.ancestorInputMap");
        }

        if (condition == JComponent.WHEN_IN_FOCUSED_WINDOW) {
            return createInputMap(condition, c);
        }
        return null;
    }

    ComponentInputMap createInputMap(int condition, JComponent c) {
        return new RootPaneInputMap(c);
    }

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.PRESS));
        map.put(new Actions(Actions.RELEASE));
        map.put(new Actions(Actions.POST_POPUP));
    }

    /**
     * Invoked when the default button property has changed. This reloads
     * the bindings from the defaults table with name
     * <code>RootPane.defaultButtonWindowKeyBindings</code>.
     */
    void updateDefaultButtonBindings(JRootPane root) {
        InputMap km = SwingUtilities.getUIInputMap(root, JComponent.
                                               WHEN_IN_FOCUSED_WINDOW);
        while (km != null && !(km instanceof RootPaneInputMap)) {
            km = km.getParent();
        }
        if (km != null) {
            km.clear();
            if (root.getDefaultButton() != null) {
                Object[] bindings = (Object[])DefaultLookup.get(root, this,
                           "RootPane.defaultButtonWindowKeyBindings");
                if (bindings != null) {
                    LookAndFeel.loadKeyBindings(km, bindings);
                }
            }
        }
    }

    /**
     * Invoked when a property changes on the root pane. If the event
     * indicates the <code>defaultButton</code> has changed, this will
     * reinstall the keyboard actions.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if(e.getPropertyName().equals("defaultButton")) {
            JRootPane rootpane = (JRootPane)e.getSource();
            updateDefaultButtonBindings(rootpane);
            if (rootpane.getClientProperty("temporaryDefaultButton") == null) {
                rootpane.putClientProperty("initialDefaultButton", e.getNewValue());
            }
        }
    }


    static class Actions extends UIAction {
        public static final String PRESS = "press";
        public static final String RELEASE = "release";
        public static final String POST_POPUP = "postPopup";

        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent evt) {
            JRootPane root = (JRootPane)evt.getSource();
            JButton owner = root.getDefaultButton();
            String key = getName();

            if (key == POST_POPUP) { // Action to post popup
                Component c = KeyboardFocusManager
                        .getCurrentKeyboardFocusManager()
                         .getFocusOwner();

                if(c instanceof JComponent) {
                    JComponent src = (JComponent) c;
                    JPopupMenu jpm = src.getComponentPopupMenu();
                    if(jpm != null) {
                        Point pt = src.getPopupLocation(null);
                        if(pt == null) {
                            Rectangle vis = src.getVisibleRect();
                            pt = new Point(vis.x+vis.width/2,
                                           vis.y+vis.height/2);
                        }
                        jpm.show(c, pt.x, pt.y);
                    }
                }
            }
            else if (owner != null
                     && SwingUtilities.getRootPane(owner) == root) {
                if (key == PRESS) {
                    owner.doClick(20);
                }
            }
        }

        public boolean isEnabled(Object sender) {
            String key = getName();
            if(key == POST_POPUP) {
                MenuElement[] elems = MenuSelectionManager
                        .defaultManager()
                        .getSelectedPath();
                if(elems != null && elems.length != 0) {
                    return false;
                    // We shall not interfere with already opened menu
                }

                Component c = KeyboardFocusManager
                       .getCurrentKeyboardFocusManager()
                        .getFocusOwner();
                if(c instanceof JComponent) {
                    JComponent src = (JComponent) c;
                    return src.getComponentPopupMenu() != null;
                }

                return false;
            }

            if (sender != null && sender instanceof JRootPane) {
                JButton owner = ((JRootPane)sender).getDefaultButton();
                return (owner != null && owner.getModel().isEnabled());
            }
            return true;
        }
    }

    private static class RootPaneInputMap extends ComponentInputMapUIResource {
        public RootPaneInputMap(JComponent c) {
            super(c);
        }
    }
                                                                                //IBM-accessibility
    private static class AltProcessor implements KeyEventPostProcessor {        //IBM-accessibility
        static boolean altKeyPressed = false;                                   //IBM-accessibility
        static boolean menuCanceledOnPress = false;                             //IBM-accessibility
        static JRootPane root = null;                                           //IBM-accessibility
        static Window winAncestor = null;                                       //IBM-accessibility
        private static JMenuBar mbar = null;                                    //IBM-accessibility
                                                                                //IBM-accessibility
        void altPressed(KeyEvent ev) {                                          //IBM-accessibility
            MenuSelectionManager msm =                                          //IBM-accessibility
                MenuSelectionManager.defaultManager();                          //IBM-accessibility
            MenuElement[] path = msm.getSelectedPath();                         //IBM-accessibility
            if (path.length > 0 && ! (path[0] instanceof ComboPopup)) {         //IBM-accessibility
                msm.clearSelectedPath();                                        //IBM-accessibility
                menuCanceledOnPress = true;                                     //IBM-accessibility
                ev.consume();                                                   //IBM-accessibility
            } else if(path.length > 0) { // We are in ComboBox                  //IBM-accessibility
                menuCanceledOnPress = false;                                    //IBM-accessibility
                ev.consume();                                                   //IBM-accessibility
            } else {                                                            //IBM-accessibility
                menuCanceledOnPress = false;                                    //IBM-accessibility
                JMenu menu = mbar != null ? mbar.getMenu(0) : null;             //IBM-accessibility
                if(menu != null) {                                              //IBM-accessibility
                    ev.consume();                                               //IBM-accessibility
                }                                                               //IBM-accessibility
            }                                                                   //IBM-accessibility
        }                                                                       //IBM-accessibility
                                                                                //IBM-accessibility
        void altReleased(KeyEvent ev) {                                         //IBM-accessibility
            if (menuCanceledOnPress) {                                          //IBM-accessibility
                return;                                                         //IBM-accessibility
            }                                                                   //IBM-accessibility
                                                                                //IBM-accessibility
            MenuSelectionManager msm =                                          //IBM-accessibility
                MenuSelectionManager.defaultManager();                          //IBM-accessibility
            if (msm.getSelectedPath().length == 0) {                            //IBM-accessibility
                // if no menu is active, we try activating the menubar          //IBM-accessibility
                JMenu menu = mbar != null ? mbar.getMenu(0) : null;             //IBM-accessibility
                                                                                //IBM-accessibility
                if (menu != null) {                                             //IBM-accessibility
                    MenuElement[] path = new MenuElement[3];                    //IBM-accessibility
                    JPopupMenu popup = menu.getPopupMenu();                     //IBM-accessibility
                    path[0] = mbar;                                             //IBM-accessibility
                    path[1] = menu;                                             //IBM-accessibility
                    path[2] = popup;                                            //IBM-accessibility
                    msm.setSelectedPath(path);                                  //IBM-accessibility
                }                                                               //IBM-accessibility
            }                                                                   //IBM-accessibility
        }                                                                       //IBM-accessibility
                                                                                //IBM-accessibility
        public boolean postProcessKeyEvent(KeyEvent ev) {                       //IBM-accessibility
            if (ev.getKeyCode() == KeyEvent.VK_ALT) {                           //IBM-accessibility
                root = SwingUtilities.getRootPane(ev.getComponent());           //IBM-accessibility
                if(root==null) return false;                                    //IBM-accessibility
                winAncestor = SwingUtilities.getWindowAncestor(root);           //IBM-accessibility
                mbar=getJMenuBar(root);                                         //IBM-accessibility
                                                                                //IBM-accessibility
                if (ev.getID() == KeyEvent.KEY_PRESSED) {                       //IBM-accessibility
                    if (!altKeyPressed) {                                       //IBM-accessibility
                        altPressed(ev);                                         //IBM-accessibility
                    }                                                           //IBM-accessibility
                    altKeyPressed = true;                                       //IBM-accessibility
                    return true;                                                //IBM-accessibility
                } else if (ev.getID() == KeyEvent.KEY_RELEASED) {               //IBM-accessibility
                    if (altKeyPressed) {                                        //IBM-accessibility
                        altReleased(ev);                                        //IBM-accessibility
                    } else {                                                    //IBM-accessibility
                        MenuSelectionManager msm =                              //IBM-accessibility
                            MenuSelectionManager.defaultManager();              //IBM-accessibility
                        MenuElement[] path = msm.getSelectedPath();             //IBM-accessibility
                    }                                                           //IBM-accessibility
                    altKeyPressed = false;                                      //IBM-accessibility
                }                                                               //IBM-accessibility
            } else {                                                            //IBM-accessibility
                altKeyPressed = false;                                          //IBM-accessibility
            }                                                                   //IBM-accessibility
            return false;                                                       //IBM-accessibility
        }                                                                       //IBM-accessibility
                                                                                //IBM-accessibility
        private JMenuBar getJMenuBar(JRootPane rp){                             //IBM-accessibility
            JMenuBar mb = rp != null ? rp.getJMenuBar() : null;                 //IBM-accessibility
            /*                                                                  //IBM-accessibility
             * If JMenuBar was not set on the content pane using setJMenuBar then //IBM-accessibility
             * search through the children of root for the closest JMenuBar.    //IBM-accessibility
             */                                                                 //IBM-accessibility
            if(rp!=null && mb==null){                                           //IBM-accessibility
                mb=getTopJMenuBar(rp);                                          //IBM-accessibility
                /*                                                              //IBM-accessibility
                 * If root is in a JInternalFrame which doesn't contain a       //IBM-accessibility
                 * JMenuBar then try searching the rootpane of its containing   //IBM-accessibility
                 * window.                                                      //IBM-accessibility
                 */                                                             //IBM-accessibility
                if((mb==null) && (rp.getParent() instanceof JInternalFrame)){   //IBM-accessibility
                    java.awt.Window parentWindow=SwingUtilities.                //IBM-accessibility
                        windowForComponent(rp.getParent());                     //IBM-accessibility
                    if((parentWindow!=null) && (parentWindow instanceof RootPaneContainer)){ //IBM-accessibility
                        rp=((RootPaneContainer)parentWindow).getRootPane();     //IBM-accessibility
                        if (rp != null) {                                       //IBM-accessibility
                            mb = rp.getJMenuBar();                              //IBM-accessibility
                            if (mb == null) {                                   //IBM-accessibility
                                mb = getTopJMenuBar(rp);                        //IBM-accessibility
                            }                                                   //IBM-accessibility
                        }                                                       //IBM-accessibility
                    }                                                           //IBM-accessibility
                }                                                               //IBM-accessibility
            }                                                                   //IBM-accessibility
            return mb;                                                          //IBM-accessibility
        }                                                                       //IBM-accessibility
                                                                                //IBM-accessibility
        private int topTier=0;                                                  //IBM-accessibility
        private JMenuBar topMBar=null;                                          //IBM-accessibility
                                                                                //IBM-accessibility
        /*                                                                      //IBM-accessibility
         * Returns highest JMenuBar from this JRootPane's content pane's        //IBM-accessibility
         * descendants.                                                         //IBM-accessibility
         */                                                                     //IBM-accessibility
        private JMenuBar getTopJMenuBar(JRootPane rp){                          //IBM-accessibility
            topTier=0;                                                          //IBM-accessibility
            topMBar=null;                                                       //IBM-accessibility
            if(rp!=null){                                                       //IBM-accessibility
                java.awt.Container cp=rp.getContentPane();                      //IBM-accessibility
                searchChildren(cp,0);                                           //IBM-accessibility
            }                                                                   //IBM-accessibility
            return topMBar;                                                     //IBM-accessibility
        }                                                                       //IBM-accessibility
        /*                                                                      //IBM-accessibility
         * Searches down through descendants of Container c looking for         //IBM-accessibility
         * the highest JMenuBar within the heirarchy. Returns true if one       //IBM-accessibility
         * of c's direct children is a JMenuBar.                                //IBM-accessibility
         */                                                                     //IBM-accessibility
        private boolean searchChildren(java.awt.Container c, int tier){         //IBM-accessibility
            if(c!=null){                                                        //IBM-accessibility
                tier++;                                                         //IBM-accessibility
                if((topTier!=0) && (tier>=topTier)){                            //IBM-accessibility
                    return false;                                               //IBM-accessibility
                }                                                               //IBM-accessibility
                Component[] children=c.getComponents();                         //IBM-accessibility
                for(int i=0;i<children.length;i++){                             //IBM-accessibility
                    Component child=children[i];                                //IBM-accessibility
                    if(child instanceof JMenuBar){                              //IBM-accessibility
                        if((topTier==0) ||(tier<topTier)){                      //IBM-accessibility
                            topTier=tier;                                       //IBM-accessibility
                            topMBar=(JMenuBar)child;                            //IBM-accessibility
                            return true;                                        //IBM-accessibility
                        }                                                       //IBM-accessibility
                    } else if(child instanceof java.awt.Container){             //IBM-accessibility
                        if(searchChildren((java.awt.Container)child,tier)){     //IBM-accessibility
                            break;                                              //IBM-accessibility
                        }                                                       //IBM-accessibility
                    }                                                           //IBM-accessibility
                }                                                               //IBM-accessibility
            }                                                                   //IBM-accessibility
            return false;                                                       //IBM-accessibility
        }                                                                       //IBM-accessibility
    }                                                                           //IBM-accessibility
}
//IBM-accessibility

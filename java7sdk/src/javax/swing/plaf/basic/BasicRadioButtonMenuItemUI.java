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
 * Copyright (c) 1997, 2001, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import javax.swing.border.*;

/**
 * BasicRadioButtonMenuItem implementation
 *
 * @author Georges Saab
 * @author David Karlton
 */
public class BasicRadioButtonMenuItemUI extends BasicMenuItemUI
{
    public static ComponentUI createUI(JComponent b) {
        return new BasicRadioButtonMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "RadioButtonMenuItem";
    }

    public void processMouseEvent(JMenuItem item,MouseEvent e,MenuElement path[],MenuSelectionManager manager) {
        Point p = e.getPoint();
        if(p.x >= 0 && p.x < item.getWidth() &&
           p.y >= 0 && p.y < item.getHeight()) {
            if(e.getID() == MouseEvent.MOUSE_RELEASED) {
                manager.clearSelectedPath();
                item.doClick(0);
                item.setArmed(false);
            } else
                manager.setSelectedPath(path);
        } else if(item.getModel().isArmed()) {
            MenuElement newPath[] = new MenuElement[path.length-1];
            int i,c;
            for(i=0,c=path.length-1;i<c;i++)
                newPath[i] = path[i];
            manager.setSelectedPath(newPath);
        }
    }
}

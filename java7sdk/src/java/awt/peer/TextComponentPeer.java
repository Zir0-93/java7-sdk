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
 * Copyright (c) 1995, 2007, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.TextComponent;
import java.awt.im.InputMethodRequests;

/**
 * The peer interface for {@link TextComponent}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface TextComponentPeer extends ComponentPeer {

    /**
     * Sets if the text component should be editable or not.
     *
     * @param editable {@code true} for editable text components,
     *        {@code false} for non-editable text components
     *
     * @see TextComponent#setEditable(boolean)
     */
    void setEditable(boolean editable);

    /**
     * Returns the current content of the text component.
     *
     * @return the current content of the text component
     *
     * @see TextComponent#getText()
     */
    String getText();

    /**
     * Sets the content for the text component.
     *
     * @param l the content to set
     *
     * @see TextComponent#setText(String)
     */
    void setText(String l);

    /**
     * Returns the start index of the current selection.
     *
     * @return the start index of the current selection
     *
     * @see TextComponent#getSelectionStart()
     */
    int getSelectionStart();

    /**
     * Returns the end index of the current selection.
     *
     * @return the end index of the current selection
     *
     * @see TextComponent#getSelectionEnd()
     */
    int getSelectionEnd();

    /**
     * Selects an area of the text component.
     *
     * @param selStart the start index of the new selection
     * @param selEnd the end index of the new selection
     *
     * @see TextComponent#select(int, int)
     */
    void select(int selStart, int selEnd);

    /**
     * Sets the caret position of the text component.
     *
     * @param pos the caret position to set
     *
     * @see TextComponent#setCaretPosition(int)
     */
    void setCaretPosition(int pos);

    /**
     * Returns the current caret position.
     *
     * @return the current caret position
     *
     * @see TextComponent#getCaretPosition()
     */
    int getCaretPosition();

    /**
     * Returns the input method requests.
     *
     * @return the input method requests
     */
    InputMethodRequests getInputMethodRequests();
}
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
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.ws;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
/**
 * Used to annotate methods in the Service Endpoint Interface with the response
 * wrapper bean to be used at runtime. The default value of the <code>localName</code> is
 * the <code>operationName</code> as defined in <code>WebMethod</code> annotation appended with
 * <code>Response</code> and the <code>targetNamespace</code> is the target namespace of the SEI.
 * <p> When starting from Java this annotation is used resolve
 * overloading conflicts in document literal mode. Only the <code>className</code>
 * is required in this case.
 *
 *  @since JAX-WS 2.0
**/

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseWrapper {

    /**
     * Element's local name.
     */
    public String localName() default "";

    /**
     * Element's namespace name.
     */
    public String targetNamespace() default "";

    /**
     * Response wrapper bean name.
     */
    public String className() default "";

    /**
     * wsdl:part name for the wrapper part
     *
     * @since JAX-WS 2.2
     */
    public String partName() default "";

}

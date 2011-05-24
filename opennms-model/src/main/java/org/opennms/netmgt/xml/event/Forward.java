/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.1.2.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.opennms.netmgt.xml.event;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The forwarding information for this event - state
 *  determines if event is forwarded, mechanism determines how
 * event is
 *  forwarded .
 * 
 * @version $Revision$ $Date$
 */

@XmlRootElement(name="forward")
@XmlAccessorType(XmlAccessType.FIELD)
public class Forward implements Serializable {
	private static final long serialVersionUID = -4795441559557516585L;

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

	/**
     * internal content storage
     */
	@XmlValue
    private java.lang.String _content = "";

    /**
     * Field _state.
     */
	@XmlAttribute(name="state")
    private java.lang.String _state = "off";

    /**
     * Field _mechanism.
     */
	@XmlAttribute(name="mechanism")
    private java.lang.String _mechanism = "snmpudp";


      //----------------/
     //- Constructors -/
    //----------------/

    public Forward() {
        super();
        setContent("");
        setState("off");
        setMechanism("snmpudp");
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'content'. The field 'content'
     * has the following description: internal content storage
     * 
     * @return the value of field 'Content'.
     */
    public java.lang.String getContent(
    ) {
        return this._content;
    }

    /**
     * Returns the value of field 'mechanism'.
     * 
     * @return the value of field 'Mechanism'.
     */
    public java.lang.String getMechanism(
    ) {
        return this._mechanism;
    }

    /**
     * Returns the value of field 'state'.
     * 
     * @return the value of field 'State'.
     */
    public java.lang.String getState(
    ) {
        return this._state;
    }

    /**
     * Sets the value of field 'content'. The field 'content' has
     * the following description: internal content storage
     * 
     * @param content the value of field 'content'.
     */
    public void setContent(
            final java.lang.String content) {
        this._content = content;
    }

    /**
     * Sets the value of field 'mechanism'.
     * 
     * @param mechanism the value of field 'mechanism'.
     */
    public void setMechanism(
            final java.lang.String mechanism) {
        this._mechanism = mechanism;
    }

    /**
     * Sets the value of field 'state'.
     * 
     * @param state the value of field 'state'.
     */
    public void setState(
            final java.lang.String state) {
        this._state = state;
    }

    public String toString() {
    	return new ToStringBuilder(this)
    		.append("content", _content)
    		.append("state", _state)
    		.append("mechanism", _mechanism)
    		.toString();
    }
}
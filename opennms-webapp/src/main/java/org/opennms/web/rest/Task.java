package org.opennms.web.rest;

import javax.xml.bind.annotation.*;

@XmlRootElement( name="task" )
@XmlAccessorType( XmlAccessType.FIELD )
public class Task
{

 @XmlAttribute(name = "href")
 private String link;

 @XmlElement( required = true )
 private Integer id;

 @XmlTransient
 private String correlationId;


 @XmlTransient
 private String ejbName;
 
 public Task(){}
 
 public Integer getId()
 {
     return id;
 }
 public void setId( Integer id )
 {
     this.id = id;
 }
 public String getCorrelationId()
 {
     return correlationId;
 }
 public void setCorrelationId( String correlationId )
 {
     this.correlationId = correlationId;
 }
 public String getEjbName()
 {
         return ejbName;
 }
 public void setEjbName( String ejbName )
 {
         this.ejbName = ejbName;
 }

 public String getLink() {
     return link;
 }

 public void setLink(String link) {
     this.link = link;
 }
}
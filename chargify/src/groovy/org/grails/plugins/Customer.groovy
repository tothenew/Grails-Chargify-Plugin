package org.grails.plugins

import groovy.xml.MarkupBuilder;

class Customer {
    // required
    String firstName
    String lastName
    String emailAddress
    String referenceId
    String companyName


    public String getXml() {
        StringWriter xmlWriter = new StringWriter()
        MarkupBuilder xmlBuilder = new MarkupBuilder(xmlWriter)
        xmlBuilder.customer() {
            first_name(firstName)
            last_name(lastName)
            email(emailAddress)
            organization(companyName)
            reference(referenceId)
        }
        return xmlWriter.toString()
    }

    boolean isValid(){
        return (firstName && lastName && emailAddress)
    }
}

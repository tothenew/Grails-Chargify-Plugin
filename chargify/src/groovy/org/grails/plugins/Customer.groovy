package org.grails.plugins

import groovy.xml.MarkupBuilder
import java.text.ParseException;

class Customer {
    // required
    String firstName
    String lastName
    String emailAddress
    String referenceId
    String companyName
    String address
    String address2
    String city
    String state
    String country
    String zip
    String phone
    Date createdAt
    Date updatedAt
    static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss"


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

    public static Customer getCustomerFromXml(String xml){
        Customer customer

        def chargifyCustomer = new XmlParser().parseText(xml);
        customer = new Customer()
        customer.firstName = chargifyCustomer.first_name.text()
        customer.lastName = chargifyCustomer.last_name.text()
        customer.emailAddress = chargifyCustomer.email.text()
        customer.address = chargifyCustomer.address.text()
        customer.address2 = chargifyCustomer.address_2.text()
        customer.city = chargifyCustomer.city.text()
        customer.state = chargifyCustomer.state.text()
        customer.country = chargifyCustomer.country.text()
        customer.zip = chargifyCustomer.zip.text()
        customer.phone = chargifyCustomer.phone.text()
        customer.companyName = chargifyCustomer.organization.text()
        customer.referenceId = chargifyCustomer.reference.text()

        try {
            customer.createdAt = Date.parse(dateFormat, chargifyCustomer.created_at.text())
            customer.updatedAt = Date.parse(dateFormat, chargifyCustomer.updated_at.text())
        } catch (ParseException e) {
            // Unable to parse date
        }
        return customer
    }

}

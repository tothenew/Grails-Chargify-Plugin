package org.grails.plugins

import groovy.xml.MarkupBuilder

class Subscription {

    public static final String UPDATE_CREDIT_CARD = 'update_cc'
    public static final String UPDATE_PRODUCT = 'update_product'
    public static final String UNSUBSCRIBE = 'unsubscribe'

    String action
    String id
    String customMessage

    String customerRef

    String status  //<state>active</state>
    String totalRevenue  //<balance_in_cents type="integer">0</balance_in_cents>
    Date signedUpDate    //created_at type="datetime">2010-06-23T08:27:38-04:00</created_at>
    Date trialExpiredDate //<trial_ended_at type="datetime" nil="true"></trial_ended_at>
    Date activatedDate  //<activated_at type="datetime">2010-06-23T08:27:38-04:00</activated_at>
    Date nextChangeDate //<next_assessment_at type="datetime">2010-07-23T08:27:38-04:00</next_assessment_at>

    // product specific fields.
    String productHandle //<product><handle>make-it-rain-pay-as-you-go</handle></product>
    String productName  //<product><name>make-it-rain-pay-as-you-go</handle></name>
    String productInfo // <product><description>20 Raindrops Included Additional Raindrops $0.50 each</description></product>
    String productPrice //$0.00 every 1 month - <product><price_in_cents type="integer">0</price_in_cents><interval type="integer">1</interval><interval_unit>month</interval_unit></product>

    // Credit Card specific fields.
    String ccNumber // <credit_card><masked_card_number>XXXX-XXXX-XXXX-1</masked_card_number></credit_card>
    String ccCVV // <credit_card><masked_card_number>XXXX-XXXX-XXXX-1</masked_card_number></credit_card>
    String ccExpiryMonth // <credit_card><expiration_month type="integer">5</expiration_month></credit_card>
    String ccExpiryYear //<credit_card><expiration_year type="integer">2012</expiration_year></credit_card>
    String ccType // <credit_card><card_type>bogus</card_type></credit_card>
    String ccFirstName  // <credit_card><first_name>Salil</first_name></credit_card>
    String ccLastName   // <credit_card><last_name>Kalia</last_name></credit_card>

    /*
    <billing_address nil="true"></billing_address>
    <billing_address_2 nil="true"></billing_address_2>
    <billing_city nil="true"></billing_city>
    <billing_country nil="true"></billing_country>
    <billing_state nil="true"></billing_state>
    <billing_zip nil="true"></billing_zip>
     */

    String notes
    String options

    public String getXml() {
        StringWriter xmlWriter = new StringWriter()
        MarkupBuilder xmlBuilder = new MarkupBuilder(xmlWriter)
        if (action == UPDATE_CREDIT_CARD) {
            processXmlBuilderToUpdateCC(xmlBuilder)
        } else if (action == UPDATE_PRODUCT) {
            processXmlBuilderToupdateProduct(xmlBuilder)
        } else if (action == UNSUBSCRIBE) {
            processXmlBuilderToUnsubscribe(xmlBuilder)
        } else {
            xmlBuilder.subscription() {
                product_handle(productHandle)
                if (!id) {
                    customer_reference(customerRef)
                }
                credit_card_attributes {
                    full_number(ccNumber)
                    expiration_month(ccExpiryMonth)
                    expiration_year(ccExpiryYear)
                    if (ccFirstName && ccLastName) {
                        first_name(ccFirstName)
                        last_name(ccLastName)
                    }
                }
            }
        }
        return xmlWriter.toString()
    }

    public void processXmlBuilderToUpdateCC(MarkupBuilder xmlBuilder) {
        xmlBuilder.subscription() {
            credit_card_attributes {
                full_number(ccNumber)
                expiration_month(ccExpiryMonth)
                expiration_year(ccExpiryYear)
                if (ccFirstName && ccLastName) {
                    first_name(ccFirstName)
                    last_name(ccLastName)
                }
            }
        }
    }

    public void processXmlBuilderToupdateProduct(MarkupBuilder xmlBuilder) {
        xmlBuilder.subscription() {
            product_handle(productHandle)
        }
    }

    public void processXmlBuilderToUnsubscribe(MarkupBuilder xmlBuilder) {
        xmlBuilder.subscription() {
            cancellation_message(customMessage)
        }
    }

    public static Subscription getTestCCSubscription() {
        Date date = new Date()
        int currentYear = date.year + 1900
        return new Subscription(ccNumber: '1', ccExpiryMonth: '5', ccExpiryYear: currentYear + 2)
    }

    public static Subscription getFromXml(String subsciptionXml) {
        def resp = new XmlParser().parseText(subsciptionXml);
        Subscription subscription = new Subscription()
        subscription.id = resp.id?.text();
        subscription.status = resp.state?.text()
        subscription.totalRevenue = resp.balance_in_cents?.text()
        String dateStr = resp.activated_at?.text()
        if (dateStr) {
            subscription.activatedDate = Date.parse('yyyy-MM-dd', dateStr.split('T')[0])
        }
        dateStr = resp.created_at?.text()
        if (dateStr) {
            subscription.signedUpDate = Date.parse('yyyy-MM-dd', dateStr.split('T')[0])
        }
        dateStr = resp.trial_ended_at?.text()
        if (dateStr) {
            subscription.trialExpiredDate = Date.parse('yyyy-MM-dd', dateStr.split('T')[0])
        }
        dateStr = resp.next_assessment_at?.text()
        if (dateStr) {
            subscription.nextChangeDate = Date.parse('yyyy-MM-dd', dateStr.split('T')[0])
        }

        // SUBSCRIPTION BASIC INFO
        subscription.productHandle = resp.product.handle.text()
        subscription.productName = resp.product.name.text()
        subscription.productInfo = resp.product.name.text()
        subscription.productPrice = "price: \$${resp.product.price_in_cents.text().toDouble() / 100} every ${resp.product.interval.text()} ${resp.product.interval_unit.text()}"

        // CREDIT CARD INFO
        subscription.ccNumber = resp.credit_card.masked_card_number.text()
        subscription.ccExpiryMonth = resp.credit_card.expiration_month.text()
        subscription.ccExpiryYear = resp.credit_card.expiration_year.text()
        subscription.ccType = resp.credit_card.card_type.text()
        subscription.ccFirstName = resp.credit_card.first_name.text()
        subscription.ccLastName = resp.credit_card.last_name.text()

        // customer info
        subscription.customerRef = resp.customer.reference.text()

        return subscription

    }

    public Date getExpiryDate() {
        if (ccExpiryMonth && ccExpiryYear) {
            return Date.parse('yyyy-MM-dd', "${ccExpiryYear}-${ccExpiryMonth}-01")
        }
        return null
    }

    public void setExpiryDate(int month, int year) {
        ccExpiryMonth = month
        ccExpiryYear = year
    }

    public String toString() {
        return "customer:${ccFirstName} - ref:${customerRef} - productHandle:${productHandle}"
    }

    public boolean isValid() {
        boolean isValid = false
        if (action == UPDATE_PRODUCT) {
            isValid = (productHandle != null)
        } else if (action == UPDATE_CREDIT_CARD) {
            isValid = (ccFirstName && ccLastName && ccNumber && ccExpiryMonth && ccExpiryYear)
        } else if (action == UNSUBSCRIBE) {
            isValid = (customMessage != null)
        } else {
            isValid = (customerRef && productHandle && ccFirstName && ccLastName && ccNumber && ccExpiryMonth && ccExpiryYear)
        }
        return isValid
    }

}
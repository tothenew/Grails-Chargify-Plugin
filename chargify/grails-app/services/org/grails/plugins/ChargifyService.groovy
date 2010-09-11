package org.grails.plugins

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

class ChargifyService {

    public static final int CHARGIFY_RESPONSE_CODE_OK = 201;
    public static final int HTTP_RESPONSE_CODE_OK = 200;

    public static final String customersUrl = "https://${CH.config.chargify.subdomain}.chargify.com/customers.xml"
    public static final String subscriptionsUrl = "https://${CH.config.chargify.subdomain}.chargify.com/subscriptions.xml"
    public static final String transactionsUrl = "https://${ CH.config.chargify.subdomain}.chargify.com/subscriptions"
    public static final String productsUrl = "https://${CH.config.chargify.subdomain}.chargify.com/products.xml"
    public static final String authKey = CH.config.chargify.authkey + CH.config.chargify.authkeySuffix

    boolean transactional = false

       private HttpURLConnection getChargifyConnection(String urlStr, String methodType) {
        URL url = new URL(urlStr)
        HttpURLConnection conn = url.openConnection()
        String encoded = new sun.misc.BASE64Encoder().encode((authKey)?.getBytes());
        conn.setRequestMethod(methodType)
        conn.setRequestProperty("Content-Type", "application/xml")
        conn.setRequestProperty("Authorization", "Basic ${encoded}");
        conn.doOutput = true
        return conn
    }

    public Customer createCustomerInChargify(Customer customer) {
        Customer retCustomer = null
        if (customer.isValid()) {
            HttpURLConnection conn = getChargifyConnection(customersUrl, "POST")
            String customerRequestXml = customer.getXml()
            def writer = new OutputStreamWriter(conn.outputStream)
            writer.write(customerRequestXml)
            writer.flush()
            writer.close()
            conn.connect()
            int responseCode = conn.getResponseCode()
            log.debug("response code : ${responseCode}")

            if (responseCode == CHARGIFY_RESPONSE_CODE_OK) {
                retCustomer = Customer.getCustomerFromXml(conn.content?.text)
            } else {
                log.error("Customer not created. ResponseCode: ${responseCode}: ResponseMessage: ${conn.responseMessage}")
            }
            conn.disconnect()
        }else{
            log.error("INVALID Customer information (REQUIRED Fields: firstName, lastName, email)")
        }
        return retCustomer
    }

    List<Product> getProductsFromChargify() {
        List<Product> products = []
        HttpURLConnection conn = getChargifyConnection(productsUrl, "GET")
        conn.connect()
        int responseCode = conn.getResponseCode()
        log.debug("Gettings products from chargify : response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            products = Product.getListFromXml(conn.content?.text)
            log.debug("number of products retrieved: ${products.size()}")
        }
        conn.disconnect()
        return products
    }

    public List<Transaction> getChargifyTransactions(String subscriptionId) {
        List<Transaction> transactions = []
        if (subscriptionId) {
            String urlStr = "${transactionsUrl}/${subscriptionId}/transactions.json"//"https://makeitrain.chargify.com/subscriptions/${subscriptionId}/transactions.json"
            HttpURLConnection conn = getChargifyConnection(urlStr, "GET")
            conn.connect()
            int responseCode = conn.getResponseCode()
            log.debug("response code : ${responseCode}")
            if (responseCode == HTTP_RESPONSE_CODE_OK) {
              String jsonResponse = conn.content?.text
              transactions << Transaction.getTransactionsFromJson(jsonResponse)
              log.debug("Getting transaction from chargify for Subscription id :${subscriptionId} ")
            }
            conn.disconnect()
        }
        return transactions?.flatten()
    }

    public String createSubscription(Subscription subscription)  {
        String subscriptionId = null
        
        HttpURLConnection conn = getChargifyConnection(subscriptionsUrl, "POST")
        String subscriptionRequestXml = subscription.getXml()
        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        log.debug("response code : ${responseCode}")

        if (responseCode == CHARGIFY_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            def resp = new XmlParser().parseText(responseXml)
            subscriptionId = resp.id.text()
            log.debug("subsciptionId: ${subscriptionId}")
        } else {
            log.error("Subscription Failure. ResponseCode: ${responseCode}: ResponseMessage: ${conn.responseMessage}")
            subscriptionId = null
            conn.disconnect()
            // this response code should be 422 - unprocessable information.
        }
        conn?.disconnect()
        return subscriptionId
    }

    public Subscription getSubscriptionByIdFromChargify(String subscriptionId) {
        Subscription subscription = null
        String urlStr = subscriptionsUrl
        urlStr = urlStr.replaceFirst(".xml", "/${subscriptionId}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, "GET")
        conn.connect()
        int responseCode = conn.getResponseCode()
        log.debug("response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            log.debug("Getting subscription from chargify : subscription: ${subscription}")
        }
        conn.disconnect()
        return subscription
    }

    public Subscription updateCreditCard(Subscription subscription) {
        String urlStr = subscriptionsUrl
        urlStr = urlStr.replaceFirst(".xml", "/${subscription.id}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, "PUT")
        String subscriptionRequestXml = subscription.getXml()
        log.debug "xml request: ${subscriptionRequestXml}"
        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        log.debug("Updating credit card information : response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            log.debug("Updating credit card information: subscription: ${subscription}")
        } else {
            subscription = null
            String responseMessage = conn.getResponseMessage()
            // this response code should be 422 - unprocessable information.
            conn.disconnect()
        }
        conn.disconnect()
        return subscription
    }

    public Subscription cancelSubscription(String subscriptionId, String message) {
        String urlStr = subscriptionsUrl
        urlStr = urlStr.replaceFirst(".xml", "/${subscriptionId}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, "DELETE")
        Subscription subscription = new Subscription(action: Subscription.UNSUBSCRIBE, customMessage: message)
        String subscriptionRequestXml = subscription.getXml()
        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        log.debug("Cancel Subscription: response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            log.debug("Cancel Subscription: subscription: ${subscription}")
        } else {
            subscription = null
        }
        conn.disconnect()
        return subscription
    }
}

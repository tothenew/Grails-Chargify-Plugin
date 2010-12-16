package org.grails.plugins.chargify;

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import groovy.xml.MarkupBuilder

class ChargifyService {

    public static final int CHARGIFY_RESPONSE_CODE_OK = 201;
    public static final int HTTP_RESPONSE_CODE_OK = 200;

    public static final String customersUrl = "https://${CH.config.chargify.subdomain}.chargify.com/customers.xml"
    public static final String customerLookupUrl = "https://${CH.config.chargify.subdomain}.chargify.com/customers/lookup.xml"
    public static final String subscriptionsUrl = "https://${CH.config.chargify.subdomain}.chargify.com/subscriptions.xml"
    public static final String transactionsUrl = "https://${ CH.config.chargify.subdomain}.chargify.com/subscriptions"
    public static final String productsUrl = "https://${CH.config.chargify.subdomain}.chargify.com/products.xml"
    public static final String authKey = CH.config.chargify.authkey + CH.config.chargify.authkeySuffix
    public static final String POST = "POST"
    public static final String GET = "GET"
    public static final String PUT = "PUT"
    public static final String DELETE = "DELETE"

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

    Response<List<Product>> getProducts() {
        List<Product> products = []
        def response = new Response<List<Product>>()
        HttpURLConnection conn = getChargifyConnection(productsUrl, GET)
        conn.connect()
        int responseCode = conn.getResponseCode()
        log.debug("Gettings products from chargify : response code : ${responseCode}")
        response.status = responseCode
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            response.message = "number of products retrieved: ${products.size()}"
            products = Product.getListFromXml(conn.content?.text)
            log.debug("number of products retrieved: ${products.size()}")
        }
        else{
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")       
            response.message = err
        }
        conn.disconnect()
        response.entity = products
        return response
    }

    public Response<Customer> createCustomer(Customer customer) {
        Customer retCustomer = null
        Response<Customer> response = new Response<Customer>()
        if (customer.isValid()) {
            HttpURLConnection conn = getChargifyConnection(customersUrl, POST)
            String customerRequestXml = customer.getXml()
            def writer = new OutputStreamWriter(conn.outputStream)
            writer.write(customerRequestXml)
            writer.flush()
            writer.close()
            conn.connect()
            int responseCode = conn.getResponseCode()
            log.debug("response code : ${responseCode}")
            response.status = responseCode
            if (responseCode == CHARGIFY_RESPONSE_CODE_OK) {
                retCustomer = Customer.getCustomerFromXml(conn.content?.text)
                response.entity = retCustomer
            } else {
                log.error("Customer not created. ResponseCode: ${responseCode}: ResponseMessage: ${conn.responseMessage}")
                def err = new XmlSlurper().parse(conn.getErrorStream())
                log.error("Errors : ${err}")
                response.message = err
            }
            conn.disconnect()
        }else{
            log.error("INVALID Customer information (REQUIRED Fields: firstName, lastName, email)")
            response.status = "Error"
            response.message ="INVALID Customer information (REQUIRED Fields: firstName, lastName, email)"
        }
        return response
    }

    public Response<String> createSubscription(Subscription subscription)  {
        String subscriptionId = null
        Response<String> response = new Response<String>()
        HttpURLConnection conn = getChargifyConnection(subscriptionsUrl, POST)
        String subscriptionRequestXml = subscription.getXml()
        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        log.debug("response code : ${responseCode}")
        response.status = responseCode
        if (responseCode == CHARGIFY_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            def resp = new XmlParser().parseText(responseXml)
            subscriptionId = resp.id.text()
            log.debug("subsciptionId: ${subscriptionId}")
            response.message = "Success"
            response.entity = subscriptionId
        } else {
            log.error("Subscription Failure. ResponseCode: ${responseCode}: ResponseMessage: ${conn.responseMessage}")
            subscriptionId = null
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")
            response.message = err
            // this response code should be 422 - unprocessable information.
        }
        conn?.disconnect()
        return response
    }

    public Response<Subscription> getSubscriptionById(String subscriptionId) {
        Subscription subscription = null
        Response<Subscription> response = new Response<Subscription>()
        String urlStr = subscriptionsUrl
        urlStr = urlStr.replaceFirst(".xml", "/${subscriptionId}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, GET)
        conn.connect()
        int responseCode = conn.getResponseCode()
        response.status = responseCode
        log.debug("response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            log.debug("Getting subscription from chargify : subscription: ${subscription}")
            response.entity = subscription
            response.message = "success"
        }
        else{
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")
            response.message = err
        }
        conn.disconnect()
        return response
    }

    public Response<Subscription> cancelSubscription(String subscriptionId, String message) {
        String urlStr = subscriptionsUrl
        Response<Subscription> response = new Response<Subscription>()
        urlStr = urlStr.replaceFirst(".xml", "/${subscriptionId}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, DELETE)
        Subscription subscription = new Subscription(action: Subscription.UNSUBSCRIBE, customMessage: message)
        String subscriptionRequestXml = subscription.getXml()
        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        response.status = responseCode
        log.debug("Cancel Subscription: response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {

            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            log.debug("Cancel Subscription: subscription: ${subscription}")
            response.message = "Cancel Subscription: subscription: ${subscription}"
            response.entity = subscription
        } else {
            subscription = null
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")
            response.message = err
        }
        conn.disconnect()
        return response
    }

    public Response<List<Transaction>> getTransactionsBySubscriptionId(String subscriptionId) {
        List<Transaction> transactions = []
        Response<List<Transaction>> response = new Response<List<Transaction>>()
        if (subscriptionId) {
            String urlStr = "${transactionsUrl}/${subscriptionId}/transactions.json"
            HttpURLConnection conn = getChargifyConnection(urlStr, GET)
            conn.connect()
            int responseCode = conn.getResponseCode()
            response.status = responseCode
            log.debug("response code : ${responseCode}")
            if (responseCode == HTTP_RESPONSE_CODE_OK) {
              String jsonData = conn.content?.text
              transactions << Transaction.getTransactionsFromJson(jsonData)
              log.debug("Getting transaction from chargify for Subscription id :${subscriptionId} ")
              response.message = "Getting transaction from chargify for Subscription id :${subscriptionId}"
              response.entity = transactions?.flatten()
            }
            else{
                 def err = new XmlSlurper().parse(conn.getErrorStream())
                 log.error("Errors : ${err}")
                 response.message = err
            }
            conn.disconnect()
        }
        return response
    }

    public Response<Subscription> changeSubscription(Subscription subscription)  {
        String urlStr = subscriptionsUrl
        Response<Subscription> response = new Response<Subscription>()
        urlStr = urlStr.replaceFirst(".xml", "/${subscription.id}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, "PUT")
        String subscriptionRequestXml = subscription.getXml()


        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        response.status = responseCode
        log.debug("updateSubscription: response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            response.entity = subscription
            response.message = "Success"
            log.debug("updateSubscription: subscription: ${subscription}")
        } else {
            subscription = null
            String responseMessage = conn.getResponseMessage()
            // this response code should be 422 - unprocessable information.
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            log.error("${RainmakerUtil.logMessagePrefix}downgradeSubscriptionOrUpdateCreditCard: Transaction failed. resposneCode: ${responseCode}. Error message is : ${responseMessage}. Error body is : ${bufferedReader.text}")
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")
            response.message = err
            conn.disconnect()
         }
        conn.disconnect()
        return response
    }


    public Response<Subscription> updateCreditCard(Subscription subscription) {
        String urlStr = subscriptionsUrl
        Response<Subscription> response = new Response<Subscription>()
        urlStr = urlStr.replaceFirst(".xml", "/${subscription.id}.xml")
        HttpURLConnection conn = getChargifyConnection(urlStr, PUT)
        String subscriptionRequestXml = subscription.getXml()
        log.debug "xml request: ${subscriptionRequestXml}"
        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(subscriptionRequestXml)
        writer.flush()
        writer.close()
        conn.connect()

        int responseCode = conn.getResponseCode()
        response.status = responseCode
        log.debug("Updating credit card information : response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            String responseXml = conn.content?.text
            subscription = Subscription.getFromXml(responseXml)
            log.debug("Updating credit card information: subscription: ${subscription}")
            response.message =  "Updating credit card information: subscription: ${subscription}"
            response.entity = subscription
        } else {
            subscription = null
            String responseMessage = conn.getResponseMessage()
            // this response code should be 422 - unprocessable information.
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")
            response.message = err
        }
        conn.disconnect()
        return response
    }

    public boolean doesCustomerExistOnChargify(referenceId){
        String urlStr = "${customerLookupUrl}?reference=${refereceId}"
        HttpURLConnection conn = getChargifyConnection(urlStr, "GET")
        conn.connect()
        int responseCode = conn.getResponseCode()
        log.debug("response code : ${responseCode}")
        conn.disconnect()
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            return true
        }else{
            return false
        }
    }

    public Response<String> createMeteredUsage(String subscriptionId, Component component, Integer amount, String memo){
        return createMeteredUsage(subscriptionId, component.id, amount, memo)
    }

    public Response<String> createMeteredUsage(String subscriptionId, String componentId, Integer amount, String memo) {
        String usageId = null                            
        Response<String> response = new Response<String>()
        String urlStr = "${transactionsUrl}/${subscriptionId}/components/${componentId}/usages.xml"
        HttpURLConnection conn = getChargifyConnection(urlStr, POST)

        String usageXml = getUsageXml(amount, memo)
        log.debug "xml request: ${usageXml}"
        
        int responseCode = processChargifyRequest(conn, usageXml)
        response.status = responseCode
        log.debug("createMeteredUsage response code: ${responseCode}")

        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            usageId = getUsageIdFromXmlResponse(conn.content?.text)
            response.entity = usageId
            response.message = "Success"
        }else
        {
            def err = new XmlSlurper().parse(conn.getErrorStream())
            log.error("Errors : ${err}")
            response.message = err
        }
        conn.disconnect()
        return response
    }

    int processChargifyRequest(HttpURLConnection connection, String requestContent){
        def writer = new OutputStreamWriter(connection.outputStream)
        writer.write(requestContent)
        writer.flush()
        writer.close()
        connection.connect()
        return connection.getResponseCode()
    }

    String getUsageXml(Integer amount, String memoNote){
        StringWriter xmlWriter = new StringWriter()
        MarkupBuilder xmlBuilder = new MarkupBuilder(xmlWriter)
        xmlBuilder.usage() {
            quantity(amount)
            memo(memoNote)
        }
        return xmlWriter.toString()
    }

    String getUsageIdFromXmlResponse(xmlResponse){
        def usage = new XmlParser().parseText(xmlResponse);
        return usage.id?.text();
    }


}

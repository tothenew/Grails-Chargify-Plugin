package org.grails.plugins

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

class ChargifyService {

    public static final int CHARGIFY_RESPONSE_CODE_OK = 201;
    public static final int HTTP_RESPONSE_CODE_OK = 200;

    public static final String customersUrl = "https://${CH.config.chargify.subdomain}.chargify.com/customers.xml"
    public static final String subscriptionsUrl = "https://${CH.config.chargify.subdomain}.chargify.com/subscriptions.xml"
//    String transactionsUrl = "https://${ CH.config.chargify.subdomain}.chargify.com/customers.xml"
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

    public boolean createCustomerInChargify(Customer customer) {
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

            //TODO: read response (xml) and create customer bean - and return it
            conn.disconnect()
           
            if (responseCode == CHARGIFY_RESPONSE_CODE_OK) {
                return true
            } else {
                log.error("Customer not created. ResponseCode: ${responseCode}: ResponseMessage: ${conn.responseMessage}")
            }
        }
        return false
    }

    List<Product> getProductsFromChargify() {
        List<Product> products = []
        HttpURLConnection conn = getChargifyConnection(productsUrl, "GET")
        conn.connect()
        int responseCode = conn.getResponseCode()
        log.debug("getProductsFromChargify: response code : ${responseCode}")
        if (responseCode == HTTP_RESPONSE_CODE_OK) {
            products = Product.getListFromXml(conn.content?.text)
            log.debug("number of products retrieved: ${products.size()}")
        }
        conn.disconnect()
        return products
    }
    
}

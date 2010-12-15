package org.grails.plugins.chargify;

import grails.test.*

class ChargifyServiceTests extends GrailsUnitTestCase {
    ChargifyService chargifyService;

    Customer customer;
    Subscription subscription;
    String subscriptionId
    String planProductHandle = "paid-plan"
    String meteredComponentId = "" // add your metered-ComponentId if you want to run usage test.

    protected void setUp() {
        super.setUp()
        mockConfig('''
            chargify.subdomain = ""
            chargify.authkey  = ""
            chargify.authkeySuffix = ":x"
        ''')
        mockLogging(ChargifyService)
        chargifyService = new ChargifyService();

        customer = getDummyCustomer();
        customer = chargifyService.createCustomer(customer).entity;
        subscription = getDummySubscription();
        subscriptionId = chargifyService.createSubscription(subscription);
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateCustomerInChargify() {
        assertNotNull("Customer could not be created in Chargify", customer)
    }

    void testCreateSubscription() {
        assertNotNull("Subscription could not be created in Chargify", subscriptionId)
    }

    void testGetSubscriptionByIdFromChargify() {
        Subscription existingSubscription;
        //Get Existing Subscription
        existingSubscription = chargifyService.getSubscriptionById(subscriptionId).entity
        assertEquals("Subscription not created or fetched property from Chargify.", planProductHandle, existingSubscription.productHandle)
        assertNotNull(existingSubscription.id)
        assertEquals(customer.referenceId, existingSubscription.customerRef)
    }

    void testGetChargifyTransactions() {
        List<Transaction> transactions = chargifyService.getTransactionsBySubscriptionId(subscriptionId).entity
        assertNotNull("Unable to get transactions", transactions);
        transactions.each {
            assertEquals("Transaction not found for specified subscripiton", subscriptionId, it.subscriptionId)
        }
    }

    void testUpdateCreditCard() {
        String zipcode = "12238";
        subscription.id = subscriptionId
        subscription.zipCode = zipcode;
        Subscription updatedSubscription = chargifyService.updateCreditCard(subscription).entity;
        assertNotNull("Credit Card could not be updated in Chargify", updatedSubscription);
        assertEquals("Subscription not updated or fetch property from chargify", zipcode, updatedSubscription.zipCode)
    }

    void testCancelSubscription() {
        Subscription cancelledSubscription = chargifyService.cancelSubscription(subscriptionId, "Cancel my subscription").entity;
        assertEquals("Unable to cancel subscription in Chargify", "canceled", cancelledSubscription.status);
    }

    void testCreateMeteredUsage(){
        if(meteredComponentId){
            String usageId = chargifyService.createMeteredUsage(subscriptionId, meteredComponentId, 2, "My test metered Usage ${System.currentTimeMillis()}")
            assertNotNull("Problem in creating Metered Component Usage.", usageId)
        }
    }


    private Customer getDummyCustomer() {
        String randomId = UUID.randomUUID().toString();
        Customer customer = new Customer();
        customer.firstName = "UnitTestFN"
        customer.lastName = "UnitTestLN"
        customer.emailAddress = "${randomId}@gmail.com"
        customer.companyName = "Test User Company"
        customer.referenceId = randomId
        return customer
    }


    private Subscription getDummySubscription() {
        Subscription subscription = new Subscription()
        Date date = new Date()
        int currentYear = date.year + 1900
        subscription.with {
            ccNumber = '1'
            ccExpiryMonth = '5'
            ccExpiryYear = currentYear + 2
            customerRef = customer.referenceId
            productHandle = planProductHandle
            zipCode = "201301"
        }
        return subscription;
    }
}

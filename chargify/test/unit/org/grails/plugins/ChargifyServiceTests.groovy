package org.grails.plugins

import grails.test.*

class ChargifyServiceTests extends GrailsUnitTestCase {
    ChargifyService chargifyService;

    Customer customer;
    Subscription subscription;
    String subscriptionId
    String planProductHandle = "paid-plan"

    protected void setUp() {
        super.setUp()
        mockConfig('''
              chargify.subdomain=" "
              chargify.authkey=" "
              chargify.authkeySuffix=":x"
        ''')
        mockLogging(ChargifyService)
        chargifyService = new ChargifyService();

        customer=getDummyCustomer();
        customer=chargifyService.createCustomerInChargify(customer);
        subscription=getDummySubscription();
        subscriptionId=chargifyService.createSubscription(subscription);
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateCustomerInChargify() {
        assertNotNull("Customer could not be created in Chargify", customer)
    }

    void testCreateSubscription(){
        assertNotNull("Subscription could not be created in Chargify", subscriptionId)
    }

    void testGetSubscriptionByIdFromChargify(){
        Subscription existingSubscription;
        //Get Existing Subscription
        existingSubscription = chargifyService.getSubscriptionByIdFromChargify(subscriptionId)
        assertEquals("Subscription not created or fetched property from Chargify.",planProductHandle, existingSubscription.productHandle)
        assertNotNull(existingSubscription.id)
        assertEquals(customer.referenceId, existingSubscription.customerRef)
    }

    void testGetChargifyTransactions(){
        List<Transaction> transactions=chargifyService.getChargifyTransactions(subscriptionId)
        assertNotNull("Unable to get transactions",transactions);
        transactions.each{
            assertEquals("Transaction not found for specified subscripiton",subscriptionId,it.subscriptionId)
        }
    }

    void testUpdateCreditCard(){
        String zipcode="12238";
        subscription.id=subscriptionId
        subscription.zipCode = zipcode;
        Subscription updatedSubscription=chargifyService.updateCreditCard(subscription);
        assertNotNull("Credit Card could not be updated in Chargify",updatedSubscription);
        assertEquals("Subscription not updated or fetch property from chargify", zipcode ,updatedSubscription.zipCode)
    }

    void testCancelSubscription(){
        Subscription cancelledSubscription=chargifyService.cancelSubscription(subscriptionId,"Cancel my subscription");
        assertEquals("Unable to cancel subscription in Chargify", "canceled",cancelledSubscription.status);
    }


    private Customer getDummyCustomer() {
        String randomId = UUID.randomUUID().toString();
        Customer customer=new Customer();
        customer.firstName = "UnitTestFN"
        customer.lastName = "UnitTestLN"
        customer.emailAddress = "${randomId}@gmail.com"
        customer.companyName = "Test User Company"
        customer.referenceId = randomId
        return customer
    }


    private Subscription getDummySubscription(){
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

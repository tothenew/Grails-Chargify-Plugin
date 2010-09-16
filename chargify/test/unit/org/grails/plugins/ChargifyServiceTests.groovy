package org.grails.plugins

import grails.test.*

class ChargifyServiceTests extends GrailsUnitTestCase {
    ChargifyService chargifyService;

    protected void setUp() {
        super.setUp()
        mockLogging(ChargifyService)
        chargifyService = new ChargifyService();
        chargifyService.with {
            authKey = 'fp2YDBIeiEHMeGz48Mo3:x'
            customersUrl = 'https://chargify-plugin.chargify.com/customers.xml'
            subscriptionsUrl = 'https://chargify-plugin.chargify.com/subscriptions.xml'
            productsUrl = 'https://chargify-plugin.chargify.com/products.xml'
            transactionsUrl = "https://chargify-plugin.chargify.com/subscriptions"
        }
    }

    void testUpgradeSubscription() {
        Customer customer = getDummyCustomer();
        chargifyService.createCustomerInChargify(customer)
        Subscription existingSubscription = new Subscription()
        Date date = new Date()
        int currentYear = date.year + 1900
        existingSubscription.with {
            ccNumber = '1'
            ccExpiryMonth = '5'
            ccExpiryYear = currentYear + 2
            customerRef = customer.referenceId
            productHandle = "make-it-rain-basic-plan"
            zipCode = "201301"
        }
        existingSubscription.id = chargifyService.createSubscription(existingSubscription)
        assertNotNull("Subscription could not be created in Chargify", existingSubscription.id)

        existingSubscription = chargifyService.getSubscriptionByIdFromChargify(existingSubscription.id)

        assertEquals("Subscription not created or fetched property from Chargify.", "make-it-rain-basic-plan", existingSubscription.productHandle)
        assertNotNull(existingSubscription.id)
        assertEquals(customer.referenceId, existingSubscription.customerRef)

        // Now, lets upgrade the subscription, the subscription id should change
        Subscription newSubscription = new Subscription();
        newSubscription.with {
            ccNumber = '1'
            ccExpiryMonth = '5'
            ccExpiryYear = currentYear + 2
            customerRef = customer.referenceId
            productHandle = "make-it-rain-plus-plan"
            zipCode = "201301"
        }
        String newSubscriptionId = chargifyService.upgradeSubscription(newSubscription, existingSubscription.id)
        newSubscription = chargifyService.getSubscriptionByIdFromChargify(newSubscriptionId)
        assertTrue(newSubscription.id != existingSubscription)
        assertEquals(customer.referenceId, newSubscription.customerRef)
        // assert that the old subscription is no longer active in Chargify
        existingSubscription = chargifyService.getSubscriptionByIdFromChargify(existingSubscription.id)
        assertEquals("canceled", existingSubscription.status)
        // assert that the new subscription is active in Chargify
        assertTrue(newSubscription.status == "active")
        assertTrue(newSubscription.productHandle == "make-it-rain-plus-plan")
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateCustomerInChargify() {
        Customer customer = getDummyCustomer();
        int responseCode = chargifyService.createCustomerInChargify(customer);
        assertEquals("Response Code should be 201", ChargifyService.CHARGIFY_RESPONSE_CODE_OK, responseCode)
    }

    void testCreateCustomerInChargify_WITHOUT_LAST_NAME() {
        Customer customer = getDummyCustomer();
        customer.lastName = ""
        customer.firstName = ""
        int responseCode = chargifyService.createCustomerInChargify(customer);
        assertEquals("Response Code should be 201", ChargifyService.CHARGIFY_RESPONSE_CODE_OK, responseCode)
    }

    private Customer getDummyCustomer() {
        String randomId = UUID.randomUUID().toString()
        Customer customer = new Customer();
        customer.firstName = "UnitTestFN"
        customer.lastName = "UnitTestLN"
        customer.emailAddress = "${randomId}@gmail.com"
        customer.companyName = "Test User Company"
        customer.referenceId = randomId
        return customer
    }

    void testCreateAndUpdateSubscription() {
        Customer customer = getDummyCustomer();
        chargifyService.createCustomerInChargify(customer)
        Subscription subscription = new Subscription()
        subscription.customerRef = customer.referenceId
        subscription.productHandle = "make-it-rain-pay-as-you-go"
        subscription.id = chargifyService.createSubscription(subscription)
        assertNotNull("Subscription could not be created in Chargify", subscription.id)
        Subscription subscriptionFromChargify = chargifyService.getSubscriptionByIdFromChargify(subscription.id)
        assertEquals("Subscription not created or fetched property from Chargify.", "make-it-rain-pay-as-you-go", subscriptionFromChargify.productHandle)

        // Now, lets update the subscription, the subscription id should remain un-changed.
        subscription.productHandle = "make-it-rain-basic-plan"
        chargifyService.downgradeSubscriptionOrUpdateCreditCard(subscription)
        subscriptionFromChargify = chargifyService.getSubscriptionByIdFromChargify(subscription.id)
        assertEquals("Subscription not created or fetched property from Chargify.", "make-it-rain-basic-plan", subscriptionFromChargify.productHandle)
    }
}

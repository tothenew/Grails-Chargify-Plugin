package org.grails.plugins.chargify;


import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import java.text.ParseException

class Transaction {

    static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
    Date createdAt
    String productId
    String endingBalance
    String memo
    String id
    String transactionType
    String type
    String amount
    String success
    String subscriptionId

    Transaction(def transaction) {
        try {
            createdAt = Date.parse(dateFormat, transaction.created_at)
        } catch (ParseException e) {
            // Unable to parse date
        }
        productId = transaction.product_id
        endingBalance = transaction.ending_balance_in_cents
        memo = transaction.memo
        id = transaction.id
        transactionType = transaction.transaction_type
        type = transaction.type
        success = transaction.success
        amount = transaction.amount_in_cents
        subscriptionId = transaction.subscription_id
    }

    static List<Transaction> getTransactionsFromJson(String jsonData) {
        List<Transaction> transactions = []
        JSONElement jsonTransaction = JSON.parse(jsonData)
        Transaction transaction = null
        jsonTransaction.each {
            transaction = new Transaction(it.transaction)
            transactions << transaction
        }
        return transactions
    }
}

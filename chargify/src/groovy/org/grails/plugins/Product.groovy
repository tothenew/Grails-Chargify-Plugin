package org.grails.plugins

class Product {
    String handle
    String name
    String familyName
    String description
    String price
    String initialCost
    String trialPrice
    boolean ccRequired
    String accountingCode

    public static List<Product> getListFromXml(String productsXml){
        List<Product> productList = []
        def resp = new XmlParser().parseText(productsXml);
        resp.product.each{chargifyProduct ->
            Product product = new Product()
            product.accountingCode = chargifyProduct.accounting_code.text()
            product.handle = chargifyProduct.handle.text()
            product.name = chargifyProduct.name.text()
            product.familyName = chargifyProduct.product_family.name.text()
            product.description = chargifyProduct.description.text()
            product.price = "\$${(chargifyProduct.price_in_cents.text().toInteger()) / 100} every ${chargifyProduct.interval.text()} ${chargifyProduct.interval_unit.text()}"
            product.trialPrice = "\$${(chargifyProduct.trial_price_in_cents.text().toInteger()) / 100} every ${chargifyProduct.trial_interval.text()} ${chargifyProduct.trial_interval_unit.text()}"

            if (chargifyProduct.initial_charge_in_cents?.text()){
                product.initialCost = "\$${(chargifyProduct.initial_charge_in_cents?.text()?.toInteger()) / 100}"
            }
            product.ccRequired = chargifyProduct.require_credit_card.text().toBoolean()
            productList << product
        }
        return productList
    }

    public String toString(){
        return name
    }

}
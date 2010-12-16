package org.grails.plugins.chargify

import groovy.xml.MarkupBuilder

/**
 * Created by IntelliJ IDEA.
 * User: kyle
 * Date: Dec 16, 2010
 * Time: 4:10:36 PM
 * To change this template use File | Settings | File Templates.
 */
class MeteredUsage {


    String id
    String memo
    String quantity

    
    String getXml(){
           StringWriter xmlWriter = new StringWriter()
           MarkupBuilder xmlBuilder = new MarkupBuilder(xmlWriter)
           xmlBuilder.usage() {
               quantity(quantity)
               memo(memo)
           }
           return xmlWriter.toString()
    }

    String getUsageIdFromXmlResponse(xmlResponse){
       def usage = new XmlParser().parseText(xmlResponse);
       return usage.id?.text();
    }

   static List<MeteredUsage> getMeteredUsagesFromXml(xmlResponse){
        List<MeteredUsage> meteredUsages = []
        MeteredUsage usage = null
        def usages = new XmlParser().parseText(xmlResponse);
        usages.each{
            usage = new MeteredUsage(memo:it.memo, quantity:it.quantity)
            meteredUsages << usage
        }
        return meteredUsages
    }

}

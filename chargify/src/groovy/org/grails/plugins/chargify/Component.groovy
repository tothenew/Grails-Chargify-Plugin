package org.grails.plugins.chargify

/**
 * Created by IntelliJ IDEA.
 * User: salil
 * Date: 15 Dec, 2010
 * Time: 5:30:37 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Component {
    public static final TYPE_METERED = "metered"
    public static final TYPE_QUANTITY = "quantity"

    String id
    String name
    String unitName
    String type
}
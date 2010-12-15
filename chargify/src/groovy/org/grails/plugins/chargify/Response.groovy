package org.grails.plugins.chargify
/**
 * Created by IntelliJ IDEA.
 * User: kyle
 * Date: Dec 15, 2010
 * Time: 11:10:11 AM
 * To change this template use File | Settings | File Templates.
 */
class Response<T> {
    public T entity
    public String status
    public String message

    public success() {
        return this.status == '200' || this.status == '201'
    }

}

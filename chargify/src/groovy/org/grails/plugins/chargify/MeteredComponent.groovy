package org.grails.plugins.chargify

class MeteredComponent implements Component{
    Double pricePerUnit

    public MeteredComponent(){
        this.type = Component.TYPE_METERED
    }

    public MeteredComponent(name, unitName){
        this.type = Component.TYPE_METERED
        this.name = name
        this.unitName = unitName
    }

    public MeteredComponent(name, unitName, pricePerUnit){
        this.type = Component.TYPE_METERED
        this.name = name
        this.unitName = unitName
        this.pricePerUnit = pricePerUnit
    }
    
}

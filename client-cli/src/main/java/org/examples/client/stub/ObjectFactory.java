package org.examples.client.stub;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

public class ObjectFactory {
    public JAXBElement<String> createReservationRequestAgence(String val){
        return new JAXBElement<>(new QName("http://service.hotel.examples.org/dto","agence"), String.class, val);
    }
}


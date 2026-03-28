package com.iso2t.configmanager.value.wrappers;

import com.iso2t.configmanager.value.AbstractValue;

public class ObjectValue<T> extends AbstractValue<T> {

    public ObjectValue(T def) {
        super(def);
    }

}

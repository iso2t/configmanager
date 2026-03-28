package com.iso2t.configmanager.value.wrappers;

import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.NumberRange;

public class ShortValue extends AbstractValue<Short> implements NumberRange<Short> {

    private final Short min;
    private final Short max;

    public ShortValue (Short def, Short min, Short max) {
        super(def);
        this.min = min;
        this.max = max;
    }

	public ShortValue (Short def) {
		this(def, Short.MIN_VALUE, Short.MAX_VALUE);
	}

    @Override
    public Short getMin () {
        return min;
    }

    @Override
    public Short getMax () {
        return max;
    }

}


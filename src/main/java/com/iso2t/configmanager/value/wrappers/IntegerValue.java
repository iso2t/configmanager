package com.iso2t.configmanager.value.wrappers;

import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.NumberRange;

public class IntegerValue extends AbstractValue<Integer> implements NumberRange<Integer> {

    private final Integer min;
    private final Integer max;

    public IntegerValue (Integer def, Integer min, Integer max) {
        super(def);
        this.min = min;
        this.max = max;
    }

	public IntegerValue (int def) {
		this(def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

    @Override
    public Integer getMin () {
        return min;
    }

    @Override
    public Integer getMax () {
        return max;
    }

}

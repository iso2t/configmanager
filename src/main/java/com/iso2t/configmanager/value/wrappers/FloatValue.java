package com.iso2t.configmanager.value.wrappers;

import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.NumberRange;

public class FloatValue extends AbstractValue<Float> implements NumberRange<Float> {

    private final Float min;
    private final Float max;

    public FloatValue(Float def, Float min, Float max) {
        super(def);
        this.min = min;
        this.max = max;
    }

	public FloatValue (Float def) {
		this(def, Float.MIN_VALUE, Float.MAX_VALUE);
	}

    @Override
    public Float getMin() {
        return min;
    }

    @Override
    public Float getMax() {
        return max;
    }

}


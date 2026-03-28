package com.iso2t.configmanager.value.wrappers;

import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.NumberRange;

public class LongValue extends AbstractValue<Long> implements NumberRange<Long> {

    private final Long min;
    private final Long max;

    public LongValue (Long def, Long min, Long max) {
        super(def);
        this.min = min;
        this.max = max;
    }

	public LongValue (Long def) {
		this(def, Long.MIN_VALUE, Long.MAX_VALUE);
	}

    @Override
    public Long getMin () {
        return min;
    }

    @Override
    public Long getMax () {
        return max;
    }

}


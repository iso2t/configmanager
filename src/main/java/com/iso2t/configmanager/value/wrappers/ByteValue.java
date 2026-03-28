package com.iso2t.configmanager.value.wrappers;

import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.NumberRange;

public class ByteValue extends AbstractValue<Byte> implements NumberRange<Byte> {

    private final Byte min;
    private final Byte max;

    public ByteValue (Byte def, Byte min, Byte max) {
        super(def);
        this.min = min;
        this.max = max;
    }

	public ByteValue (Byte def) {
		this(def, Byte.MIN_VALUE, Byte.MAX_VALUE);
	}

    @Override
    public Byte getMin () {
        return min;
    }

    @Override
    public Byte getMax () {
        return max;
    }
}


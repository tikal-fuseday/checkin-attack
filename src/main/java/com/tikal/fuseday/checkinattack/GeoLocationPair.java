package com.tikal.fuseday.checkinattack;

public class GeoLocationPair {
	private final float x;
	private final float y;

	public GeoLocationPair(final float x, final float y) {
		this.x = x;
		this.y = y;
	}
	
	
	private float getGeoCord(final Number number) {
		return (int)(number.floatValue()*10000);
	}
	

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final GeoLocationPair other = (GeoLocationPair) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return x+"@"+y;
	}
	
	

}

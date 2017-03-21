package gen;

public enum Size {

	BYTE(0x80, 0xFF), WORD(0x8000, 0xFFFF), LONG(0x8000_0000, 0xFFFF_FFFFL);
	
	int msb;
	long max;
	
	Size(int msb, long maxSize) {
		this.msb = msb;
		this.max = maxSize;
	}

	public int getMsb() {
		return this.msb;
	}
	
	public long getMax() {
		return this.max;
	}
	
}

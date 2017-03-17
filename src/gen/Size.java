package gen;

public enum Size {

	byt(0x80), word(0x8000), longW(0x8000_0000);
	
	int msb;
	
	Size(int msb) {
		this.msb = msb;
	}

	public int getMsb() {
		return this.msb;
	}
	
}

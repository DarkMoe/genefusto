package gen.addressing;

import gen.Gen68;
import gen.instruction.Operation;

public class FullIndex implements AddressingMode {

	private Gen68 cpu;
	
	public FullIndex(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public void setWord(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public void setLong(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public long getByte(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public long getWord(Operation o) {
		throw new RuntimeException();
	}

	@Override
	public long getLong(Operation o) {
		throw new RuntimeException();
	}

}

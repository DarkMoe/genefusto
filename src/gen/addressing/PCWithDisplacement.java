package gen.addressing;

import gen.Gen68;
import gen.Size;
import gen.instruction.Operation;

public class PCWithDisplacement implements AddressingMode {

	private Gen68 cpu;
	
	public PCWithDisplacement(Gen68 cpu) {
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

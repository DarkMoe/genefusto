package gen.addressing;

import gen.Gen68;
import gen.instruction.Operation;

public class PCWithIndex implements AddressingMode {

	private Gen68 cpu;
	
	public PCWithIndex(Gen68 cpu) {
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
		long address = o.getAddress();
		long data  = cpu.bus.read(address) << 8;
			 data |= cpu.bus.read(address + 1);
		return data;
	}

	@Override
	public long getLong(Operation o) {
		throw new RuntimeException();
	}

}

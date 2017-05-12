package gen.addressing;

import gen.Gen68;
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
		long address = o.getAddress();
		long data = cpu.bus.read(address);
		return data;
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
		long address = o.getAddress();
		long data  = cpu.bus.read(address) << 24;
			 data |= cpu.bus.read(address + 1) << 16;
			 data |= cpu.bus.read(address + 2) << 8;
			 data |= cpu.bus.read(address + 3);
		return data;
	}

}

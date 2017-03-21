package gen.addressing;

import gen.Gen68;
import gen.Size;
import gen.instruction.Operation;

public class AddressDisplacement implements AddressingMode {

	private Gen68 cpu;
	
	public AddressDisplacement(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, data & 0xFF, Size.BYTE);
	}

	@Override
	public void setWord(Operation o) {
		throw new RuntimeException("NOO");
	}

	@Override
	public void setLong(Operation o) {
		throw new RuntimeException("NOO");
	}
	
	@Override
	public long getByte(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr) & 0xFF;
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr) & 0xFFFF;
		
		return data;
	}

	@Override
	public long getLong(Operation o) {
		throw new RuntimeException("NOO");
	}

}

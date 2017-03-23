package gen.addressing;

import gen.Gen68;
import gen.Size;
import gen.instruction.Operation;

public class AbsoluteLong implements AddressingMode {

	private Gen68 cpu;
	
	public AbsoluteLong(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long address = o.getAddress();
		long data = o.getData();
		
		cpu.bus.write(address, data, Size.BYTE);
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
		long data = (cpu.bus.read(addr)) & 0xFF;
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long addr = o.getAddress();
		long data  = (cpu.bus.read(addr)) << 8;
			 data |= cpu.bus.read(addr + 1);
		
		return data;
	}

	@Override
	public long getLong(Operation o) {
		long addr = o.getAddress();
		long  data = (cpu.bus.read(addr)) << 24;
			 data |= cpu.bus.read(addr + 1) << 16;
			 data |= cpu.bus.read(addr + 2) << 8;
			 data |= cpu.bus.read(addr + 3);
		
		return data;
	}

}

package gen.addressing;

import gen.Gen68;
import gen.instruction.Operation;

public class ImmediateData implements AddressingMode {

	private Gen68 cpu;
	
	public ImmediateData(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		throw new RuntimeException("NOO");
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
		long data  = (cpu.bus.read(addr) << 8);
		 	 data |= (cpu.bus.read(addr + 1));
		data = data & 0xFF;
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long addr = o.getAddress();
		long data  = (cpu.bus.read(addr) << 8);
			 data |= (cpu.bus.read(addr + 1));
			 
		return data;
	}

	@Override
	public long getLong(Operation o) {
		long addr = o.getAddress();
		long data = (cpu.bus.read(addr) << 24);
			data |= (cpu.bus.read(addr + 1) << 16);
			data |= (cpu.bus.read(addr + 2) << 8);
			data |= (cpu.bus.read(addr + 3));
		
		return data;
	}

}

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
		long address = o.getAddress();
		long data = o.getData();
		
		cpu.bus.write(address, data, Size.WORD);
	}

	@Override
	public void setLong(Operation o) {
		long address = o.getAddress();
		long data = o.getData();
		
		cpu.bus.write(address, data, Size.LONG);
	}
	
	@Override
	public long getByte(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr, Size.BYTE) & 0xFF;
		
		return data;
	}

	@Override
	public long getWord(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr, Size.WORD);
		
		return data;
	}

	@Override
	public long getLong(Operation o) {
		long addr = o.getAddress();
		long data = cpu.bus.read(addr, Size.LONG);
		
		return data;
	}

	@Override
	public void calculateAddress(Operation o, Size size) {
		long addr = cpu.bus.read(cpu.PC + 2, Size.LONG);
		o.setAddress(addr);
		
		cpu.PC += 4;
	}

}

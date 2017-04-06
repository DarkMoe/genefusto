package gen.addressing;

import gen.Gen68;
import gen.Size;
import gen.instruction.Operation;

public class AddressRegisterWithIndex implements AddressingMode {

	private Gen68 cpu;
	
	public AddressRegisterWithIndex(Gen68 cpu) {
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
		
		cpu.bus.write(address, (data >> 16), Size.LONG);
		cpu.bus.write(address + 2, (data & 0xFFFF), Size.LONG);
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

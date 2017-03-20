package gen.addressing;

import gen.Gen68;
import gen.Size;
import gen.instruction.Operation;

public class AddressRegisterIndirect implements AddressingMode {

	private Gen68 cpu;
	
	public AddressRegisterIndirect(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, (data & 0xFF), Size.byt);
	}

	@Override
	public void setWord(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, (data & 0xFFFF), Size.word);
	}

	@Override
	public void setLong(Operation o) {
		long addr = o.getAddress();
		long data = o.getData();

		cpu.bus.write(addr, (data >> 16), Size.longW);
		cpu.bus.write(addr + 2, (data & 0xFFFF), Size.longW);
	}

	@Override
	public long getByte(Operation o) {
		long addr = o.getAddress();

		long data = (cpu.bus.read(addr));
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

		long data  = (cpu.bus.read(addr)     << 24);
			 data |= (cpu.bus.read(addr + 1) << 16);
			 data |= (cpu.bus.read(addr + 2) << 8);
			 data |= (cpu.bus.read(addr + 3) << 0);
		return data;
	}

}

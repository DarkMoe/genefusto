package gen.addressing;

import gen.Gen68;
import gen.Size;
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
		long data = cpu.bus.read(addr, Size.WORD);	//	lee 2 bytes
		data = data & 0xFF;
		
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
		o.setAddress(cpu.PC + 2);
		
		if (size == Size.BYTE) {		//	aunque sea byte, siempre ocupa 2 bytes y cuenta el de la derecha
			cpu.PC += 2;
			
		} else if (size == Size.WORD) {
			cpu.PC += 2;
			
		} else if (size == Size.LONG) {	// long
			cpu.PC += 4;
			
		}
	}

}

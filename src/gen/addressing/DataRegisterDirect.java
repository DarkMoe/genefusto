package gen.addressing;

import gen.Gen68;
import gen.instruction.Operation;

public class DataRegisterDirect implements AddressingMode {

	private Gen68 cpu;
	
	public DataRegisterDirect(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		int register = o.getRegister();
		long data = cpu.D[register];
		
		cpu.D[register] = data & 0xFF;
	}

	@Override
	public void setWord(Operation o) {
		long data = cpu.D[o.getRegister()];
		int register = o.getRegister();
		
		cpu.D[register] = data & 0xFFFF;
	}

	@Override
	public void setLong(Operation o) {
		long data = cpu.D[o.getRegister()];
		int register = o.getRegister();
		
		cpu.D[register] = data;
	}

	@Override
	public long getByte(Operation o) {
		int register = o.getRegister();
		
		return cpu.D[register] & 0xFF;
	}

	@Override
	public long getWord(Operation o) {
		int register = o.getRegister();
		
		return cpu.D[register] & 0xFFFF;
	}

	@Override
	public long getLong(Operation o) {
		int register = o.getRegister();
		
		return cpu.D[register];
	}

}

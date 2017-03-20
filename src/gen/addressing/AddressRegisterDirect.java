package gen.addressing;

import gen.Gen68;
import gen.instruction.Operation;

public class AddressRegisterDirect implements AddressingMode {

	private Gen68 cpu;
	
	public AddressRegisterDirect(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long data = cpu.A[o.getRegister()];
		int register = o.getRegister();
		
		cpu.A[register] = data & 0xFF;
	}

	@Override
	public void setWord(Operation o) {
		long data = cpu.A[o.getRegister()];
		int register = o.getRegister();
		
		cpu.A[register] = data & 0xFFFF;
	}

	@Override
	public void setLong(Operation o) {
		long data = cpu.A[o.getRegister()];
		int register = o.getRegister();
		
		cpu.A[register] = data;
	}

	@Override
	public long getByte(Operation o) {
		int register = o.getRegister();
		
		return cpu.A[register] & 0xFF;
	}

	@Override
	public long getWord(Operation o) {
		int register = o.getRegister();
		
		return cpu.A[register] & 0xFFFF;
	}

	@Override
	public long getLong(Operation o) {
		int register = o.getRegister();
		
		return cpu.A[register];
	}

}

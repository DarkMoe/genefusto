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
		int register = o.getRegister();
		long data = o.getData();
		
		cpu.setAByte(register, data);
	}

	@Override
	public void setWord(Operation o) {
		int register = o.getRegister();
		long data = o.getData();
		
		cpu.setAWord(register, data);
	}

	@Override
	public void setLong(Operation o) {
		int register = o.getRegister();
		long data = o.getData();
		
		cpu.setALong(register, data);
	}

	@Override
	public long getByte(Operation o) {
		int register = o.getRegister();
		
		return cpu.getA(register) & 0xFF;
	}

	@Override
	public long getWord(Operation o) {
		int register = o.getRegister();
		
		return cpu.getA(register) & 0xFFFF;
	}

	@Override
	public long getLong(Operation o) {
		int register = o.getRegister();
		
		return cpu.getA(register);
	}

}

package gen.addressing;

import gen.Gen68;
import gen.Size;
import gen.instruction.Operation;

public class DataRegisterDirect implements AddressingMode {

	private Gen68 cpu;
	
	public DataRegisterDirect(Gen68 cpu) {
		this.cpu = cpu;
	}
	
	@Override
	public void setByte(Operation o) {
		long data = o.getData();
		int register = o.getRegister();
		
		cpu.setDByte(register, data);
	}

	@Override
	public void setWord(Operation o) {
		long data = o.getData();
		int register = o.getRegister();
		
		cpu.setDWord(register, data);
	}

	@Override
	public void setLong(Operation o) {
		long data = o.getData();
		int register = o.getRegister();
		
		cpu.setDLong(register, data);
	}

	@Override
	public long getByte(Operation o) {
		int register = o.getRegister();
		
		return cpu.getDByte(register);
	}

	@Override
	public long getWord(Operation o) {
		int register = o.getRegister();
		
		return cpu.getDWord(register);
	}

	@Override
	public long getLong(Operation o) {
		int register = o.getRegister();
		
		return cpu.getDLong(register);
	}

	@Override
	public void calculateAddress(Operation o, Size size) {
//		throw new RuntimeException("Never enter");
	}

}

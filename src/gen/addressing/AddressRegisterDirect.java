package gen.addressing;

import gen.Gen68;
import gen.Size;
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
		
		if (register == 7) {
			System.out.println("CHECK A7 VALUE !");
		}
		
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
		
		return cpu.getAByte(register);
	}

	@Override
	public long getWord(Operation o) {
		int register = o.getRegister();
		
		return cpu.getAWord(register);
	}

	@Override
	public long getLong(Operation o) {
		int register = o.getRegister();
		
		return cpu.getALong(register);
	}

	@Override
	public void calculateAddress(Operation o, Size size) {
//		throw new RuntimeException("No address");
	}

}

package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class UNLK implements GenInstructionHandler {

	final Gen68 cpu;
	
	public UNLK(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	UNLK -- Free stack frame created by LINK
//
//SYNOPSIS
//	UNLK	An
//
//FUNCTION
//	This instruction does the inverse process of LINK instruction.
//	Address register specified is moved in SP.
//	Contents of SP is moved into address register.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|---|---|---|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | 1 | 1 | REGISTER  |
//	-----------------------------------------------------------------
//
//	"REGISTER" indicates the number of address register, used as area
//	pointer.
//
//RESULT
//	None.
//
//SEE ALSO
//	LINK
	
	@Override
	public void generate() {
		int base = 0x4E58;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				UNLINK(opcode);
			}
			
		};
		
		for (int r = 0; r < 8; r++) {
			int opcode = base | r;
			cpu.addInstruction(opcode, ins);
		}
		
	}
	
	private void UNLINK(int opcode) {
		int register = opcode & 0x7;
		
		long addr = cpu.getALong(register);
		long fromSP = cpu.bus.read(addr, Size.LONG);
		
		cpu.setALong(register, fromSP);
		
		long newSP = addr + 4;
		cpu.setALong(7, newSP);
	}

}

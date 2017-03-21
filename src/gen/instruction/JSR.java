package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class JSR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public JSR(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	JSR -- Jump to subroutine
//
//SYNOPSIS
//	JSR	<ea>
//
//FUNCTION
//	Pushes the long word address of the instruction immediately
//	following the JSR instruction onto the stack. The PC contains
//	the address of the instruction word plus two. Program execution
//	continues at location specified by <ea>.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 1 | 0 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                   <ea>
//REGISTER
//	<ea> specifies address of next instruction.
//	Allowed addressing modes are:
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       | -  |    -     | |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     | -  |    -     | |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      | -  |    -     | |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x4E80;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				JumpSR(opcode);
			}
		};
		
		for (int m = 0; m < 8; m++) {
			if (m == 0 || m == 1 || m == 3 || m == 4) {
				continue;
			}
			for (int r = 0; r < 8; r++) {
				if (m == 0b111 & r > 0b011) {
					continue;
				}
				int opcode = base | (m << 3) | r;
				cpu.addInstruction(opcode, ins);
			}
		}
		
	}
	
	private void JumpSR(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		Operation o = cpu.resolveAddressingMode(cpu.PC + 2, Size.WORD, mode, register);
		long newPC = o.getAddress();
		
		long oldPC = cpu.PC + 2;
		
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, oldPC & 0xFF, Size.BYTE);
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, (oldPC >> 8) & 0xFF, Size.BYTE);
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, (oldPC >> 16) & 0xFF, Size.BYTE);
		cpu.SSP--;
		cpu.bus.write(cpu.SSP, (oldPC >> 24), Size.BYTE);
		
		cpu.setALong(7, cpu.SSP);
		
		cpu.PC = newPC - 2;
	}

}

package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class JMP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public JMP(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	JMP -- Unconditional jump
//
//
//SYNOPSIS
//	JMP	<ea>
//
//FUNCTION
//	Program execution continues at the address specified by
//	the operand.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 1 | 1 |    MODE   | REGISTER  |
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
		int base = 0x4EC0;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				JMPUnconditional(opcode);
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
	
	private void JMPUnconditional(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		Operation o = cpu.resolveAddressingMode(cpu.PC + 2, Size.LONG, mode, register);
		long newPC = o.getAddress();
		
		cpu.PC = newPC - 2;
	}

}

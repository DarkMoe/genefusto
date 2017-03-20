package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class LEA implements GenInstructionHandler {

	final Gen68 cpu;
	
	public LEA(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	LEA -- Load effective address
//
//SYNOPSIS
//	LEA	<ea>,An
//
//	Size = (Long)
//
//FUNCTION
//	Places the specified address into the destination address
//	register. Note: All 32 bits of An are affected by this instruction.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|-----------|-----------|
//	| 0 | 1 | 0 | 0 | REGISTER  | 1 | 1 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                   <ea>
//REGISTER
//	"REGISTER" indicates the number of address register
//
//	<ea> specifies address which must be loaded in the address register.
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
		int base = 0x41C0;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				LEAWord(opcode);
			}
			
		};
		
		for (int register = 0; register < 8; register++) {
			for (int m = 0; m < 8; m++) {
				for (int r = 0; r < 8; r++) {
					if (m == 0b000 || m == 0b001 || m == 0b011 || m == 0b100) {
						continue;
					}
					if (m == 0b111 && r > 0b11) {
						continue;
					}
					
					int opcode = base + ((register << 9) | (m << 3) | r);
					cpu.addInstruction(opcode, ins);
				}
			}
		}
		
	}
	
	private void LEAWord(int opcode) {
		int destReg = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.word, mode, register);
		long addr = o.getAddress();
		
		cpu.A[destReg] = addr;
	}

}

package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class MOVEA implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVEA(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	MOVEA -- Source -> Destination
//
//SYNOPSIS
//	MOVEA	<ea>,An
//
//	Size = (Word, Long)
//
//FUNCTION
//	Move the contents of the source to the destination address
//	register. Word sized operands are sign extended to 32 bits
//	before the operation is done.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|-------|-----------|---|---|---|-----------|-----------|
//	| 0 | 0 |  SIZE |  ADDRESS  | 0 | 0 | 1 |    MODE   | REGISTER  |
//	|   |   |       |  REGISTER |   |   |   |           |           |
//	----------------------------------------=========================
//	                                              source <ea>
//
//REGISTER
//	"ADDRESS REGISTER" specifies the number of destination address
//	register.
//
//	Source <ea> specifies source operand, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An *     |001 |N° reg. An| |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//	 * Word or Long only.
//
//SIZE
//	11->Word, 32 bits of address register are altered by sign extension.
//	10->Long
//
//RESULT
//	None.

	@Override
	public void generate() {
		int base = 0x0040;
		GenInstruction ins = null;
		
		for (int s = 2; s < 4; s++) {
			if (s == 0b11) {
				
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						MOVEAWord(opcode);
					}
				};
			} else if (s == 0b10) {
				
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						MOVEALong(opcode);
					}
				};
			}
			
			for (int addrReg = 0; addrReg < 8; addrReg++) {
				for (int m = 0; m < 8; m++) {
					for (int r = 0; r < 8; r++) {
						if (m == 0b111 & r > 0b100) {
							continue;
						}
						int opcode = base + (s << 12) | (addrReg << 9) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}
	
	private void MOVEAWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		int addrReg = (opcode >> 9) & 0x7;
		
		Operation o = cpu.resolveAddressingMode(Size.word, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		if ((data & 0x8000) > 0) {
			data |= 0xFFFF_0000;
		}
		cpu.A[addrReg] = data;
	}

	private void MOVEALong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		int addrReg = (opcode >> 9) & 0x7;
		
		Operation o = cpu.resolveAddressingMode(Size.longW, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		cpu.A[addrReg] = data;
	}
	
}

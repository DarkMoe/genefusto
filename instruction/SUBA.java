package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class SUBA implements GenInstructionHandler {

	final Gen68 cpu;
	
	public SUBA(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	SUBA -- Subtract address
//
//SYNOPSIS
//	SUBA	<ea>,An
//
//	Size = (Word, Long)
//
//FUNCTION
//        Subtracts source operand to destination operand.
//        Source operand with a Word size is extended to 32 bits before
//        operation. Result is stored to destination's place.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|-----------|-----------|
//	| 1 | 0 | 0 | 1 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//	----------------------------------------=========================
//                                                          <ea>
//OP-MODE
//	Indicates operation lenght:
//	011->one Word operation: source operand is extended to 32 bits
//	111->one Long operation
//
//REGISTER
//	<ea> is source, allowed addressing modes are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       |001 |N° reg. An| |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d16,An)    |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//RESULT
//        None.
	
	@Override
	public void generate() {
		int base = 0x90C0;
		GenInstruction ins = null;
		
		for (int opMode = 0; opMode < 2; opMode++) {
			if (opMode == 0) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUBAWord(opcode);
					}
				};
			} else if (opMode == 1) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUBALong(opcode);
					}
				};
			}
			
			for (int register = 0; register < 8; register++) {
				for (int m = 0; m < 8; m++) {
					for (int r = 0; r < 8; r++) {
						if (m == 0b111 & r > 0b100) {
							continue;
						}
						int opcode = base | (register << 9) | (opMode << 8) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
		
	}
	
	private void SUBAWord(int opcode) {
		int addrRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		if ((data & 0x8000) > 0) {
			data = 0xFFFF_0000 | data;
		}
		
		long toSub = cpu.getA(addrRegister);
		
		long tot = toSub - data;
		cpu.setALong(addrRegister, tot);	// setLong porque afecta a todo el registro
	}
	
	private void SUBALong(int opcode) {
		int addrRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toSub = cpu.getA(addrRegister);
		
		long tot = toSub - data;
		cpu.setALong(addrRegister, tot);
	}
	
}

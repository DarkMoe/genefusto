package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class CMPA implements GenInstructionHandler {

	final Gen68 cpu;
	
	public CMPA(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	CMPA -- Compare address
//
//SYNOPSIS
//	CMPA	<ea>,An
//
//	Size = (Word, Long)
//
//FUNCTION
//	Subtracts the source operand from the destination address
//	register and sets the condition codes according to the result. The
//	address register is NOT changed. Word sized source operands are
//	sign extended to long for comparison.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|-----------|-----------|
//	| 1 | 0 | 1 | 1 |  REGISTER |  OP-MODE  |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//OP-MODE
//	011	16 bits operation.
//	111	32 bits operation.
//
//REGISTER
//	The address register specifies destination An.
//	<ea> specifies source operand, addressing modes allowed are:
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
//
//RESULT
//	X - Not affected
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow occours. Cleared otherwise.
//	C - Set if a borrow occours. Cleared otherwise.
	
	@Override
	public void generate() {
		int base = 0xB000;
		GenInstruction ins = null;
		for (int i = 0; i < 2; i++) {
			int opMode = (i << 2) | 0b11;
			if (opMode == 0b011) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPAWord(opcode);
					}
				};
			} else if (opMode == 0b111) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPALong(opcode);
					}
				};
			}
			for (int m = 0; m < 8; m++) {
				for (int r = 0; r < 8; r++) {
					if ((m == 7) && r > 0b100) {
						continue;
					}
					for (int register = 0; register < 8; register++) {
						int opcode = base | (register << 9) | (opMode << 6) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}

	private void CMPAWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		int addressRegister = (opcode >> 9) & 0x7;
		
		long data = cpu.getA(addressRegister);

		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long toSub = o.getAddressingMode().getWord(o);
		
		if ((toSub & 0x8000) == 0x8000) {
			toSub |= 0xFFFF_0000L;
		}
		
		long res = data - toSub;
		
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF);
	}
	
	private void CMPALong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		int addressRegister = (opcode >> 9) & 0x7;
		
		long data = cpu.getA(addressRegister);

		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long toSub = o.getAddressingMode().getLong(o);
		cpu.print = true;
		long res = data - toSub;
		
		calcFlags(res, Size.LONG.getMsb(), 0xFFFF_FFFFL);
	}
	
	void calcFlags(long data, int msb, long maxSize) {	// TODO V
		if ((data & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if (((data & maxSize) & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		if (data < 0) {	// validar esto
			cpu.setC();
		} else {
			cpu.clearC();
		}
	}
	
}

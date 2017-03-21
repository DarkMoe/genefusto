package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class CMP implements GenInstructionHandler {

	final Gen68 cpu;
	
	public CMP(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	CMP -- Compare
//
//SYNOPSIS
//	CMP	<ea>,Dn
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Subtracts the source operand from the destination data register and
//	sets the condition codes according to the result. The data register
//	is NOT changed.
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
//	000	8 bits operation.
//	001	16 bits operation.
//	010	32 bits operation.
//
//REGISTER
//	The data register specifies destination Dn.
//	<ea> specifies source operand, addressing modes allowed are:
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
//	 * Word or Long only
//	
//RESULT
//	X - Not affected
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow is generated. Cleared otherwise.
//	C - Set if a carry is generated. Cleared otherwise.
	
	@Override
	public void generate() {
		int base = 0xB000;
		GenInstruction ins = null;
		for (int opMode = 0; opMode < 3; opMode++) {
			if (opMode == 0b000) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPByte(opcode);
					}
				};
			} else if (opMode == 0b001) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPWord(opcode);
					}
				};
			} else if (opMode == 0b010) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPLong(opcode);
					}
				};
			}
			for (int m = 0; m < 8; m++) {
				if (m == 1 && opMode == 0b000) {	//	byte no tiene este modo
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if ((m == 7) && r > 0b100) {
						continue;
					}
					for (int register = 0; register < 8; register++) {
						int opcode = base + ((register << 9) | (opMode << 6) | (m << 3) | r);
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}

	private void CMPByte(int opcode) {
		throw new RuntimeException("JJ");
	}
	
	private void CMPWord(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long toSub = (cpu.getD(dataRegister) & 0xFFFF);
		long res = toSub - data;
		
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF);
	}
	
	private void CMPLong(int opcode) {
		int dataRegister = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toSub = cpu.getD(dataRegister);
		long res = toSub - data;
		
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

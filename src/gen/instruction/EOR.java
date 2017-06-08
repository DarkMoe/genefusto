package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class EOR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public EOR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	EOR -- Exclusive logical OR
//
//SYNOPSIS
//	EOR	Dn,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Performs an exclusive OR operation on the destination operand
//	with the source operand.
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
//	100	8 bits operation.
//	101	16 bits operation.
//	110	32 bits operation.
//
//REGISTER
//	The data register specifies source Dn.
//	<ea> specifies destination operand, addressing modes allowed are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N° reg. An| |   (d8,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N° reg. An| |   (bd,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//RESULT
//	X - Not Affected
//	N - Set to the value of the most significant bit.
//	Z - Set if the result is zero.
//	V - Always cleared
//	C - Always cleared
	
	@Override
	public void generate() {
		int base = 0xB000;
		GenInstruction ins = null;
		
		for (int opMode = 0b100; opMode < 7; opMode++) {
			if (opMode == 0b100) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						EORByte(opcode);
					}

				};
			} else if (opMode == 0b101) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						EORWord(opcode);
					}

				};
			} else if (opMode == 0b110) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						EORLong(opcode);
					}

				};
			}
		
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				
				for (int r = 0; r < 8; r++) {
					if ((m == 7) && r > 0b001) {
						continue;
					}
					
					for (int register = 0; register < 8; register++) {
						int opcode = base + (register << 9) | (opMode << 6) | ((m << 3) | r);
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
		
	}
	
	private void EORByte(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int dataReg = (opcode >> 9) & 0x7;
		
		long toEor = cpu.getDByte(dataReg);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long res = data ^ toEor;
		cpu.writeKnownAddressingMode(o, res, Size.BYTE);
		
		calcFlags(res, Size.BYTE.getMsb());
	}

	private void EORWord(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int dataReg = (opcode >> 9) & 0x7;
		
		long toEor = cpu.getDWord(dataReg);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long res = data ^ toEor;
		cpu.writeKnownAddressingMode(o, res, Size.WORD);
		
		calcFlags(res, Size.WORD.getMsb());
	}
	
	private void EORLong(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int dataReg = (opcode >> 9) & 0x7;
		
		long toEor = cpu.getDLong(dataReg);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long res = data ^ toEor;
		cpu.writeKnownAddressingMode(o, res, Size.LONG);
		
		calcFlags(res, Size.LONG.getMsb());
	}
	
	void calcFlags(long data, long msb) {
		if (data == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((data & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		cpu.clearV();
		cpu.clearC();
	}
	
}

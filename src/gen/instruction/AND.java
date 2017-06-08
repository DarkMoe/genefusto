package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class AND implements GenInstructionHandler {

	final Gen68 cpu;
	
	public AND(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	AND -- Logical AND
//
//SYNOPSIS
//	AND	<ea>,Dn
//	AND	Dn,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Performs a bit-wise AND operation with the source operand and
//	the destination operand and stores the result in the destination.
//	The size of ther operation can be specified as byte, word, or long.
//	The contents of an address register may not be used as an operand.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|-----------|-----------|-----------|
//	| 1 | 1 | 0 | 0 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//	----------------------------------------=========================
//                                                          <ea>
//
//OP-MODE
//	Byte	Word	Long
//	000		001		010		(Dn)AND(<ea>)-> Dn
//	100		101		110		(<ea>)AND(Dn)-> <ea>
//
//
//REGISTER
//	One of the 8 datas registers
//	If <ea> is source, allowed addressing modes are:
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
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
//	If <ea> is destination, allowed addressing modes are:
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       | -  |     -    | |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
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
//	AND between two datas registers is allowed if you consider the
//	syntax where Dn is at destination's place.
//
//	If you use this instruction with an immediate data, it does the
//	same as instruction ANDI.
//	
//
//RESULT
//	X - Not affected
//	N - Set if the most-significant bit of the result was set. Cleared
//	    otherwise.
//	Z - Set if the result was zero. Cleared otherwise.
//	V - Always cleared.
//	C - Always cleared.
	
	@Override
	public void generate() {
		int base = 0xC000;
		GenInstruction ins = null;
		
		for (int opMode = 0; opMode < 3; opMode++) {
			if (opMode == 0b000) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ANDSourceEAByte(opcode);
					}

				};
			} else if (opMode == 0b001) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ANDSourceEAWord(opcode);
					}

				};
			} else if (opMode == 0b010) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ANDSourceEALong(opcode);
					}

				};
			}
		
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				
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
		
		
		for (int opMode = 4; opMode < 7; opMode++) {
			if (opMode == 0b100) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ANDDestEAByte(opcode);
					}

				};
			} else if (opMode == 0b101) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ANDDestEAWord(opcode);
					}

				};
			} else if (opMode == 0b110) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ANDDestEALong(opcode);
					}

				};
			}
		
			for (int m = 0; m < 8; m++) {
				if (m == 0 || m == 1) {
					continue;
				}
				
				for (int r = 0; r < 8; r++) {
					if ((m == 7) && r > 0b001) {
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
	
	private void ANDSourceEAByte(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int destRegister = (opcode >> 9) & 0x7;
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long toAnd = cpu.getDByte(destRegister);
		long res = toAnd & data;
		cpu.setDByte(destRegister, res);
		
		calcFlags(res, Size.BYTE.getMsb());
	}

	private void ANDSourceEAWord(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int destRegister = (opcode >> 9) & 0x7;
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long toAnd = cpu.getDWord(destRegister);
		long res = toAnd & data;
		cpu.setDWord(destRegister, res);
		
		calcFlags(res, Size.WORD.getMsb());
	}
	
	private void ANDSourceEALong(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int destRegister = (opcode >> 9) & 0x7;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long toAnd = cpu.getDLong(destRegister);
		long res = toAnd & data;
		cpu.setDLong(destRegister, res);
		
		calcFlags(res, Size.LONG.getMsb());
	}
	
	private void ANDDestEAByte(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int sourceRegister = (opcode >> 9) & 0x7;
		
		long toAnd = cpu.getDByte(sourceRegister);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long res = toAnd & data;
		
		cpu.writeKnownAddressingMode(o, res, Size.BYTE);
		
		calcFlags(res, Size.BYTE.getMsb());
	}
	
	private void ANDDestEAWord(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int sourceRegister = (opcode >> 9) & 0x7;
		
		long toAnd = cpu.getDWord(sourceRegister);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long res = toAnd & data;
		
		cpu.writeKnownAddressingMode(o, res, Size.WORD);
		
		calcFlags(res, Size.WORD.getMsb());
	}
	
	private void ANDDestEALong(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		int sourceRegister = (opcode >> 9) & 0x7;
		
		long toAnd = cpu.getDLong(sourceRegister);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long res = toAnd & data;
		
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

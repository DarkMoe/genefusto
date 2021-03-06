package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ANDI implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ANDI(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	ANDI -- Logical AND immediate
//
//SYNOPSIS
//	ANDI	#<data>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Performs a bit-wise AND operation with the immediate data and
//	the destination operand and stores the result in the destination. The
//	size of ther operation can be specified as byte, word, or long. The
//	size of the immediate data matches the operation size.
//
//FORMAT
//                                                          <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | SIZE  |   MODE    |  REGISTER |
//	|---------------------------------------------------------------|
//	| 16 BITS DATA (with last Byte) |          8 BITS DATA          |
//	|---------------------------------------------------------------|
//	|             32 BITS DATA (included last Word)                 |
//	-----------------------------------------------------------------
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//REGISTER
//	<ea> is always destination, addressing modes are the followings:
//
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N� reg. Dn| |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |     -    | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N� reg. An| |   (d16,PC)    | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     |011 |N� reg. An| |   (d8,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      |100 |N� reg. An| |   (bd,PC,Xi)  | -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N� reg. An| |([bd,PC,Xi],od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N� reg. An| |([bd,PC],Xi,od)| -  |   -    |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N� reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N� reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N� reg. An|
//	---------------------------------
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
		int base = 0x0200;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ANDIByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ANDIWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ANDILong(opcode);
					}
				};
			}
			for (int m = 0; m < 8; m++) {
				if (m == 1) {
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if (m == 0b111 & r > 0b001) {
						continue;
					}
					int opcode = base + ((s << 6) | (m << 3) | r);
					cpu.addInstruction(opcode, ins);
				}
			}
		}
		
	}
	
	private void ANDIByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long toAnd = cpu.bus.read(cpu.PC + 2, Size.WORD);
	 	toAnd = toAnd & 0xFF;	//	ocupa 2 bytes, pero solo se toma el ultimo
		
	 	cpu.PC += 2;
	 	 
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long res = data & toAnd;
		cpu.writeKnownAddressingMode(o, res, Size.BYTE);
		 	 
		calcFlags(res, Size.BYTE.getMsb());
	}
	
	private void ANDIWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long toAnd = cpu.bus.read(cpu.PC + 2, Size.WORD);
		
	 	cpu.PC += 2;
	 	 
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long res = data & toAnd;
		cpu.writeKnownAddressingMode(o, res, Size.WORD);
		 	 
		calcFlags(res, Size.WORD.getMsb());
	}
	
	private void ANDILong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long toAnd = cpu.bus.read(cpu.PC + 2, Size.LONG);
		
	 	cpu.PC += 4;
	 	 
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long res = data & toAnd;
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

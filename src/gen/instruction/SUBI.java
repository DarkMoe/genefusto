package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class SUBI implements GenInstructionHandler {

	final Gen68 cpu;
	
	public SUBI(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	SUBI -- Subtract immediate
//
//SYNOPSIS
//	SUBI	#<data>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Subtracts the immediate data to the destination operand, and
//	stores the result in the destination location. The size of the
//	operation may be specified as byte, word, or long.
//        The size of the	immediate data matches the operation size.
//
//FORMAT
//                                                          <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | SIZE  |   MODE    |  REGISTER |
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
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       |000 |N° reg. Dn| |    Abs.W      |111 |  000   |
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
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow is generated. Cleared otherwise.
//	C - Set if a carry is generated. Cleared otherwise.
	
	@Override
	public void generate() {
		int base = 0x400;
		GenInstruction ins;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						SUBIByte(opcode);
					}

				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						SUBIWord(opcode);
					}

				};
			} else {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						SUBILong(opcode);
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
					
					int opcode = base | (s << 6) | (m << 3) | r;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
		
	}
	
	private void SUBIByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
	
		long toSub = cpu.bus.read(cpu.PC + 2, Size.WORD);
 	 	toSub &= 0xFF;	//	last byte
 	 	cpu.PC += 2;
 	 	
 	 	if ((toSub & 0x80) == 0x80) {
 	 		toSub |= 0xFFFF_FF00;
 	 	}
 	 	
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		if ((data & 0x80) == 0x80) {
 	 		data |= 0xFFFF_FF00;
 	 	}
		
		long tot = data - toSub;
		cpu.writeKnownAddressingMode(o, tot, Size.BYTE);
		
		calcFlags(tot, data, toSub, Size.BYTE.getMsb(), Size.BYTE.getMax());
	}

	private void SUBIWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);

		long toSub = cpu.bus.read(cpu.PC + 2, Size.WORD);
	 	cpu.PC += 2;

		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);

		long tot = data - toSub;
		cpu.writeKnownAddressingMode(o, tot, Size.WORD);
		
		calcFlags(tot, data, toSub, Size.WORD.getMsb(), Size.WORD.getMax());
	}
	
	private void SUBILong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);

		long toSub = cpu.bus.read(cpu.PC + 2, Size.LONG);
	 	cpu.PC += 4;

		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);

		long tot = data - toSub;
		cpu.writeKnownAddressingMode(o, tot, Size.LONG);
		
		calcFlags(tot, data, toSub, Size.LONG.getMsb(), Size.LONG.getMax());
	}
	
	void calcFlags(long r, long d, long s, long msb, long maxSize) {
		if ((r & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((r & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		boolean Dm = (d & msb) > 0;
		boolean Sm = (s & msb) > 0;
		boolean Rm = (r & msb) > 0;
		if ((!Sm && Dm && !Rm) || (Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		if ((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm)) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}

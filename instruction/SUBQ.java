package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class SUBQ implements GenInstructionHandler {

	final Gen68 cpu;
	
	public SUBQ(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	SUBQ -- Subtract 3-bit immediate quick
//
//SYNOPSIS
//	SUBQ	#<data>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Subtracts the immediate value of 1 to 8 to the operand at the
//	destination location. The size of the operation may be specified as
//	byte, word, or long. When subtracting to address registers,
//        the condition codes are not altered, and the entire destination
//        address register is used regardless of the operation size.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|-----------|-----------|
//	| 0 | 1 | 0 | 1 |    DATA   | 1 | SIZE  |    MODE   |  REGISTER |
//	----------------------------------------=========================
//                                                          <ea>
//
//DATA
//	000        ->represent value 8
//	001 to 111 ->immediate data from 1 to 7
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
//	|      An *     |001 |N� reg. An| |    Abs.L      |111 |  001   |
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
//	 * Word or Long only.
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow is generated. Cleared otherwise.
//	C - Set if a carry is generated. Cleared otherwise.
	
	@Override
	public void generate() {
		int base = 0x5100;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUBQByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUBQWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						SUBQLong(opcode);
					}
				};
			}
			for (int m = 0; m < 8; m++) {
				if (m == 1 && s == 0b00) {	//	size byte no tiene address direct
					continue;
				}
				for (int r = 0; r < 8; r++) {
					if (m == 0b111 & r > 0b001) {
						continue;
					}
					for (int d = 0; d < 8; d++) {
						int opcode = base | (d << 9) | (s << 6) | (m << 3) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
		
	}
	
	private void SUBQByte(int opcode) {
		int toSub = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		if (toSub == 0) {
			toSub = 8;
		}
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = o.getAddressingMode().getByte(o);
		
		long tot = (data - toSub);
		
		cpu.writeKnownAddressingMode(o, tot, Size.BYTE);
		
		calcFlags(tot, data, toSub, Size.BYTE.getMsb(), Size.BYTE.getMax());
	}
	
	private void SUBQWord(int opcode) {
		int toSub = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		if (toSub == 0) {
			toSub = 8;
		}

		if (mode != 1) {
			Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
			long data = o.getAddressingMode().getWord(o);
			if ((data & 0x8000) == 0x8000) {
				data |= 0xFFFF_0000;
			}
			
			long tot = (data - toSub);
			
			cpu.writeKnownAddressingMode(o, tot, Size.WORD);
			calcFlags(tot, data, toSub, Size.WORD.getMsb(), Size.WORD.getMax());
			
		} else {		//	address register, siempre guarda longword y no calcula flags
			long data = cpu.getA(register);
			
			long tot = (data - toSub);
			cpu.setALong(register, tot);
		}
	}
	
	private void SUBQLong(int opcode) {
		int toSub = (opcode >> 9) & 0x7;
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		if (toSub == 0) {
			toSub = 8;
		}
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long data = o.getAddressingMode().getLong(o);
		
		long tot = (data - toSub);

		cpu.writeKnownAddressingMode(o, tot, Size.LONG);
		
		// if destination is An no cambian los flags
		if (mode != 1) {
			calcFlags(tot, data, toSub, Size.LONG.getMsb(), Size.LONG.getMax());
		}
	}
	
	void calcFlags(long r, long d, long s, long msb, long maxSize) {
		if ((r & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if (((r & maxSize) & msb) > 0) {
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

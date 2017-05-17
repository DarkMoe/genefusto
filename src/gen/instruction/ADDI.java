package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ADDI implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ADDI(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ADDI -- Add immediate
//
//SYNOPSIS
//	ADDI	#<data>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Adds the immediate data to the destination operand, and
//	stores the result in the destination location. The size of the
//	operation may be specified as byte, word, or long. The size of the
//	immediate data matches the operation size.
//
//FORMAT
//                                                          <ea>
//	----------------------------------------=========================
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//	| 0 | 0 | 0 | 0 | 0 | 1 | 1 | 0 | SIZE  |   MODE    |  REGISTER |
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
		int base = 0x0600;
		GenInstruction ins;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ADDIByte(opcode);
					}

				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ADDIWord(opcode);
					}

				};
			} else {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						ADDILong(opcode);
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
	
	private void ADDIByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
	
		long data = cpu.bus.read(cpu.PC + 2, Size.WORD);
		data = data & 0xFF;
			 
		cpu.PC += 2;
			 
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long toAdd = o.getAddressingMode().getByte(o);
		
		long tot = toAdd + data;
		cpu.writeKnownAddressingMode(o, tot, Size.BYTE);
		
		calcFlags(tot, data, toAdd, Size.BYTE.getMsb(), Size.BYTE.getMax());
	}

	private void ADDIWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		long data = cpu.bus.read(cpu.PC + 2, Size.WORD);
		
	 	cpu.PC += 2;
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long toAdd = o.getAddressingMode().getWord(o);
		
		long tot = toAdd + data;
		cpu.writeKnownAddressingMode(o, tot, Size.WORD);
		
		calcFlags(tot, data, toAdd, Size.WORD.getMsb(), Size.WORD.getMax());
	}
	
	private void ADDILong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);

		long data = cpu.bus.read(cpu.PC + 2, Size.LONG);
		
	 	cpu.PC += 4;
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long toAdd = o.getAddressingMode().getLong(o);
		
		long tot = toAdd + data;
		cpu.writeKnownAddressingMode(o, tot, Size.LONG);
		
		calcFlags(tot, data, toAdd, Size.LONG.getMsb(), Size.LONG.getMax());
	}
	
	void calcFlags(long tot, long data, long toAdd, long msb, long maxSize) {
		if ((tot & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((tot & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		boolean Dm = (data & msb) > 0;
		boolean Sm = (toAdd & msb) > 0;
		boolean Rm = (tot & msb) > 0;
		if((Sm && Dm && !Rm) || (!Sm && !Dm && Rm)) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		if (tot > maxSize) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}

package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ASR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ASR(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	ASL, ASR -- Arithmetic shift left and arithmetic shift right
//
//SYNOPSIS
//	ASd	Dx,Dy
//	ASd	#<data>,Dy
//	ASd	<ea>
//	where d is direction, L or R
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Performs an arithmetic shifting bit operation in the indicated
//        direction, with an immediate data, or with a data register.
//        If you shift address contents, you only can do ONE shift, and
//        your operand is ONE word exclusively.
//
//	ASL:              <--  
//	      C <------ OPERAND <--- 0
//	            |
//	            |
//	      X <---'
//	       
//
//	ASR:      -->
//	  .---> OPERAND ------> C
//          |    T          |
//	  |    |          |
//	  `----'          `---> X
//
//FORMAT
//	In the case of the shifting of a register:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//	| 1 | 1 | 1 | 0 |  NUMBER/  |dr |  SIZE |i/r| 0 | 0 | REGISTER  |
//	|   |   |   |   |  REGISTER |   |       |   |   |   |           |
//	-----------------------------------------------------------------
//
//	In the case of the shifting of a memory area:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 1 | 1 | 1 | 0 | 0 | 0 | 0 |dr | 1 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
//
//
//NUMBER/REGISTER
//	Specifies number of shifting or number of register which contents
//	the number of shifting.
//	If i/r = 0, number of shifting is specified in the instruction as
//	immediate data
//	If i/r = 1, it's specified in the data register.
//	If dr = 0, right shifting
//	If dr = 1, left shifting
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//REGISTER
//	For a register shifting:
//	Indicates the number of data register on which shifting is applied.
//
//	For a memory shifting:
//	<ea> indicates operand which should be shifted.
//	Only addressing modes relatives to memory are allowed:
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
//
//RESULT
//	X - Set according to the last bit shifted out of the operand.
//	    Unaffected for a shift count of zero.
//	N - Set if the most-significant bit of the result is set. Cleared
//	    otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if the most significant bit is changed at any time during
//	    the shift operation. Cleared otherwise.
//	C - Set according to the last bit shifted out of the operand.
//	    Cleared for a shift count of zero.
	
	@Override
	public void generate() {
		generateImmediateRegister();
		generateMemory();
	}

	private void generateImmediateRegister() {
		int base = 0xE000;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ASRByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ASRWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ASRLong(opcode);
					}
				};
			}
				
			for (int ir = 0; ir < 2; ir++) {
				for (int r = 0; r < 8; r++) {
					for (int nReg = 0; nReg < 8; nReg++) {
						int opcode = base | (nReg << 9) |(s << 6) | (ir << 5) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}
	
	private void generateMemory() {
		int base = 0xE0C0;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ASRMemoryWord(opcode);
			}
			
		};
				
		for (int m = 0; m < 8; m++) {
			if (m == 0 || m == 1) {
				continue;
			}
			for (int r = 0; r < 8; r++) {
				if (m == 0b111 && r > 0b001) {
					continue;
				}
				int opcode = base | (m << 3) | r;
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void ASRByte(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				toShift = 8;
			} else {
				toShift = numRegister;
			}
		} else {
			toShift = cpu.getD(numRegister);
		}
		
		long data = cpu.getD(register) & 0xFF;
		long res = data >> toShift;
								
		boolean carry = false;
		if (toShift != 0) {
			if (((data >> toShift - 1) & 1) > 0) {
				carry = true;
			}
		}
		
		cpu.setDByte(register, res);
					
		calcFlags(res, data, Size.BYTE.getMsb(), carry, 0xFF);
	}
	
	private void ASRWord(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				toShift = 8;
			} else {
				toShift = numRegister;
			}
		} else {
			toShift = cpu.getD(numRegister);
		}
		
		long data = cpu.getD(register) & 0xFFFF;
		long res = data >> toShift;
								
		boolean carry = false;
		if (toShift != 0) {
			if (((data >> toShift - 1) & 1) > 0) {
				carry = true;
			}
		}
								
		cpu.setDWord(register, res);
					
		calcFlags(res, data, Size.WORD.getMsb(), carry, 0xFFFF);
	}
	
	private void ASRLong(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				toShift = 8;
			} else {
				toShift = numRegister;
			}
		} else {
			toShift = cpu.getD(numRegister);
		}
		
		long data = cpu.getD(register);
		long res = data >> toShift;
		
		boolean carry = false;
		if (toShift != 0) {
			if (((data >> toShift - 1) & 1) > 0) {
				carry = true;
			}
		}
		
		cpu.setDLong(register, res);
					
		calcFlags(res, data, Size.LONG.getMsb(), carry,  0xFFFF_FFFFL);
	}

	private void ASRMemoryWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		long toShift = 1;
		
		long res = data >> toShift;
								
		boolean carry = false;
		if ((data & 1) > 0) {
			carry = true;
		}
								
		cpu.writeKnownAddressingMode(o, res, Size.WORD);
					
		calcFlags(res, data, Size.WORD.getMsb(), carry, 0xFFFF);		
	}
	
	private void calcFlags(long data, long old, long msb, boolean carry, long maxSize) {
		if ((data & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((data & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		
		if (((data & msb) ^ (old & msb)) == msb) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		if (carry) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}

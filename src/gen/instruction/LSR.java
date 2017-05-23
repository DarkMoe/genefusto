package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class LSR implements GenInstructionHandler {

	final Gen68 cpu;
	
	public LSR(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	LSL, LSR -- Logical shift left and logical shift right
//
//SYNOPSIS
//	LSd	Dx,Dy
//	LSd	#<data>,Dy
//	LSd	<ea>
//	where d is direction, L or R
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Shift the bits of the operand in the specified direction.
//	The carry bit set set to the last bit shifted out of the operand.
//	The shift count for the shifting of a register may be specified in
//	two different ways:
//
//	1. Immediate - the shift count is specified in the instruction (shift
//	               range 1-8).
//	2. Register  - the shift count is contained in a data register
//	               specified in the instruction (shift count mod 64)
//
//	For a register, the size may be byte, word, or long, but for a memory
//	location, the size must be a word. The shift count is also restricted
//	to one for a memory location.
//
//	LSL:              <--  
//	      C <------ OPERAND <--- 0
//	            |
//	            |    (V = 0)
//	      X <---'
//	       
//
//	LSR:
//                 -->
//	0 ---> OPERAND -------> C
//                          |
//	                  |
//	                  `---> X
//
//FORMAT
//	In the case of the shifting of a register:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//	| 1 | 1 | 1 | 0 |  NUMBER/  |dr |  SIZE |i/r| 0 | 1 | REGISTER  |
//	|   |   |   |   |  REGISTER |   |       |   |   |   |           |
//	-----------------------------------------------------------------
//
//	In the case of the shifting of a memory area:
//	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 1 | 1 | 1 | 0 | 0 | 0 | 1 |dr | 1 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                  <ea>
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
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Always cleared
//	C - Set according to the last bit shifted out of the operand.
	
	@Override
	public void generate() {
		generateRegisterShift();
		generateMemoryShift();
	}
	
	private void generateRegisterShift() {
		int base = 0xE008;
		GenInstruction ins = null;
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						LSRRegisterByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						LSRRegisterWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						LSRRegisterLong(opcode);
					}
				};
			}
			
			for (int ir = 0; ir < 2; ir++) {
				for (int r = 0; r < 8; r++) {
					for (int nReg = 0; nReg < 8; nReg++) {
						int opcode = base | (nReg << 9) | (s << 6) | (ir << 5) | r;
						cpu.addInstruction(opcode, ins);
					}
				}
			}
		}
	}
	
	private void generateMemoryShift() {
		int base = 0xE2C0;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				LSRMemoryWord(opcode);
			}
		};
			
		for (int m = 0; m < 8; m++) {
			for (int r = 0; r < 8; r++) {
				if (m == 0 || m == 1) {
					continue;
				}
				if (m == 0b111 && r > 0b001) {
					continue;
				}
				int opcode = base | (m << 3) | r;
				cpu.addInstruction(opcode, ins);
			}
		}
	}

	private void LSRRegisterByte(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			toShift = numRegister;
		} else {
			toShift = cpu.getD(numRegister);
			toShift = toShift & 63;
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
		
		calcFlags(res, Size.BYTE.getMsb(), 0xFF, carry);
	}
	
	private void LSRRegisterWord(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			toShift = numRegister;
		} else {
			toShift = cpu.getD(numRegister);
			toShift = toShift & 63;
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
		
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF, carry);
	}

	private void LSRRegisterLong(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long toShift;
		if (!ir) {
			if (numRegister == 0) {
				numRegister = 8;
			}
			toShift = numRegister;
		} else {
			toShift = cpu.getD(numRegister);
			toShift = toShift & 63;
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
		
		calcFlags(res, Size.LONG.getMsb(), 0xFFFF_FFFFL, carry);
	}
	
	private void LSRMemoryWord(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = opcode & 0x7;
		
		long toShift = 1;

		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getAddressingMode().getWord(o);
		
		long res = data >> toShift;
						
		boolean carry = false;
		if (toShift != 0) {
			if (((data >> toShift - 1) & 1) > 0) {
				carry = true;
			}
		}
			
		cpu.writeKnownAddressingMode(o, res, Size.WORD);
		
		calcFlags(res, Size.WORD.getMsb(), 0xFFFF, carry);
	}

	void calcFlags(long data, long msb, long maxSize, boolean carry) {
		long wrapped = data & maxSize;
		if (wrapped == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((wrapped & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}

		cpu.clearV();
		
		if (carry) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}

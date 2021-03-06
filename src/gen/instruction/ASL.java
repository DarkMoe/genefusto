package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ASL implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ASL(Gen68 cpu) {
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
		int base = 0xE100;
		GenInstruction ins = null;
		
		for (int s = 0; s < 3; s++) {
			if (s == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ASLByte(opcode);
					}
				};
			} else if (s == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ASLWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ASLLong(opcode);
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
	
	private void generateMemory() {
		int base = 0xE1C0;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				ASLMemoryWord(opcode);
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
	
	private void ASLByte(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long shift;
		if (!ir) {
			if (numRegister == 0) {
				shift = 8;
			} else {
				shift = numRegister;
			}
		} else {
			shift = cpu.getDLong(numRegister);
			shift = shift & 63;
		}
		
		int data = (int) (cpu.getDByte(register));
		
		int msb;
		int last_out = 0;
		int msb_changed = 0;
		for(int s= 0; s < shift; s++) {
			last_out = data & 0x80;
			data <<= 1;
			msb = data & 0x80;
			if (msb != last_out) {
				msb_changed = 1;
			}
		}
		data &= 0xFF;
		
		cpu.setDByte(register, data);
		
		calcFlags(data, shift, msb_changed, last_out, Size.BYTE.getMsb());
	}
	
	private void ASLWord(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long shift;
		if (!ir) {
			if (numRegister == 0) {
				shift = 8;
			} else {
				shift = numRegister;
			}
		} else {
			shift = cpu.getDLong(numRegister);
			shift = shift & 63;
		}
		
		int data = (int) (cpu.getDWord(register));
		
		int msb;
		int last_out = 0;
		int msb_changed = 0;
		for(int s= 0; s < shift; s++) {
			last_out = data & 0x8000;
			data <<= 1;
			msb = data & 0x8000;
			if (msb != last_out) {
				msb_changed = 1;
			}
		}
		data &= 0xFFFF;
		
		cpu.setDWord(register, data);
		
		calcFlags(data, shift, msb_changed, last_out, Size.WORD.getMsb());
	}
	
	private void ASLLong(int opcode) {
		int register = (opcode & 0x7);
		boolean ir = cpu.bitTest(opcode, 5);
		int numRegister = (opcode >> 9) & 0x7;
		
		long shift;
		if (!ir) {
			if (numRegister == 0) {
				shift = 8;
			} else {
				shift = numRegister;
			}
		} else {
			shift = cpu.getDLong(numRegister);
			shift = shift & 63;
		}
		
		long data = cpu.getDLong(register);
		
		long msb;
		long last_out = 0;
		long msb_changed = 0;
		for(int s= 0; s < shift; s++) {
			last_out = data & 0x8000_0000;
			data <<= 1;
			msb = data & 0x8000_0000;
			if (msb != last_out) {
				msb_changed = 1;
			}
		}
		
		cpu.setDLong(register, data);
					
		calcFlags(data, shift, msb_changed, last_out, Size.LONG.getMsb());
	}
	
	private void ASLMemoryWord(int opcode) {
		int register = (opcode & 0x7);
		int mode = (opcode >> 3) & 0x7;
		
		Operation o = cpu.resolveAddressingMode(Size.WORD, mode, register);
		long data = o.getData() & 0xFFFF;
		int last_out = (int) (data & 0x8000);
		int msb_changed = 0;

		data <<= 1;
		if ((data & 0x8000) != last_out) {
			msb_changed = 1;
		}
		
		cpu.setDWord(register, data);
					
		calcFlags(data, 1, msb_changed, last_out, Size.WORD.getMsb());
	}
	
	private void calcFlags(long data, long shift, long msb_changed, long last_out, long msb) {
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
		
		if (msb_changed != 0) {
			cpu.setV();
		} else {
			cpu.clearV();
		}
		
		if (shift != 0) {
			if (last_out != 0) {
				cpu.setC();
				cpu.setX();
			} else {
				cpu.clearC();
				cpu.clearX();
			}
		}
	}
	
}

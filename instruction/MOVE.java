package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class MOVE implements GenInstructionHandler {

	final Gen68 cpu;
	
	public MOVE(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	MOVE -- Source -> Destination
//
//SYNOPSIS
//	MOVE	<ea>,<ea>
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Move the content of the source to the destination location.
//	The data is examined as it is moved, and the condition codes
//	set accordingly.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|-------|-----------|-----------|-----------|-----------|
//	| 0 | 0 |  SIZE |  REGISTER |    MODE   |    MODE   | REGISTER  |
//	----------------************************=========================
//	                    destination <ea>           source <ea>
//
//REGISTER
//	Destination <ea> specifies destination operand, addressing modes
//	allowed are:
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
//	Source <ea> specifies source operand, addressing modes allowed are:
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
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      |111 |  100   |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//	 * Word or Long only.
//
//SIZE
//	01->Byte
//	11->Word
//	10->Long
//
//RESULT
//	X - Not affected.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Always cleared.
//	C - Always cleared.
	
	@Override
	public void generate() {
		int base = 0x0000;
		GenInstruction ins = null;
		
		for (int s = 1; s < 4; s++) {
			if (s == 0b01) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						MOVEByte(opcode);
					}
				};
			} else if (s == 0b11) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						MOVEWord(opcode);
					}
				};
			} else if (s == 0b10) {
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						MOVELong(opcode);
					}
				};
			}
			
			for (int mSource = 0; mSource < 8; mSource++) {
				for (int rSource = 0; rSource < 8; rSource++) {
					if (mSource == 0b111 && rSource > 0b100) {
						continue;
					}
					for (int m = 0; m < 8; m++) {
						if (m == 1) {
							continue;
						}
						for (int r = 0; r < 8; r++) {
							if (m == 0b111 && r > 0b001) {
								continue;
							}
							
							int opcode = base + (s << 12) | ((r << 9) | (m << 6)) | ((mSource << 3) | rSource);
							cpu.addInstruction(opcode, ins);
						}
					}
				}
			}
		}
	}
	
	private void MOVEByte(int opcode) {
		int register = (opcode >> 9) & 0x7;
		int mode = (opcode >> 6) & 0x7;
		int sourceMode = (opcode >> 3) & 0x7;
		int sourceReg = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(cpu.PC + 2, Size.BYTE, sourceMode, sourceReg);
		long data = o.getAddressingMode().getByte(o);
		
		Operation oDest = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		oDest.setData(data);
		
		oDest.getAddressingMode().setByte(oDest);
		
		calcFlags(data, Size.BYTE.getMsb());
	}
	
	private void MOVEWord(int opcode) {
		int register = (opcode >> 9) & 0x7;
		int mode = (opcode >> 6) & 0x7;
		int sourceMode = (opcode >> 3) & 0x7;
		int sourceReg = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(cpu.PC + 2, Size.WORD, sourceMode, sourceReg);
		long data = o.getAddressingMode().getWord(o);
		
		Operation oDest = cpu.resolveAddressingMode(Size.WORD, mode, register);
		oDest.setData(data);
		
		oDest.getAddressingMode().setWord(oDest);
		
		calcFlags(data, Size.WORD.getMsb());
	}
	
	private void MOVELong(int opcode) {
		int register = (opcode >> 9) & 0x7;
		int mode = (opcode >> 6) & 0x7;
		int sourceMode = (opcode >> 3) & 0x7;
		int sourceReg = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(cpu.PC + 2, Size.LONG, sourceMode, sourceReg);
		long data = o.getAddressingMode().getLong(o);
		
		Operation oDest = cpu.resolveAddressingMode(Size.LONG, mode, register);
		oDest.setData(data);
		
		oDest.getAddressingMode().setLong(oDest);
		
		calcFlags(data, Size.LONG.getMsb());
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

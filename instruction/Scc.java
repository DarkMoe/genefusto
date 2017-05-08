package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class Scc implements GenInstructionHandler {

	final Gen68 cpu;
	
	public Scc(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	Scc -- Conditional set
//
//SYNOPSIS
//	Scc	<ea>
//
//        Size = (Byte)
//
//FUNCTION
//        If condition is true then byte addressed by <ea> is set to $FF,
//        else byte addressed by <ea> is set to $00.
//
//	Condition code 'cc' specifies one of the following:
//0000 F  False            Z = 1      1000 VC oVerflow Clear   V = 0
//0001 T  True             Z = 0      1001 VS oVerflow Set     V = 1
//0010 HI HIgh             C + Z = 0  1010 PL PLus             N = 0
//0011 LS Low or Same      C + Z = 1  1011 MI MInus            N = 1
//0100 CC Carry Clear      C = 0      1100 GE Greater or Equal N (+) V = 0
//0101 CS Carry Set        C = 1      1101 LT Less Than        N (+) V = 1
//0110 NE Not Equal        Z = 0      1110 GT Greater Than     Z + (N (+) V) = 0
//0111 EQ EQual            Z = 1      1111 LE Less or Equal    Z + (N (+) V) = 1
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---------------|---|---|-----------|-----------|
//	| 0 | 1 | 0 | 1 | cc CONDITION  | 1 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                   <ea>
//
//REGISTER
//	<ea> specifies operand to set, addressing modes allowed are:
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
//	None.
	
	@Override
	public void generate() {
		int base = 0x50C0;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				SccByte(opcode);
			}

		};
		
		for (int m = 0; m < 8; m++) {
			if (m == 1) {
				continue;
			}
			
			for (int r = 0; r < 8; r++) {
				if ((m == 7) && r > 0b001) {
					continue;
				}
				
				for (int cc = 0; cc < 16; cc++) {
					int opcode = base | (cc << 8) | (m << 3) | r;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}
	
	private void SccByte(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		int cc = (opcode >> 8) & 0xF;

		boolean taken;
		if (cc == 1) {
			taken = false;	// override del caso 1
		} else {
			taken = cpu.evaluateBranchCondition(cc, Size.BYTE);
		}
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, mode, register);
		long data = 0;
		if (taken) {
			data = 0xFF;
		}
		cpu.writeKnownAddressingMode(o, data, Size.BYTE);
	}

}
